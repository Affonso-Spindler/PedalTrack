package com.affonso.pedaltrack.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DailyMetric(
    val date: LocalDate,
    val km: Double,
    val durationMin: Int,
    val calories: Double,
    val sessionCount: Int
)

object SummaryCalculator {

    fun calculate(sessions: List<CyclingSessionRecord>): SummaryMetrics {
        if (sessions.isEmpty()) return SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0)
        val totalKm = sessions.sumOf { it.km }
        val totalCalories = sessions.sumOf { it.calories ?: 0.0 }
        val avgDuration = sessions.map { it.durationMin }.average()
        return SummaryMetrics(
            totalKm = totalKm,
            avgKmPerSession = totalKm / sessions.size,
            totalCalories = totalCalories,
            avgDurationMin = avgDuration,
            sessionCount = sessions.size,
            avgCaloriesPerSession = totalCalories / sessions.size
        )
    }

    /**
     * Filters sessions to the period selected by [period], shifted by [offset] whole
     * periods relative to the one containing [now] (0 = current, -1 = previous, ...).
     * [offset] is ignored for [SummaryPeriod.ALL].
     */
    fun filterByPeriod(
        sessions: List<CyclingSessionRecord>,
        period: SummaryPeriod,
        now: Instant,
        zone: ZoneId,
        offset: Int = 0
    ): List<CyclingSessionRecord> {
        if (period == SummaryPeriod.ALL) return sessions
        val (start, end) = periodBounds(period, now, zone, offset)
        return sessions.filter { it.startTime >= start && it.startTime < end }
    }

    /** Human-readable label for the currently selected period, e.g. "18–24 ago" or "Agosto 2026". */
    fun periodLabel(period: SummaryPeriod, now: Instant, zone: ZoneId, offset: Int): String {
        if (period == SummaryPeriod.ALL) return "Tudo"
        val (start, end) = periodBounds(period, now, zone, offset)
        val startDate = start.atZone(zone).toLocalDate()
        val endDate = end.atZone(zone).toLocalDate().minusDays(1)
        return when (period) {
            SummaryPeriod.WEEK -> {
                val dayFormatter = DateTimeFormatter.ofPattern("d/MM")
                if (startDate.month == endDate.month) {
                    "${startDate.dayOfMonth}–${endDate.format(dayFormatter)}"
                } else {
                    "${startDate.format(dayFormatter)} – ${endDate.format(dayFormatter)}"
                }
            }
            SummaryPeriod.MONTH -> {
                val monthName = startDate.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }
                "$monthName ${startDate.year}"
            }
            SummaryPeriod.ALL -> "Tudo"
        }
    }

    /** True when [offset] is already at the current period, so navigating forward would go into the future. */
    fun isAtLatestPeriod(offset: Int): Boolean = offset >= 0

    /**
     * One entry per calendar day in the period (7 for WEEK, the days in the month for MONTH),
     * aggregating that day's sessions. Not defined for [SummaryPeriod.ALL].
     */
    fun dailyMetrics(
        sessions: List<CyclingSessionRecord>,
        period: SummaryPeriod,
        now: Instant,
        zone: ZoneId,
        offset: Int = 0
    ): List<DailyMetric> {
        require(period != SummaryPeriod.ALL) { "dailyMetrics is not defined for SummaryPeriod.ALL" }
        val (start, end) = periodBounds(period, now, zone, offset)
        val startDate = start.atZone(zone).toLocalDate()
        val endDateExclusive = end.atZone(zone).toLocalDate()
        val byDate = sessions.groupBy { it.startTime.atZone(zone).toLocalDate() }
        val days = generateSequence(startDate) { it.plusDays(1) }.takeWhile { it < endDateExclusive }
        return days.map { date ->
            val daySessions = byDate[date].orEmpty()
            DailyMetric(
                date = date,
                km = daySessions.sumOf { it.km },
                durationMin = daySessions.sumOf { it.durationMin },
                calories = daySessions.sumOf { it.calories ?: 0.0 },
                sessionCount = daySessions.size
            )
        }.toList()
    }

    private fun periodBounds(period: SummaryPeriod, now: Instant, zone: ZoneId, offset: Int): Pair<Instant, Instant> {
        val today = now.atZone(zone).toLocalDate()
        return when (period) {
            SummaryPeriod.WEEK -> {
                val start = today.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
                start.atStartOfDay(zone).toInstant() to start.plusWeeks(1).atStartOfDay(zone).toInstant()
            }
            SummaryPeriod.MONTH -> {
                val start = today.withDayOfMonth(1).plusMonths(offset.toLong())
                start.atStartOfDay(zone).toInstant() to start.plusMonths(1).atStartOfDay(zone).toInstant()
            }
            SummaryPeriod.ALL -> now to now
        }
    }
}
