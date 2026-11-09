package com.example

import com.example.data.local.ActivityEntity
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.FluxSeedData
import com.example.data.local.TaskItemEntity
import com.example.platform.InstalledAppInfo
import com.example.platform.LocalAssistantEngine
import com.example.platform.SystemSettingShortcut
import com.example.viewmodel.FluxSearchEngine
import com.example.viewmodel.SearchResultFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `search engine ranks exact app match and filters categories accurately`() {
        val sampleApps = listOf(
            InstalledAppInfo(
                packageName = "com.example.notes",
                activityName = "MainActivity",
                label = "Notes Studio",
                autoCategory = "Study",
                isSystemApp = false,
                firstInstallTime = 1000L,
                versionName = "1.2"
            ),
            InstalledAppInfo(
                packageName = "com.example.music",
                activityName = "MainActivity",
                label = "Pulse Music",
                autoCategory = "Media",
                isSystemApp = false,
                firstInstallTime = 1000L,
                versionName = "2.0"
            )
        )
        val appPrefs = mapOf(
            "com.example.notes" to AppPreferenceEntity(
                packageName = "com.example.notes",
                launchCount = 5
            )
        )
        val activities = FluxSeedData.initialActivities()
        val spaces = FluxSeedData.defaultSpaces()
        val objects = FluxSeedData.initialObjects()
        val tasks = FluxSeedData.initialTasks()
        val settings = listOf(
            SystemSettingShortcut("set_wifi", "Wi-Fi & Internet", "Network connections", "android.settings.WIFI_SETTINGS")
        )

        val allResults = FluxSearchEngine.rankResults(
            rawQuery = "physics",
            filter = SearchResultFilter.ALL,
            installedApps = sampleApps,
            appPrefs = appPrefs,
            activities = activities,
            spaces = spaces,
            objects = objects,
            tasks = tasks,
            settingsShortcuts = settings
        )

        assertTrue("Expected physics search to match Physics Revision activity and PDF object", allResults.size >= 2)
        assertTrue(allResults.any { it.title.contains("Physics", ignoreCase = true) })

        val appsOnly = FluxSearchEngine.rankResults(
            rawQuery = "notes",
            filter = SearchResultFilter.APPS,
            installedApps = sampleApps,
            appPrefs = appPrefs,
            activities = activities,
            spaces = spaces,
            objects = objects,
            tasks = tasks,
            settingsShortcuts = settings
        )
        assertEquals(1, appsOnly.size)
        assertEquals("com.example.notes", appsOnly.first().packageName)
    }

    @Test
    fun `local assistant generates checklists, summarizes notes, and parses tasks`() {
        val rawNote = "Verify Maxwell equations\nCheck oscilloscope phase shift\nExport PDF lab report"
        val checklistResp = LocalAssistantEngine.generateChecklistFromText(rawNote)
        assertEquals(3, checklistResp.generatedChecklist.size)
        assertTrue(checklistResp.generatedChecklist.first().contains("Maxwell"))

        val summaryResp = LocalAssistantEngine.summarizeContent("Electrodynamics", rawNote)
        assertTrue(summaryResp.summaryText.contains("Maxwell"))

        val taskResp = LocalAssistantEngine.parseTaskFromInstruction(
            "create task Finish physics exam paper chapter 7",
            FluxSeedData.defaultSpaces()
        )
        assertNotNull(taskResp.suggestedTaskTitle)
        assertEquals("space_study", taskResp.suggestedSpaceId)
    }

    @Test
    fun `contextual action suggestions match object type`() {
        val screenshotObj = FluxObjectEntity(
            id = "obj_test_shot",
            activityId = "act_1",
            spaceId = "space_study",
            title = "Oscilloscope.png",
            subtitle = "Capture",
            objectType = FluxObjectType.SCREENSHOT.name,
            uriOrTarget = "content://media/external/images/1"
        )
        val suggestions = LocalAssistantEngine.suggestActionsForObject(screenshotObj)
        assertFalse(suggestions.isEmpty())
        assertTrue(suggestions.any { it.contains("Extract", ignoreCase = true) || it.contains("Wallpaper", ignoreCase = true) })
    }
}
