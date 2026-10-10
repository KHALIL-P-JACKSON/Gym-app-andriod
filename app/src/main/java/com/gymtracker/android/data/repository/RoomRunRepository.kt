package com.gymtracker.android.data.repository

import androidx.room.withTransaction
import com.gymtracker.android.data.local.*
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.repository.RunRepository
import kotlinx.coroutines.flow.map

class RoomRunRepository(private val database: TrackerDatabase) : RunRepository {
    private val dao = database.runDao()
    override val completed = dao.completed().map { rows -> rows.map { it.model() } }
    override suspend fun unfinished() = dao.unfinished()?.model()
    override suspend fun save(run: RunRecord) = database.withTransaction {
        dao.save(RunEntity(run.id, run.startedAt, run.clockStartMillis, run.bootCount, run.elapsedMillis,
            run.endedAt, run.distanceMeters, run.unit.name, run.goalMeters, run.audio,
            run.goalAnnounced, run.hasGaps))
        val savedCount = dao.pointCount(run.id)
        dao.addPoints(run.points.drop(savedCount).mapIndexed { index, point ->
            RunPointEntity(run.id, savedCount + index, point.latitude, point.longitude,
                point.elapsedMillis, point.accuracyMeters, point.segment)
        })
        dao.saveSplits(run.splits.map { RunSplitEntity(run.id, it.number, it.distanceMeters, it.durationMillis, it.partial) })
    }
    override suspend fun delete(id: String) = dao.delete(id)

    private fun RunWithRoute.model() = run.let { r ->
        RunRecord(r.id, r.startedAt, r.clockStartMillis, r.bootCount, r.elapsedMillis, r.endedAt,
            r.distanceMeters, DistanceUnit.valueOf(r.unit), r.goalMeters, r.audio, r.goalAnnounced,
            r.hasGaps, points.sortedBy { it.position }.map {
                RunPoint(it.latitude, it.longitude, it.elapsedMillis, it.accuracyMeters, it.segment)
            }, splits.sortedBy { it.number }.map { RunSplit(it.number, it.distanceMeters, it.durationMillis, it.partial) })
    }
}
