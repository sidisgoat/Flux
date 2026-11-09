package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FluxDao {

    // --- Spaces ---
    @Query("SELECT * FROM flux_spaces ORDER BY sortOrder ASC")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM flux_spaces ORDER BY sortOrder ASC")
    suspend fun getSpacesList(): List<SpaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpace(space: SpaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpaces(spaces: List<SpaceEntity>)

    @Query("DELETE FROM flux_spaces WHERE id = :spaceId AND isSystemDefault = 0")
    suspend fun deleteCustomSpace(spaceId: String)

    // --- Activities ---
    @Query("SELECT * FROM flux_activities ORDER BY canvasOrder ASC, lastUpdatedEpochMs DESC")
    fun observeAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM flux_activities WHERE spaceId = :spaceId ORDER BY canvasOrder ASC, lastUpdatedEpochMs DESC")
    fun observeActivitiesForSpace(spaceId: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM flux_activities WHERE id = :activityId LIMIT 1")
    suspend fun getActivityById(activityId: String): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(activities: List<ActivityEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM flux_activities WHERE id = :activityId")
    suspend fun deleteActivity(activityId: String)

    @Query("DELETE FROM flux_activities WHERE isSampleContent = 1")
    suspend fun deleteSampleActivities()

    @Query("DELETE FROM flux_activities")
    suspend fun deleteAllActivities()

    // --- Connected Objects ---
    @Query("SELECT * FROM flux_objects ORDER BY sortOrder ASC, lastAccessedEpochMs DESC")
    fun observeAllObjects(): Flow<List<FluxObjectEntity>>

    @Query("SELECT * FROM flux_objects WHERE activityId = :activityId ORDER BY sortOrder ASC")
    fun observeObjectsForActivity(activityId: String): Flow<List<FluxObjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObject(obj: FluxObjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObjects(objects: List<FluxObjectEntity>)

    @Query("DELETE FROM flux_objects WHERE id = :objectId")
    suspend fun deleteObject(objectId: String)

    @Query("DELETE FROM flux_objects WHERE activityId = :activityId")
    suspend fun deleteObjectsForActivity(activityId: String)

    @Query("DELETE FROM flux_objects WHERE isSampleContent = 1")
    suspend fun deleteSampleObjects()

    @Query("DELETE FROM flux_objects WHERE objectType IN ('DOCUMENT', 'SCREENSHOT', 'MEDIA')")
    suspend fun clearStoredFileReferences()

    // --- Tasks & Checklists ---
    @Query("SELECT * FROM flux_tasks ORDER BY isDone ASC, priority DESC, createdAtEpochMs DESC")
    fun observeAllTasks(): Flow<List<TaskItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskItemEntity>)

    @Query("DELETE FROM flux_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM flux_tasks WHERE isSampleContent = 1")
    suspend fun deleteSampleTasks()

    // --- Pinned Contacts (People Space) ---
    @Query("SELECT * FROM flux_pinned_contacts ORDER BY lastContactedEpochMs DESC")
    fun observePinnedContacts(): Flow<List<PinnedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPinnedContact(contact: PinnedContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPinnedContacts(contacts: List<PinnedContactEntity>)

    @Query("DELETE FROM flux_pinned_contacts WHERE id = :contactId")
    suspend fun deletePinnedContact(contactId: String)

    @Query("DELETE FROM flux_pinned_contacts WHERE isSampleContent = 1")
    suspend fun deleteSampleContacts()

    // --- Hosted Widgets ---
    @Query("SELECT * FROM flux_widgets ORDER BY sortOrder ASC")
    fun observeWidgets(): Flow<List<HostedWidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidget(widget: HostedWidgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgets(widgets: List<HostedWidgetEntity>)

    @Query("DELETE FROM flux_widgets WHERE id = :widgetId")
    suspend fun deleteWidget(widgetId: String)

    // --- Recent Continuity Events ---
    @Query("SELECT * FROM flux_recent_events ORDER BY timestampEpochMs DESC LIMIT 25")
    fun observeRecentEvents(): Flow<List<RecentEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentEvent(event: RecentEventEntity)

    @Query("DELETE FROM flux_recent_events")
    suspend fun clearRecentEvents()

    // --- App Preferences ---
    @Query("SELECT * FROM flux_app_preferences")
    fun observeAppPreferences(): Flow<List<AppPreferenceEntity>>

    @Query("SELECT * FROM flux_app_preferences WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppPreference(packageName: String): AppPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppPreference(pref: AppPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppPreferences(prefs: List<AppPreferenceEntity>)

    @Query("DELETE FROM flux_app_preferences WHERE packageName = :packageName")
    suspend fun deleteAppPreference(packageName: String)
}
