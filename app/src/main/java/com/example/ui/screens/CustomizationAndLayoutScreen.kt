package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxSettingsState
import com.example.data.local.HomeLayoutStyle
import com.example.data.local.IconShapeOption
import com.example.data.local.LayoutDensityOption
import com.example.data.local.ObjectCardSize
import com.example.platform.LauncherPlatformManager
import com.example.ui.components.FluxSurfaceCard
import com.example.ui.components.resolveIconShape
import com.example.ui.theme.FluxAccentPalette
import com.example.ui.theme.FluxThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationStudioScreen(
    settings: FluxSettingsState,
    platformManager: LauncherPlatformManager,
    onSetThemeMode: (FluxThemeMode) -> Unit,
    onSetAccentPalette: (FluxAccentPalette) -> Unit,
    onSetSurfaceOpacity: (Float) -> Unit,
    onSetCornerRadius: (Int) -> Unit,
    onSetShowThinBorders: (Boolean) -> Unit,
    onSetAnimationIntensity: (Float) -> Unit,
    onSetIconShape: (IconShapeOption) -> Unit,
    onSetIconSize: (Int) -> Unit,
    onSetShowAppLabels: (Boolean) -> Unit,
    onSetLayoutStyle: (HomeLayoutStyle) -> Unit,
    onSetLayoutDensity: (LayoutDensityOption) -> Unit,
    onOpenLayoutEditor: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onNotify: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var activeTab by remember { mutableStateOf("Themes") }
    val tabs = listOf("Themes", "Icons", "Layout", "Animations")
    val installedIconPacks = remember { platformManager.discoverInstalledIconPacks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("customization_studio_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("customize_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Customize",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            OutlinedButton(
                onClick = onRestoreDefaults,
                modifier = Modifier.testTag("restore_defaults_button")
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }
        }

        // Category Tabs (Matches Screen 9 of concept: Themes, Icons, Layout, Animations)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val selected = tab == activeTab
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { activeTab = tab }
                        .testTag("customize_tab_${tab.lowercase()}")
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeTab) {
                "Themes" -> {
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "APPEARANCE & THEME ENGINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FluxThemeMode.entries.forEach { mode ->
                                        val active = settings.themeMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                width = if (active) 2.dp else 1.dp,
                                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier
                                                .clickable { onSetThemeMode(mode) }
                                                .testTag("theme_mode_${mode.id}")
                                        ) {
                                            Text(
                                                text = mode.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Accent Color Swatches (Matches Screen 9 Accent Color row)
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ACCENT COLOR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FluxAccentPalette.entries.forEach { palette ->
                                        val selected = settings.accentPalette == palette
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(palette.primary)
                                                .border(
                                                    width = if (selected) 3.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { onSetAccentPalette(palette) }
                                                .testTag("accent_swatch_${palette.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = palette.label,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Surface Opacity, Corner Radius & Borders
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "SURFACE GEOMETRY & DEPTH",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Surface Opacity: ${(settings.surfaceOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = settings.surfaceOpacity,
                                    onValueChange = onSetSurfaceOpacity,
                                    valueRange = 0.50f..1.0f
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Corner Radius: ${settings.cornerRadiusDp}dp",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = settings.cornerRadiusDp.toFloat(),
                                    onValueChange = { onSetCornerRadius(it.roundToInt()) },
                                    valueRange = 0f..30f
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Thin Structural Borders", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = settings.showThinBorders,
                                        onCheckedChange = onSetShowThinBorders
                                    )
                                }
                            }
                        }
                    }

                    // Wallpaper Integration
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "WALLPAPER INTEGRATION",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { platformManager.openWallpaperChooser() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("System Picker")
                                    }
                                    Button(
                                        onClick = {
                                            val ok = platformManager.applyProceduralFluxWallpaper(
                                                settings.accentPalette.containerDark.toArgb(),
                                                settings.accentPalette.primary.copy(alpha = 0.35f).toArgb()
                                            )
                                            onNotify(if (ok) "Applied FLUX Atmospheric Wallpaper" else "Wallpaper update restricted")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Apply FLUX Canvas")
                                    }
                                }
                            }
                        }
                    }
                }

                "Icons" -> {
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ICON SHAPE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconShapeOption.entries.forEach { shapeOpt ->
                                        val selected = settings.iconShape == shapeOpt
                                        val shape = resolveIconShape(shapeOpt)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { onSetIconShape(shapeOpt) }
                                                .testTag("icon_shape_${shapeOpt.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(shape)
                                                    .background(
                                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = shapeOpt.label.take(8),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Icon Size: ${settings.iconSizeDp}dp",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = settings.iconSizeDp.toFloat(),
                                    onValueChange = { onSetIconSize(it.roundToInt()) },
                                    valueRange = 40f..68f
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Display Application Labels", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = settings.showAppLabels,
                                        onCheckedChange = onSetShowAppLabels
                                    )
                                }
                            }
                        }
                    }

                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "INSTALLED ICON PACKS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (installedIconPacks.isEmpty()) {
                                    Text(
                                        text = "Using native installed application icons with FLUX geometric masking. Install any ADW-compatible Android icon pack to select it here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    installedIconPacks.forEach { (pkg, name) ->
                                        Text(text = "• $name ($pkg)", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                "Layout" -> {
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "HOME ARCHITECTURE STYLE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HomeLayoutStyle.entries.forEach { style ->
                                        val selected = settings.homeLayoutStyle == style
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { onSetLayoutStyle(style) }
                                        ) {
                                            Text(
                                                text = style.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SPACING DENSITY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LayoutDensityOption.entries.forEach { density ->
                                        val selected = settings.layoutDensity == density
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { onSetLayoutDensity(density) }
                                        ) {
                                            Text(
                                                text = density.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onOpenLayoutEditor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Interactive Home Layout Editor")
                                }
                            }
                        }
                    }
                }

                "Animations" -> {
                    item {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "MOTION & REDUCED-MOTION CONTROLS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Animation Intensity: ${(settings.animationIntensity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = settings.animationIntensity,
                                    onValueChange = onSetAnimationIntensity,
                                    valueRange = 0f..1.5f
                                )
                                Text(
                                    text = "Set to 0% for instant transitions and minimum battery/GPU overhead.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * SECTION 16: DEDICATED VISUAL LAYOUT EDITOR
 * Allows selecting, dragging/nudging, resizing, reordering, toggling section visibility,
 * previewing, saving, or canceling unsaved changes.
 */
@Composable
fun LayoutEditorScreen(
    activities: List<ActivityEntity>,
    settings: FluxSettingsState,
    onSaveActivityTransform: (ActivityEntity, Float, Float, ObjectCardSize, Int) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onUpdateSectionVisibility: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onRestoreDefaults: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    // Local draft state so user can Preview, Save, or Cancel unsaved changes!
    val draftOffsetsX = remember(activities) {
        mutableStateMapOf<String, Float>().apply {
            activities.forEach { put(it.id, it.canvasOffsetXDp) }
        }
    }
    val draftSizes = remember(activities) {
        mutableStateMapOf<String, ObjectCardSize>().apply {
            activities.forEach {
                put(it.id, runCatching { ObjectCardSize.valueOf(it.cardSize) }.getOrDefault(ObjectCardSize.MEDIUM))
            }
        }
    }
    val draftOrders = remember(activities) {
        mutableStateMapOf<String, Int>().apply {
            activities.forEachIndexed { idx, act -> put(act.id, idx) }
        }
    }

    var draftWeather by remember(settings) { mutableStateOf(settings.showWeatherHeader) }
    var draftRecent by remember(settings) { mutableStateOf(settings.showRecentContinuity) }
    var draftPeople by remember(settings) { mutableStateOf(settings.showPinnedPeopleSection) }
    var draftDock by remember(settings) { mutableStateOf(settings.showQuickDock) }
    var draftWidgets by remember(settings) { mutableStateOf(settings.showWidgetsSection) }
    var isPreviewMode by remember { mutableStateOf(false) }

    val sortedActivities = remember(activities, draftOrders.toMap()) {
        activities.sortedBy { draftOrders[it.id] ?: it.canvasOrder }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("layout_editor_screen")
    ) {
        // Top Editor Bar: Cancel, Preview, Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("layout_editor_cancel_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel Edits")
                }
                Column {
                    Text(
                        text = if (isPreviewMode) "PREVIEW MODE" else "VISUAL LAYOUT EDITOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Drag handles, resize, or reorder",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isPreviewMode = !isPreviewMode },
                    modifier = Modifier.testTag("layout_editor_preview_button")
                ) {
                    Text(if (isPreviewMode) "Edit" else "Preview")
                }
                Button(
                    onClick = {
                        sortedActivities.forEachIndexed { index, act ->
                            onSaveActivityTransform(
                                act,
                                draftOffsetsX[act.id] ?: act.canvasOffsetXDp,
                                act.canvasOffsetYDp,
                                draftSizes[act.id] ?: ObjectCardSize.MEDIUM,
                                index
                            )
                        }
                        onUpdateSectionVisibility(draftWeather, draftRecent, draftPeople, draftDock, draftWidgets)
                        onBack()
                    },
                    modifier = Modifier.testTag("layout_editor_save_button")
                ) {
                    Text("Save")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Visibility Toggles
            if (!isPreviewMode) {
                item(key = "section_visibility_controls") {
                    FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VISIBLE HOME SECTIONS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Restore Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onRestoreDefaults() }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            EditorSwitchRow("Atmospheric Weather Pill", draftWeather) { draftWeather = it }
                            EditorSwitchRow("Recent Continuity Stream", draftRecent) { draftRecent = it }
                            EditorSwitchRow("Pinned People Shortcuts", draftPeople) { draftPeople = it }
                            EditorSwitchRow("Bottom Quick Access Dock", draftDock) { draftDock = it }
                        }
                    }
                }
            }

            item(key = "canvas_objects_header") {
                Text(
                    text = "DRAG HORIZONTAL OFFSET, RESIZE OR REORDER ACTIVITIES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(sortedActivities, key = { _, act -> act.id }) { idx, act ->
                val currentX = draftOffsetsX[act.id] ?: 0f
                val currentSize = draftSizes[act.id] ?: ObjectCardSize.MEDIUM

                FluxSurfaceCard(
                    accentBorderColor = if (!isPreviewMode) MaterialTheme.colorScheme.primary else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset((currentX * 0.8f).roundToInt(), 0) }
                        .pointerInput(act.id, isPreviewMode) {
                            if (!isPreviewMode) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val nextX = ((draftOffsetsX[act.id] ?: 0f) + dragAmount.x * 0.35f)
                                        .coerceIn(-90f, 90f)
                                    draftOffsetsX[act.id] = nextX
                                }
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isPreviewMode) {
                                Icon(
                                    Icons.Default.DragIndicator,
                                    contentDescription = "Drag Handle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = act.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Size: ${currentSize.label} · Offset X: ${currentX.roundToInt()}dp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isPreviewMode) {
                                IconButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val prevAct = sortedActivities[idx - 1]
                                            draftOrders[act.id] = idx - 1
                                            draftOrders[prevAct.id] = idx
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (idx < sortedActivities.lastIndex) {
                                            val nextAct = sortedActivities[idx + 1]
                                            draftOrders[act.id] = idx + 1
                                            draftOrders[nextAct.id] = idx
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteActivity(act.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Activity",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (!isPreviewMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ObjectCardSize.entries.forEach { sz ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (currentSize == sz) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { draftSizes[act.id] = sz }
                                        ) {
                                            Text(
                                                text = sz.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (currentSize == sz) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Center X",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { draftOffsetsX[act.id] = 0f }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
