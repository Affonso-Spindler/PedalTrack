package com.affonso.pedaltrack.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LogSessionUiState(
    val loading: Boolean = true,
    val loggableSessions: List<HealthConnectSession> = emptyList(),
    val error: String? = null,
    val syncWarning: String? = null
)

class LogSessionViewModel(private val repository: CyclingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LogSessionUiState())
    val uiState: StateFlow<LogSessionUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val sessions = repository.getLoggableSessions()
                _uiState.value = _uiState.value.copy(loading = false, loggableSessions = sessions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun submit(session: HealthConnectSession, km: Double, carga: String?) {
        viewModelScope.launch {
            repository.logSession(session, km, carga)
                .onSuccess { logResult ->
                    _uiState.value = _uiState.value.copy(
                        syncWarning = if (!logResult.healthConnectSynced)
                            "Salvo localmente, mas não sincronizou com o Health Connect"
                        else null
                    )
                    loadSessions()
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }
}
