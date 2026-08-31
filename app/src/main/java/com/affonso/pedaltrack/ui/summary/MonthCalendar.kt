package com.affonso.pedaltrack.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.affonso.pedaltrack.domain.DailyMetric
import com.affonso.pedaltrack.ui.theme.PedalPrimary
import com.affonso.pedaltrack.ui.theme.PedalSecondary
import java.time.DayOfWeek

private val weekdayHeaders = listOf("S", "T", "Q", "Q", "S", "S", "D")
private val cellSize = 28.dp

/** Compact month grid calendar — one small cell per day of [dailyMetrics], with a gradient dot on days that have a session. */
@Composable
fun MonthCalendar(dailyMetrics: List<DailyMetric>, modifier: Modifier = Modifier) {
    if (dailyMetrics.isEmpty()) return

    val leadingBlanks = (dailyMetrics.first().date.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells: List<DailyMetric?> = List(leadingBlanks) { null } + dailyMetrics
    val weeks = cells.chunked(7)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekdayHeaders.forEach { header ->
                Text(
                    header,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(cellSize)
                )
            }
        }
        weeks.forEach { week ->
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { index ->
                    val day = week.getOrNull(index)
                    Box(Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(day)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: DailyMetric) {
    val hasSession = day.sessionCount > 0
    Box(
        Modifier
            .fillMaxSize(0.85f)
            .clip(CircleShape)
            .background(
                if (hasSession) Brush.linearGradient(listOf(PedalPrimary, PedalSecondary))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            fontSize = 10.sp,
            color = if (hasSession) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (hasSession) FontWeight.Bold else FontWeight.Normal
        )
    }
}
