package com.affonso.pedaltrack.domain

import java.time.Instant

data class HealthConnectSession(
    val healthConnectSessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMin: Int,
    val calories: Double?,
    val avgHeartRate: Int?
)

data class CyclingSessionRecord(
    val id: Long,
    val healthConnectSessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMin: Int,
    val calories: Double?,
    val avgHeartRate: Int?,
    val km: Double,
    val carga: String?,
    val createdAt: Instant
)

data class SummaryMetrics(
    val totalKm: Double,
    val avgKmPerSession: Double,
    val totalCalories: Double,
    val avgDurationMin: Double,
    val sessionCount: Int
)

enum class SummaryPeriod { WEEK, MONTH, ALL }
