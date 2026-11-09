package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.IconShapeOption
import com.example.data.local.ObjectCardSize
import com.example.data.local.SpaceEntity
import com.example.platform.LauncherPlatformManager
import com.example.platform.LocalAssistantEngine
import com.example.ui.theme.LocalFluxVisuals
import kotlin.math.cos
import kotlin.math.sin

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF38BDF8)): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val longVal = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(longVal or 0xFF000000L)
        } else {
            Color(longVal)
        }
    } catch (_: Exception) {
        fallback
    }
}

fun resolveIconShape(option: IconShapeOption): Shape {
    return when (option) {
        IconShapeOption.CIRCLE -> CircleShape
        IconShapeOption.SQUIRCLE -> RoundedCornerShape(percent = 28)
        IconShapeOption.ROUNDED_SQUARE -> RoundedCornerShape(percent = 14)
        IconShapeOption.TEARDROP -> RoundedCornerShape(
            topStartPercent = 50,
            topEndPercent = 50,
            bottomStartPercent = 50,
            bottomEndPercent = 14
        )
        IconShapeOption.HEXAGON -> GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(cx, cy)
            for (i in 0..5) {
                val angle = Math.toRadians((60 * i - 30).toDouble())
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}

fun iconForObjectType(typeName: String): ImageVector {
    return when (runCatching { FluxObjectType.valueOf(typeName) }.getOrDefault(FluxObjectType.NOTE)) {
        FluxObjectType.NOTE -> Icons.Outlined.StickyNote2
        FluxObjectType.DOCUMENT -> Icons.Default.Description
        FluxObjectType.LINK -> Icons.Default.Link
        FluxObjectType.SHORTCUT -> Icons.Outlined.Widgets
        FluxObjectType.CHECKLIST -> Icons.Default.Checklist
        FluxObjectType.SCREENSHOT -> Icons.Default.Image
        FluxObjectType.MEDIA -> Icons.Default.MusicNote
        FluxObjectType.CONTACT -> Icons.Default.Person
        FluxObjectType.LOCATION -> Icons.AutoMirrored.Filled.OpenInNew
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FluxSurfaceCard(
    modifier: Modifier = Modifier,
    accentBorderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val visuals = LocalFluxVisuals.current
    val shape = RoundedCornerShape(visuals.cornerRadiusDp.dp)
    val borderColor = accentBorderColor ?: MaterialTheme.colorScheme.outline.copy(
        alpha = if (visuals.showThinBorders) 0.65f else 0f
    )
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = visuals.surfaceOpacity)

    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = surfaceColor,
        border = if (visuals.showThinBorders || accentBorderColor != null) {
            BorderStroke(1.dp, borderColor)
        } else null
    ) {
        content()
    }
}

@Composable
fun AppIconBadge(
    packageName: String,
    label: String,
    platformManager: LauncherPlatformManager,
    sizeDp: Dp = 50.dp,
    shapeOption: IconShapeOption = IconShapeOption.SQUIRCLE,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val bitmap = remember(packageName) { platformManager.getAppIconBitmap(packageName) }
    val shape = remember(shapeOption) { resolveIconShape(shapeOption) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier
                    .size(sizeDp * 0.78f)
                    .clip(shape)
            )
        } else {
            val monogram = label.trim().take(2).uppercase().ifBlank { "AP" }
            Text(
                text = monogram,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContextObjectActionSheet(
    fluxObject: FluxObjectEntity,
    spaces: List<SpaceEntity>,
    activities: List<ActivityEntity>,
    enableAssistant: Boolean,
    onDismiss: () -> Unit,
    onOpenTarget: (FluxObjectEntity) -> Unit,
    onShare: (FluxObjectEntity) -> Unit,
    onCopyContent: (FluxObjectEntity) -> Unit,
    onSetAsWallpaper: () -> Unit,
    onSaveEdits: (String, String, String?, String, ObjectCardSize, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onSummarizeWithAssistant: (FluxObjectEntity) -> Unit,
    onExplainWithAssistant: (FluxObjectEntity) -> Unit,
    onCreateNoteFromObject: (FluxObjectEntity) -> Unit
) {
    var isEditing by remember(fluxObject.id) { mutableStateOf(false) }
    var editedTitle by remember(fluxObject.id) { mutableStateOf(fluxObject.title) }
    var editedBody by remember(fluxObject.id) { mutableStateOf(fluxObject.uriOrTarget) }
    var selectedSpaceId by remember(fluxObject.id) { mutableStateOf(fluxObject.spaceId) }
    var selectedActivityId by remember(fluxObject.id) { mutableStateOf(fluxObject.activityId) }
    var selectedSize by remember(fluxObject.id) {
        mutableStateOf(
            runCatching { ObjectCardSize.valueOf(fluxObject.sizeMode) }.getOrDefault(ObjectCardSize.MEDIUM)
        )
    }
    var isCompleted by remember(fluxObject.id) { mutableStateOf(fluxObject.isCompleted) }

    val objType = runCatching { FluxObjectType.valueOf(fluxObject.objectType) }.getOrDefault(FluxObjectType.NOTE)
    val aiSuggestions = remember(fluxObject.id) { LocalAssistantEngine.suggestActionsForObject(fluxObject) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("context_actions_sheet")
        ) {
            // Top Object Preview Header (Matches Screen 5 of reference)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForObjectType(fluxObject.objectType),
                        contentDescription = objType.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1 item selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = fluxObject.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${objType.label}${if (fluxObject.fileSizeLabel.isNotBlank()) " · ${fluxObject.fileSizeLabel}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_context_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close contextual actions")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Object Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedBody,
                    onValueChange = { editedBody = it },
                    label = { Text("Content / URL / Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Assign to Space",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spaces.forEach { sp ->
                        val active = sp.id == selectedSpaceId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedSpaceId = sp.id }
                        ) {
                            Text(
                                text = sp.title,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Link to Continuity Activity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activities.take(5).forEach { act ->
                        val active = act.id == selectedActivityId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedActivityId = act.id }
                        ) {
                            Text(
                                text = act.title.take(22),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSaveEdits(
                                editedTitle,
                                editedBody,
                                selectedActivityId,
                                selectedSpaceId,
                                selectedSize,
                                isCompleted
                            )
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes")
                    }
                }
            } else {
                // Content excerpt preview box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = fluxObject.uriOrTarget.ifBlank { fluxObject.subtitle },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Circular Action Row: Open, Share, Edit, Copy/Extract, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ContextualActionCircle(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Open",
                        tag = "context_action_open",
                        onClick = { onOpenTarget(fluxObject) }
                    )
                    ContextualActionCircle(
                        icon = Icons.Default.Share,
                        label = "Share",
                        tag = "context_action_share",
                        onClick = { onShare(fluxObject) }
                    )
                    ContextualActionCircle(
                        icon = Icons.Default.Edit,
                        label = "Edit / Move",
                        tag = "context_action_edit",
                        onClick = { isEditing = true }
                    )
                    ContextualActionCircle(
                        icon = Icons.Default.ContentCopy,
                        label = if (objType == FluxObjectType.SCREENSHOT) "Extract Text" else "Copy",
                        tag = "context_action_copy",
                        onClick = { onCopyContent(fluxObject) }
                    )
                    if (objType == FluxObjectType.SCREENSHOT) {
                        ContextualActionCircle(
                            icon = Icons.Default.Wallpaper,
                            label = "Wallpaper",
                            tag = "context_action_wallpaper",
                            onClick = onSetAsWallpaper
                        )
                    } else {
                        ContextualActionCircle(
                            icon = Icons.Default.DeleteOutline,
                            label = "Remove",
                            tag = "context_action_delete",
                            onClick = { onDelete(fluxObject.id) }
                        )
                    }
                }

                if (enableAssistant) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Assistant & Continuity Actions (Local)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AssistantActionRow(
                        title = "Summarize this",
                        subtitle = "Generate concise local bullet points & checklist",
                        onClick = { onSummarizeWithAssistant(fluxObject) }
                    )
                    AssistantActionRow(
                        title = "Explain / Analyze this",
                        subtitle = "Break down formulas, telemetry, or technical concepts",
                        onClick = { onExplainWithAssistant(fluxObject) }
                    )
                    AssistantActionRow(
                        title = "Create linked note",
                        subtitle = "Duplicate excerpt into an editable study/work note",
                        onClick = { onCreateNoteFromObject(fluxObject) }
                    )

                    if (aiSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Suggested next: ${aiSuggestions.first()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContextualActionCircle(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AssistantActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.DriveFileMove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
