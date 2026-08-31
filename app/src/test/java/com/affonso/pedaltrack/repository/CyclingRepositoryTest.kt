package com.affonso.pedaltrack.repository

import com.affonso.pedaltrack.data.local.CyclingSessionDao
import com.affonso.pedaltrack.data.local.CyclingSessionEntity
import com.affonso.pedaltrack.data.samsunghealth.HealthConnectManager
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import android.app.Activity
import java.time.Instant

private class FakeDao : CyclingSessionDao {
    val sessions = mutableListOf<CyclingSessionEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<CyclingSessionEntity>>(emptyList())

    override suspend fun insert(session: CyclingSessionEntity): Long {
        if (sessions.any { it.healthConnectSessionId == session.healthConnectSessionId }) {
            throw IllegalStateException("duplicate healthConnectSessionId")
        }
        val withId = session.copy(id = nextId++)
        sessions.add(withId)
        flow.value = sessions.toList()
        return withId.id
    }

    override suspend fun update(session: CyclingSessionEntity) {
        val index = sessions.indexOfFirst { it.id == session.id }
        sessions[index] = session
        flow.value = sessions.toList()
    }

    override suspend fun deleteById(id: Long) {
        sessions.removeAll { it.id == id }
        flow.value = sessions.toList()
    }

    override fun observeAll(): Flow<List<CyclingSessionEntity>> = flow
    override suspend fun getAll(): List<CyclingSessionEntity> = sessions.toList()
    override suspend fun getAllHealthConnectIds(): List<String> = sessions.map { it.healthConnectSessionId }
    override suspend fun getById(id: Long): CyclingSessionEntity? = sessions.find { it.id == id }
}

private class FakeHealthConnectManager(
    private val sessionsToReturn: List<HealthConnectSession> = emptyList()
) : HealthConnectManager {
    override suspend fun hasAllPermissions(): Boolean = true
    override suspend fun requestPermissions(activity: Activity): Boolean = true
    override suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession> = sessionsToReturn
}

class CyclingRepositoryTest {

    private val sampleSession = HealthConnectSession(
        healthConnectSessionId = "hc-1",
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130
    )

    @Test
    fun `getLoggableSessions excludes already logged sessions`() = runBlocking {
        val dao = FakeDao()
        dao.insert(
            CyclingSessionEntity(
                healthConnectSessionId = "hc-1",
                startTime = sampleSession.startTime,
                endTime = sampleSession.endTime,
                durationMin = 30,
                calories = 250.0,
                avgHeartRate = 130,
                km = 10.0,
                carga = null,
                createdAt = Instant.now()
            )
        )
        val repository = CyclingRepositoryImpl(dao, FakeHealthConnectManager(sessionsToReturn = listOf(sampleSession)))

        val result = repository.getLoggableSessions()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `logSession saves the session locally`() = runBlocking {
        val dao = FakeDao()
        val repository = CyclingRepositoryImpl(dao, FakeHealthConnectManager())

        val result = repository.logSession(sampleSession, km = 12.5, carga = "media")

        assertTrue(result.isSuccess)
        assertEquals(1, dao.sessions.size)
        assertEquals(12.5, dao.sessions[0].km, 0.001)
    }

    @Test
    fun `getSummary calculates metrics for the selected period`() = runBlocking {
        val dao = FakeDao()
        val now = Instant.now()
        dao.insert(
            CyclingSessionEntity(
                healthConnectSessionId = "hc-1",
                startTime = now.minusSeconds(3600),
                endTime = now,
                durationMin = 60,
                calories = 400.0,
                avgHeartRate = 140,
                km = 20.0,
                carga = null,
                createdAt = now
            )
        )
        val repository = CyclingRepositoryImpl(dao, FakeHealthConnectManager())

        val summary = repository.getSummary(SummaryPeriod.ALL)

        assertEquals(20.0, summary.totalKm, 0.001)
        assertEquals(1, summary.sessionCount)
    }
}
