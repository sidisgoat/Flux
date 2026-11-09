package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityEntity
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.FluxDatabase
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.FluxPreferencesRepository
import com.example.data.local.FluxSeedData
import com.example.data.local.FluxSettingsState
import com.example.data.local.GestureTargetAction
import com.example.data.local.HomeLayoutStyle
import com.example.data.local.HostedWidgetEntity
import com.example.data.local.IconShapeOption
import com.example.data.local.LayoutDensityOption
import com.example.data.local.ObjectCardSize
import com.example.data.local.PinnedContactEntity
import com.example.data.local.RecentEventEntity
import com.example.data.local.SpaceEntity
import com.example.data.local.SpaceRole
import com.example.data.local.TaskItemEntity
import com.example.data.local.fluxDataStore
import com.example.platform.AssistantResponse
import com.example.platform.AvailableWidgetProvider
import com.example.platform.DeviceContactItem
import com.example.platform.InstalledAppInfo
import com.example.platform.LauncherPlatformManager
import com.example.platform.LocalAssistantEngine
import com.example.platform.SystemSettingShortcut
import com.example.platform.WeatherPresetCity
import com.example.platform.WeatherService
import com.example.platform.WidgetHostHelper
import com.example.ui.theme.FluxAccentPalette
import com.example.ui.theme.FluxThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class FluxDestination(val routeId: String, val label: String) {
    HOME_LIVING_CANVAS("home_living_canvas", "Home"),
    SPACES_DECK("spaces_deck", "Spaces"),
    ACTION_FIELD("action_field", "Action Field"),
    CONTENT_SURFACE("content_surface", "Activity Workspace"),
    APP_INDEX("app_index", "Apps"),
    WIDGETS_STUDIO("widgets_studio", "Widgets"),
    CUSTOMIZATION_STUDIO("customization_studio", "Customize"),
    LAYOUT_EDITOR("layout_editor", "Layout Editor"),
    GESTURES_CONFIG("gestures_config", "Gestures"),
    SETTINGS_PRIVACY("settings_privacy", "Settings"),
    LOCAL_ASSISTANT("local_assistant", "Assistant")
}

enum class SearchResultFilter(val label: String) {
    ALL("All"),
    APPS("Apps"),
    ACTIONS("Actions"),
    FILES("Files"),
    SETTINGS("Settings"),
    WEB("Web")
}

enum class AppIndexSortMode(val label: String) {
    SPATIAL_CLUSTERS("Clusters"),
    BY_USE("By Use"),
    ALPHABETICAL("A–Z"),
    HIDDEN("Hidden")
}

data class UnifiedSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val categoryBadge: String,
    val filterCategory: SearchResultFilter,
    val score: Int,
    val packageName: String? = null,
    val activityId: String? = null,
    val spaceId: String? = null,
    val fluxObject: FluxObjectEntity? = null,
    val systemSettingAction: String? = null,
    val webUrlOrQuery: String? = null,
    val intentActionKey: String? = null
)

data class IntentOrientedAction(
    val key: String,
    val title: String,
    val subtitle: String,
    val badgeColorHex: String
)

object FluxSearchEngine {

    val defaultIntentActions = listOf(
        IntentOrientedAction("CREATE_NOTE", "Create Note", "Capture thought or formula into active Space", "#A855F7"),
        IntentOrientedAction("CREATE_ACTIVITY", "New Activity", "Start a continuity workspace with linked items", "#38BDF8"),
        IntentOrientedAction("OPEN_DOCUMENT", "Import File / PDF", "Link a document URI without copying storage", "#F59E0B"),
        IntentOrientedAction("PLAN_TASK", "Plan Task", "Add deadline or checklist item to Study/Work", "#10B981"),
        IntentOrientedAction("SHARE_CLIPBOARD", "Send / Share", "Invoke Android Sharesheet with text or link", "#F43F5E"),
        IntentOrientedAction("LAUNCH_CAMERA", "Capture Image", "Open system camera for visual reference", "#38BDF8"),
        IntentOrientedAction("SUMMARIZE_AI", "Assistant Organize", "Summarize notes or generate checklist locally", "#818CF8")
    )

    fun rankResults(
        rawQuery: String,
        filter: SearchResultFilter,
        installedApps: List<InstalledAppInfo>,
        appPrefs: Map<String, AppPreferenceEntity>,
        activities: List<ActivityEntity>,
        spaces: List<SpaceEntity>,
        objects: List<FluxObjectEntity>,
        tasks: List<TaskItemEntity>,
        settingsShortcuts: List<SystemSettingShortcut>
    ): List<UnifiedSearchResult> {
        val q = rawQuery.trim().lowercase()
        val results = mutableListOf<UnifiedSearchResult>()

        // 1. Installed Applications
        if (filter == SearchResultFilter.ALL || filter == SearchResultFilter.APPS) {
            for (app in installedApps) {
                val pref = appPrefs[app.packageName]
                if (pref?.isHidden == true && q.length < 2) continue
                val labelLower = app.label.lowercase()
                val catLower = (pref?.customCategory?.ifBlank { app.autoCategory } ?: app.autoCategory).lowercase()
                val matches = q.isEmpty() || labelLower.contains(q) || app.packageName.lowercase().contains(q) || catLower.contains(q)
                if (matches) {
                    val exactBoost = if (labelLower == q) 100 else if (labelLower.startsWith(q)) 60 else 25
                    val usageBoost = ((pref?.launchCount ?: 0) * 4).coerceAtMost(40)
                    results.add(
                        UnifiedSearchResult(
                            id = "app_${app.packageName}",
                            title = app.label,
                            subtitle = "Open application · ${pref?.customCategory?.ifBlank { app.autoCategory } ?: app.autoCategory}",
                            categoryBadge = "App",
                            filterCategory = SearchResultFilter.APPS,
                            score = exactBoost + usageBoost,
                            packageName = app.packageName
                        )
                    )
                }
            }
        }

        // 2. FLUX Continuity Activities & Spaces
        if (filter == SearchResultFilter.ALL || filter == SearchResultFilter.ACTIONS) {
            for (act in activities) {
                val hay = "${act.title} ${act.summary}".lowercase()
                if (q.isEmpty() || hay.contains(q)) {
                    val score = if (act.title.lowercase().startsWith(q)) 95 else 55
                    results.add(
                        UnifiedSearchResult(
                            id = "act_${act.id}",
                            title = act.title,
                            subtitle = "Activity Workspace · ${act.progressPercent}% complete",
                            categoryBadge = "Activity",
                            filterCategory = SearchResultFilter.ACTIONS,
                            score = score,
                            activityId = act.id,
                            spaceId = act.spaceId
                        )
                    )
                }
            }
            for (sp in spaces) {
                val hay = "${sp.title} ${sp.subtitle}".lowercase()
                if (q.isNotEmpty() && hay.contains(q)) {
                    results.add(
                        UnifiedSearchResult(
                            id = "space_${sp.id}",
                            title = "${sp.title} (Space)",
                            subtitle = "Switch to ${sp.title} Space · ${sp.subtitle}",
                            categoryBadge = "Space",
                            filterCategory = SearchResultFilter.ACTIONS,
                            score = 72,
                            spaceId = sp.id
                        )
                    )
                }
            }
            for (action in defaultIntentActions) {
                val hay = "${action.title} ${action.subtitle}".lowercase()
                if (q.isEmpty() || hay.contains(q)) {
                    results.add(
                        UnifiedSearchResult(
                            id = "intent_${action.key}",
                            title = action.title,
                            subtitle = action.subtitle,
                            categoryBadge = "Action",
                            filterCategory = SearchResultFilter.ACTIONS,
                            score = if (q.isEmpty()) 48 else 68,
                            intentActionKey = action.key
                        )
                    )
                }
            }
        }

        // 3. Stored Objects (Files, Notes, Links, Screenshots) & Tasks
        if (filter == SearchResultFilter.ALL || filter == SearchResultFilter.FILES) {
            for (obj in objects) {
                val hay = "${obj.title} ${obj.subtitle} ${obj.uriOrTarget} ${obj.groupLabel}".lowercase()
                if (q.isEmpty() || hay.contains(q)) {
                    val typeLabel = runCatching { FluxObjectType.valueOf(obj.objectType).label }.getOrDefault("Object")
                    val sizeSuffix = if (obj.fileSizeLabel.isNotBlank()) " · ${obj.fileSizeLabel}" else ""
                    results.add(
                        UnifiedSearchResult(
                            id = "obj_${obj.id}",
                            title = obj.title,
                            subtitle = "$typeLabel$sizeSuffix · ${obj.subtitle.take(48)}",
                            categoryBadge = typeLabel,
                            filterCategory = SearchResultFilter.FILES,
                            score = if (obj.title.lowercase().contains(q)) 84 else 52,
                            fluxObject = obj,
                            activityId = obj.activityId
                        )
                    )
                }
            }
            for (task in tasks) {
                if (q.isNotEmpty() && task.title.lowercase().contains(q)) {
                    results.add(
                        UnifiedSearchResult(
                            id = "task_${task.id}",
                            title = task.title,
                            subtitle = "Task · ${task.dueLabel} (${if (task.isDone) "Completed" else "Open"})",
                            categoryBadge = "Task",
                            filterCategory = SearchResultFilter.FILES,
                            score = 62,
                            spaceId = task.spaceId,
                            activityId = task.activityId
                        )
                    )
                }
            }
        }

        // 4. System Settings Shortcuts
        if (filter == SearchResultFilter.ALL || filter == SearchResultFilter.SETTINGS) {
            for (setting in settingsShortcuts) {
                val hay = "${setting.title} ${setting.subtitle}".lowercase()
                if (q.isEmpty() || hay.contains(q)) {
                    results.add(
                        UnifiedSearchResult(
                            id = "sys_${setting.id}",
                            title = setting.title,
                            subtitle = "System Setting · ${setting.subtitle}",
                            categoryBadge = "Setting",
                            filterCategory = SearchResultFilter.SETTINGS,
                            score = if (q.isEmpty()) 30 else 64,
                            systemSettingAction = setting.settingsAction
                        )
                    )
                }
            }
        }

        // 5. Web / Direct URL entry (Clearly distinguished from local results)
        if ((filter == SearchResultFilter.ALL || filter == SearchResultFilter.WEB) && q.isNotBlank()) {
            val isDirectDomain = q.contains(".") && !q.contains(" ")
            val webTitle = if (isDirectDomain) "Open https://$q" else "Search web for \"$rawQuery\""
            results.add(
                UnifiedSearchResult(
                    id = "web_query_$q",
                    title = webTitle,
                    subtitle = "Network Web Action · Opens default browser externally",
                    categoryBadge = "Web",
                    filterCategory = SearchResultFilter.WEB,
                    score = if (filter == SearchResultFilter.WEB) 110 else 20,
                    webUrlOrQuery = rawQuery.trim()
                )
            )
        }

        return results.sortedByDescending { it.score }.take(40)
    }
}

@OptIn(FlowPreview::class)
class FluxViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FluxDatabase.getInstance(application)
    val dao = database.fluxDao()
    val preferencesRepo = FluxPreferencesRepository(application.fluxDataStore)
    val platformManager = LauncherPlatformManager(application)
    val widgetHostHelper = WidgetHostHelper(application)

    val settingsState: StateFlow<FluxSettingsState> = preferencesRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FluxSettingsState()
    )

    val spaces: StateFlow<List<SpaceEntity>> = dao.observeSpaces().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FluxSeedData.defaultSpaces()
    )

    val activities: StateFlow<List<ActivityEntity>> = dao.observeAllActivities().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allObjects: StateFlow<List<FluxObjectEntity>> = dao.observeAllObjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTasks: StateFlow<List<TaskItemEntity>> = dao.observeAllTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pinnedContacts: StateFlow<List<PinnedContactEntity>> = dao.observePinnedContacts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hostedWidgets: StateFlow<List<HostedWidgetEntity>> = dao.observeWidgets().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentEvents: StateFlow<List<RecentEventEntity>> = dao.observeRecentEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val appPreferencesList: StateFlow<List<AppPreferenceEntity>> = dao.observeAppPreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentDestination = MutableStateFlow(FluxDestination.HOME_LIVING_CANVAS)
    val currentDestination: StateFlow<FluxDestination> = _currentDestination.asStateFlow()

    private val _navigationBackStack = MutableStateFlow<List<FluxDestination>>(emptyList())

    private val _activeSpaceId = MutableStateFlow("space_personal")
    val activeSpaceId: StateFlow<String> = _activeSpaceId.asStateFlow()

    private val _selectedActivityId = MutableStateFlow<String?>(null)
    val selectedActivityId: StateFlow<String?> = _selectedActivityId.asStateFlow()

    private val _selectedContextObject = MutableStateFlow<FluxObjectEntity?>(null)
    val selectedContextObject: StateFlow<FluxObjectEntity?> = _selectedContextObject.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _deviceContacts = MutableStateFlow<List<DeviceContactItem>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContactItem>> = _deviceContacts.asStateFlow()

    private val _availableWidgetProviders = MutableStateFlow<List<AvailableWidgetProvider>>(emptyList())
    val availableWidgetProviders: StateFlow<List<AvailableWidgetProvider>> = _availableWidgetProviders.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchResultFilter.ALL)
    val searchFilter: StateFlow<SearchResultFilter> = _searchFilter.asStateFlow()

    private val _appIndexSortMode = MutableStateFlow(AppIndexSortMode.SPATIAL_CLUSTERS)
    val appIndexSortMode: StateFlow<AppIndexSortMode> = _appIndexSortMode.asStateFlow()

    private val _selectedAppForSheet = MutableStateFlow<InstalledAppInfo?>(null)
    val selectedAppForSheet: StateFlow<InstalledAppInfo?> = _selectedAppForSheet.asStateFlow()

    private val _assistantResponse = MutableStateFlow<AssistantResponse?>(null)
    val assistantResponse: StateFlow<AssistantResponse?> = _assistantResponse.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _isAmoledRestOverlayActive = MutableStateFlow(false)
    val isAmoledRestOverlayActive: StateFlow<Boolean> = _isAmoledRestOverlayActive.asStateFlow()

    // Debounced Universal Search Results
    val searchResults: StateFlow<List<UnifiedSearchResult>> = combine(
        _searchQuery.debounce(90L),
        _searchFilter,
        _installedApps,
        appPreferencesList,
        activities,
        spaces,
        allObjects,
        allTasks
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val query = values[0] as String
        val filter = values[1] as SearchResultFilter
        val apps = values[2] as List<InstalledAppInfo>
        val prefs = (values[3] as List<AppPreferenceEntity>).associateBy { it.packageName }
        val acts = values[4] as List<ActivityEntity>
        val sps = values[5] as List<SpaceEntity>
        val objs = values[6] as List<FluxObjectEntity>
        val tsks = values[7] as List<TaskItemEntity>

        FluxSearchEngine.rankResults(
            rawQuery = query,
            filter = filter,
            installedApps = apps,
            appPrefs = prefs,
            activities = acts,
            spaces = sps,
            objects = objs,
            tasks = tsks,
            settingsShortcuts = platformManager.systemSettingsShortcuts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        widgetHostHelper.startListening()
        initializeFirstRunDataIfNeeded()
        refreshPlatformState()
    }

    override fun onCleared() {
        super.onCleared()
        widgetHostHelper.stopListening()
    }

    private fun initializeFirstRunDataIfNeeded() {
        viewModelScope.launch {
            val currentSettings = preferencesRepo.settingsFlow.first()
            val existingSpaces = dao.getSpacesList()
            if (existingSpaces.isEmpty()) {
                dao.upsertSpaces(FluxSeedData.defaultSpaces())
            }
            if (!currentSettings.isFirstRunInitialized) {
                dao.upsertActivities(FluxSeedData.initialActivities())
                dao.upsertObjects(FluxSeedData.initialObjects())
                dao.upsertTasks(FluxSeedData.initialTasks())
                dao.upsertWidgets(FluxSeedData.initialWidgets())
                dao.upsertPinnedContacts(
                    listOf(
                        PinnedContactEntity(
                            id = "contact_sample_lab",
                            displayName = "Dr. Elena Vance (Physics Lab)",
                            handleOrNumber = "+14155550142",
                            groupName = "Study Group",
                            preferredAction = "SMS",
                            avatarInitials = "EV",
                            isPinnedHome = true,
                            isSampleContent = true
                        ),
                        PinnedContactEntity(
                            id = "contact_sample_design",
                            displayName = "Kenji Sato (Systems Lead)",
                            handleOrNumber = "+14155550198",
                            groupName = "Team",
                            preferredAction = "DIAL",
                            avatarInitials = "KS",
                            isPinnedHome = true,
                            isSampleContent = true
                        )
                    )
                )
                preferencesRepo.markFirstRunInitialized()
            }
        }
    }

    fun refreshPlatformState() {
        viewModelScope.launch {
            _isDefaultLauncher.value = platformManager.isFluxDefaultLauncher()
            _isLoadingApps.value = true
            val loadedApps = platformManager.loadInstalledApps()
            _installedApps.value = loadedApps
            _isLoadingApps.value = false

            // Ensure default dock has up to 4 useful installed apps if none are docked yet
            val currentPrefs = dao.observeAppPreferences().first()
            if (currentPrefs.none { it.isDocked } && loadedApps.isNotEmpty()) {
                val initialDock = loadedApps.take(4).mapIndexed { index, app ->
                    AppPreferenceEntity(
                        packageName = app.packageName,
                        autoCategoryOrCustom(app),
                        isDocked = true,
                        dockOrder = index
                    )
                }
                dao.upsertAppPreferences(initialDock)
            }

            _availableWidgetProviders.value = widgetHostHelper.discoverInstalledWidgetProviders()

            val currentSettings = preferencesRepo.settingsFlow.first()
            if (currentSettings.enableBackgroundWeatherRefresh) {
                refreshWeather(
                    city = currentSettings.weatherCity,
                    lat = currentSettings.weatherLatitude,
                    lon = currentSettings.weatherLongitude
                )
            }
        }
    }

    private fun autoCategoryOrCustom(app: InstalledAppInfo): String = app.autoCategory

    fun refreshWeather(city: String, lat: Float, lon: Float) {
        viewModelScope.launch {
            val s = settingsState.value
            val snap = WeatherService.fetchCurrentWeather(
                city = city,
                latitude = lat,
                longitude = lon,
                fallbackTempC = s.cachedTempC,
                fallbackSummary = s.cachedWeatherSummary
            )
            preferencesRepo.updateWeatherCache(
                city = snap.city,
                lat = lat,
                lon = lon,
                tempC = snap.temperatureC,
                summary = snap.conditionText
            )
        }
    }

    fun selectWeatherCityPreset(preset: WeatherPresetCity) {
        refreshWeather(preset.name, preset.latitude, preset.longitude)
        postBanner("Weather location updated to ${preset.name}")
    }

    // --- Navigation & Backstack ---
    fun navigateTo(destination: FluxDestination) {
        val current = _currentDestination.value
        if (current != destination) {
            _navigationBackStack.value = (_navigationBackStack.value + current).takeLast(8)
            _currentDestination.value = destination
        }
    }

    fun navigateHome() {
        _navigationBackStack.value = emptyList()
        _selectedContextObject.value = null
        _selectedAppForSheet.value = null
        _isAmoledRestOverlayActive.value = false
        _currentDestination.value = FluxDestination.HOME_LIVING_CANVAS
    }

    fun navigateBack(): Boolean {
        if (_isAmoledRestOverlayActive.value) {
            _isAmoledRestOverlayActive.value = false
            return true
        }
        if (_selectedContextObject.value != null) {
            _selectedContextObject.value = null
            return true
        }
        if (_selectedAppForSheet.value != null) {
            _selectedAppForSheet.value = null
            return true
        }
        val stack = _navigationBackStack.value
        if (stack.isNotEmpty()) {
            val prev = stack.last()
            _navigationBackStack.value = stack.dropLast(1)
            _currentDestination.value = prev
            return true
        }
        if (_currentDestination.value != FluxDestination.HOME_LIVING_CANVAS) {
            _currentDestination.value = FluxDestination.HOME_LIVING_CANVAS
            return true
        }
        return false
    }

    fun selectSpace(spaceId: String) {
        _activeSpaceId.value = spaceId
    }

    fun cycleSpace(delta: Int) {
        val list = spaces.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _activeSpaceId.value }.coerceAtLeast(0)
        val nextIdx = (currentIdx + delta).mod(list.size)
        _activeSpaceId.value = list[nextIdx].id
    }

    fun openActivityWorkspace(activityId: String) {
        _selectedActivityId.value = activityId
        navigateTo(FluxDestination.CONTENT_SURFACE)
        viewModelScope.launch {
            val act = dao.getActivityById(activityId)
            if (act != null) {
                recordRecentEvent(
                    title = act.title,
                    subtitle = "Resumed Activity Workspace",
                    eventKind = "RESUME_ACTIVITY",
                    target = act.id
                )
            }
        }
    }

    fun openContextActionsForObject(obj: FluxObjectEntity?) {
        _selectedContextObject.value = obj
    }

    fun openAppOptionsSheet(app: InstalledAppInfo?) {
        _selectedAppForSheet.value = app
    }

    // --- Search & Action Field ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSearchFilter(filter: SearchResultFilter) {
        _searchFilter.value = filter
    }

    fun setAppIndexSort(mode: AppIndexSortMode) {
        _appIndexSortMode.value = mode
    }

    fun executeSearchResult(result: UnifiedSearchResult) {
        when {
            result.packageName != null -> launchInstalledApp(result.packageName)
            result.fluxObject != null -> openContextActionsForObject(result.fluxObject)
            result.activityId != null -> openActivityWorkspace(result.activityId)
            result.spaceId != null -> {
                selectSpace(result.spaceId)
                navigateTo(FluxDestination.SPACES_DECK)
            }
            result.systemSettingAction != null -> {
                platformManager.openSystemSetting(result.systemSettingAction)
            }
            result.webUrlOrQuery != null -> {
                platformManager.openUriOrWebSearch(result.webUrlOrQuery)
            }
            result.intentActionKey != null -> {
                executeIntentOrientedAction(result.intentActionKey)
            }
        }
    }

    fun executeIntentOrientedAction(actionKey: String) {
        when (actionKey) {
            "CREATE_NOTE" -> {
                createQuickNoteObject(
                    title = "Quick Note · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                    body = "Captured from Action Field. Tap to edit or attach to an activity.",
                    spaceId = _activeSpaceId.value,
                    activityId = _selectedActivityId.value
                )
                postBanner("Created new note in current Space")
            }
            "CREATE_ACTIVITY" -> {
                createActivity(
                    title = "New Workspace Activity",
                    summary = "Continuity workspace for related documents, links, and notes.",
                    spaceId = _activeSpaceId.value,
                    accentHex = "#38BDF8"
                )
            }
            "PLAN_TASK" -> {
                createTask(
                    title = "New Action Item",
                    dueLabel = "Today",
                    spaceId = _activeSpaceId.value,
                    activityId = _selectedActivityId.value,
                    priority = 2
                )
                postBanner("Added new task to active Space")
            }
            "SHARE_CLIPBOARD" -> {
                val clip = platformManager.readClipboardText() ?: "Shared from FLUX Activity Environment"
                platformManager.shareTextOrUri("FLUX Share", clip)
            }
            "LAUNCH_CAMERA" -> {
                val ok = platformManager.launchCameraIntent()
                if (!ok) postBanner("No camera application available on this device")
            }
            "SUMMARIZE_AI" -> {
                navigateTo(FluxDestination.LOCAL_ASSISTANT)
            }
            else -> {
                navigateTo(FluxDestination.ACTION_FIELD)
            }
        }
    }

    // --- Gesture Execution ---
    fun triggerConfiguredGesture(gestureTarget: GestureTargetAction) {
        when (gestureTarget) {
            GestureTargetAction.OPEN_ACTION_FIELD -> navigateTo(FluxDestination.ACTION_FIELD)
            GestureTargetAction.EXPAND_NOTIFICATIONS -> {
                val expanded = platformManager.expandNotificationShade()
                if (!expanded) {
                    postBanner("Notification shade restricted by OEM; opening Quick Settings / Action Field")
                    navigateTo(FluxDestination.ACTION_FIELD)
                }
            }
            GestureTargetAction.SWITCH_SPACES -> cycleSpace(1)
            GestureTargetAction.OPEN_SPACES_OVERVIEW -> navigateTo(FluxDestination.SPACES_DECK)
            GestureTargetAction.OPEN_APP_INDEX -> navigateTo(FluxDestination.APP_INDEX)
            GestureTargetAction.ENTER_EDIT_MODE -> navigateTo(FluxDestination.LAYOUT_EDITOR)
            GestureTargetAction.OPEN_UNIVERSAL_SEARCH -> {
                _searchFilter.value = SearchResultFilter.ALL
                navigateTo(FluxDestination.ACTION_FIELD)
            }
            GestureTargetAction.OPEN_ASSISTANT -> navigateTo(FluxDestination.LOCAL_ASSISTANT)
            GestureTargetAction.LOCK_OR_DIM_OVERLAY -> {
                _isAmoledRestOverlayActive.value = true
            }
            GestureTargetAction.DISABLED -> Unit
        }
    }

    fun dismissAmoledRestOverlay() {
        _isAmoledRestOverlayActive.value = false
    }

    // --- App Launching & Preferences ---
    fun launchInstalledApp(packageName: String) {
        val app = _installedApps.value.firstOrNull { it.packageName == packageName }
        val ok = platformManager.launchApp(packageName)
        if (ok) {
            viewModelScope.launch {
                val existing = dao.getAppPreference(packageName)
                val updated = (existing ?: AppPreferenceEntity(
                    packageName = packageName,
                    customCategory = app?.autoCategory ?: "Tools"
                )).copy(
                    launchCount = (existing?.launchCount ?: 0) + 1,
                    lastLaunchedEpochMs = System.currentTimeMillis()
                )
                dao.upsertAppPreference(updated)
                if (app != null) {
                    recordRecentEvent(
                        title = app.label,
                        subtitle = "Launched application",
                        eventKind = "LAUNCH_APP",
                        target = packageName
                    )
                }
            }
        } else {
            postBanner("Unable to launch $packageName (App may have been uninstalled)")
            refreshPlatformState()
        }
    }

    fun toggleAppDocked(packageName: String) {
        viewModelScope.launch {
            val existing = dao.getAppPreference(packageName)
            val nowDocked = !(existing?.isDocked ?: false)
            val updated = (existing ?: AppPreferenceEntity(packageName = packageName)).copy(
                isDocked = nowDocked,
                dockOrder = System.currentTimeMillis().toInt()
            )
            dao.upsertAppPreference(updated)
            postBanner(if (nowDocked) "Pinned to Quick Dock" else "Removed from Quick Dock")
        }
    }

    fun toggleAppHidden(packageName: String) {
        viewModelScope.launch {
            val existing = dao.getAppPreference(packageName)
            val nowHidden = !(existing?.isHidden ?: false)
            val updated = (existing ?: AppPreferenceEntity(packageName = packageName)).copy(
                isHidden = nowHidden
            )
            dao.upsertAppPreference(updated)
            postBanner(if (nowHidden) "App hidden from primary index" else "App restored to primary index")
        }
    }

    fun setAppCustomCategory(packageName: String, category: String) {
        viewModelScope.launch {
            val existing = dao.getAppPreference(packageName)
            val updated = (existing ?: AppPreferenceEntity(packageName = packageName)).copy(
                customCategory = category
            )
            dao.upsertAppPreference(updated)
            postBanner("Assigned to $category cluster")
        }
    }

    fun pinAppShortcutToActivity(app: InstalledAppInfo, activityId: String?) {
        viewModelScope.launch {
            val targetActId = activityId ?: activities.value.firstOrNull()?.id
            val obj = FluxObjectEntity(
                id = "obj_app_${UUID.randomUUID().toString().take(8)}",
                activityId = targetActId,
                spaceId = _activeSpaceId.value,
                title = app.label,
                subtitle = "App Shortcut · ${app.packageName}",
                objectType = FluxObjectType.SHORTCUT.name,
                uriOrTarget = app.packageName,
                mimeType = "application/vnd.android.package-archive",
                groupLabel = "Shortcuts",
                fileSizeLabel = "App"
            )
            dao.upsertObject(obj)
            postBanner("Added ${app.label} shortcut to Activity")
        }
    }

    // --- Spaces & Activities Continuity CRUD ---
    fun createSpace(title: String, subtitle: String, accentHex: String, layoutMode: String = "CANVAS") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val count = spaces.value.size
            val newSpace = SpaceEntity(
                id = "space_${UUID.randomUUID().toString().take(8)}",
                title = title.trim(),
                subtitle = subtitle.trim().ifBlank { "Custom Context Space" },
                roleKey = SpaceRole.CUSTOM.key,
                iconName = "AutoAwesomeMosaic",
                accentHex = accentHex,
                layoutMode = layoutMode,
                sortOrder = count,
                isSystemDefault = false
            )
            dao.upsertSpace(newSpace)
            _activeSpaceId.value = newSpace.id
            postBanner("Created Space \"${newSpace.title}\"")
        }
    }

    fun renameSpace(space: SpaceEntity, newTitle: String, newSubtitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            dao.upsertSpace(space.copy(title = newTitle.trim(), subtitle = newSubtitle.trim()))
            postBanner("Updated Space \"${newTitle.trim()}\"")
        }
    }

    fun deleteCustomSpace(space: SpaceEntity) {
        if (space.isSystemDefault) {
            postBanner("Default spaces can be renamed or customized, not deleted")
            return
        }
        viewModelScope.launch {
            dao.deleteCustomSpace(space.id)
            _activeSpaceId.value = "space_personal"
            postBanner("Deleted Space \"${space.title}\"")
        }
    }

    fun createActivity(
        title: String,
        summary: String,
        spaceId: String = _activeSpaceId.value,
        accentHex: String = "#38BDF8",
        openImmediately: Boolean = true
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = "act_${UUID.randomUUID().toString().take(8)}"
            val entity = ActivityEntity(
                id = id,
                spaceId = spaceId,
                title = title.trim(),
                summary = summary.trim().ifBlank { "Connected notes, files, links, and tasks." },
                statusLabel = "Active",
                progressPercent = 15,
                isPinnedHome = true,
                canvasOrder = activities.value.size,
                accentHex = accentHex,
                isSampleContent = false
            )
            dao.upsertActivity(entity)
            if (openImmediately) {
                openActivityWorkspace(id)
            } else {
                postBanner("Created Activity \"${entity.title}\"")
            }
        }
    }

    fun updateActivityDetails(
        activity: ActivityEntity,
        newTitle: String,
        newSummary: String,
        newSpaceId: String,
        newProgress: Int,
        isPinnedHome: Boolean,
        cardSize: ObjectCardSize
    ) {
        viewModelScope.launch {
            dao.upsertActivity(
                activity.copy(
                    title = newTitle.trim().ifBlank { activity.title },
                    summary = newSummary.trim(),
                    spaceId = newSpaceId,
                    progressPercent = newProgress.coerceIn(0, 100),
                    statusLabel = if (newProgress >= 100) "Completed" else "Active",
                    isPinnedHome = isPinnedHome,
                    cardSize = cardSize.name,
                    isSampleContent = false,
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            )
            postBanner("Saved changes to \"${newTitle.trim()}\"")
        }
    }

    fun updateActivityCanvasTransform(
        activity: ActivityEntity,
        offsetXDp: Float,
        offsetYDp: Float,
        newSize: ObjectCardSize,
        newOrder: Int
    ) {
        viewModelScope.launch {
            dao.upsertActivity(
                activity.copy(
                    canvasOffsetXDp = offsetXDp.coerceIn(-120f, 120f),
                    canvasOffsetYDp = offsetYDp.coerceIn(-80f, 160f),
                    cardSize = newSize.name,
                    canvasOrder = newOrder,
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            dao.deleteObjectsForActivity(activityId)
            dao.deleteActivity(activityId)
            if (_selectedActivityId.value == activityId) {
                _selectedActivityId.value = null
                navigateHome()
            }
            postBanner("Deleted Activity and unlinked references")
        }
    }

    // --- Objects & Contextual Continuity ---
    fun createQuickNoteObject(
        title: String,
        body: String,
        spaceId: String = _activeSpaceId.value,
        activityId: String? = _selectedActivityId.value,
        objectType: FluxObjectType = FluxObjectType.NOTE
    ) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch {
            val obj = FluxObjectEntity(
                id = "obj_${UUID.randomUUID().toString().take(8)}",
                activityId = activityId,
                spaceId = spaceId,
                title = title.trim().ifBlank { "Untitled ${objectType.label}" },
                subtitle = body.trim().take(120),
                objectType = objectType.name,
                uriOrTarget = body.trim(),
                mimeType = if (objectType == FluxObjectType.LINK) "text/uri-list" else "text/plain",
                groupLabel = when (objectType) {
                    FluxObjectType.LINK -> "Web Sources"
                    FluxObjectType.CHECKLIST -> "Checklists"
                    else -> "Notes & Excerpts"
                },
                fileSizeLabel = if (objectType == FluxObjectType.LINK) "URL" else "${(body.length / 1024.0).coerceAtLeast(0.1).let { String.format("%.1f KB", it) }}"
            )
            dao.upsertObject(obj)
            recordRecentEvent(obj.title, "Added ${objectType.label}", "OPEN_OBJECT", obj.id)
        }
    }

    fun attachDocumentUri(
        uri: Uri,
        mimeType: String,
        spaceId: String = _activeSpaceId.value,
        activityId: String? = _selectedActivityId.value
    ) {
        platformManager.takePersistablePermissionIfPossible(uri)
        val (displayName, sizeLabel) = platformManager.resolveDisplayNameAndSize(uri)
        val inferredType = when {
            mimeType.startsWith("image/") -> FluxObjectType.SCREENSHOT
            mimeType.startsWith("audio/") || mimeType.startsWith("video/") -> FluxObjectType.MEDIA
            else -> FluxObjectType.DOCUMENT
        }
        viewModelScope.launch {
            val obj = FluxObjectEntity(
                id = "obj_uri_${UUID.randomUUID().toString().take(8)}",
                activityId = activityId,
                spaceId = spaceId,
                title = displayName,
                subtitle = "Document URI Reference · $mimeType",
                objectType = inferredType.name,
                uriOrTarget = uri.toString(),
                mimeType = mimeType,
                groupLabel = "Linked Files",
                fileSizeLabel = sizeLabel.ifBlank { "URI Ref" }
            )
            dao.upsertObject(obj)
            recordRecentEvent(displayName, "Linked file reference", "OPEN_OBJECT", obj.id)
            postBanner("Linked \"$displayName\" without duplicating file")
        }
    }

    fun handleIncomingShareIntent(sharedText: String?, sharedUri: Uri?, mimeType: String?) {
        viewModelScope.launch {
            val targetAct = activities.value.firstOrNull()
            if (sharedUri != null) {
                attachDocumentUri(
                    uri = sharedUri,
                    mimeType = mimeType ?: "application/octet-stream",
                    spaceId = _activeSpaceId.value,
                    activityId = targetAct?.id
                )
            } else if (!sharedText.isNullOrBlank()) {
                val isUrl = sharedText.startsWith("http://") || sharedText.startsWith("https://")
                createQuickNoteObject(
                    title = if (isUrl) "Shared Link" else "Shared Snippet",
                    body = sharedText,
                    spaceId = _activeSpaceId.value,
                    activityId = targetAct?.id,
                    objectType = if (isUrl) FluxObjectType.LINK else FluxObjectType.NOTE
                )
                postBanner("Saved shared content to FLUX")
            }
        }
    }

    fun updateFluxObject(
        obj: FluxObjectEntity,
        newTitle: String,
        newBodyOrTarget: String,
        newActivityId: String?,
        newSpaceId: String,
        newSize: ObjectCardSize,
        isCompleted: Boolean
    ) {
        viewModelScope.launch {
            val updated = obj.copy(
                title = newTitle.trim().ifBlank { obj.title },
                subtitle = newBodyOrTarget.trim().take(120),
                uriOrTarget = newBodyOrTarget.trim(),
                activityId = newActivityId,
                spaceId = newSpaceId,
                sizeMode = newSize.name,
                isCompleted = isCompleted,
                isSampleContent = false,
                lastAccessedEpochMs = System.currentTimeMillis()
            )
            dao.upsertObject(updated)
            if (_selectedContextObject.value?.id == obj.id) {
                _selectedContextObject.value = updated
            }
            postBanner("Updated \"${updated.title}\"")
        }
    }

    fun deleteFluxObject(objectId: String) {
        viewModelScope.launch {
            dao.deleteObject(objectId)
            if (_selectedContextObject.value?.id == objectId) {
                _selectedContextObject.value = null
            }
            postBanner("Removed object reference")
        }
    }

    // --- Tasks & Checklists ---
    fun createTask(
        title: String,
        dueLabel: String = "Today",
        spaceId: String = _activeSpaceId.value,
        activityId: String? = _selectedActivityId.value,
        priority: Int = 1
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskItemEntity(
                id = "task_${UUID.randomUUID().toString().take(8)}",
                spaceId = spaceId,
                activityId = activityId,
                title = title.trim(),
                dueLabel = dueLabel.trim().ifBlank { "Today" },
                isDone = false,
                priority = priority.coerceIn(0, 2)
            )
            dao.upsertTask(task)
        }
    }

    fun toggleTaskDone(task: TaskItemEntity) {
        viewModelScope.launch {
            dao.upsertTask(task.copy(isDone = !task.isDone))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            dao.deleteTask(taskId)
        }
    }

    // --- People Space & Pinned Contacts ---
    fun refreshDeviceContacts() {
        viewModelScope.launch {
            _deviceContacts.value = platformManager.queryDeviceContactsIfPermitted()
        }
    }

    fun pinContact(
        displayName: String,
        handleOrNumber: String,
        groupName: String = "Starred",
        preferredAction: String = "DIAL"
    ) {
        if (displayName.isBlank()) return
        viewModelScope.launch {
            val initials = displayName.trim().split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { "FX" }
            val entity = PinnedContactEntity(
                id = "person_${UUID.randomUUID().toString().take(8)}",
                displayName = displayName.trim(),
                handleOrNumber = handleOrNumber.trim(),
                groupName = groupName.trim().ifBlank { "Starred" },
                preferredAction = preferredAction,
                avatarInitials = initials,
                isPinnedHome = true
            )
            dao.upsertPinnedContact(entity)
            postBanner("Pinned ${entity.displayName} to People Space")
        }
    }

    fun removePinnedContact(contactId: String) {
        viewModelScope.launch {
            dao.deletePinnedContact(contactId)
            postBanner("Removed from FLUX (Device address book untouched)")
        }
    }

    // --- Widgets Studio (Native + Android AppWidgetHost) ---
    fun addNativeFluxWidget(widgetKind: String, label: String, spanColumns: Int = 1) {
        viewModelScope.launch {
            val widget = HostedWidgetEntity(
                id = "widget_${UUID.randomUUID().toString().take(8)}",
                spaceId = _activeSpaceId.value,
                appWidgetId = -1,
                widgetKind = widgetKind,
                label = label,
                heightDp = if (spanColumns == 2) 180 else 148,
                spanColumns = spanColumns,
                sortOrder = hostedWidgets.value.size
            )
            dao.upsertWidget(widget)
            postBanner("Added $label widget")
        }
    }

    fun bindOrAddAndroidAppWidget(provider: AvailableWidgetProvider, onPermissionIntentRequired: (android.content.Intent, Int, AvailableWidgetProvider) -> Unit) {
        val allocatedId = widgetHostHelper.allocateWidgetId()
        if (allocatedId < 0) {
            postBanner("Could not allocate AppWidget ID")
            return
        }
        val bound = widgetHostHelper.tryBindWidget(allocatedId, provider.providerComponent)
        if (bound) {
            persistBoundAndroidWidget(allocatedId, provider)
        } else {
            val bindIntent = widgetHostHelper.buildBindPermissionIntent(allocatedId, provider.providerComponent)
            onPermissionIntentRequired(bindIntent, allocatedId, provider)
        }
    }

    fun persistBoundAndroidWidget(appWidgetId: Int, provider: AvailableWidgetProvider) {
        viewModelScope.launch {
            val entity = HostedWidgetEntity(
                id = "appwidget_$appWidgetId",
                spaceId = _activeSpaceId.value,
                appWidgetId = appWidgetId,
                widgetKind = "ANDROID_APPWIDGET",
                providerFlatten = provider.providerComponent.flattenToString(),
                label = provider.label,
                heightDp = provider.minHeightDp.coerceIn(130, 320),
                spanColumns = if (provider.minWidthDp > 180) 2 else 1,
                sortOrder = hostedWidgets.value.size
            )
            dao.upsertWidget(entity)
            postBanner("Hosted Android widget \"${provider.label}\"")
        }
    }

    fun resizeWidget(widget: HostedWidgetEntity, newSpanColumns: Int, newHeightDp: Int) {
        viewModelScope.launch {
            dao.upsertWidget(
                widget.copy(
                    spanColumns = newSpanColumns.coerceIn(1, 2),
                    heightDp = newHeightDp.coerceIn(120, 360)
                )
            )
        }
    }

    fun removeWidget(widget: HostedWidgetEntity) {
        viewModelScope.launch {
            if (widget.appWidgetId >= 0) {
                widgetHostHelper.deleteWidgetId(widget.appWidgetId)
            }
            dao.deleteWidget(widget.id)
            postBanner("Removed widget \"${widget.label}\"")
        }
    }

    // --- Optional Local AI Assistant Operations ---
    fun runAssistantSummarizeObject(obj: FluxObjectEntity) {
        if (!settingsState.value.enableLocalAssistant) {
            postBanner("Local Assistant is disabled in Privacy & AI Settings")
            return
        }
        _assistantResponse.value = LocalAssistantEngine.summarizeContent(
            title = obj.title,
            rawContent = "${obj.subtitle}\n${obj.uriOrTarget}"
        )
        navigateTo(FluxDestination.LOCAL_ASSISTANT)
    }

    fun runAssistantExplainObject(obj: FluxObjectEntity) {
        if (!settingsState.value.enableLocalAssistant) {
            postBanner("Local Assistant is disabled in Privacy & AI Settings")
            return
        }
        _assistantResponse.value = LocalAssistantEngine.explainTextOrFormula(
            title = obj.title,
            content = "${obj.subtitle}\n${obj.uriOrTarget}"
        )
        navigateTo(FluxDestination.LOCAL_ASSISTANT)
    }

    fun runAssistantGenerateChecklist(rawText: String) {
        _assistantResponse.value = LocalAssistantEngine.generateChecklistFromText(rawText)
    }

    fun runAssistantCreateTaskFromPrompt(instruction: String) {
        val resp = LocalAssistantEngine.parseTaskFromInstruction(instruction, spaces.value)
        _assistantResponse.value = resp
        if (!resp.suggestedTaskTitle.isNullOrBlank()) {
            createTask(
                title = resp.suggestedTaskTitle,
                dueLabel = "Today · Assistant",
                spaceId = resp.suggestedSpaceId ?: _activeSpaceId.value
            )
            postBanner("Created task: ${resp.suggestedTaskTitle}")
        }
    }

    fun runAssistantLocateInIndex(query: String) {
        _assistantResponse.value = LocalAssistantEngine.locateInLocalIndex(
            query = query,
            activities = activities.value,
            objects = allObjects.value,
            tasks = allTasks.value
        )
    }

    // --- Privacy & Reset Operations ---
    private suspend fun recordRecentEvent(title: String, subtitle: String, eventKind: String, target: String) {
        if (!settingsState.value.recordRecentHistory) return
        dao.insertRecentEvent(
            RecentEventEntity(
                id = "rec_${UUID.randomUUID().toString().take(8)}",
                title = title,
                subtitle = subtitle,
                eventKind = eventKind,
                targetIdOrPackage = target
            )
        )
    }

    fun clearRecentActivityHistory() {
        viewModelScope.launch {
            dao.clearRecentEvents()
            postBanner("Cleared recent continuity history")
        }
    }

    fun clearStoredFileReferences() {
        viewModelScope.launch {
            dao.clearStoredFileReferences()
            postBanner("Removed all stored file & screenshot URI references")
        }
    }

    fun clearSampleDemonstrationContent() {
        viewModelScope.launch {
            dao.deleteSampleObjects()
            dao.deleteSampleActivities()
            dao.deleteSampleTasks()
            dao.deleteSampleContacts()
            postBanner("Removed sample demonstration content")
        }
    }

    fun restoreDefaultHomeArrangement() {
        viewModelScope.launch {
            preferencesRepo.resetAllCustomizationsToDefault()
            dao.upsertSpaces(FluxSeedData.defaultSpaces())
            dao.upsertActivities(FluxSeedData.initialActivities())
            dao.upsertObjects(FluxSeedData.initialObjects())
            dao.upsertTasks(FluxSeedData.initialTasks())
            dao.upsertWidgets(FluxSeedData.initialWidgets())
            postBanner("Restored default FLUX layout and appearance")
        }
    }

    // --- Customization Setters ---
    fun setThemeMode(mode: FluxThemeMode) = viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
    fun setAccentPalette(palette: FluxAccentPalette) = viewModelScope.launch { preferencesRepo.setAccentPalette(palette) }
    fun setSurfaceOpacity(opacity: Float) = viewModelScope.launch { preferencesRepo.setSurfaceOpacity(opacity) }
    fun setCornerRadiusDp(radius: Int) = viewModelScope.launch { preferencesRepo.setCornerRadiusDp(radius) }
    fun setShowThinBorders(show: Boolean) = viewModelScope.launch { preferencesRepo.setShowThinBorders(show) }
    fun setAnimationIntensity(intensity: Float) = viewModelScope.launch { preferencesRepo.setAnimationIntensity(intensity) }
    fun setIconShape(shape: IconShapeOption) = viewModelScope.launch { preferencesRepo.setIconShape(shape) }
    fun setIconSizeDp(sizeDp: Int) = viewModelScope.launch { preferencesRepo.setIconSizeDp(sizeDp) }
    fun setShowAppLabels(show: Boolean) = viewModelScope.launch { preferencesRepo.setShowAppLabels(show) }
    fun setHomeLayoutStyle(style: HomeLayoutStyle) = viewModelScope.launch { preferencesRepo.setHomeLayoutStyle(style) }
    fun setLayoutDensity(density: LayoutDensityOption) = viewModelScope.launch { preferencesRepo.setLayoutDensity(density) }
    fun updateGesture(key: String, target: GestureTargetAction) = viewModelScope.launch { preferencesRepo.updateGestureMapping(key, target) }

    fun postBanner(message: String?) {
        _statusBannerMessage.value = message
    }
}
