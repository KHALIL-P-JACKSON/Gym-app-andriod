package com.gymtracker.android.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtracker.android.data.local.*
import com.gymtracker.android.data.repository.*
import com.gymtracker.android.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunPersistenceTest {
    @Test fun checkpointReopenFinishAndCascadeDelete() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filename = "run-persistence-test.db"
        context.deleteDatabase(filename)
        fun open() = Room.databaseBuilder(context, TrackerDatabase::class.java, filename).build()
        var db = open()
        try {
            var repository = RoomRunRepository(db)
            val active = RunRecord(startedAt = 1000, clockStartMillis = 100, bootCount = 1,
                elapsedMillis = 2000, points = listOf(RunPoint(40.0, -73.0, 1000, 5f)))
            repository.save(active); repository.save(active)
            assertTrue(repository.completed.first().isEmpty())
            db.close(); db = open(); repository = RoomRunRepository(db)
            assertEquals(active, repository.unfinished())
            val completed = active.copy(endedAt = 3000, distanceMeters = 10.0,
                points = active.points + RunPoint(40.0001, -73.0, 2000, 4f),
                splits = listOf(RunSplit(1, 10.0, 2000, true)))
            repository.save(completed); repository.save(completed)
            assertNull(repository.unfinished())
            assertEquals(completed, repository.completed.first().single())
            repository.delete(completed.id)
            assertEquals(0, db.runDao().pointCount(completed.id))
            assertTrue(repository.completed.first().isEmpty())
            repository.save(completed)
            RoomTrackerRepository(db).deleteAllData()
            assertTrue(repository.completed.first().isEmpty())
            assertEquals(0, db.runDao().pointCount(completed.id))
        } finally { db.close(); context.deleteDatabase(filename) }
    }

    @Test fun migrateVersionOnePreservesExistingWorkoutsAndPlans() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filename = "run-migration-test.db"
        context.deleteDatabase(filename)
        val testContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
        val schema = JSONObject(testContext.assets.open("com.gymtracker.android.data.local.TrackerDatabase/1.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(filename, 0, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            old.execSQL("INSERT INTO exercises VALUES ('bench','Bench','CHEST','PUSH',1,0)")
            old.execSQL("INSERT INTO workouts VALUES ('workout','Old workout',1234,'notes',60)")
            old.execSQL("INSERT INTO workout_sets VALUES ('set','workout','bench',185,8,1,0)")
            old.execSQL("INSERT INTO plans VALUES ('plan','Old plan','bench',0)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, filename).addMigrations(RUN_MIGRATION_1_2).build()
        try {
            val data = RoomTrackerRepository(db).data.first()
            assertEquals("Old workout", data.workouts.single().name)
            assertEquals(185.0, data.workouts.single().sets.single().weightLb, 0.001)
            assertEquals("Old plan", data.plans.single().name)
            assertTrue(RoomRunRepository(db).completed.first().isEmpty())
            assertNull(RoomRunRepository(db).unfinished())
        } finally { db.close(); context.deleteDatabase(filename) }
    }
}
