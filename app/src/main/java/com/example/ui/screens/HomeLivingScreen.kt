package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityEntity
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxSettingsState
import com.example.data.local.HomeLayoutStyle
import com.example.data.local.PinnedContactEntity
import com.example.data.local.RecentEventEntity
import com.example.data.local.SpaceEntity
import com.example.data.local.TaskItemEntity
import com.example.platform.InstalledAppInfo
import com.example.platform.LauncherPlatformManager
import com.example.platform.WeatherService
import com.example.ui.components.AppIconBadge
import com.example.ui.components.FluxSurfaceCard
import com.example.ui.components.iconForObjectType
import com.example.ui.components.parseHexColorSafe
import com.example.viewmodel.FluxDestination
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeLivingScreen(
    settings: FluxSettingsState,
    spaces: List<SpaceEntity>,
    activeSpaceId: String,
    activities: List<ActivityEntity>,
    allObjects: List<FluxObjectEntity>,
    tasks: List<TaskItemEntity>,
    pinnedContacts: List<PinnedContactEntity>,
    recentEvents: List<RecentEventEntity>,
    installedApps: List<InstalledAppInfo>,
    appPrefs: List<AppPreferenceEntity>,
    isDefaultLauncher: Boolean,
    platformManager: LauncherPlatformManager,
    onSelectSpace: (String) -> Unit,
    onCycleSpace: (Int) -> Unit,
    onOpenActivity: (String) -> Unit,
    onSelectObjectForContext: (FluxObjectEntity) -> Unit,
    onNavigate: (FluxDestination) -> Unit,
    onLaunchApp: (String) -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit,
    onCreateActivity: (String, String, String) -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onClearSampleContent: () -> Unit,
    onClearRecentHistory: () -> Unit,
    onSelectWeatherCity: (com.example.platform.WeatherPresetCity) -> Unit,
    onContactAction: (PinnedContactEntity) -> Unit,
    onTriggerGesture: (com.example.data.local.GestureTargetAction) -> Unit
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    var showCreateActivityDialog by remember { mutableStateOf(false) }
    var showWeatherPickerDialog by remember { mutableStateOf(false) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val hourOfDay = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }.get(Calendar.HOUR_OF_DAY)
    }
    val greeting = when (hourOfDay) {
        in 5..11 -> "Good morning."
        in 12..16 -> "Good afternoon."
        in 17..21 -> "Good evening."
        else -> "Late focus."
    }

    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId } ?: spaces.firstOrNull()
    val spaceActivities = remember(activities, activeSpaceId) {
        val inSpace = activities.filter { it.spaceId == activeSpaceId || it.isPinnedHome }
        if (inSpace.isNotEmpty()) inSpace else activities
    }
    val openTasksCount = remember(tasks) { tasks.count { !it.isDone } }
    val hasSampleData = remember(activities, allObjects) {
        activities.any { it.isSampleContent } || allObjects.any { it.isSampleContent }
    }

    val dockedApps = remember(installedApps, appPrefs) {
        val prefMap = appPrefs.associateBy { it.packageName }
        val explicitDocked = installedApps.filter { prefMap[it.packageName]?.isDocked == true }
            .sortedBy { prefMap[it.packageName]?.dockOrder ?: 0 }
        if (explicitDocked.isNotEmpty()) explicitDocked.take(5) else installedApps.take(4)
    }

    val spacing = settings.layoutDensity.spacingDp.dp
    val accentColor = MaterialTheme.colorScheme.primary

    var verticalDragDelta by remember { mutableFloatStateOf(0f) }
    var horizontalDragDelta by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // Subtle spatial radial atmosphere (restrained, non-neon editorial depth)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (settings.themeMode.id == "amoled") 0.06f else 0.11f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.18f),
                        radius = size.width * 0.75f
                    )
                )
            }
            .pointerInput(settings) {
                detectTapGestures(
                    onDoubleTap = { onTriggerGesture(settings.doubleTapAction) },
                    onLongPress = { onTriggerGesture(settings.longPressAction) }
                )
            }
            .pointerInput(settings) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragDelta = 0f },
                    onHorizontalDrag = { _, dragAmount -> horizontalDragDelta += dragAmount },
                    onDragEnd = {
                        if (horizontalDragDelta < -90f) {
                            onCycleSpace(1)
                        } else if (horizontalDragDelta > 90f) {
                            onCycleSpace(-1)
                        }
                    }
                )
            }
            .testTag("home_living_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // 1. Top Spatial Header: Date, Large Time, Weather & Edit/Settings Controls
            item(key = "header_clock_weather") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(settings) {
                            detectVerticalDragGestures(
                                onDragStart = { verticalDragDelta = 0f },
                                onVerticalDrag = { _, dragAmount -> verticalDragDelta += dragAmount },
                                onDragEnd = {
                                    if (verticalDragDelta > 75f) {
                                        onTriggerGesture(settings.swipeDownAction)
                                    } else if (verticalDragDelta < -75f) {
                                        onTriggerGesture(settings.swipeUpAction)
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = dateFormat.format(Date(currentTimeMillis)).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = timeFormat.format(Date(currentTimeMillis)),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("home_clock_text")
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            // Top utility strip: Edit Canvas, Widgets, Customize, Settings
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onNavigate(FluxDestination.LAYOUT_EDITOR) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("open_layout_editor_button")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Home Layout",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onNavigate(FluxDestination.WIDGETS_STUDIO) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("open_widgets_button")
                                ) {
                                    Icon(
                                        Icons.Default.Widgets,
                                        contentDescription = "Interactive Widgets",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onNavigate(FluxDestination.CUSTOMIZATION_STUDIO) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("open_customize_button")
                                ) {
                                    Icon(
                                        Icons.Default.DashboardCustomize,
                                        contentDescription = "Customize FLUX",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onNavigate(FluxDestination.SETTINGS_PRIVACY) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("open_settings_button")
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "FLUX Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (settings.showWeatherHeader) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clickable { showWeatherPickerDialog = true }
                                        .testTag("weather_pill_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = "Weather",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${settings.cachedTempC}°C · ${settings.weatherCity}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = settings.cachedWeatherSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contextual Status Summary Capsule (Matches Screen 1 & 2 "Good morning. 2 activities · 3 tasks · 21°")
                    FluxSurfaceCard(
                        onClick = { onNavigate(FluxDestination.ACTION_FIELD) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contextual_summary_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = greeting,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${activities.size} activities · $openTasksCount open tasks · ${activeSpace?.title ?: "Personal"} Space",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Action Field",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Default Launcher Banner / Sample Content Control Strip
            if (!isDefaultLauncher || hasSampleData) {
                item(key = "system_status_strip") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isDefaultLauncher) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onRequestDefaultLauncher() }
                                    .testTag("set_default_launcher_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Set FLUX as Default Home",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        if (hasSampleData) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onClearSampleContent() }
                                    .testTag("clear_sample_data_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clear Sample Demo Activities",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Spaces Rail (Horizontal Selector + Deck Overview Button)
            item(key = "spaces_selector_rail") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SPACES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Deck View →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigate(FluxDestination.SPACES_DECK) }
                                .padding(4.dp)
                                .testTag("open_spaces_deck_button")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("spaces_lazy_row")
                    ) {
                        items(spaces, key = { it.id }) { space ->
                            val isSelected = space.id == activeSpaceId
                            val spaceAccent = parseHexColorSafe(space.accentHex, MaterialTheme.colorScheme.primary)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    spaceAccent.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = settings.surfaceOpacity)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) spaceAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .clickable { onSelectSpace(space.id) }
                                    .testTag("space_chip_${space.roleKey}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) spaceAccent else MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = space.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = space.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Continuity Activities Section (Spatial Canvas / Editorial Stream / Matrix)
            item(key = "activities_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONTINUITY ACTIVITIES · ${activeSpace?.title?.uppercase() ?: "HOME"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap an activity workspace or select any connected object",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { showCreateActivityDialog = true }
                            .testTag("new_activity_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create Activity",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Activity",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (spaceActivities.isEmpty()) {
                item(key = "empty_activities_state") {
                    FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active workspaces in ${activeSpace?.title ?: "this Space"}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Group PDFs, notes, links, and app shortcuts around what you are working on.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(onClick = { showCreateActivityDialog = true }) {
                                Text("Create First Activity")
                            }
                        }
                    }
                }
            } else {
                items(spaceActivities, key = { it.id }) { activity ->
                    val linkedObjects = remember(allObjects, activity.id) {
                        allObjects.filter { it.activityId == activity.id }
                    }
                    val actColor = parseHexColorSafe(activity.accentHex, MaterialTheme.colorScheme.primary)
                    // Apply asymmetrical canvas offset when in CANVAS layout mode
                    val xOffsetPx = if (settings.homeLayoutStyle == HomeLayoutStyle.CANVAS) {
                        (activity.canvasOffsetXDp * 0.6f).roundToInt()
                    } else 0

                    FluxSurfaceCard(
                        accentBorderColor = actColor.copy(alpha = 0.55f),
                        onClick = { onOpenActivity(activity.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(xOffsetPx, 0) }
                            .testTag("activity_card_${activity.id}")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = actColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = activity.statusLabel.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = actColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${activity.progressPercent}% · ${linkedObjects.size} objects",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activity.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (activity.progressPercent / 100f).coerceIn(0f, 1f) },
                                color = actColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )

                            if (linkedObjects.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    linkedObjects.take(4).forEach { obj ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clickable { onSelectObjectForContext(obj) }
                                                .testTag("object_chip_${obj.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = iconForObjectType(obj.objectType),
                                                    contentDescription = null,
                                                    tint = actColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = obj.title.take(24),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Active Tasks & Quick Checklists in Current Space
            if (tasks.isNotEmpty()) {
                item(key = "quick_tasks_card") {
                    FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FOCUS CHECKLIST",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${tasks.count { it.isDone }}/${tasks.size} completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            tasks.take(4).forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleTask(task) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = task.dueLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Pinned People (Direct Communication Shortcuts)
            if (settings.showPinnedPeopleSection && pinnedContacts.isNotEmpty()) {
                item(key = "pinned_people_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PINNED PEOPLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "People Space →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    onSelectSpace("space_people")
                                    onNavigate(FluxDestination.SPACES_DECK)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(pinnedContacts, key = { it.id }) { person ->
                                FluxSurfaceCard(
                                    onClick = { onContactAction(person) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = person.avatarInitials,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = person.displayName,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "${person.groupName} · ${person.preferredAction}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Contact",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Recent Continuity History Section
            if (settings.showRecentContinuity && recentEvents.isNotEmpty()) {
                item(key = "recent_continuity_section") {
                    FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RECENT CONTINUITY",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onClearRecentHistory() }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            recentEvents.take(3).forEach { ev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            when (ev.eventKind) {
                                                "LAUNCH_APP" -> onLaunchApp(ev.targetIdOrPackage)
                                                "RESUME_ACTIVITY" -> onOpenActivity(ev.targetIdOrPackage)
                                            }
                                        }
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ev.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Dock + Universal Action Field Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            if (settings.showQuickDock && dockedApps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("home_quick_dock"),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockedApps.forEach { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onLaunchApp(app.packageName) }
                                .padding(horizontal = 4.dp)
                        ) {
                            AppIconBadge(
                                packageName = app.packageName,
                                label = app.label,
                                platformManager = platformManager,
                                sizeDp = settings.iconSizeDp.dp,
                                shapeOption = settings.iconShape
                            )
                            if (settings.showAppLabels) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Field Entry Bar ("What do you want to do?") + App Index + Assistant
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable { onNavigate(FluxDestination.ACTION_FIELD) }
                        .testTag("home_action_field_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Open Action Field",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "What do you want to do?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // App Index Button (Spatial Apps View)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { onNavigate(FluxDestination.APP_INDEX) }
                        .testTag("open_app_index_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Open Application Index",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (settings.enableLocalAssistant) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { onNavigate(FluxDestination.LOCAL_ASSISTANT) }
                            .testTag("open_assistant_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Open FLUX Local Assistant",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { platformManager.launchCameraIntent() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Open Camera",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateActivityDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newSummary by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateActivityDialog = false },
            title = { Text("Create Continuity Activity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Activity Title (e.g., Quantum Mechanics)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_activity_title_input")
                    )
                    OutlinedTextField(
                        value = newSummary,
                        onValueChange = { newSummary = it },
                        label = { Text("Goal or Context Summary") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_activity_summary_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onCreateActivity(newTitle, newSummary, activeSpaceId)
                            showCreateActivityDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_activity_button")
                ) {
                    Text("Create Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateActivityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWeatherPickerDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherPickerDialog = false },
            title = { Text("Atmospheric Glance Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select a city to fetch live Open-Meteo conditions (falls back to cached data offline):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    WeatherService.presetCities.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (preset.name == settings.weatherCity) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectWeatherCity(preset)
                                    showWeatherPickerDialog = false
                                }
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showWeatherPickerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
