package com.affonso.pedaltrack.ui.summary

import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import com.affonso.pedaltrack.repository.LogResult
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

private class FakeCyclingRepository : CyclingRepository {
    override suspend fun getLoggableSessions(): List<HealthConnectSession> = emptyList()
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult> =
        Result.success(LogResult(true))
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = flowOf(emptyList())
    override suspend fun updateSession(id: Long, km: Double, carga: String?) {}
    override suspend fun deleteSession(id: Long) {}
    override suspend fun getSummary(period: SummaryPeriod): SummaryMetrics = when (period) {
        SummaryPeriod.WEEK -> SummaryMetrics(10.0, 10.0, 100.0, 30.0, 1)
        SummaryPeriod.MONTH -> SummaryMetrics(40.0, 10.0, 400.0, 30.0, 4)
        SummaryPeriod.ALL -> SummaryMetrics(100.0, 10.0, 1000.0, 30.0, 10)
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
}
