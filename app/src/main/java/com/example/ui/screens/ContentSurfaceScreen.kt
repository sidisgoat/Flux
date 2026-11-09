package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.ObjectCardSize
import com.example.data.local.SpaceEntity
import com.example.data.local.TaskItemEntity
import com.example.ui.components.FluxSurfaceCard
import com.example.ui.components.iconForObjectType
import com.example.ui.components.parseHexColorSafe

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentSurfaceScreen(
    activity: ActivityEntity?,
    spaces: List<SpaceEntity>,
    connectedObjects: List<FluxObjectEntity>,
    activityTasks: List<TaskItemEntity>,
    onBack: () -> Unit,
    onSelectObject: (FluxObjectEntity) -> Unit,
    onOpenObjectDirect: (FluxObjectEntity) -> Unit,
    onAddNoteOrLink: (String, String, FluxObjectType) -> Unit,
    onPickDocumentFile: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit,
    onUpdateActivity: (ActivityEntity, String, String, String, Int, Boolean, ObjectCardSize) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onAskAssistantForActivity: (FluxObjectEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    if (activity == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Activity workspace not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Return Home") }
            }
        }
        return
    }

    var isCompactListMode by remember { mutableStateOf(false) }
    var showAddObjectDialog by remember { mutableStateOf<FluxObjectType?>(null) }
    var showEditActivityDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }

    val actAccent = parseHexColorSafe(activity.accentHex, MaterialTheme.colorScheme.primary)
    val groupedObjects = remember(connectedObjects) {
        connectedObjects.groupBy { it.groupLabel.ifBlank { "Workspace Objects" } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("content_surface_screen")
    ) {
        // Top Bar
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
                    modifier = Modifier.testTag("content_surface_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
                }
                Column {
                    Text(
                        text = "CONTENT SURFACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = actAccent
                    )
                    Text(
                        text = activity.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { isCompactListMode = !isCompactListMode }) {
                    Icon(
                        imageVector = if (isCompactListMode) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Toggle Compact List / Spatial View"
                    )
                }
                IconButton(
                    onClick = { showEditActivityDialog = true },
                    modifier = Modifier.testTag("edit_activity_workspace_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Activity")
                }
                IconButton(
                    onClick = { onDeleteActivity(activity.id) },
                    modifier = Modifier.testTag("delete_activity_workspace_button")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Activity",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Activity Hero Header
            item(key = "workspace_hero") {
                FluxSurfaceCard(
                    accentBorderColor = actAccent.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activity.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continuity Progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${activity.progressPercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = actAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (activity.progressPercent / 100f).coerceIn(0f, 1f) },
                            color = actAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        // Quick Add Object Action Bar
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkspaceAddButton(
                                icon = Icons.Outlined.StickyNote2,
                                label = "+ Note",
                                tag = "workspace_add_note_button",
                                onClick = { showAddObjectDialog = FluxObjectType.NOTE }
                            )
                            WorkspaceAddButton(
                                icon = Icons.Default.AttachFile,
                                label = "+ File / PDF",
                                tag = "workspace_add_file_button",
                                onClick = onPickDocumentFile
                            )
                            WorkspaceAddButton(
                                icon = Icons.Default.Link,
                                label = "+ Web Link",
                                tag = "workspace_add_link_button",
                                onClick = { showAddObjectDialog = FluxObjectType.LINK }
                            )
                            WorkspaceAddButton(
                                icon = Icons.Default.Checklist,
                                label = "+ Checklist",
                                tag = "workspace_add_checklist_button",
                                onClick = { showAddObjectDialog = FluxObjectType.CHECKLIST }
                            )
                        }
                    }
                }
            }

            // Grouped Connected Objects
            groupedObjects.forEach { (groupName, objectsInGroup) ->
                item(key = "group_header_$groupName") {
                    Text(
                        text = groupName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(objectsInGroup, key = { it.id }) { obj ->
                    val objType = runCatching { FluxObjectType.valueOf(obj.objectType) }.getOrDefault(FluxObjectType.NOTE)
                    FluxSurfaceCard(
                        onClick = { onSelectObject(obj) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workspace_object_${obj.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(actAccent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconForObjectType(obj.objectType),
                                        contentDescription = objType.label,
                                        tint = actAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = obj.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${objType.label}${if (obj.fileSizeLabel.isNotBlank()) " · ${obj.fileSizeLabel}" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onOpenObjectDirect(obj) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open target",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onAskAssistantForActivity(obj) }) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Summarize with Local Assistant",
                                        tint = actAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (!isCompactListMode) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = obj.uriOrTarget.ifBlank { obj.subtitle },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Activity Tasks & Action Items
            item(key = "workspace_tasks_section") {
                FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "WORKSPACE ACTION ITEMS",
                            style = MaterialTheme.typography.labelMedium,
                            color = actAccent
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTaskTitle,
                                onValueChange = { newTaskTitle = it },
                                placeholder = { Text("Add a task to this activity…") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("workspace_new_task_input")
                            )
                            Button(
                                onClick = {
                                    if (newTaskTitle.isNotBlank()) {
                                        onAddTask(newTaskTitle)
                                        newTaskTitle = ""
                                    }
                                },
                                modifier = Modifier.testTag("workspace_add_task_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Task")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        activityTasks.forEach { task ->
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
                                    tint = if (task.isDone) actAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Note / Link / Checklist Dialog
    showAddObjectDialog?.let { objType ->
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddObjectDialog = null },
            title = { Text("Add ${objType.label} to Activity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = {
                            Text(
                                when (objType) {
                                    FluxObjectType.LINK -> "https:// URL"
                                    FluxObjectType.CHECKLIST -> "Checklist items (one per line)"
                                    else -> "Note content or formula"
                                }
                            )
                        },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNoteOrLink(title, body, objType)
                        showAddObjectDialog = null
                    }
                ) {
                    Text("Add to Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddObjectDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditActivityDialog) {
        var title by remember(activity.id) { mutableStateOf(activity.title) }
        var summary by remember(activity.id) { mutableStateOf(activity.summary) }
        var spaceId by remember(activity.id) { mutableStateOf(activity.spaceId) }
        var progress by remember(activity.id) { mutableFloatStateOf(activity.progressPercent.toFloat()) }
        var pinnedHome by remember(activity.id) { mutableStateOf(activity.isPinnedHome) }
        var size by remember(activity.id) {
            mutableStateOf(runCatching { ObjectCardSize.valueOf(activity.cardSize) }.getOrDefault(ObjectCardSize.MEDIUM))
        }

        AlertDialog(
            onDismissRequest = { showEditActivityDialog = false },
            title = { Text("Configure Activity Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Activity Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Summary") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Progress: ${progress.toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        valueRange = 0f..100f
                    )
                    Text(
                        text = "Move to Space:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        spaces.forEach { sp ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sp.id == spaceId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { spaceId = sp.id }
                            ) {
                                Text(
                                    text = sp.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateActivity(
                            activity,
                            title,
                            summary,
                            spaceId,
                            progress.toInt(),
                            pinnedHome,
                            size
                        )
                        showEditActivityDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditActivityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WorkspaceAddButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
