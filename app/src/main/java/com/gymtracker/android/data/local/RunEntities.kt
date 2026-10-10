package com.gymtracker.android.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val id: String, val startedAt: Long, val clockStartMillis: Long,
    val bootCount: Int, val elapsedMillis: Long, val endedAt: Long?,
    val distanceMeters: Double, val unit: String, val goalMeters: Double?,
    val audio: Boolean, val goalAnnounced: Boolean, val hasGaps: Boolean
)

@Entity(tableName = "run_points", primaryKeys = ["runId", "position"],
    foreignKeys = [ForeignKey(entity = RunEntity::class, parentColumns = ["id"],
        childColumns = ["runId"], onDelete = ForeignKey.CASCADE)])
data class RunPointEntity(
    val runId: String, val position: Int, val latitude: Double, val longitude: Double,
    val elapsedMillis: Long, val accuracyMeters: Float, val segment: Int
)

@Entity(tableName = "run_splits", primaryKeys = ["runId", "number"],
    foreignKeys = [ForeignKey(entity = RunEntity::class, parentColumns = ["id"],
        childColumns = ["runId"], onDelete = ForeignKey.CASCADE)])
data class RunSplitEntity(
    val runId: String, val number: Int, val distanceMeters: Double,
    val durationMillis: Long, val partial: Boolean
)

data class RunWithRoute(
    @Embedded val run: RunEntity,
    @Relation(parentColumn = "id", entityColumn = "runId") val points: List<RunPointEntity>,
    @Relation(parentColumn = "id", entityColumn = "runId") val splits: List<RunSplitEntity>
)

@Dao
interface RunDao {
    @Transaction @Query("SELECT * FROM runs WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun completed(): Flow<List<RunWithRoute>>
    @Transaction @Query("SELECT * FROM runs WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun unfinished(): RunWithRoute?
    @Upsert suspend fun save(run: RunEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addPoints(points: List<RunPointEntity>)
    @Upsert suspend fun saveSplits(splits: List<RunSplitEntity>)
    @Query("SELECT COUNT(*) FROM run_points WHERE runId = :id") suspend fun pointCount(id: String): Int
    @Query("DELETE FROM runs WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM runs") suspend fun clear()
}

val RUN_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS runs (
            id TEXT NOT NULL PRIMARY KEY, startedAt INTEGER NOT NULL, clockStartMillis INTEGER NOT NULL,
            bootCount INTEGER NOT NULL, elapsedMillis INTEGER NOT NULL, endedAt INTEGER,
            distanceMeters REAL NOT NULL, unit TEXT NOT NULL, goalMeters REAL,
            audio INTEGER NOT NULL, goalAnnounced INTEGER NOT NULL, hasGaps INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS run_points (
            runId TEXT NOT NULL, position INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL,
            elapsedMillis INTEGER NOT NULL, accuracyMeters REAL NOT NULL, segment INTEGER NOT NULL,
            PRIMARY KEY(runId, position), FOREIGN KEY(runId) REFERENCES runs(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS run_splits (
            runId TEXT NOT NULL, number INTEGER NOT NULL, distanceMeters REAL NOT NULL,
            durationMillis INTEGER NOT NULL, partial INTEGER NOT NULL,
            PRIMARY KEY(runId, number), FOREIGN KEY(runId) REFERENCES runs(id) ON DELETE CASCADE)""")
    }
}
