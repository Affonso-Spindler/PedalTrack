package com.affonso.pedaltrack.ui.history

import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val history: MutableStateFlow<List<CyclingSessionRecord>>
) : CyclingRepository {
    var deletedId: Long? = null
    var updatedKm: Double? = null

    override suspend fun getLoggableSessions(): List<HealthConnectSession> = emptyList()
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<Unit> =
        Result.success(Unit)
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = history
    override suspend fun updateSession(id: Long, km: Double, carga: String?) { updatedKm = km }
    override suspend fun deleteSession(id: Long) { deletedId = id }
    override suspend fun getSummary(period: SummaryPeriod, offset: Int): SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0)
    override suspend fun getSessionsInPeriod(period: SummaryPeriod, offset: Int): List<CyclingSessionRecord> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val sampleRecord = CyclingSessionRecord(
        id = 1,
        healthConnectSessionId = "hc-1",
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130,
        km = 12.5,
        carga = "media",
        createdAt = Instant.parse("2026-08-01T10:31:00Z")
    )

    @Test
    fun `exposes sessions from repository`() = runTest {
        val viewModel = HistoryViewModel(FakeCyclingRepository(MutableStateFlow(listOf(sampleRecord))))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessions.size)
    }

    @Test
    fun `delete forwards id to repository`() = runTest {
        val repository = FakeCyclingRepository(MutableStateFlow(listOf(sampleRecord)))
        val viewModel = HistoryViewModel(repository)

        viewModel.delete(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1L, repository.deletedId)
    }

    @Test
    fun `update forwards km to repository`() = runTest {
        val repository = FakeCyclingRepository(MutableStateFlow(listOf(sampleRecord)))
        val viewModel = HistoryViewModel(repository)

        viewModel.update(1, 15.0, "alta")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.updatedKm == 15.0)
    }
}
