package com.affonso.pedaltrack.ui.log

import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

private class FakeCyclingRepository(
    private val loggableSessions: List<HealthConnectSession> = emptyList(),
    private val logResult: Result<Unit> = Result.success(Unit)
) : CyclingRepository {
    var loggedKm: Double? = null

    override suspend fun getLoggableSessions(): List<HealthConnectSession> = loggableSessions
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<Unit> {
        loggedKm = km
        return logResult
    }
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = flowOf(emptyList())
    override suspend fun updateSession(id: Long, km: Double, carga: String?) {}
    override suspend fun deleteSession(id: Long) {}
    override suspend fun getSummary(period: SummaryPeriod, offset: Int): SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0)
    override suspend fun getSessionsInPeriod(period: SummaryPeriod, offset: Int): List<CyclingSessionRecord> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogSessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val sampleSession = HealthConnectSession(
        healthConnectSessionId = "hc-1",
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130
    )

    @Test
    fun `loads loggable sessions on init`() = runTest {
        val viewModel = LogSessionViewModel(FakeCyclingRepository(loggableSessions = listOf(sampleSession)))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.loggableSessions.size)
        assertEquals(false, viewModel.uiState.value.loading)
    }

    @Test
    fun `submit logs the session with the entered km`() = runTest {
        val repository = FakeCyclingRepository(loggableSessions = listOf(sampleSession))
        val viewModel = LogSessionViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(sampleSession, 12.5, "media")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(12.5, repository.loggedKm)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `submit sets error when logSession fails`() = runTest {
        val repository = FakeCyclingRepository(
            loggableSessions = listOf(sampleSession),
            logResult = Result.failure(RuntimeException("db error"))
        )
        val viewModel = LogSessionViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(sampleSession, 12.5, "media")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error != null)
    }
}
