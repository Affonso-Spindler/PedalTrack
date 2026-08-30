package com.affonso.pedaltrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(val sessions: List<CyclingSessionRecord> = emptyList())

class HistoryViewModel(private val repository: CyclingRepository) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repository.observeHistory()
        .map { HistoryUiState(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    fun update(id: Long, km: Double, carga: String?) {
        viewModelScope.launch { repository.updateSession(id, km, carga) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteSession(id) }
    }
}
