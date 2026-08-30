package com.affonso.pedaltrack.domain

import java.time.Instant
import java.time.temporal.ChronoUnit

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
        now: Instant
    ): List<CyclingSessionRecord> = when (period) {
        SummaryPeriod.ALL -> sessions
        SummaryPeriod.WEEK -> sessions.filter { it.startTime >= now.minus(7, ChronoUnit.DAYS) }
        SummaryPeriod.MONTH -> sessions.filter { it.startTime >= now.minus(30, ChronoUnit.DAYS) }
    }
}
