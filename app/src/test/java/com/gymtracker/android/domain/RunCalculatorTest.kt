package com.gymtracker.android.domain

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.service.*
import org.junit.Assert.*
import org.junit.Test

class RunCalculatorTest {
    private fun run(unit: DistanceUnit = DistanceUnit.KM) = RunRecord(startedAt = 1000,
        clockStartMillis = 5000, bootCount = 2, elapsedMillis = 400_000, unit = unit)
    private fun point(meters: Double, seconds: Long, accuracy: Float = 3f) =
        RunPoint(Math.toDegrees(meters / 6371000.0), 0.0, seconds * 1000, accuracy)

    @Test fun clockIncludesLockedScreenTimeAndFreezesWhenEnded() {
        val active = run().copy(elapsedMillis = 10_000)
        assertEquals(95_000L, active.elapsed(100_000, 2))
        assertEquals(10_000L, active.elapsed(100_000, 3)) // reboot cannot reuse uptime
        val finished = RunCalculator.finish(active.copy(elapsedMillis = 95_000), 9999)
        assertEquals(95_000L, finished.elapsed(200_000, 2))
        assertEquals(finished, RunCalculator.finish(finished, 88888))
    }

    @Test fun stationaryNoisePoorFixesDuplicatesAndJumpsDoNotAddDistance() {
        val first = RunCalculator.accept(run(), point(0.0, 0))
        assertEquals(first, RunCalculator.accept(first, point(1.0, 2)))
        assertEquals(first, RunCalculator.accept(first, point(20.0, 2, 100f)))
        assertEquals(first, RunCalculator.accept(first, point(2000.0, 2)))
        assertEquals(first, RunCalculator.accept(first, point(10.0, 0)))
        assertEquals(first, RunCalculator.accept(first, point(10.0, -1)))
        assertEquals(first, RunCalculator.accept(first, point(10.0, 2).copy(latitude = Double.NaN)))
        assertEquals(first, RunCalculator.accept(first, point(10.0, 2).copy(longitude = 190.0)))
        assertEquals(0.0, first.distanceMeters, 0.001)
    }

    @Test fun knownRouteInterpolatesSplitsAndIncludesFinalPartial() {
        var current = run()
        for (i in 0..25) current = RunCalculator.accept(current, point(i * 100.0, i * 10L))
        assertEquals(2500.0, current.distanceMeters, 0.001)
        assertEquals(2, current.splits.size)
        assertEquals(100_000L, current.splits[0].durationMillis)
        assertEquals(100_000L, current.splits[1].durationMillis)
        val finished = RunCalculator.finish(current.copy(elapsedMillis = 260_000), 99999)
        assertEquals(3, finished.splits.size)
        assertTrue(finished.splits.last().partial)
        assertEquals(500.0, finished.splits.last().distanceMeters, 0.001)
        assertEquals(60_000L, finished.splits.last().durationMillis)
        assertEquals("1:44 /km", RunCalculator.pace(260_000, 2500.0, DistanceUnit.KM))
        assertEquals(finished, RunCalculator.accept(finished, point(2600.0, 270)))
    }

    @Test fun mileSplitsUseMilesAndDoNotChangeCanonicalMeters() {
        var current = run(DistanceUnit.MILES)
        for (i in 0..20) current = RunCalculator.accept(current, point(i * 100.0, i * 10L))
        assertEquals(1, current.splits.size)
        assertEquals(1609.344, current.splits.single().distanceMeters, 0.0001)
        assertEquals(160_934L, current.splits.single().durationMillis)
        assertEquals("1.24 mi", RunCalculator.distance(current.distanceMeters, DistanceUnit.MILES))
        assertEquals("2.00 km", RunCalculator.distance(current.distanceMeters, DistanceUnit.KM))
    }

    @Test fun outageAndRecoveryStartNewSegmentsWithoutInventingDistance() {
        var current = RunCalculator.accept(run(), point(0.0, 0))
        current = RunCalculator.accept(current, point(100.0, 10))
        current = RunCalculator.accept(current, point(900.0, 100))
        assertEquals(100.0, current.distanceMeters, 0.001)
        assertTrue(current.hasGaps)
        assertEquals(1, current.points.last().segment)
        current = RunCalculator.accept(current, point(1000.0, 110))
        assertEquals(200.0, current.distanceMeters, 0.001)
        current = RunCalculator.accept(current, point(1100.0, 120), forceGap = true)
        assertEquals(200.0, current.distanceMeters, 0.001)
        assertEquals(2, current.points.last().segment)
    }

    @Test fun stoppedOrUninitializedPaceIsUnavailableAndExportHandlesZeroDistance() {
        assertEquals("—", RunCalculator.pace(0, 0.0, DistanceUnit.KM))
        var current = RunCalculator.accept(run(), point(0.0, 0))
        current = RunCalculator.accept(current, point(100.0, 10))
        assertEquals("1:40 /km", RunCalculator.currentPace(current.copy(elapsedMillis = 11_000)))
        assertEquals("—", RunCalculator.currentPace(current.copy(elapsedMillis = 30_000)))
        val finished = RunCalculator.finish(run(), 20000)
        val csv = CsvExporter.exportRuns(listOf(finished, run()))
        assertEquals(2, csv.lines().size)
        assertFalse(csv.contains("NaN")); assertFalse(csv.contains("Infinity"))
        assertEquals("1:01:01", RunCalculator.duration(3_661_000))
    }
}
