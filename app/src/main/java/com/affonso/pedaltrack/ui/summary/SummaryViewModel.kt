package com.affonso.pedaltrack.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SummaryUiState(
    val period: SummaryPeriod = SummaryPeriod.MONTH,
    val metrics: SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0)
)

class SummaryViewModel(private val repository: CyclingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        setPeriod(SummaryPeriod.MONTH)
    }

    fun setPeriod(period: SummaryPeriod) {
        viewModelScope.launch {
            val metrics = repository.getSummary(period)
            _uiState.value = SummaryUiState(period, metrics)
        }
    }
}
