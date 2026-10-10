package com.gymtracker.android.tracking

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.repository.RunRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunControllerTest {
    private class Environment : RunEnvironment {
        var now = 100_000L
        var starts = 0
        var stops = 0
        var permitted = true
        override fun monotonicMillis() = now
        override fun wallMillis() = now + 1_000_000
        override fun bootCount() = 1
        override fun canTrack() = permitted
        override fun startService(unit: DistanceUnit, goalMeters: Double?, audio: Boolean) { starts++ }
        override fun stopService() { stops++ }
    }
    private class Repository : RunRepository {
        var active: RunRecord? = null
        var failing = false
        override val completed = MutableStateFlow<List<RunRecord>>(emptyList())
        override suspend fun unfinished() = active
        override suspend fun save(run: RunRecord) {
            check(!failing) { "Disk full" }
            if (run.endedAt == null) active = run else {
                active = null
                completed.value = completed.value.filter { it.id != run.id } + run
            }
        }
        override suspend fun delete(id: String) { completed.value = completed.value.filter { it.id != id } }
    }

    @Test fun repeatedStartAndEndSaveOnlyOneRunAndTimeContinuesWithoutFixes() = runTest {
        val env = Environment(); val repository = Repository()
        val controller = RunController(env, repository, backgroundScope)
        runCurrent()
        controller.requestStart(DistanceUnit.KM, null, false)
        controller.requestStart(DistanceUnit.KM, null, false)
        runCurrent()
        assertEquals(1, env.starts)
        assertTrue(controller.activate(DistanceUnit.KM, null, false) {})
        env.now += 60_000
        advanceTimeBy(1000); runCurrent()
        assertEquals(60_000L, controller.state.value.run!!.elapsedMillis)
        assertTrue(controller.state.value.tracking)
        controller.end(); controller.end(); runCurrent()
        assertEquals(1, repository.completed.value.size)
        assertEquals(60_000L, repository.completed.value.single().elapsedMillis)
        assertFalse(controller.state.value.tracking)
    }

    @Test fun endingSaveFailureKeepsFrozenResultForRetry() = runTest {
        val env = Environment(); val repository = Repository()
        val controller = RunController(env, repository, backgroundScope)
        runCurrent(); controller.requestStart(DistanceUnit.KM, null, false); runCurrent()
        controller.activate(DistanceUnit.KM, null, false) {}
        env.now += 20_000; repository.failing = true
        controller.end(); runCurrent()
        val frozen = controller.state.value.run!!
        assertNotNull(frozen.endedAt)
        assertFalse(controller.state.value.tracking)
        env.now += 90_000; advanceTimeBy(1000); runCurrent()
        assertEquals(frozen, controller.state.value.run)
        repository.failing = false; controller.end(); runCurrent()
        assertEquals(frozen, repository.completed.value.single())
    }

    @Test fun recoveringRunRetainsElapsedTimeAndDoesNotConnectAcrossGap() = runTest {
        val env = Environment(); val repository = Repository()
        repository.active = RunRecord(startedAt = 1, clockStartMillis = env.now - 60_000, bootCount = 1,
            elapsedMillis = 40_000, distanceMeters = 20.0, unit = DistanceUnit.KM,
            points = listOf(RunPoint(0.0, 0.0, 30000, 3f)))
        val controller = RunController(env, repository, backgroundScope)
        runCurrent()
        assertFalse(controller.state.value.tracking)
        controller.requestStart(DistanceUnit.MILES, null, false); runCurrent()
        assertTrue(controller.activate(DistanceUnit.MILES, null, false) {})
        assertEquals(DistanceUnit.KM, controller.state.value.run!!.unit) // original split unit survives preference changes
        controller.onLocation(LocationSample(1.0, 0.0, env.now, 3f)); runCurrent()
        assertTrue(controller.state.value.run!!.hasGaps)
        assertEquals(20.0, controller.state.value.run!!.distanceMeters, 0.001)
        assertEquals(1, controller.state.value.run!!.points.last().segment)
        assertEquals(60_000L, controller.state.value.run!!.elapsedMillis)
    }

    @Test fun goalIsAnnouncedOnceAndDoesNotEndTheRun() = runTest {
        val env = Environment(); val repository = Repository()
        val controller = RunController(env, repository, backgroundScope)
        val announcements = mutableListOf<String>()
        runCurrent(); controller.requestStart(DistanceUnit.KM, 10.0, true); runCurrent()
        controller.activate(DistanceUnit.KM, 10.0, true) { announcements += it }
        controller.onLocation(LocationSample(0.0, 0.0, env.now, 3f)); runCurrent()
        env.now += 5000
        controller.onLocation(LocationSample(Math.toDegrees(20.0 / 6371000.0), 0.0, env.now, 3f)); runCurrent()
        env.now += 5000
        controller.onLocation(LocationSample(Math.toDegrees(40.0 / 6371000.0), 0.0, env.now, 3f)); runCurrent()
        assertEquals(1, announcements.count { it.startsWith("Distance goal reached") })
        assertTrue(controller.state.value.tracking)
        assertTrue(repository.active!!.goalAnnounced)
    }

    @Test fun permissionDenialAndDeleteAllCancelPendingActivation() = runTest {
        val env = Environment(); val repository = Repository()
        val controller = RunController(env, repository, backgroundScope)
        runCurrent(); env.permitted = false
        controller.requestStart(DistanceUnit.KM, null, false); runCurrent()
        assertEquals(0, env.starts)
        assertNotNull(controller.state.value.error)
        env.permitted = true
        controller.requestStart(DistanceUnit.KM, null, false); runCurrent()
        controller.clearForDelete { repository.active = null }
        assertFalse(controller.activate(DistanceUnit.KM, null, false) {})
        assertNull(controller.state.value.run)
    }
}
