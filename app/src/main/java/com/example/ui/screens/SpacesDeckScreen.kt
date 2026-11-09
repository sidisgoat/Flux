package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.PinnedContactEntity
import com.example.data.local.SpaceEntity
import com.example.data.local.SpaceRole
import com.example.data.local.TaskItemEntity
import com.example.platform.DeviceContactItem
import com.example.platform.InstalledAppInfo
import com.example.platform.LauncherPlatformManager
import com.example.ui.components.AppIconBadge
import com.example.ui.components.FluxSurfaceCard
import com.example.ui.components.iconForObjectType
import com.example.ui.components.parseHexColorSafe

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpacesDeckScreen(
    spaces: List<SpaceEntity>,
    activeSpaceId: String,
    activities: List<ActivityEntity>,
    allObjects: List<FluxObjectEntity>,
    tasks: List<TaskItemEntity>,
    pinnedContacts: List<PinnedContactEntity>,
    deviceContacts: List<DeviceContactItem>,
    installedApps: List<InstalledAppInfo>,
    platformManager: LauncherPlatformManager,
    onSelectSpace: (String) -> Unit,
    onOpenActivity: (String) -> Unit,
    onSelectObject: (FluxObjectEntity) -> Unit,
    onCreateSpace: (String, String, String) -> Unit,
    onRenameSpace: (SpaceEntity, String, String) -> Unit,
    onDeleteSpace: (SpaceEntity) -> Unit,
    onCreateActivityInSpace: (String, String, String) -> Unit,
    onAddTaskInSpace: (String, String, String) -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit,
    onPinContact: (String, String, String, String) -> Unit,
    onRemovePinnedContact: (String) -> Unit,
    onRefreshDeviceContacts: () -> Unit,
    onPickMediaOrDocument: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId } ?: spaces.firstOrNull()
    val spaceAccent = parseHexColorSafe(activeSpace?.accentHex ?: "#38BDF8", MaterialTheme.colorScheme.primary)

    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var showEditSpaceDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    var spaceSearchQuery by remember { mutableStateOf("") }
    var selectedMediaCategory by remember { mutableStateOf("ALL") } // ALL, PHOTO, MUSIC, VIDEO, FILES
    var selectedContactGroup by remember { mutableStateOf("All") }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted) onRefreshDeviceContacts()
    }

    LaunchedEffect(activeSpace?.roleKey, hasContactsPermission) {
        if (activeSpace?.roleKey == SpaceRole.PEOPLE.key && hasContactsPermission) {
            onRefreshDeviceContacts()
        }
    }

    val spaceActivities = remember(activities, activeSpaceId, spaceSearchQuery) {
        val q = spaceSearchQuery.trim().lowercase()
        activities.filter { act ->
            act.spaceId == activeSpaceId &&
                (q.isEmpty() || act.title.lowercase().contains(q) || act.summary.lowercase().contains(q))
        }
    }

    val spaceObjects = remember(allObjects, activeSpaceId, spaceSearchQuery) {
        val q = spaceSearchQuery.trim().lowercase()
        allObjects.filter { obj ->
            (obj.spaceId == activeSpaceId || activeSpace?.roleKey == SpaceRole.MEDIA.key) &&
                (q.isEmpty() || obj.title.lowercase().contains(q) || obj.subtitle.lowercase().contains(q))
        }
    }

    val spaceTasks = remember(tasks, activeSpaceId, spaceSearchQuery) {
        val q = spaceSearchQuery.trim().lowercase()
        tasks.filter { t ->
            t.spaceId == activeSpaceId && (q.isEmpty() || t.title.lowercase().contains(q))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("spaces_deck_screen")
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
                    modifier = Modifier.testTag("spaces_deck_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "SPACES ENVIRONMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = spaceAccent
                    )
                    Text(
                        text = activeSpace?.title ?: "Spaces",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (activeSpace != null) {
                    IconButton(onClick = { showEditSpaceDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename Space")
                    }
                    if (!activeSpace.isSystemDefault) {
                        IconButton(onClick = { onDeleteSpace(activeSpace) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Custom Space",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable { showCreateSpaceDialog = true }
                        .testTag("create_space_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Space", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Spatial Deck Cards Carousel (Matches Screen 3 of concept)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spaces_deck_carousel")
        ) {
            items(spaces, key = { it.id }) { space ->
                val selected = space.id == activeSpaceId
                val cardAccent = parseHexColorSafe(space.accentHex, MaterialTheme.colorScheme.primary)
                val actCount = activities.count { it.spaceId == space.id }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) cardAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .width(if (selected) 210.dp else 168.dp)
                        .height(126.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (selected) {
                                    listOf(cardAccent.copy(alpha = 0.28f), MaterialTheme.colorScheme.surface)
                                } else {
                                    listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.surface)
                                }
                            )
                        )
                        .clickable { onSelectSpace(space.id) }
                        .testTag("deck_card_${space.roleKey}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = space.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = space.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$actCount workspaces",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardAccent
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(cardAccent)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Role-specific content inside the selected Space
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeSpace?.roleKey) {
                // --- SECTION 9: PEOPLE SPACE ---
                SpaceRole.PEOPLE.key -> {
                    item(key = "people_space_controls") {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "PEOPLE & COMMUNICATION SHORTCUTS",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = spaceAccent
                                        )
                                        Text(
                                            text = "Pin contacts or groups. Removing a pin never deletes from your address book.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { showAddContactDialog = true },
                                        modifier = Modifier.testTag("pin_contact_button")
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pin")
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                val groups = listOf("All", "Starred", "Study Group", "Team", "Family")
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groups.forEach { grp ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selectedContactGroup == grp) spaceAccent else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { selectedContactGroup = grp }
                                        ) {
                                            Text(
                                                text = grp,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selectedContactGroup == grp) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val visiblePinned = pinnedContacts.filter {
                        selectedContactGroup == "All" || it.groupName.equals(selectedContactGroup, ignoreCase = true)
                    }

                    items(visiblePinned, key = { it.id }) { person ->
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(spaceAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.avatarInitials,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = spaceAccent
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = person.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${person.groupName} · ${person.handleOrNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { platformManager.initiateCommunication("DIAL", person.handleOrNumber) }) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = spaceAccent)
                                }
                                IconButton(onClick = { platformManager.initiateCommunication("SMS", person.handleOrNumber) }) {
                                    Icon(Icons.Default.Sms, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { platformManager.initiateCommunication("EMAIL", person.handleOrNumber) }) {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onRemovePinnedContact(person.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Unpin from FLUX", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Device Address Book Integration (Permission-gated with clear explanation)
                    item(key = "device_contacts_permission_card") {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Contacts, contentDescription = null, tint = spaceAccent)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Device Address Book Discovery",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (!hasContactsPermission) {
                                    Text(
                                        text = "Contacts permission is optional. Grant READ_CONTACTS to search and pin people directly from your phone's address book, or pin contacts manually above.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                                    ) {
                                        Text("Grant Contacts Access")
                                    }
                                } else if (deviceContacts.isEmpty()) {
                                    Text(
                                        text = "Permission granted. No device contacts found in address book.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Tap any device contact below to pin to People Space:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    deviceContacts.take(6).forEach { dc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onPinContact(dc.displayName, dc.phoneNumber, "Starred", "DIAL")
                                                }
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = dc.displayName, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = "+ Pin",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = spaceAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SECTION 10: MEDIA SPACE ---
                SpaceRole.MEDIA.key -> {
                    item(key = "media_space_header") {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "MEDIA & SCOPED REFERENCES",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = spaceAccent
                                        )
                                        Text(
                                            text = "Organize photos, music, videos, and downloads using URI references without duplicating files.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(onClick = onPickMediaOrDocument) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Link Media")
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val filters = listOf(
                                    "ALL" to "All Media",
                                    "PHOTO" to "Photos",
                                    "MUSIC" to "Music",
                                    "VIDEO" to "Videos",
                                    "FILES" to "Files & PDFs"
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filters.forEach { (key, label) ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selectedMediaCategory == key) spaceAccent else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { selectedMediaCategory = key }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selectedMediaCategory == key) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val mediaObjects = allObjects.filter { obj ->
                        when (selectedMediaCategory) {
                            "PHOTO" -> obj.objectType == FluxObjectType.SCREENSHOT.name || obj.mimeType.startsWith("image/")
                            "MUSIC" -> obj.objectType == FluxObjectType.MEDIA.name || obj.mimeType.startsWith("audio/")
                            "VIDEO" -> obj.mimeType.startsWith("video/")
                            "FILES" -> obj.objectType == FluxObjectType.DOCUMENT.name
                            else -> obj.objectType in listOf(
                                FluxObjectType.SCREENSHOT.name,
                                FluxObjectType.MEDIA.name,
                                FluxObjectType.DOCUMENT.name
                            )
                        }
                    }

                    items(mediaObjects, key = { it.id }) { obj ->
                        FluxSurfaceCard(
                            onClick = { onSelectObject(obj) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(spaceAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            obj.mimeType.startsWith("image/") -> Icons.Default.Image
                                            obj.mimeType.startsWith("video/") -> Icons.Default.Movie
                                            obj.mimeType.startsWith("audio/") -> Icons.Default.MusicNote
                                            else -> iconForObjectType(obj.objectType)
                                        },
                                        contentDescription = null,
                                        tint = spaceAccent
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = obj.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${obj.fileSizeLabel.ifBlank { obj.mimeType }} · ${obj.subtitle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { platformManager.openUriOrWebSearch(obj.uriOrTarget, obj.mimeType) }) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open")
                                }
                                IconButton(onClick = { platformManager.shareTextOrUri(obj.title, obj.uriOrTarget, obj.mimeType) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }
                    }
                }

                // --- SECTION 11: STUDY, WORK, PERSONAL, GAMES & CUSTOM SPACES ---
                else -> {
                    item(key = "space_search_and_actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = spaceSearchQuery,
                                onValueChange = { spaceSearchQuery = it },
                                placeholder = { Text("Filter documents, tasks & activities in ${activeSpace?.title ?: "Space"}…") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { showAddTaskDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Task")
                            }
                        }
                    }

                    // Unfinished Activities Overview
                    item(key = "space_activities_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVITIES IN ${activeSpace?.title?.uppercase() ?: "SPACE"} (${spaceActivities.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = spaceAccent
                            )
                            Text(
                                text = "+ Link Document",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onPickMediaOrDocument() }
                            )
                        }
                    }

                    items(spaceActivities, key = { it.id }) { act ->
                        FluxSurfaceCard(
                            accentBorderColor = spaceAccent.copy(alpha = 0.5f),
                            onClick = { onOpenActivity(act.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = act.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${act.progressPercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = spaceAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = act.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (act.progressPercent / 100f).coerceIn(0f, 1f) },
                                    color = spaceAccent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    // Searchable Document, Note & Link Reference List
                    if (spaceObjects.isNotEmpty()) {
                        item(key = "space_objects_header") {
                            Text(
                                text = "PINNED DOCUMENTS, NOTES & LINKS (${spaceObjects.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(spaceObjects, key = { it.id }) { obj ->
                            FluxSurfaceCard(
                                onClick = { onSelectObject(obj) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = iconForObjectType(obj.objectType),
                                        contentDescription = null,
                                        tint = spaceAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = obj.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = obj.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = obj.fileSizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Space Tasks & Deadlines
                    item(key = "space_tasks_card") {
                        FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "TASKS & UPCOMING DEADLINES",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = spaceAccent
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (spaceTasks.isEmpty()) {
                                    Text(
                                        text = "No tasks scheduled for ${activeSpace?.title}. Tap '+ Task' above to add one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    spaceTasks.forEach { task ->
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
                                                tint = if (task.isDone) spaceAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = task.dueLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = spaceAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Relevant Installed Apps for this Space
                    val relevantApps = installedApps.filter { app ->
                        when (activeSpace?.roleKey) {
                            SpaceRole.STUDY.key -> app.autoCategory in listOf("Study", "Work", "Tools")
                            SpaceRole.WORK.key -> app.autoCategory in listOf("Work", "Tools")
                            SpaceRole.GAMES.key -> app.autoCategory == "Games"
                            else -> true
                        }
                    }.take(6)

                    if (relevantApps.isNotEmpty()) {
                        item(key = "space_apps_row") {
                            FluxSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "PREFERRED SPACE SHORTCUTS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        relevantApps.forEach { app ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .clickable { onLaunchApp(app.packageName) }
                                            ) {
                                                AppIconBadge(
                                                    packageName = app.packageName,
                                                    label = app.label,
                                                    platformManager = platformManager,
                                                    sizeDp = 48.dp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = app.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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
        }
    }

    if (showCreateSpaceDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateSpaceDialog = false },
            title = { Text("Create New Space") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Space Name (e.g., Research, Travel)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle (e.g., Papers · Experiments)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreateSpace(title, subtitle, "#38BDF8")
                            showCreateSpaceDialog = false
                        }
                    }
                ) {
                    Text("Create Space")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSpaceDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditSpaceDialog && activeSpace != null) {
        var title by remember(activeSpace.id) { mutableStateOf(activeSpace.title) }
        var subtitle by remember(activeSpace.id) { mutableStateOf(activeSpace.subtitle) }
        AlertDialog(
            onDismissRequest = { showEditSpaceDialog = false },
            title = { Text("Customize Space") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Space Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Space Subtitle") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameSpace(activeSpace, title, subtitle)
                        showEditSpaceDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSpaceDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var group by remember { mutableStateOf("Starred") }
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Pin Person to People Space") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Phone Number or Email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = group,
                        onValueChange = { group = it },
                        label = { Text("Group (Starred, Study Group, Team, Family)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onPinContact(name, number, group, "DIAL")
                            showAddContactDialog = false
                        }
                    }
                ) {
                    Text("Pin Person")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var dueLabel by remember { mutableStateOf("Today · 17:00") }
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Task / Deadline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Description") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueLabel,
                        onValueChange = { dueLabel = it },
                        label = { Text("Deadline Label") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onAddTaskInSpace(taskTitle, dueLabel, activeSpaceId)
                            showAddTaskDialog = false
                        }
                    }
                ) {
                    Text("Save Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            }
        )
    }
}
