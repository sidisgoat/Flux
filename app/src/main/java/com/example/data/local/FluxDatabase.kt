package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SpaceEntity::class,
        ActivityEntity::class,
        FluxObjectEntity::class,
        TaskItemEntity::class,
        PinnedContactEntity::class,
        HostedWidgetEntity::class,
        RecentEventEntity::class,
        AppPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun fluxDao(): FluxDao

    companion object {
        @Volatile
        private var INSTANCE: FluxDatabase? = null

        fun getInstance(context: Context): FluxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FluxDatabase::class.java,
                    "flux_launcher.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object FluxSeedData {
    fun defaultSpaces(): List<SpaceEntity> = listOf(
        SpaceEntity(
            id = "space_personal",
            title = "Personal",
            subtitle = "Home · Life · People",
            roleKey = SpaceRole.PERSONAL.key,
            iconName = "Home",
            accentHex = "#38BDF8",
            layoutMode = "CANVAS",
            sortOrder = 0,
            isSystemDefault = true
        ),
        SpaceEntity(
            id = "space_study",
            title = "Study",
            subtitle = "Revision · Papers · Formulas",
            roleKey = SpaceRole.STUDY.key,
            iconName = "School",
            accentHex = "#F59E0B",
            layoutMode = "CANVAS",
            sortOrder = 1,
            isSystemDefault = true
        ),
        SpaceEntity(
            id = "space_work",
            title = "Work",
            subtitle = "Projects · Specs · Deep Focus",
            roleKey = SpaceRole.WORK.key,
            iconName = "Work",
            accentHex = "#10B981",
            layoutMode = "LIST",
            sortOrder = 2,
            isSystemDefault = true
        ),
        SpaceEntity(
            id = "space_media",
            title = "Media",
            subtitle = "Audio · Visuals · Captures",
            roleKey = SpaceRole.MEDIA.key,
            iconName = "GraphicEq",
            accentHex = "#A855F7",
            layoutMode = "GRID",
            sortOrder = 3,
            isSystemDefault = true
        ),
        SpaceEntity(
            id = "space_people",
            title = "People",
            subtitle = "Direct Channels · Groups",
            roleKey = SpaceRole.PEOPLE.key,
            iconName = "Group",
            accentHex = "#F43F5E",
            layoutMode = "GRID",
            sortOrder = 4,
            isSystemDefault = true
        ),
        SpaceEntity(
            id = "space_games",
            title = "Games",
            subtitle = "Play · Sessions · Performance",
            roleKey = SpaceRole.GAMES.key,
            iconName = "SportsEsports",
            accentHex = "#38BDF8",
            layoutMode = "GRID",
            sortOrder = 5,
            isSystemDefault = true
        )
    )

    fun initialActivities(now: Long = System.currentTimeMillis()): List<ActivityEntity> = listOf(
        ActivityEntity(
            id = "act_physics_revision",
            spaceId = "space_study",
            title = "Physics Revision: Electromagnetism",
            summary = "Maxwell equations, wave propagation PDF, lab derivation notes & problem set.",
            statusLabel = "In Progress · Sample",
            progressPercent = 68,
            isPinnedHome = true,
            canvasOrder = 0,
            canvasOffsetXDp = 0f,
            canvasOffsetYDp = 0f,
            cardSize = ObjectCardSize.EXPANDED.name,
            accentHex = "#F59E0B",
            isSampleContent = true,
            lastUpdatedEpochMs = now - 600_000L
        ),
        ActivityEntity(
            id = "act_system_design",
            spaceId = "space_work",
            title = "OS Interaction & Spatial UI Spec",
            summary = "Latency budgets, frame pacing benchmarks, and typography tokens.",
            statusLabel = "Active · Sample",
            progressPercent = 82,
            isPinnedHome = true,
            canvasOrder = 1,
            canvasOffsetXDp = 12f,
            canvasOffsetYDp = 8f,
            cardSize = ObjectCardSize.MEDIUM.name,
            accentHex = "#10B981",
            isSampleContent = true,
            lastUpdatedEpochMs = now - 1_800_000L
        ),
        ActivityEntity(
            id = "act_weekend_trip",
            spaceId = "space_personal",
            title = "Kyoto Autumn Itinerary & Transit",
            summary = "Rail pass reservation, studio checklist, and offline phrases.",
            statusLabel = "Planning · Sample",
            progressPercent = 45,
            isPinnedHome = true,
            canvasOrder = 2,
            canvasOffsetXDp = -8f,
            canvasOffsetYDp = 4f,
            cardSize = ObjectCardSize.MEDIUM.name,
            accentHex = "#38BDF8",
            isSampleContent = true,
            lastUpdatedEpochMs = now - 3_600_000L
        ),
        ActivityEntity(
            id = "act_sound_design",
            spaceId = "space_media",
            title = "Ambient Field Recordings",
            summary = "Binaural rain stems, mastering chain reference, and waveform screenshot.",
            statusLabel = "Session · Sample",
            progressPercent = 54,
            isPinnedHome = false,
            canvasOrder = 3,
            cardSize = ObjectCardSize.MEDIUM.name,
            accentHex = "#A855F7",
            isSampleContent = true,
            lastUpdatedEpochMs = now - 7_200_000L
        )
    )

    fun initialObjects(now: Long = System.currentTimeMillis()): List<FluxObjectEntity> = listOf(
        FluxObjectEntity(
            id = "obj_phys_pdf",
            activityId = "act_physics_revision",
            spaceId = "space_study",
            title = "Physics_Notes_Electrodynamics.pdf",
            subtitle = "Chapter 7 · Faraday & Maxwell Boundary Conditions",
            objectType = FluxObjectType.DOCUMENT.name,
            uriOrTarget = "https://ocw.mit.edu/courses/8-02-physics-ii-electricity-and-magnetism-spring-2007/",
            mimeType = "application/pdf",
            groupLabel = "Reference Material",
            sizeMode = ObjectCardSize.MEDIUM.name,
            sortOrder = 0,
            isSampleContent = true,
            fileSizeLabel = "4.2 MB",
            lastAccessedEpochMs = now - 300_000L
        ),
        FluxObjectEntity(
            id = "obj_phys_formula",
            activityId = "act_physics_revision",
            spaceId = "space_study",
            title = "Curl of E & B Field Derivation",
            subtitle = "∇ × E = -∂B/∂t  |  ∇ × B = μ₀J + μ₀ε₀∂E/∂t — Verify boundary continuity at dielectric interface.",
            objectType = FluxObjectType.NOTE.name,
            uriOrTarget = "∇ × E = -∂B/∂t\n∇ × B = μ₀J + μ₀ε₀∂E/∂t\nRemember: Tangential E is continuous across sourceless boundary; normal D jumps by free surface charge σ_f.",
            mimeType = "text/plain",
            groupLabel = "Formulas & Notes",
            sizeMode = ObjectCardSize.EXPANDED.name,
            sortOrder = 1,
            isSampleContent = true,
            fileSizeLabel = "1.1 KB",
            lastAccessedEpochMs = now - 450_000L
        ),
        FluxObjectEntity(
            id = "obj_phys_screenshot",
            activityId = "act_physics_revision",
            spaceId = "space_study",
            title = "Lab_Oscilloscope_Screenshot.png",
            subtitle = "Phase shift measurement across RLC resonant tank circuit (f₀ = 14.2 kHz)",
            objectType = FluxObjectType.SCREENSHOT.name,
            uriOrTarget = "Phase shift measurement: V_in peak = 4.8V, V_out peak = 3.9V, Δφ = 18.4 deg, Q-factor = 6.4",
            mimeType = "image/png",
            groupLabel = "Captures",
            sizeMode = ObjectCardSize.MEDIUM.name,
            sortOrder = 2,
            isSampleContent = true,
            fileSizeLabel = "2.4 MB",
            lastAccessedEpochMs = now - 520_000L
        ),
        FluxObjectEntity(
            id = "obj_phys_link",
            activityId = "act_physics_revision",
            spaceId = "space_study",
            title = "Feynman Lectures Vol. II Ch. 18",
            subtitle = "https://www.feynmanlectures.caltech.edu/II_18.html",
            objectType = FluxObjectType.LINK.name,
            uriOrTarget = "https://www.feynmanlectures.caltech.edu/II_18.html",
            mimeType = "text/uri-list",
            groupLabel = "Web Sources",
            sizeMode = ObjectCardSize.COMPACT.name,
            sortOrder = 3,
            isSampleContent = true,
            fileSizeLabel = "URL",
            lastAccessedEpochMs = now - 800_000L
        ),
        FluxObjectEntity(
            id = "obj_spec_note",
            activityId = "act_system_design",
            spaceId = "space_work",
            title = "Activity-First Navigation Principles",
            subtitle = "Users remember what they were working on, not which icon holds the file.",
            objectType = FluxObjectType.NOTE.name,
            uriOrTarget = "1. Zero mandatory icon grid on Home.\n2. Bind files, URLs, notes, and shortcuts into persistent Continuity Activities.\n3. Keep frame times < 8ms on 120Hz displays.",
            mimeType = "text/plain",
            groupLabel = "Core Spec",
            sizeMode = ObjectCardSize.EXPANDED.name,
            sortOrder = 0,
            isSampleContent = true,
            fileSizeLabel = "2.0 KB",
            lastAccessedEpochMs = now - 900_000L
        ),
        FluxObjectEntity(
            id = "obj_spec_link",
            activityId = "act_system_design",
            spaceId = "space_work",
            title = "Android AppWidgetHost Architecture",
            subtitle = "https://developer.android.com/develop/ui/views/appwidgets/host",
            objectType = FluxObjectType.LINK.name,
            uriOrTarget = "https://developer.android.com/develop/ui/views/appwidgets/host",
            mimeType = "text/uri-list",
            groupLabel = "Documentation",
            sizeMode = ObjectCardSize.MEDIUM.name,
            sortOrder = 1,
            isSampleContent = true,
            fileSizeLabel = "URL",
            lastAccessedEpochMs = now - 1_200_000L
        ),
        FluxObjectEntity(
            id = "obj_kyoto_checklist",
            activityId = "act_weekend_trip",
            spaceId = "space_personal",
            title = "Pre-Departure Essentials",
            subtitle = "Passport, ICoca card, 35mm lens, USB-C PD adapter",
            objectType = FluxObjectType.CHECKLIST.name,
            uriOrTarget = "- [x] Renew JR Kansai Pass\n- [x] Pack 35mm f/1.4 prime lens\n- [ ] Download offline Kansai rail map\n- [ ] Confirm Arashiyama morning reservation",
            mimeType = "text/plain",
            groupLabel = "Logistics",
            sizeMode = ObjectCardSize.MEDIUM.name,
            sortOrder = 0,
            isSampleContent = true,
            fileSizeLabel = "4 items",
            lastAccessedEpochMs = now - 1_500_000L
        )
    )

    fun initialTasks(now: Long = System.currentTimeMillis()): List<TaskItemEntity> = listOf(
        TaskItemEntity(
            id = "task_1",
            spaceId = "space_study",
            activityId = "act_physics_revision",
            title = "Complete Problem Set 4: Poynting Vector Flux",
            dueLabel = "Today · 18:00",
            isDone = false,
            priority = 2,
            isSampleContent = true,
            createdAtEpochMs = now - 500_000L
        ),
        TaskItemEntity(
            id = "task_2",
            spaceId = "space_work",
            activityId = "act_system_design",
            title = "Verify AppWidgetHost binding & resize persistence",
            dueLabel = "Today · 16:30",
            isDone = true,
            priority = 2,
            isSampleContent = true,
            createdAtEpochMs = now - 700_000L
        ),
        TaskItemEntity(
            id = "task_3",
            spaceId = "space_personal",
            activityId = "act_weekend_trip",
            title = "Export offline transit passes to Study/Personal Space",
            dueLabel = "Tomorrow",
            isDone = false,
            priority = 1,
            isSampleContent = true,
            createdAtEpochMs = now - 900_000L
        )
    )

    fun initialWidgets(): List<HostedWidgetEntity> = listOf(
        HostedWidgetEntity(
            id = "widget_native_pulse",
            spaceId = "space_personal",
            appWidgetId = -1,
            widgetKind = "NATIVE_PULSE",
            label = "Daily Continuity Pulse",
            heightDp = 148,
            spanColumns = 1,
            sortOrder = 0
        ),
        HostedWidgetEntity(
            id = "widget_native_weather",
            spaceId = "space_personal",
            appWidgetId = -1,
            widgetKind = "NATIVE_WEATHER",
            label = "Atmospheric Glance",
            heightDp = 148,
            spanColumns = 1,
            sortOrder = 1
        ),
        HostedWidgetEntity(
            id = "widget_native_tasks",
            spaceId = "space_study",
            appWidgetId = -1,
            widgetKind = "NATIVE_TASKS",
            label = "Active Checklist",
            heightDp = 180,
            spanColumns = 2,
            sortOrder = 2
        ),
        HostedWidgetEntity(
            id = "widget_native_focus",
            spaceId = "space_work",
            appWidgetId = -1,
            widgetKind = "NATIVE_FOCUS",
            label = "Device Energy & Focus",
            heightDp = 148,
            spanColumns = 1,
            sortOrder = 3
        )
    )
}
