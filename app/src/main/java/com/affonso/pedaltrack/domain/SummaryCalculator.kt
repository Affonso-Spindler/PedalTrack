package com.affonso.pedaltrack.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

object SummaryCalculator {

    fun calculate(sessions: List<CyclingSessionRecord>): SummaryMetrics {
        if (sessions.isEmpty()) return SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0)
        val totalKm = sessions.sumOf { it.km }
        val totalCalories = sessions.sumOf { it.calories ?: 0.0 }
        val avgDuration = sessions.map { it.durationMin }.average()
        return SummaryMetrics(
            totalKm = totalKm,
            avgKmPerSession = totalKm / sessions.size,
            totalCalories = totalCalories,
            avgDurationMin = avgDuration,
            sessionCount = sessions.size
        )
    }

    fun filterByPeriod(
        sessions: List<CyclingSessionRecord>,
        period: SummaryPeriod,
        now: Instant,
        zone: ZoneId
    ): List<CyclingSessionRecord> = when (period) {
        SummaryPeriod.ALL -> sessions
        SummaryPeriod.WEEK -> {
            val weekStart = now.atZone(zone).toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant()
            sessions.filter { it.startTime >= weekStart }
        }
        SummaryPeriod.MONTH -> {
            val monthStart = now.atZone(zone).toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant()
            sessions.filter { it.startTime >= monthStart }
        }
    }
}
