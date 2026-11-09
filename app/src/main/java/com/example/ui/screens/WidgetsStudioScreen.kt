package com.example.ui.screens

import android.app.Activity
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxSettingsState
import com.example.data.local.HostedWidgetEntity
import com.example.data.local.TaskItemEntity
import com.example.platform.AvailableWidgetProvider
import com.example.platform.LauncherPlatformManager
import com.example.platform.WidgetHostHelper
import com.example.ui.components.FluxSurfaceCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WidgetsStudioScreen(
    hostedWidgets: List<HostedWidgetEntity>,
    availableSystemProviders: List<AvailableWidgetProvider>,
    activities: List<ActivityEntity>,
    tasks: List<TaskItemEntity>,
    settings: FluxSettingsState,
    widgetHostHelper: WidgetHostHelper,
    platformManager: LauncherPlatformManager,
    onAddNativeWidget: (String, String, Int) -> Unit,
    onBindAndroidWidget: (AvailableWidgetProvider, (android.content.Intent, Int, AvailableWidgetProvider) -> Unit) -> Unit,
    onPersistBoundAndroidWidget: (Int, AvailableWidgetProvider) -> Unit,
    onResizeWidget: (HostedWidgetEntity, Int, Int) -> Unit,
    onRemoveWidget: (HostedWidgetEntity) -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit,
    onAddTask: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var showWidgetPickerModal by remember { mutableStateOf(false) }
    var pendingWidgetId by remember { mutableStateOf(-1) }
    var pendingProvider by remember { mutableStateOf<AvailableWidgetProvider?>(null) }
    var isMediaPlaying by remember { mutableStateOf(false) }
    var mediaProgress by remember { mutableFloatStateOf(0.42f) }
    var quickTaskInput by remember { mutableStateOf("") }

    val bindPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val prov = pendingProvider
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId >= 0 && prov != null) {
            onPersistBoundAndroidWidget(pendingWidgetId, prov)
        } else if (pendingWidgetId >= 0) {
            widgetHostHelper.deleteWidgetId(pendingWidgetId)
        }
    }

    val batteryPercent = remember { platformManager.getDeviceBatteryPercent() }
    val dateLabel = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("widgets_studio_screen")
    ) {
        // Top Header
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
                    modifier = Modifier.testTag("widgets_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "INTERACTIVE WIDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Widgets & AppWidgetHost",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            Button(
                onClick = { showWidgetPickerModal = true },
                modifier = Modifier.testTag("add_widget_picker_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Widget")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top 2-Column Glance Row (Daily Pulse + Weather Glance matching Screen 8)
            item(key = "top_dual_widgets_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Daily Continuity Pulse Widget
                    FluxSurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(152.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "• ${activities.size} active workspaces",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• ${tasks.count { !it.isDone }} pending tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• ${activities.firstOrNull()?.title?.take(18) ?: "Focus session"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 2. Atmospheric Weather Glance Widget
                    FluxSurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(152.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "${settings.cachedTempC}°",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = settings.weatherCity,
                                    style = MaterialTheme.typography.titleSmall
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

            // 3. Interactive Media Controller Widget (Dispatches real Android media key events)
            item(key = "interactive_media_widget") {
                FluxSurfaceCard(
                    accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMediaPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ambient Focus Session",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isMediaPlaying) "Playing · System Audio Channel" else "Ready · System Media Controls",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                    mediaProgress = 0.15f
                                }
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                            }
                            IconButton(
                                onClick = {
                                    isMediaPlaying = !isMediaPlaying
                                    dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                },
                                modifier = Modifier.testTag("widget_media_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isMediaPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play or Pause",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                                    mediaProgress = (mediaProgress + 0.25f) % 1.0f
                                }
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { mediaProgress },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // 4. Interactive Today Checklist + Battery/Energy Ring Row (Matches Screen 8 bottom)
            item(key = "interactive_tasks_and_battery_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Today Interactive Checklist Widget
                    FluxSurfaceCard(
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            tasks.take(4).forEach { t ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleTask(t) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (t.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (t.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = t.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = quickTaskInput,
                                    onValueChange = { quickTaskInput = it },
                                    placeholder = { Text("Add item…", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (quickTaskInput.isNotBlank()) {
                                            onAddTask(quickTaskInput)
                                            quickTaskInput = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Quick add task", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Device Battery & Focus Ring Widget
                    FluxSurfaceCard(
                        modifier = Modifier
                            .weight(0.9f)
                            .height(210.dp)
                    ) {
                        val ringColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DEVICE ENERGY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier.size(104.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 10.dp.toPx()
                                    drawArc(
                                        color = trackColor,
                                        startAngle = 135f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = ringColor,
                                        startAngle = 135f,
                                        sweepAngle = 270f * (batteryPercent / 100f),
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "$batteryPercent%",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            Text(
                                text = "${(batteryPercent * 6) / 60}h ${(batteryPercent * 6) % 60}m est.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Hosted Third-Party & User-Added Widgets (Backed by Android AppWidgetHost)
            item(key = "hosted_widgets_header") {
                Text(
                    text = "HOSTED WIDGET SURFACES (${hostedWidgets.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(hostedWidgets, key = { it.id }) { widget ->
                FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = widget.label, style = MaterialTheme.typography.titleSmall)
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        val nextHeight = if (widget.heightDp < 200) 220 else 148
                                        val nextSpan = if (widget.spanColumns == 1) 2 else 1
                                        onResizeWidget(widget, nextSpan, nextHeight)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Expand,
                                        contentDescription = "Resize Widget",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onRemoveWidget(widget) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove Widget",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (widget.widgetKind == "ANDROID_APPWIDGET" && widget.appWidgetId >= 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val hostView = remember(widget.appWidgetId) {
                                widgetHostHelper.createHostView(context, widget.appWidgetId, widget.providerFlatten)
                            }
                            if (hostView != null) {
                                AndroidView(
                                    factory = { hostView },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(widget.heightDp.dp)
                                )
                            } else {
                                Text(
                                    text = "Android AppWidget (#${widget.appWidgetId}) provider bound (${widget.providerFlatten}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Size: ${if (widget.spanColumns == 2) "Full Width" else "Half Width"} · ${widget.heightDp}dp height",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWidgetPickerModal) {
        AlertDialog(
            onDismissRequest = { showWidgetPickerModal = false },
            title = { Text("Add Interactive or System Widget") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "FLUX NATIVE INTERACTIVE WIDGETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val nativeOptions = listOf(
                        Triple("NATIVE_PULSE", "Continuity Pulse Summary", 1),
                        Triple("NATIVE_WEATHER", "Open-Meteo Weather Glance", 1),
                        Triple("NATIVE_TASKS", "Interactive Space Checklist", 2),
                        Triple("NATIVE_MEDIA", "Media Session Controller", 2),
                        Triple("NATIVE_FOCUS", "Battery & Focus Ring", 1)
                    )
                    items(nativeOptions) { (kind, label, span) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddNativeWidget(kind, label, span)
                                    showWidgetPickerModal = false
                                }
                        ) {
                            Text(
                                text = "+ $label",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "INSTALLED ANDROID APPWIDGET PROVIDERS (${availableSystemProviders.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(availableSystemProviders, key = { it.providerComponent.flattenToString() }) { provider ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showWidgetPickerModal = false
                                    onBindAndroidWidget(provider) { intent, allocId, prov ->
                                        pendingWidgetId = allocId
                                        pendingProvider = prov
                                        try {
                                            bindPermissionLauncher.launch(intent)
                                        } catch (_: Exception) {
                                            onPersistBoundAndroidWidget(allocId, prov)
                                        }
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = provider.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${provider.packageName} · ${provider.minWidthDp}x${provider.minHeightDp}dp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showWidgetPickerModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun dispatchMediaKey(context: android.content.Context, keyCode: Int) {
    try {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    } catch (_: Exception) {
    }
}
