package com.affonso.pedaltrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

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

        assertEquals(SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0), result)
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
        assertEquals(250.0, result.avgCaloriesPerSession, 0.001)
    }

    @Test
    fun `calculate treats missing calories as zero`() {
        val sessions = listOf(record(1, Instant.parse("2026-08-01T10:00:00Z"), km = 10.0, calories = null, durationMin = 30))

        val result = SummaryCalculator.calculate(sessions)

        assertEquals(0.0, result.totalCalories, 0.001)
    }

    @Test
    fun `filterByPeriod keeps only sessions within the current calendar week and month`() {
        // Fixed clock: Saturday 2026-08-15, 10:00 local time in America/Sao_Paulo (UTC-3, no DST).
        // Current ISO week starts Monday 2026-08-10T00:00 local; current month starts 2026-08-01T00:00 local.
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-08-15T13:00:00Z")
        val sessions = listOf(
            // Wednesday 2026-08-12, within the current week and month.
            record(1, Instant.parse("2026-08-12T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30),
            // 2026-08-05, before the current week's Monday start but within the current month.
            record(2, Instant.parse("2026-08-05T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30),
            // 2026-07-20, in the previous calendar month entirely.
            record(3, Instant.parse("2026-07-20T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30)
        )

        assertEquals(listOf(1L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.WEEK, now, zone).map { it.id })
        assertEquals(listOf(1L, 2L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.MONTH, now, zone).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.ALL, now, zone).map { it.id })
    }

    @Test
    fun `filterByPeriod with a negative offset selects a past week or month`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-08-15T13:00:00Z")
        val sessions = listOf(
            // Wednesday 2026-08-12 local, in the current week and month.
            record(1, Instant.parse("2026-08-12T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30),
            // Wednesday 2026-08-05 local, in the previous week (Aug 3-9) but the current month.
            record(2, Instant.parse("2026-08-05T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30),
            // 2026-07-20 local, in the previous calendar month.
            record(3, Instant.parse("2026-07-20T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30)
        )

        assertEquals(
            listOf(2L),
            SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.WEEK, now, zone, offset = -1).map { it.id }
        )
        assertEquals(
            listOf(3L),
            SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.MONTH, now, zone, offset = -1).map { it.id }
        )
    }

    @Test
    fun `periodLabel formats week and month labels in Portuguese`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-08-15T13:00:00Z")

        assertEquals("Agosto 2026", SummaryCalculator.periodLabel(SummaryPeriod.MONTH, now, zone, offset = 0))
        assertEquals("Julho 2026", SummaryCalculator.periodLabel(SummaryPeriod.MONTH, now, zone, offset = -1))
        assertEquals("Tudo", SummaryCalculator.periodLabel(SummaryPeriod.ALL, now, zone, offset = 0))
    }

    @Test
    fun `isAtLatestPeriod is true only for the current period`() {
        assertEquals(true, SummaryCalculator.isAtLatestPeriod(0))
        assertEquals(false, SummaryCalculator.isAtLatestPeriod(-1))
    }

    @Test
    fun `dailyMetrics returns one entry per day of the week with zeroed days for no sessions`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-08-15T13:00:00Z") // current week: Mon 2026-08-10 .. Sun 2026-08-16
        val sessions = listOf(
            record(1, Instant.parse("2026-08-12T12:00:00Z"), km = 10.0, calories = 100.0, durationMin = 30)
        )

        val days = SummaryCalculator.dailyMetrics(sessions, SummaryPeriod.WEEK, now, zone, offset = 0)

        assertEquals(7, days.size)
        assertEquals(java.time.LocalDate.of(2026, 8, 10), days.first().date)
        assertEquals(java.time.LocalDate.of(2026, 8, 16), days.last().date)
        val wednesday = days.first { it.date == java.time.LocalDate.of(2026, 8, 12) }
        assertEquals(10.0, wednesday.km, 0.001)
        assertEquals(1, wednesday.sessionCount)
        val emptyDay = days.first { it.date == java.time.LocalDate.of(2026, 8, 11) }
        assertEquals(0.0, emptyDay.km, 0.001)
        assertEquals(0, emptyDay.sessionCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `dailyMetrics rejects SummaryPeriod ALL`() {
        SummaryCalculator.dailyMetrics(emptyList(), SummaryPeriod.ALL, Instant.now(), ZoneId.systemDefault())
    }
}
