package com.affonso.pedaltrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SummaryCalculatorTest {

    private fun record(id: Long, start: Instant, km: Double, calories: Double?, durationMin: Int) = CyclingSessionRecord(
        id = id,
        healthConnectSessionId = "hc-$id",
        startTime = start,
        endTime = start.plusSeconds(durationMin * 60L),
        durationMin = durationMin,
        calories = calories,
        avgHeartRate = 130,
        km = km,
        carga = null,
        createdAt = start
    )

    @Test
    fun `calculate returns zeroed metrics for an empty list`() {
        val result = SummaryCalculator.calculate(emptyList())

        assertEquals(SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0), result)
    }

    @Test
    fun `calculate averages and totals across sessions`() {
        val sessions = listOf(
            record(1, Instant.parse("2026-08-01T10:00:00Z"), km = 10.0, calories = 200.0, durationMin = 30),
            record(2, Instant.parse("2026-08-02T10:00:00Z"), km = 20.0, calories = 300.0, durationMin = 60)
        )

        val result = SummaryCalculator.calculate(sessions)

        assertEquals(30.0, result.totalKm, 0.001)
        assertEquals(15.0, result.avgKmPerSession, 0.001)
        assertEquals(500.0, result.totalCalories, 0.001)
        assertEquals(45.0, result.avgDurationMin, 0.001)
        assertEquals(2, result.sessionCount)
    }

    @Test
    fun `calculate treats missing calories as zero`() {
        val sessions = listOf(record(1, Instant.parse("2026-08-01T10:00:00Z"), km = 10.0, calories = null, durationMin = 30))

        val result = SummaryCalculator.calculate(sessions)

        assertEquals(0.0, result.totalCalories, 0.001)
    }

    @Test
    fun `filterByPeriod keeps only sessions within the window`() {
        val now = Instant.parse("2026-08-30T00:00:00Z")
        val sessions = listOf(
            record(1, now.minusSeconds(3 * 86400), km = 10.0, calories = 100.0, durationMin = 30),
            record(2, now.minusSeconds(20 * 86400), km = 10.0, calories = 100.0, durationMin = 30),
            record(3, now.minusSeconds(40 * 86400), km = 10.0, calories = 100.0, durationMin = 30)
        )

        assertEquals(listOf(1L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.WEEK, now).map { it.id })
        assertEquals(listOf(1L, 2L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.MONTH, now).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.ALL, now).map { it.id })
    }
}
