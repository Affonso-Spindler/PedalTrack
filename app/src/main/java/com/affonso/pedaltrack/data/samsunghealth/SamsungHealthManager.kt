package com.affonso.pedaltrack.data.samsunghealth

import android.app.Activity
import android.content.Context
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reads exercise sessions directly from the Samsung Health app via the Samsung Health Data SDK.
 *
 * This app originally read via Health Connect, but on the target device Samsung Health was
 * granted full read/write access to Health Connect and still never wrote any Exercise records
 * there (confirmed via Health Connect's own "Aplicativos conectados" > per-app data view showing
 * zero records from Samsung Health, even after a fresh test workout). This SDK reads from
 * Samsung Health directly, bypassing that broken sync path. Writing back is not attempted here —
 * write access requires a Samsung partnership approval this app doesn't have; km stays local-only
 * in Room, which was already the architecture's source of truth.
 */
interface HealthConnectManager {
    suspend fun hasAllPermissions(): Boolean
    suspend fun requestPermissions(activity: Activity): Boolean
    suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession>
}

class SamsungHealthManagerImpl(context: Context) : HealthConnectManager {

    private val store: HealthDataStore = HealthDataService.getStore(context)
    private val requiredPermissions = setOf(Permission.of(DataTypes.EXERCISE, AccessType.READ))

    override suspend fun hasAllPermissions(): Boolean =
        store.getGrantedPermissions(requiredPermissions).containsAll(requiredPermissions)

    override suspend fun requestPermissions(activity: Activity): Boolean =
        store.requestPermissions(requiredPermissions, activity).containsAll(requiredPermissions)

    override suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession> {
        val localSince = LocalDateTime.ofInstant(since, ZoneId.systemDefault())
        val request = DataTypes.EXERCISE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.since(localSince))
            .build()
        val points = store.readData(request).dataList
        return points
            .flatMap { point -> point.getValue(DataType.ExerciseType.SESSIONS) ?: emptyList() }
            .filter { it.exerciseType == DataType.ExerciseType.PredefinedExerciseType.STATIONARY_BIKING }
            .map { it.toHealthConnectSession() }
    }

    private fun ExerciseSession.toHealthConnectSession(): HealthConnectSession = HealthConnectSession(
        healthConnectSessionId = "shealth-${startTime.toEpochMilli()}",
        startTime = startTime,
        endTime = endTime,
        durationMin = duration.toMinutes().toInt(),
        calories = calories.toDouble(),
        avgHeartRate = meanHeartRate?.toInt()
    )
}
