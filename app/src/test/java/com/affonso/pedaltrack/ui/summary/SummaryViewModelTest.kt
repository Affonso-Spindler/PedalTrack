package com.affonso.pedaltrack.ui.summary

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
import org.junit.Before
import org.junit.Test
import java.time.Instant

private class FakeCyclingRepository : CyclingRepository {
    var lastOffset: Int? = null

    private fun session(id: Long, km: Double) = CyclingSessionRecord(
        id = id,
        healthConnectSessionId = "hc-$id",
        startTime = Instant.parse("2026-08-12T12:00:00Z"),
        endTime = Instant.parse("2026-08-12T12:30:00Z"),
        durationMin = 30,
        calories = 100.0,
        avgHeartRate = 130,
        km = km,
        carga = null,
        createdAt = Instant.parse("2026-08-12T12:30:00Z")
    )

    override suspend fun getLoggableSessions(): List<HealthConnectSession> = emptyList()
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<Unit> =
        Result.success(Unit)
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = flowOf(emptyList())
    override suspend fun updateSession(id: Long, km: Double, carga: String?) {}
    override suspend fun deleteSession(id: Long) {}
    override suspend fun getSummary(period: SummaryPeriod, offset: Int): SummaryMetrics =
        SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0)

    override suspend fun getSessionsInPeriod(period: SummaryPeriod, offset: Int): List<CyclingSessionRecord> {
        lastOffset = offset
        return when (period) {
            SummaryPeriod.WEEK -> listOf(session(1, 10.0))
            SummaryPeriod.MONTH -> listOf(session(1, 10.0), session(2, 10.0), session(3, 10.0), session(4, 10.0))
            SummaryPeriod.ALL -> List(10) { session(it.toLong(), 10.0) }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads month summary by default`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(40.0, viewModel.uiState.value.metrics.totalKm, 0.001)
    }

    @Test
    fun `setPeriod switches metrics`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setPeriod(SummaryPeriod.WEEK)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(10.0, viewModel.uiState.value.metrics.totalKm, 0.001)
        assertEquals(SummaryPeriod.WEEK, viewModel.uiState.value.period)
    }

    @Test
    fun `navigatePrevious decreases the offset and forwards it to the repository`() = runTest {
        val repository = FakeCyclingRepository()
        val viewModel = SummaryViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.navigatePrevious()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(-1, viewModel.uiState.value.offset)
        assertEquals(-1, repository.lastOffset)
        assertEquals(true, viewModel.uiState.value.canGoToNextPeriod)
    }

    @Test
    fun `navigateNext does not go past the current period`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.navigateNext()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.offset)
        assertEquals(false, viewModel.uiState.value.canGoToNextPeriod)
    }

    @Test
    fun `setPeriod resets the offset back to zero`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.navigatePrevious()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setPeriod(SummaryPeriod.ALL)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.offset)
    }

    @Test
    fun `dailyMetrics has 7 entries for WEEK and is empty for ALL`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setPeriod(SummaryPeriod.WEEK)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(7, viewModel.uiState.value.dailyMetrics.size)

        viewModel.setPeriod(SummaryPeriod.ALL)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.dailyMetrics.size)
    }
}
