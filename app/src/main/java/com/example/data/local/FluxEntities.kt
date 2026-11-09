package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SpaceRole(val key: String, val defaultTitle: String) {
    PERSONAL("personal", "Personal"),
    STUDY("study", "Study"),
    WORK("work", "Work"),
    MEDIA("media", "Media"),
    GAMES("games", "Games"),
    PEOPLE("people", "People"),
    CUSTOM("custom", "Custom")
}

enum class FluxObjectType(val label: String) {
    NOTE("Note"),
    DOCUMENT("PDF / Document"),
    LINK("Web Link"),
    SHORTCUT("App Shortcut"),
    CHECKLIST("Checklist"),
    SCREENSHOT("Screenshot / Image"),
    MEDIA("Audio / Video"),
    CONTACT("Contact Shortcut"),
    LOCATION("Location")
}

enum class ObjectCardSize(val label: String, val weightSpan: Int) {
    COMPACT("Compact", 1),
    MEDIUM("Standard", 2),
    EXPANDED("Expanded", 3)
}

@Entity(
    tableName = "flux_spaces",
    indices = [Index(value = ["sortOrder"])]
)
data class SpaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val roleKey: String,
    val iconName: String,
    val accentHex: String,
    val layoutMode: String = "CANVAS", // CANVAS, LIST, GRID
    val sortOrder: Int = 0,
    val isSystemDefault: Boolean = false
)

@Entity(
    tableName = "flux_activities",
    indices = [Index(value = ["spaceId"]), Index(value = ["isPinnedHome"])]
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val title: String,
    val summary: String,
    val statusLabel: String = "Active", // Active, Paused, Completed, Reference
    val progressPercent: Int = 0,
    val isPinnedHome: Boolean = true,
    val canvasOrder: Int = 0,
    val canvasOffsetXDp: Float = 0f,
    val canvasOffsetYDp: Float = 0f,
    val cardSize: String = ObjectCardSize.MEDIUM.name,
    val accentHex: String = "#38BDF8",
    val isSampleContent: Boolean = false,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flux_objects",
    indices = [Index(value = ["activityId"]), Index(value = ["spaceId"])]
)
data class FluxObjectEntity(
    @PrimaryKey val id: String,
    val activityId: String?, // Nullable if placed directly in a Space or Home
    val spaceId: String,
    val title: String,
    val subtitle: String,
    val objectType: String, // FluxObjectType.name
    val uriOrTarget: String, // Document URI, URL, package name, or internal note text
    val mimeType: String = "text/plain",
    val groupLabel: String = "Core",
    val sizeMode: String = ObjectCardSize.MEDIUM.name,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val isSampleContent: Boolean = false,
    val fileSizeLabel: String = "",
    val lastAccessedEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flux_tasks",
    indices = [Index(value = ["spaceId"]), Index(value = ["activityId"])]
)
data class TaskItemEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val activityId: String? = null,
    val title: String,
    val dueLabel: String = "Today",
    val isDone: Boolean = false,
    val priority: Int = 1, // 0 = Low, 1 = Normal, 2 = High
    val isSampleContent: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flux_pinned_contacts",
    indices = [Index(value = ["groupName"])]
)
data class PinnedContactEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val handleOrNumber: String,
    val contactUri: String = "",
    val groupName: String = "Starred",
    val preferredAction: String = "DIAL", // DIAL, SMS, EMAIL, VIEW
    val avatarInitials: String = "FX",
    val isPinnedHome: Boolean = true,
    val isSampleContent: Boolean = false,
    val lastContactedEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flux_widgets",
    indices = [Index(value = ["spaceId"])]
)
data class HostedWidgetEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val appWidgetId: Int, // -1 for native FLUX interactive widgets, >=0 for Android AppWidgetHost widgets
    val widgetKind: String, // NATIVE_WEATHER, NATIVE_TASKS, NATIVE_MEDIA, NATIVE_PULSE, NATIVE_FOCUS, ANDROID_APPWIDGET
    val providerFlatten: String = "",
    val label: String,
    val heightDp: Int = 148,
    val spanColumns: Int = 1, // 1 = Half width, 2 = Full width
    val sortOrder: Int = 0
)

@Entity(
    tableName = "flux_recent_events",
    indices = [Index(value = ["timestampEpochMs"])]
)
data class RecentEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val eventKind: String, // RESUME_ACTIVITY, OPEN_OBJECT, LAUNCH_APP, EDIT_NOTE
    val targetIdOrPackage: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flux_app_preferences"
)
data class AppPreferenceEntity(
    @PrimaryKey val packageName: String,
    val customCategory: String = "", // Social, Media, Study, Work, Games, Tools, Shopping
    val isHidden: Boolean = false,
    val isDocked: Boolean = false,
    val dockOrder: Int = 0,
    val isPinnedToSpaceId: String = "",
    val launchCount: Int = 0,
    val lastLaunchedEpochMs: Long = 0L
)
