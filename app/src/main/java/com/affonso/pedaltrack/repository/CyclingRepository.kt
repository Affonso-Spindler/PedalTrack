package com.affonso.pedaltrack.repository

import com.affonso.pedaltrack.data.healthconnect.HealthConnectManager
import com.affonso.pedaltrack.data.local.CyclingSessionDao
import com.affonso.pedaltrack.data.local.CyclingSessionEntity
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SessionFilter
import com.affonso.pedaltrack.domain.SummaryCalculator
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

data class LogResult(val healthConnectSynced: Boolean)

interface CyclingRepository {
    suspend fun getLoggableSessions(): List<HealthConnectSession>
    suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult>
    fun observeHistory(): Flow<List<CyclingSessionRecord>>
    suspend fun updateSession(id: Long, km: Double, carga: String?)
    suspend fun deleteSession(id: Long)
    suspend fun getSummary(period: SummaryPeriod): SummaryMetrics
}

class CyclingRepositoryImpl(
    private val dao: CyclingSessionDao,
    private val healthConnectManager: HealthConnectManager
) : CyclingRepository {

    override suspend fun getLoggableSessions(): List<HealthConnectSession> {
        val since = Instant.now().minus(30, ChronoUnit.DAYS)
        val hcSessions = healthConnectManager.readRecentStationaryBikeSessions(since)
        val loggedIds = dao.getAllHealthConnectIds().toSet()
        return SessionFilter.loggable(hcSessions, loggedIds)
    }

    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult> =
        try {
            dao.insert(
                CyclingSessionEntity(
                    healthConnectSessionId = session.healthConnectSessionId,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    durationMin = session.durationMin,
                    calories = session.calories,
                    avgHeartRate = session.avgHeartRate,
                    km = km,
                    carga = carga,
                    createdAt = Instant.now()
                )
            )
            val syncResult = healthConnectManager.writeDistanceRecord(session.startTime, session.endTime, km)
            Result.success(LogResult(healthConnectSynced = syncResult.isSuccess))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun observeHistory(): Flow<List<CyclingSessionRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateSession(id: Long, km: Double, carga: String?) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(km = km, carga = carga))
    }

    override suspend fun deleteSession(id: Long) = dao.deleteById(id)

    override suspend fun getSummary(period: SummaryPeriod): SummaryMetrics {
        val all = dao.getAll().map { it.toDomain() }
        val filtered = SummaryCalculator.filterByPeriod(all, period, Instant.now())
        return SummaryCalculator.calculate(filtered)
    }
}

private fun CyclingSessionEntity.toDomain() = CyclingSessionRecord(
    id = id,
    healthConnectSessionId = healthConnectSessionId,
    startTime = startTime,
    endTime = endTime,
    durationMin = durationMin,
    calories = calories,
    avgHeartRate = avgHeartRate,
    km = km,
    carga = carga,
    createdAt = createdAt
)
