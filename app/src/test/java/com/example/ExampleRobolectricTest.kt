package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ActivityEntity
import com.example.data.local.FluxDatabase
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.FluxSeedData
import com.example.data.local.ObjectCardSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: FluxDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FluxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `manifest registers FLUX as a genuine Android HOME and DEFAULT launcher`() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            setPackage(context.packageName)
        }
        val activities = context.packageManager.queryIntentActivities(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        assertTrue(
            "MainActivity must resolve Intent.ACTION_MAIN with CATEGORY_HOME and CATEGORY_DEFAULT",
            activities.any { it.activityInfo.name.endsWith("MainActivity") }
        )
        assertEquals("FLUX", context.getString(R.string.app_name))
    }

    @Test
    fun `room database persists spaces, continuity activities, objects, and layout coordinates`() = runTest {
        val dao = database.fluxDao()

        // 1. Seed default spaces
        dao.upsertSpaces(FluxSeedData.defaultSpaces())
        val spaces = dao.observeSpaces().first()
        assertEquals(6, spaces.size)

        // 2. Create a custom continuity activity and persist layout transform
        val activity = ActivityEntity(
            id = "act_quantum_lab",
            spaceId = "space_study",
            title = "Quantum Optics Lab",
            summary = "Interferometer alignment & photon statistics",
            progressPercent = 40,
            canvasOffsetXDp = 28f,
            canvasOffsetYDp = 12f,
            cardSize = ObjectCardSize.EXPANDED.name
        )
        dao.upsertActivity(activity)

        // 3. Link objects to the activity
        val linkedNote = FluxObjectEntity(
            id = "obj_q_note",
            activityId = "act_quantum_lab",
            spaceId = "space_study",
            title = "Beam Splitter Matrix",
            subtitle = "Unitary transformation matrix",
            objectType = FluxObjectType.NOTE.name,
            uriOrTarget = "U = 1/sqrt(2) [[1, i], [i, 1]]"
        )
        dao.upsertObject(linkedNote)

        val fetchedActivity = dao.getActivityById("act_quantum_lab")
        assertNotNull(fetchedActivity)
        assertEquals("Quantum Optics Lab", fetchedActivity?.title)
        assertEquals(28f, fetchedActivity?.canvasOffsetXDp ?: 0f, 0.01f)

        val linkedObjects = dao.observeObjectsForActivity("act_quantum_lab").first()
        assertEquals(1, linkedObjects.size)
        assertEquals("Beam Splitter Matrix", linkedObjects.first().title)
    }
}
