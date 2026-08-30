package com.affonso.pedaltrack.ui.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.affonso.pedaltrack.domain.SummaryPeriod

@Composable
fun SummaryScreen(
    uiState: SummaryUiState,
    onPeriodChange: (SummaryPeriod) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            FilterChip(selected = uiState.period == SummaryPeriod.WEEK, onClick = { onPeriodChange(SummaryPeriod.WEEK) }, label = { Text("Semana") })
            FilterChip(selected = uiState.period == SummaryPeriod.MONTH, onClick = { onPeriodChange(SummaryPeriod.MONTH) }, label = { Text("Mês") })
            FilterChip(selected = uiState.period == SummaryPeriod.ALL, onClick = { onPeriodChange(SummaryPeriod.ALL) }, label = { Text("Tudo") })
        }
        Text("Sessões: ${uiState.metrics.sessionCount}", modifier = Modifier.padding(top = 16.dp))
        Text("Km total: ${"%.1f".format(uiState.metrics.totalKm)}")
        Text("Km médio: ${"%.1f".format(uiState.metrics.avgKmPerSession)}")
        Text("Calorias totais: ${"%.0f".format(uiState.metrics.totalCalories)}")
        Text("Duração média: ${"%.0f".format(uiState.metrics.avgDurationMin)} min")
    }
}
