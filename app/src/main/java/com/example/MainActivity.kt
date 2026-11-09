package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FluxObjectType
import com.example.ui.components.ContextObjectActionSheet
import com.example.ui.screens.ActionFieldScreen
import com.example.ui.screens.AppIndexScreen
import com.example.ui.screens.ContentSurfaceScreen
import com.example.ui.screens.CustomizationStudioScreen
import com.example.ui.screens.GesturesConfigScreen
import com.example.ui.screens.HomeLivingScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.LocalAssistantScreen
import com.example.ui.screens.SettingsAndPrivacyScreen
import com.example.ui.screens.SpacesDeckScreen
import com.example.ui.screens.WidgetsStudioScreen
import com.example.ui.theme.FluxTheme
import com.example.viewmodel.FluxDestination
import com.example.viewmodel.FluxViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FluxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        inspectIncomingIntent(intent)

        setContent {
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()

            FluxTheme(
                themeMode = settings.themeMode,
                accent = settings.accentPalette,
                surfaceOpacity = settings.surfaceOpacity,
                cornerRadiusDp = settings.cornerRadiusDp,
                showThinBorders = settings.showThinBorders,
                animationIntensity = settings.animationIntensity
            ) {
                FluxLauncherRoot(
                    viewModel = viewModel,
                    onRequestDefaultLauncher = {
                        viewModel.platformManager.openDefaultHomeSelector(this@MainActivity)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.navigateHome()
        } else {
            inspectIncomingIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPlatformState()
    }

    private fun inspectIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            @Suppress("DEPRECATION")
            val sharedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            viewModel.handleIncomingShareIntent(sharedText, sharedUri, intent.type)
        }
    }
}

@Composable
fun FluxLauncherRoot(
    viewModel: FluxViewModel,
    onRequestDefaultLauncher: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    val activeSpaceId by viewModel.activeSpaceId.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val allObjects by viewModel.allObjects.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val pinnedContacts by viewModel.pinnedContacts.collectAsStateWithLifecycle()
    val hostedWidgets by viewModel.hostedWidgets.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()
    val appPrefs by viewModel.appPreferencesList.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val availableWidgetProviders by viewModel.availableWidgetProviders.collectAsStateWithLifecycle()
    val selectedActivityId by viewModel.selectedActivityId.collectAsStateWithLifecycle()
    val selectedContextObject by viewModel.selectedContextObject.collectAsStateWithLifecycle()
    val selectedAppForSheet by viewModel.selectedAppForSheet.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val appIndexSortMode by viewModel.appIndexSortMode.collectAsStateWithLifecycle()
    val assistantResponse by viewModel.assistantResponse.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
    val isAmoledRestActive by viewModel.isAmoledRestOverlayActive.collectAsStateWithLifecycle()

    // Document picker for attaching PDFs, images, or files via Android Scoped Storage
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachDocumentUri(
                uri = uri,
                mimeType = "application/octet-stream",
                spaceId = activeSpaceId,
                activityId = selectedActivityId
            )
        }
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3200L)
            viewModel.postBanner(null)
        }
    }

    BackHandler(enabled = destination != FluxDestination.HOME_LIVING_CANVAS || selectedContextObject != null || isAmoledRestActive) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            FluxGlobalNavigationStrip(
                currentDestination = destination,
                onSelectDestination = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (destination) {
                FluxDestination.HOME_LIVING_CANVAS -> {
                    HomeLivingScreen(
                        settings = settings,
                        spaces = spaces,
                        activeSpaceId = activeSpaceId,
                        activities = activities,
                        allObjects = allObjects,
                        tasks = allTasks,
                        pinnedContacts = pinnedContacts,
                        recentEvents = recentEvents,
                        installedApps = installedApps,
                        appPrefs = appPrefs,
                        isDefaultLauncher = isDefaultLauncher,
                        platformManager = viewModel.platformManager,
                        onSelectSpace = { viewModel.selectSpace(it) },
                        onCycleSpace = { viewModel.cycleSpace(it) },
                        onOpenActivity = { viewModel.openActivityWorkspace(it) },
                        onSelectObjectForContext = { viewModel.openContextActionsForObject(it) },
                        onNavigate = { viewModel.navigateTo(it) },
                        onLaunchApp = { viewModel.launchInstalledApp(it) },
                        onToggleTask = { viewModel.toggleTaskDone(it) },
                        onCreateActivity = { title, summary, spaceId ->
                            viewModel.createActivity(title, summary, spaceId)
                        },
                        onRequestDefaultLauncher = onRequestDefaultLauncher,
                        onClearSampleContent = { viewModel.clearSampleDemonstrationContent() },
                        onClearRecentHistory = { viewModel.clearRecentActivityHistory() },
                        onSelectWeatherCity = { viewModel.selectWeatherCityPreset(it) },
                        onContactAction = { person ->
                            viewModel.platformManager.initiateCommunication(person.preferredAction, person.handleOrNumber)
                        },
                        onTriggerGesture = { viewModel.triggerConfiguredGesture(it) }
                    )
                }

                FluxDestination.CONTENT_SURFACE -> {
                    val activeActivity = activities.firstOrNull { it.id == selectedActivityId } ?: activities.firstOrNull()
                    val connected = allObjects.filter { it.activityId == activeActivity?.id }
                    val actTasks = allTasks.filter { it.activityId == activeActivity?.id || it.spaceId == activeActivity?.spaceId }

                    ContentSurfaceScreen(
                        activity = activeActivity,
                        spaces = spaces,
                        connectedObjects = connected,
                        activityTasks = actTasks,
                        onBack = { viewModel.navigateBack() },
                        onSelectObject = { viewModel.openContextActionsForObject(it) },
                        onOpenObjectDirect = { obj ->
                            if (obj.objectType == FluxObjectType.SHORTCUT.name) {
                                viewModel.launchInstalledApp(obj.uriOrTarget)
                            } else {
                                viewModel.platformManager.openUriOrWebSearch(obj.uriOrTarget, obj.mimeType)
                            }
                        },
                        onAddNoteOrLink = { title, body, objType ->
                            viewModel.createQuickNoteObject(
                                title = title,
                                body = body,
                                spaceId = activeActivity?.spaceId ?: activeSpaceId,
                                activityId = activeActivity?.id,
                                objectType = objType
                            )
                        },
                        onPickDocumentFile = {
                            try {
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            } catch (_: Exception) {
                                viewModel.postBanner("System file picker unavailable on this device")
                            }
                        },
                        onAddTask = { title ->
                            viewModel.createTask(
                                title = title,
                                spaceId = activeActivity?.spaceId ?: activeSpaceId,
                                activityId = activeActivity?.id
                            )
                        },
                        onToggleTask = { viewModel.toggleTaskDone(it) },
                        onUpdateActivity = { act, title, sum, spId, prog, pinned, size ->
                            viewModel.updateActivityDetails(act, title, sum, spId, prog, pinned, size)
                        },
                        onDeleteActivity = { viewModel.deleteActivity(it) },
                        onAskAssistantForActivity = { viewModel.runAssistantSummarizeObject(it) }
                    )
                }

                FluxDestination.ACTION_FIELD -> {
                    ActionFieldScreen(
                        query = searchQuery,
                        activeFilter = searchFilter,
                        results = searchResults,
                        settings = settings,
                        activities = activities,
                        platformManager = viewModel.platformManager,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onFilterChange = { viewModel.selectSearchFilter(it) },
                        onExecuteResult = { viewModel.executeSearchResult(it) },
                        onExecuteIntentAction = { viewModel.executeIntentOrientedAction(it) },
                        onPickDocumentToImport = {
                            try {
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            } catch (_: Exception) {
                                viewModel.postBanner("File picker unavailable")
                            }
                        },
                        onDismissSuggestion = { id ->
                            scope.launch { viewModel.preferencesRepo.dismissSuggestion(id) }
                        },
                        onNavigate = { viewModel.navigateTo(it) },
                        onBack = { viewModel.navigateBack() },
                        onNotify = { viewModel.postBanner(it) }
                    )
                }

                FluxDestination.APP_INDEX -> {
                    AppIndexScreen(
                        installedApps = installedApps,
                        appPrefs = appPrefs,
                        sortMode = appIndexSortMode,
                        settings = settings,
                        selectedAppForSheet = selectedAppForSheet,
                        platformManager = viewModel.platformManager,
                        onSelectSortMode = { viewModel.setAppIndexSort(it) },
                        onLaunchApp = { viewModel.launchInstalledApp(it) },
                        onOpenAppSheet = { viewModel.openAppOptionsSheet(it) },
                        onToggleDock = { viewModel.toggleAppDocked(it) },
                        onToggleHidden = { viewModel.toggleAppHidden(it) },
                        onAssignCategory = { pkg, cat -> viewModel.setAppCustomCategory(pkg, cat) },
                        onPinAppToActivity = { app -> viewModel.pinAppShortcutToActivity(app, selectedActivityId) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.SPACES_DECK -> {
                    SpacesDeckScreen(
                        spaces = spaces,
                        activeSpaceId = activeSpaceId,
                        activities = activities,
                        allObjects = allObjects,
                        tasks = allTasks,
                        pinnedContacts = pinnedContacts,
                        deviceContacts = deviceContacts,
                        installedApps = installedApps,
                        platformManager = viewModel.platformManager,
                        onSelectSpace = { viewModel.selectSpace(it) },
                        onOpenActivity = { viewModel.openActivityWorkspace(it) },
                        onSelectObject = { viewModel.openContextActionsForObject(it) },
                        onCreateSpace = { t, s, hex -> viewModel.createSpace(t, s, hex) },
                        onRenameSpace = { sp, t, s -> viewModel.renameSpace(sp, t, s) },
                        onDeleteSpace = { viewModel.deleteCustomSpace(it) },
                        onCreateActivityInSpace = { t, s, spId -> viewModel.createActivity(t, s, spId) },
                        onAddTaskInSpace = { t, due, spId -> viewModel.createTask(t, due, spId) },
                        onToggleTask = { viewModel.toggleTaskDone(it) },
                        onPinContact = { n, h, g, a -> viewModel.pinContact(n, h, g, a) },
                        onRemovePinnedContact = { viewModel.removePinnedContact(it) },
                        onRefreshDeviceContacts = { viewModel.refreshDeviceContacts() },
                        onPickMediaOrDocument = {
                            try {
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            } catch (_: Exception) {
                                viewModel.postBanner("File picker unavailable")
                            }
                        },
                        onLaunchApp = { viewModel.launchInstalledApp(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.WIDGETS_STUDIO -> {
                    WidgetsStudioScreen(
                        hostedWidgets = hostedWidgets,
                        availableSystemProviders = availableWidgetProviders,
                        activities = activities,
                        tasks = allTasks,
                        settings = settings,
                        widgetHostHelper = viewModel.widgetHostHelper,
                        platformManager = viewModel.platformManager,
                        onAddNativeWidget = { kind, label, span -> viewModel.addNativeFluxWidget(kind, label, span) },
                        onBindAndroidWidget = { prov, cb -> viewModel.bindOrAddAndroidAppWidget(prov, cb) },
                        onPersistBoundAndroidWidget = { id, prov -> viewModel.persistBoundAndroidWidget(id, prov) },
                        onResizeWidget = { w, span, h -> viewModel.resizeWidget(w, span, h) },
                        onRemoveWidget = { viewModel.removeWidget(it) },
                        onToggleTask = { viewModel.toggleTaskDone(it) },
                        onAddTask = { viewModel.createTask(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.CUSTOMIZATION_STUDIO -> {
                    CustomizationStudioScreen(
                        settings = settings,
                        platformManager = viewModel.platformManager,
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onSetAccentPalette = { viewModel.setAccentPalette(it) },
                        onSetSurfaceOpacity = { viewModel.setSurfaceOpacity(it) },
                        onSetCornerRadius = { viewModel.setCornerRadiusDp(it) },
                        onSetShowThinBorders = { viewModel.setShowThinBorders(it) },
                        onSetAnimationIntensity = { viewModel.setAnimationIntensity(it) },
                        onSetIconShape = { viewModel.setIconShape(it) },
                        onSetIconSize = { viewModel.setIconSizeDp(it) },
                        onSetShowAppLabels = { viewModel.setShowAppLabels(it) },
                        onSetLayoutStyle = { viewModel.setHomeLayoutStyle(it) },
                        onSetLayoutDensity = { viewModel.setLayoutDensity(it) },
                        onOpenLayoutEditor = { viewModel.navigateTo(FluxDestination.LAYOUT_EDITOR) },
                        onRestoreDefaults = { viewModel.restoreDefaultHomeArrangement() },
                        onNotify = { viewModel.postBanner(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.LAYOUT_EDITOR -> {
                    LayoutEditorScreen(
                        activities = activities,
                        settings = settings,
                        onSaveActivityTransform = { act, x, y, sz, ord ->
                            viewModel.updateActivityCanvasTransform(act, x, y, sz, ord)
                        },
                        onDeleteActivity = { viewModel.deleteActivity(it) },
                        onUpdateSectionVisibility = { w, r, p, d, wg ->
                            scope.launch {
                                viewModel.preferencesRepo.setSectionVisibility(w, r, p, d, wg)
                            }
                        },
                        onRestoreDefaults = { viewModel.restoreDefaultHomeArrangement() },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.GESTURES_CONFIG -> {
                    GesturesConfigScreen(
                        settings = settings,
                        onUpdateGesture = { key, target -> viewModel.updateGesture(key, target) },
                        onTestGesture = { viewModel.triggerConfiguredGesture(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.LOCAL_ASSISTANT -> {
                    LocalAssistantScreen(
                        enabled = settings.enableLocalAssistant,
                        latestResponse = assistantResponse,
                        onToggleEnabled = { enabled ->
                            scope.launch {
                                viewModel.preferencesRepo.setPrivacyAndAiOptions(localAssistant = enabled)
                            }
                        },
                        onGenerateChecklist = { viewModel.runAssistantGenerateChecklist(it) },
                        onCreateTaskFromInstruction = { viewModel.runAssistantCreateTaskFromPrompt(it) },
                        onLocateInIndex = { viewModel.runAssistantLocateInIndex(it) },
                        onAddChecklistItemsToTasks = { items ->
                            items.forEach { title -> viewModel.createTask(title = title, dueLabel = "Checklist") }
                            viewModel.postBanner("Added ${items.size} items to active Space checklist")
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                FluxDestination.SETTINGS_PRIVACY -> {
                    SettingsAndPrivacyScreen(
                        settings = settings,
                        platformManager = viewModel.platformManager,
                        onNavigate = { viewModel.navigateTo(it) },
                        onSetSectionVisibility = { w, r, p, d, wg ->
                            scope.launch {
                                viewModel.preferencesRepo.setSectionVisibility(w, r, p, d, wg)
                            }
                        },
                        onSetPrivacyAndAi = { sug, ai, rec, bgW ->
                            scope.launch {
                                viewModel.preferencesRepo.setPrivacyAndAiOptions(sug, ai, rec, bgW)
                            }
                        },
                        onClearRecentHistory = { viewModel.clearRecentActivityHistory() },
                        onClearFileReferences = { viewModel.clearStoredFileReferences() },
                        onClearAllActivities = { viewModel.clearSampleDemonstrationContent() },
                        onExportJsonConfig = { viewModel.preferencesRepo.exportConfigurationJson(settings) },
                        onImportJsonConfig = { json ->
                            scope.launch {
                                val ok = viewModel.preferencesRepo.importConfigurationJson(json)
                                viewModel.postBanner(if (ok) "Imported FLUX configuration JSON" else "Invalid JSON format")
                            }
                        },
                        onRequestDefaultHome = onRequestDefaultLauncher,
                        onNotify = { viewModel.postBanner(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }

            // Contextual Object Actions Bottom Sheet (Section 7)
            selectedContextObject?.let { obj ->
                ContextObjectActionSheet(
                    fluxObject = obj,
                    spaces = spaces,
                    activities = activities,
                    enableAssistant = settings.enableLocalAssistant,
                    onDismiss = { viewModel.openContextActionsForObject(null) },
                    onOpenTarget = { item ->
                        if (item.objectType == FluxObjectType.SHORTCUT.name) {
                            viewModel.launchInstalledApp(item.uriOrTarget)
                        } else {
                            viewModel.platformManager.openUriOrWebSearch(item.uriOrTarget, item.mimeType)
                        }
                        viewModel.openContextActionsForObject(null)
                    },
                    onShare = { item ->
                        viewModel.platformManager.shareTextOrUri(item.title, item.uriOrTarget, item.mimeType)
                        viewModel.openContextActionsForObject(null)
                    },
                    onCopyContent = { item ->
                        viewModel.platformManager.copyToClipboard(item.title, item.uriOrTarget.ifBlank { item.subtitle })
                        viewModel.postBanner("Copied content to clipboard")
                        viewModel.openContextActionsForObject(null)
                    },
                    onSetAsWallpaper = {
                        viewModel.platformManager.applyProceduralFluxWallpaper(
                            settings.accentPalette.containerDark.toArgb(),
                            settings.accentPalette.primary.toArgb()
                        )
                        viewModel.postBanner("Applied object palette as wallpaper")
                        viewModel.openContextActionsForObject(null)
                    },
                    onSaveEdits = { title, body, actId, spId, sz, done ->
                        viewModel.updateFluxObject(obj, title, body, actId, spId, sz, done)
                    },
                    onDelete = { id -> viewModel.deleteFluxObject(id) },
                    onSummarizeWithAssistant = { item ->
                        viewModel.openContextActionsForObject(null)
                        viewModel.runAssistantSummarizeObject(item)
                    },
                    onExplainWithAssistant = { item ->
                        viewModel.openContextActionsForObject(null)
                        viewModel.runAssistantExplainObject(item)
                    },
                    onCreateNoteFromObject = { item ->
                        viewModel.createQuickNoteObject(
                            title = "Note: ${item.title}",
                            body = item.uriOrTarget.ifBlank { item.subtitle },
                            spaceId = item.spaceId,
                            activityId = item.activityId
                        )
                        viewModel.postBanner("Created linked note from ${item.title}")
                        viewModel.openContextActionsForObject(null)
                    }
                )
            }

            // Status Toast Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                ) {
                    Text(
                        text = statusBanner.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // AMOLED Rest Overlay (Legitimate Double-Tap Screen Rest Alternative)
            if (isAmoledRestActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { viewModel.dismissAmoledRestOverlay() }
                        .testTag("amoled_rest_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AMOLED REST MODE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap anywhere to wake FLUX",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.30f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FluxGlobalNavigationStrip(
    currentDestination: FluxDestination,
    onSelectDestination: (FluxDestination) -> Unit
) {
    val navItems = listOf(
        Triple(FluxDestination.HOME_LIVING_CANVAS, "Home", Icons.Default.Home),
        Triple(FluxDestination.SPACES_DECK, "Spaces", Icons.Default.Layers),
        Triple(FluxDestination.ACTION_FIELD, "Action", Icons.Default.Search),
        Triple(FluxDestination.APP_INDEX, "Apps", Icons.Default.Apps),
        Triple(FluxDestination.WIDGETS_STUDIO, "Widgets", Icons.Default.Widgets),
        Triple(FluxDestination.CUSTOMIZATION_STUDIO, "Style", Icons.Default.DashboardCustomize),
        Triple(FluxDestination.SETTINGS_PRIVACY, "Settings", Icons.Default.Settings)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("flux_global_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (dest, label, icon) ->
                val selected = currentDestination == dest
                NavPillItem(
                    label = label,
                    icon = icon,
                    selected = selected,
                    tag = "nav_tab_${dest.routeId}",
                    onClick = { onSelectDestination(dest) }
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
