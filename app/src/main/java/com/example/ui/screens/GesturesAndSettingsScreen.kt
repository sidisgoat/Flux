package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.FluxSettingsState
import com.example.data.local.GestureTargetAction
import com.example.platform.AssistantResponse
import com.example.platform.LauncherPlatformManager
import com.example.ui.components.FluxSurfaceCard
import com.example.viewmodel.FluxDestination

data class GestureRowSpec(
    val key: String,
    val title: String,
    val currentTarget: GestureTargetAction
)

@Composable
fun GesturesConfigScreen(
    settings: FluxSettingsState,
    onUpdateGesture: (String, GestureTargetAction) -> Unit,
    onTestGesture: (GestureTargetAction) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var editingGestureSpec by remember { mutableStateOf<GestureRowSpec?>(null) }

    val gestureSpecs = remember(settings) {
        listOf(
            GestureRowSpec("swipe_up", "Swipe up", settings.swipeUpAction),
            GestureRowSpec("swipe_down", "Swipe down", settings.swipeDownAction),
            GestureRowSpec("swipe_horizontal", "Swipe left / right", settings.swipeHorizontalAction),
            GestureRowSpec("double_tap", "Double tap", settings.doubleTapAction),
            GestureRowSpec("pinch_in", "Pinch in", settings.pinchInAction),
            GestureRowSpec("pinch_out", "Pinch out", settings.pinchOutAction),
            GestureRowSpec("long_press", "Long press", settings.longPressAction)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("gestures_config_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("gestures_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "Gestures",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Configure spatial navigation or disable gestures individually",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(gestureSpecs, key = { it.key }) { spec ->
                FluxSurfaceCard(
                    onClick = { editingGestureSpec = spec },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gesture_row_${spec.key}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = spec.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = spec.currentTarget.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = { onTestGesture(spec.currentTarget) }) {
                                Text("Test")
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (spec.currentTarget.requiresSystemNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = spec.currentTarget.requiresSystemNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    editingGestureSpec?.let { spec ->
        AlertDialog(
            onDismissRequest = { editingGestureSpec = null },
            title = { Text("Map Gesture: ${spec.title}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GestureTargetAction.entries) { action ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (action == spec.currentTarget) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateGesture(spec.key, action)
                                    editingGestureSpec = null
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = action.label, style = MaterialTheme.typography.titleSmall)
                                if (action.requiresSystemNote.isNotBlank()) {
                                    Text(
                                        text = action.requiresSystemNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { editingGestureSpec = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * SECTION 18: OPTIONAL LOCAL AI ASSISTANT
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalAssistantScreen(
    enabled: Boolean,
    latestResponse: AssistantResponse?,
    onToggleEnabled: (Boolean) -> Unit,
    onGenerateChecklist: (String) -> Unit,
    onCreateTaskFromInstruction: (String) -> Unit,
    onLocateInIndex: (String) -> Unit,
    onAddChecklistItemsToTasks: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var promptText by remember {
        mutableStateOf("Complete Maxwell boundary derivation, verify oscilloscope phase shift, export PDF summary")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("local_assistant_screen")
    ) {
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
                    modifier = Modifier.testTag("assistant_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "OPTIONAL ENHANCEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "FLUX Local Assistant",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("assistant_enable_switch")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero-Cloud Privacy Boundary",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "FLUX never requires an API key, account, or internet connection. All summarization, checklist generation, and index location run locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (enabled) {
                item {
                    FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "INPUT TEXT, NOTES OR INSTRUCTION",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("assistant_prompt_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onGenerateChecklist(promptText) },
                                    modifier = Modifier.testTag("assistant_generate_checklist_button")
                                ) {
                                    Text("Generate Checklist")
                                }
                                OutlinedButton(
                                    onClick = { onCreateTaskFromInstruction(promptText) },
                                    modifier = Modifier.testTag("assistant_create_task_button")
                                ) {
                                    Text("Create Task")
                                }
                                OutlinedButton(
                                    onClick = { onLocateInIndex(promptText) },
                                    modifier = Modifier.testTag("assistant_locate_index_button")
                                ) {
                                    Text("Locate in FLUX Index")
                                }
                            }
                        }
                    }
                }

                latestResponse?.let { resp ->
                    item {
                        FluxSurfaceCard(
                            accentBorderColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("assistant_response_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = resp.operationTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = resp.summaryText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (resp.generatedChecklist.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    resp.generatedChecklist.forEach { item ->
                                        Text(
                                            text = "☐ $item",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onAddChecklistItemsToTasks(resp.generatedChecklist) }
                                    ) {
                                        Text("Save All ${resp.generatedChecklist.size} Items to Space Tasks")
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = resp.disclosureNote,
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
}

/**
 * SECTION 19: SETTINGS AND PRIVACY (All 11 Required Sections + Search)
 */
@Composable
fun SettingsAndPrivacyScreen(
    settings: FluxSettingsState,
    platformManager: LauncherPlatformManager,
    onNavigate: (FluxDestination) -> Unit,
    onSetSectionVisibility: (Boolean?, Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onSetPrivacyAndAi: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onClearRecentHistory: () -> Unit,
    onClearFileReferences: () -> Unit,
    onClearAllActivities: () -> Unit,
    onExportJsonConfig: () -> String,
    onImportJsonConfig: (String) -> Unit,
    onRequestDefaultHome: () -> Unit,
    onNotify: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var settingsQuery by remember { mutableStateOf("") }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    data class SettingsSectionMeta(
        val number: Int,
        val title: String,
        val keywords: String
    )

    val allSections = remember {
        listOf(
            SettingsSectionMeta(1, "1. Home Screen", "home living environment weather dock recent people density"),
            SettingsSectionMeta(2, "2. Activities and Spaces", "activities spaces continuity workspaces demo sample clear"),
            SettingsSectionMeta(3, "3. Application Index", "apps index clusters categories hidden labels"),
            SettingsSectionMeta(4, "4. Gestures", "swipe up down double tap pinch long press gestures"),
            SettingsSectionMeta(5, "5. Appearance", "theme dark light amoled accent opacity corner borders"),
            SettingsSectionMeta(6, "6. Icons and Widgets", "icons shape size pack widgets appwidgethost"),
            SettingsSectionMeta(7, "7. Search", "universal search action field filters local web"),
            SettingsSectionMeta(8, "8. AI and Suggestions", "ai assistant local personalized suggestions"),
            SettingsSectionMeta(9, "9. Privacy and Permissions", "privacy permissions contacts clear history files"),
            SettingsSectionMeta(10, "10. Backup and Restore", "backup restore export import json configuration"),
            SettingsSectionMeta(11, "11. About FLUX", "about version package id default launcher architecture")
        )
    }

    val visibleSections = remember(settingsQuery) {
        val q = settingsQuery.trim().lowercase()
        if (q.isEmpty()) allSections else allSections.filter {
            it.title.lowercase().contains(q) || it.keywords.contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("settings_privacy_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "Settings & Privacy",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Local-first configuration · Zero analytics",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = settingsQuery,
            onValueChange = { settingsQuery = it },
            placeholder = { Text("Search 11 FLUX settings sections…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("settings_search_input")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleSections, key = { it.number }) { sec ->
                FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = sec.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        when (sec.number) {
                            1 -> {
                                SettingsToggleRow("Show Atmospheric Weather Header", settings.showWeatherHeader) {
                                    onSetSectionVisibility(it, null, null, null, null)
                                }
                                SettingsToggleRow("Show Recent Continuity Stream", settings.showRecentContinuity) {
                                    onSetSectionVisibility(null, it, null, null, null)
                                }
                                SettingsToggleRow("Show Pinned People on Home", settings.showPinnedPeopleSection) {
                                    onSetSectionVisibility(null, null, it, null, null)
                                }
                                SettingsToggleRow("Show Bottom Quick Access Dock", settings.showQuickDock) {
                                    onSetSectionVisibility(null, null, null, it, null)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(onClick = { onNavigate(FluxDestination.LAYOUT_EDITOR) }) {
                                    Text("Open Visual Home Layout Editor")
                                }
                            }
                            2 -> {
                                Text(
                                    text = "Manage Continuity Activities and context Spaces (Personal, Study, Work, Media, People, Games).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onNavigate(FluxDestination.SPACES_DECK) }) {
                                        Text("Manage Spaces")
                                    }
                                    OutlinedButton(onClick = onClearAllActivities) {
                                        Text("Clear Sample Items")
                                    }
                                }
                            }
                            3 -> {
                                Text(
                                    text = "Configure spatial clusters, hidden apps, and usage sorting in the Application Index.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { onNavigate(FluxDestination.APP_INDEX) }) {
                                    Text("Open Application Index")
                                }
                            }
                            4 -> {
                                Text(
                                    text = "Swipe up (${settings.swipeUpAction.label}), Swipe down (${settings.swipeDownAction.label}), Double tap (${settings.doubleTapAction.label}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onNavigate(FluxDestination.GESTURES_CONFIG) },
                                    modifier = Modifier.testTag("settings_open_gestures_button")
                                ) {
                                    Text("Configure Gestures")
                                }
                            }
                            5 -> {
                                Text(
                                    text = "Active Theme: ${settings.themeMode.label} · Accent: ${settings.accentPalette.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { onNavigate(FluxDestination.CUSTOMIZATION_STUDIO) }) {
                                    Text("Open Appearance Studio")
                                }
                            }
                            6 -> {
                                Text(
                                    text = "Icon Shape: ${settings.iconShape.label} (${settings.iconSizeDp}dp) · AppWidgetHost ID: 2048",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onNavigate(FluxDestination.WIDGETS_STUDIO) }) {
                                        Text("Manage Widgets")
                                    }
                                    OutlinedButton(onClick = { onNavigate(FluxDestination.CUSTOMIZATION_STUDIO) }) {
                                        Text("Icon Settings")
                                    }
                                }
                            }
                            7 -> {
                                Text(
                                    text = "Local index searches apps, activities, notes, file URIs, and system settings offline with zero external telemetry.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { onNavigate(FluxDestination.ACTION_FIELD) }) {
                                    Text("Open Universal Search")
                                }
                            }
                            8 -> {
                                SettingsToggleRow("Enable Local Assistant", settings.enableLocalAssistant) {
                                    onSetPrivacyAndAi(null, it, null, null)
                                }
                                SettingsToggleRow("Enable Contextual Suggestions", settings.enablePersonalizedSuggestions) {
                                    onSetPrivacyAndAi(it, null, null, null)
                                }
                            }
                            9 -> {
                                SettingsToggleRow("Record Local Continuity History", settings.recordRecentHistory) {
                                    onSetPrivacyAndAi(null, null, it, null)
                                }
                                SettingsToggleRow("Background Open-Meteo Weather Refresh", settings.enableBackgroundWeatherRefresh) {
                                    onSetPrivacyAndAi(null, null, null, it)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onClearRecentHistory,
                                        modifier = Modifier.testTag("privacy_clear_recent_button")
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear History")
                                    }
                                    OutlinedButton(
                                        onClick = onClearFileReferences,
                                        modifier = Modifier.testTag("privacy_clear_files_button")
                                    ) {
                                        Text("Unlink Files")
                                    }
                                }
                            }
                            10 -> {
                                Text(
                                    text = "Export or import your FLUX layout, theme, and gesture settings as portable JSON.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            backupJsonText = onExportJsonConfig()
                                            showBackupDialog = true
                                        },
                                        modifier = Modifier.testTag("backup_export_button")
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export / Import JSON")
                                    }
                                }
                            }
                            11 -> {
                                Text(
                                    text = "FLUX Native Android Launcher v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Application ID: ${BuildConfig.APPLICATION_ID}\nArchitecture: Kotlin · Jetpack Compose · Room · DataStore · AppWidgetHost",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onRequestDefaultHome) {
                                    Text("Select Default Home Launcher in Android Settings")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore Configuration JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Copy this JSON to back up your FLUX settings, or paste a saved JSON configuration below and tap Import:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            platformManager.copyToClipboard("FLUX Backup", backupJsonText)
                            onNotify("Copied configuration JSON to clipboard")
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            onImportJsonConfig(backupJsonText)
                            showBackupDialog = false
                        }
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
