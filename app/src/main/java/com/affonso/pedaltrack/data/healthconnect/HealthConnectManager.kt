package com.affonso.pedaltrack.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import com.affonso.pedaltrack.domain.HealthConnectSession
import java.time.Instant
import java.time.temporal.ChronoUnit

interface HealthConnectManager {
    fun permissions(): Set<String>
    suspend fun hasAllPermissions(): Boolean
    suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession>
    suspend fun writeDistanceRecord(startTime: Instant, endTime: Instant, km: Double): Result<Unit>
}

class HealthConnectManagerImpl(private val client: HealthConnectClient) : HealthConnectManager {

    override fun permissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    override suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions())

    override suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession> {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since)
            )
        )
        return response.records
            .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY }
            .map { toHealthConnectSession(it) }
    }

    private suspend fun toHealthConnectSession(record: ExerciseSessionRecord): HealthConnectSession {
        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL, HeartRateRecord.BPM_AVG),
                timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
            )
        )
        return HealthConnectSession(
            healthConnectSessionId = record.metadata.id,
            startTime = record.startTime,
            endTime = record.endTime,
            durationMin = ChronoUnit.MINUTES.between(record.startTime, record.endTime).toInt(),
            calories = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories,
            avgHeartRate = aggregate[HeartRateRecord.BPM_AVG]?.toInt()
        )
    }

    override suspend fun writeDistanceRecord(startTime: Instant, endTime: Instant, km: Double): Result<Unit> =
        try {
            client.insertRecords(
                listOf(
                    DistanceRecord(
                        startTime = startTime,
                        startZoneOffset = null,
                        endTime = endTime,
                        endZoneOffset = null,
                        distance = Length.kilometers(km),
                        metadata = Metadata(
                            id = "",
                            dataOrigin = DataOrigin(""),
                            lastModifiedTime = Instant.EPOCH,
                            clientRecordId = null,
                            clientRecordVersion = 0,
                            device = null,
                            recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY
                        )
                    )
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
}
