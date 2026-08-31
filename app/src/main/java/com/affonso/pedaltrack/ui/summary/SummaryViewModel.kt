package com.affonso.pedaltrack.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.DailyMetric
import com.affonso.pedaltrack.domain.SummaryCalculator
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class SummaryUiState(
    val period: SummaryPeriod = SummaryPeriod.MONTH,
    val offset: Int = 0,
    val periodLabel: String = "",
    val canGoToNextPeriod: Boolean = false,
    val metrics: SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0),
    val dailyMetrics: List<DailyMetric> = emptyList()
)

class SummaryViewModel(
    private val repository: CyclingRepository,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        load(SummaryPeriod.MONTH, offset = 0)
    }

    fun setPeriod(period: SummaryPeriod) {
        load(period, offset = 0)
    }

    fun navigatePrevious() {
        load(_uiState.value.period, offset = _uiState.value.offset - 1)
    }

    fun navigateNext() {
        val nextOffset = _uiState.value.offset + 1
        if (nextOffset > 0) return
        load(_uiState.value.period, offset = nextOffset)
    }

    private fun load(period: SummaryPeriod, offset: Int) {
        viewModelScope.launch {
            val now = Instant.now()
            val sessions = repository.getSessionsInPeriod(period, offset)
            _uiState.value = SummaryUiState(
                period = period,
                offset = offset,
                periodLabel = SummaryCalculator.periodLabel(period, now, zone, offset),
                canGoToNextPeriod = !SummaryCalculator.isAtLatestPeriod(offset),
                metrics = SummaryCalculator.calculate(sessions),
                dailyMetrics = if (period == SummaryPeriod.ALL) {
                    emptyList()
                } else {
                    SummaryCalculator.dailyMetrics(sessions, period, now, zone, offset)
                }
            )
        }
    }
}
