package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxSettingsState
import com.example.platform.LauncherPlatformManager
import com.example.ui.components.AppIconBadge
import com.example.ui.components.FluxSurfaceCard
import com.example.ui.components.parseHexColorSafe
import com.example.viewmodel.FluxDestination
import com.example.viewmodel.SearchResultFilter
import com.example.viewmodel.UnifiedSearchResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionFieldScreen(
    query: String,
    activeFilter: SearchResultFilter,
    results: List<UnifiedSearchResult>,
    settings: FluxSettingsState,
    activities: List<ActivityEntity>,
    platformManager: LauncherPlatformManager,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchResultFilter) -> Unit,
    onExecuteResult: (UnifiedSearchResult) -> Unit,
    onExecuteIntentAction: (String) -> Unit,
    onPickDocumentToImport: () -> Unit,
    onDismissSuggestion: (String) -> Unit,
    onNavigate: (FluxDestination) -> Unit,
    onBack: () -> Unit,
    onNotify: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onQueryChange(spoken)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("action_field_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Search Input Bar ("What do you want to do?")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("action_field_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "What do you want to do?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.testTag("clear_search_query_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak action or search term")
                                    }
                                    voiceLauncher.launch(speechIntent)
                                } catch (_: Exception) {
                                    onNotify("Voice recognition service not installed on this device")
                                }
                            },
                            modifier = Modifier.testTag("voice_search_button")
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_field_search_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips: All, Apps, Actions, Files, Settings, Web
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchResultFilter.entries.forEach { filter ->
                val selected = filter == activeFilter
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clickable { onFilterChange(filter) }
                        .testTag("search_filter_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Intent-Oriented Spatial Nodes when query is blank
            if (query.isBlank() && (activeFilter == SearchResultFilter.ALL || activeFilter == SearchResultFilter.ACTIONS)) {
                item(key = "intent_constellation") {
                    FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "START WITH AN INTENTION",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "No App Drawer Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SpatialIntentPill(
                                    label = "Create Note",
                                    colorHex = "#A855F7",
                                    icon = Icons.Default.EditNote,
                                    tag = "intent_pill_create_note",
                                    onClick = { onExecuteIntentAction("CREATE_NOTE") }
                                )
                                SpatialIntentPill(
                                    label = "Open File",
                                    colorHex = "#38BDF8",
                                    icon = Icons.Default.FolderOpen,
                                    tag = "intent_pill_open_file",
                                    onClick = onPickDocumentToImport
                                )
                                SpatialIntentPill(
                                    label = "New Activity",
                                    colorHex = "#F59E0B",
                                    icon = Icons.Default.AutoAwesome,
                                    tag = "intent_pill_new_activity",
                                    onClick = { onExecuteIntentAction("CREATE_ACTIVITY") }
                                )
                                SpatialIntentPill(
                                    label = "Plan Task",
                                    colorHex = "#10B981",
                                    icon = Icons.Default.TaskAlt,
                                    tag = "intent_pill_plan_task",
                                    onClick = { onExecuteIntentAction("PLAN_TASK") }
                                )
                                SpatialIntentPill(
                                    label = "Send / Share",
                                    colorHex = "#F43F5E",
                                    icon = Icons.Default.Send,
                                    tag = "intent_pill_send",
                                    onClick = { onExecuteIntentAction("SHARE_CLIPBOARD") }
                                )
                                SpatialIntentPill(
                                    label = "Media Space",
                                    colorHex = "#818CF8",
                                    icon = Icons.Default.PlayArrow,
                                    tag = "intent_pill_play",
                                    onClick = { onNavigate(FluxDestination.SPACES_DECK) }
                                )
                            }
                        }
                    }
                }

                // Contextual Suggestions (Dismissible & Respects Privacy Settings)
                if (settings.enablePersonalizedSuggestions) {
                    val activeSuggestions = activities.filter { act ->
                        !settings.dismissedSuggestionIds.contains(act.id)
                    }.take(2)

                    if (activeSuggestions.isNotEmpty()) {
                        item(key = "contextual_suggestions_header") {
                            Text(
                                text = "CONTEXTUAL CONTINUITY SUGGESTIONS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(activeSuggestions, key = { "sug_${it.id}" }) { act ->
                            FluxSurfaceCard(
                                onClick = {
                                    onExecuteResult(
                                        UnifiedSearchResult(
                                            id = "sug_${act.id}",
                                            title = act.title,
                                            subtitle = act.summary,
                                            categoryBadge = "Resume",
                                            filterCategory = SearchResultFilter.ACTIONS,
                                            score = 100,
                                            activityId = act.id
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Continue: ${act.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${act.progressPercent}% progress · ${act.summary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDismissSuggestion(act.id) }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss suggestion",
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

            // Unified Ranked Search Results
            item(key = "results_section_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (query.isBlank()) "QUICK ACCESS & DISCOVERY" else "RESULTS (${results.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Full App Index →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigate(FluxDestination.APP_INDEX) }
                            .testTag("action_field_to_app_index")
                    )
                }
            }

            items(results, key = { it.id }) { res ->
                FluxSurfaceCard(
                    onClick = { onExecuteResult(res) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_result_${res.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (res.packageName != null) {
                            AppIconBadge(
                                packageName = res.packageName,
                                label = res.title,
                                platformManager = platformManager,
                                sizeDp = 42.dp,
                                shapeOption = settings.iconShape
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (res.filterCategory) {
                                        SearchResultFilter.SETTINGS -> Icons.Default.Settings
                                        SearchResultFilter.WEB -> Icons.Default.Language
                                        SearchResultFilter.FILES -> Icons.Default.AttachFile
                                        else -> Icons.Default.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = res.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = res.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = res.categoryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Action Dock (Camera, Import File, Clipboard, App Index) matching Screen 4 of concept
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomQuickTile(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera",
                    onClick = { platformManager.launchCameraIntent() }
                )
                BottomQuickTile(
                    icon = Icons.Default.FolderOpen,
                    label = "Files / PDF",
                    onClick = onPickDocumentToImport
                )
                BottomQuickTile(
                    icon = Icons.Default.ContentPaste,
                    label = "Clipboard",
                    onClick = {
                        val clip = platformManager.readClipboardText()
                        if (!clip.isNullOrBlank()) {
                            onQueryChange(clip)
                        } else {
                            onNotify("Clipboard is currently empty")
                        }
                    }
                )
                BottomQuickTile(
                    icon = Icons.Default.Apps,
                    label = "All Apps",
                    onClick = { onNavigate(FluxDestination.APP_INDEX) }
                )
            }
        }
    }
}

@Composable
private fun SpatialIntentPill(
    label: String,
    colorHex: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val accent = parseHexColorSafe(colorHex)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.08f))
                )
            )
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BottomQuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
