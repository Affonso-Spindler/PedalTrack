package com.affonso.pedaltrack.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.affonso.pedaltrack.domain.DailyMetric

enum class ChartMetric(val label: String) {
    KM("Km"),
    TIME("Tempo"),
    KCAL("Kcal")
}

private fun DailyMetric.valueFor(metric: ChartMetric): Double = when (metric) {
    ChartMetric.KM -> km
    ChartMetric.TIME -> durationMin.toDouble()
    ChartMetric.KCAL -> calories
}

@Composable
private fun colorFor(metric: ChartMetric): Color = when (metric) {
    ChartMetric.KM -> MaterialTheme.colorScheme.primary
    ChartMetric.TIME -> MaterialTheme.colorScheme.secondary
    ChartMetric.KCAL -> MaterialTheme.colorScheme.tertiary
}

private fun totalLabel(dailyMetrics: List<DailyMetric>, metric: ChartMetric): String {
    val total = dailyMetrics.sumOf { it.valueFor(metric) }
    return when (metric) {
        ChartMetric.KM -> "${"%.1f".format(total)} km total"
        ChartMetric.TIME -> "${total.toInt()} min totais"
        ChartMetric.KCAL -> "${total.toInt()} kcal totais"
    }
}

private val plotHeight = 110.dp

/** Bar chart with a scale (0/half/max gridlines) and a period-total summary line, colored per [metric]. */
@Composable
fun DailyBarChart(dailyMetrics: List<DailyMetric>, metric: ChartMetric, periodLabel: String, modifier: Modifier = Modifier) {
    if (dailyMetrics.isEmpty()) return
    val maxValue = dailyMetrics.maxOf { it.valueFor(metric) }.coerceAtLeast(0.0001)
    val color = colorFor(metric)
    val barGradient = Brush.verticalGradient(listOf(color.copy(alpha = 0.55f), color))
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val showDayLabel = dailyMetrics.size <= 7
    val barWidthFraction = if (dailyMetrics.size <= 7) 0.45f else 0.75f
    val spacing = if (dailyMetrics.size <= 7) 10.dp else 3.dp

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(periodLabel.lowercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(totalLabel(dailyMetrics, metric), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(
                Modifier.width(24.dp).height(plotHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (step in 4 downTo 0) {
                    Text(
                        "%.0f".format(maxValue * step / 4),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(Modifier.weight(1f).height(plotHeight).padding(start = 6.dp)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(5) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(gridColor.copy(alpha = 0.4f)))
                    }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    dailyMetrics.forEach { day ->
                        val value = day.valueFor(metric)
                        val fraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth(barWidthFraction)
                                    .fillMaxHeight(if (day.sessionCount > 0) fraction.coerceAtLeast(0.05f) else 0.06f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (day.sessionCount > 0) barGradient else Brush.verticalGradient(listOf(emptyColor, emptyColor)))
                            )
                        }
                    }
                }
            }
        }

        if (showDayLabel) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 26.dp), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                dailyMetrics.forEach { day ->
                    Text(
                        day.date.dayOfMonth.toString(),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
