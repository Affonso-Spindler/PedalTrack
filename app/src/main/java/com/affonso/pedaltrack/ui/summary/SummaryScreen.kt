package com.affonso.pedaltrack.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.ui.theme.PedalPrimary
import com.affonso.pedaltrack.ui.theme.PedalSecondary

private val periodOptions = listOf(
    SummaryPeriod.WEEK to "Semana",
    SummaryPeriod.MONTH to "Mês",
    SummaryPeriod.ALL to "Tudo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    uiState: SummaryUiState,
    onPeriodChange: (SummaryPeriod) -> Unit,
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {}
) {
    var chartMetric by remember { mutableStateOf(ChartMetric.KM) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            periodOptions.forEachIndexed { index, (period, label) ->
                SegmentedButton(
                    selected = uiState.period == period,
                    onClick = { onPeriodChange(period) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periodOptions.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = Color.White,
                        activeBorderColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text(label) }
            }
        }

        if (uiState.period != SummaryPeriod.ALL) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigatePrevious) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Período anterior")
                }
                Text(
                    uiState.periodLabel,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onNavigateNext, enabled = uiState.canGoToNextPeriod) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo período")
                }
            }
        }

        if (uiState.period == SummaryPeriod.MONTH) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                MonthCalendar(uiState.dailyMetrics, modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
        }

        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientStatCard(
                value = "%.1f".format(uiState.metrics.totalKm),
                label = "km no período",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = uiState.metrics.sessionCount.toString(),
                label = "sessões",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = "%.0f".format(uiState.metrics.totalCalories),
                label = "calorias totais",
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "%.0f".format(uiState.metrics.avgDurationMin),
                label = "min médio",
                valueColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = "%.1f".format(uiState.metrics.avgKmPerSession),
                label = "km médio",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "%.0f".format(uiState.metrics.avgCaloriesPerSession),
                label = "kcal médio",
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.period != SummaryPeriod.ALL) {
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartMetric.entries.forEach { option ->
                    FilterChip(
                        selected = chartMetric == option,
                        onClick = { chartMetric = option },
                        label = { Text(option.label) }
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    DailyBarChart(
                        dailyMetrics = uiState.dailyMetrics,
                        metric = chartMetric,
                        periodLabel = uiState.periodLabel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(
                Brush.linearGradient(listOf(PedalPrimary, PedalSecondary)),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun StatCard(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
