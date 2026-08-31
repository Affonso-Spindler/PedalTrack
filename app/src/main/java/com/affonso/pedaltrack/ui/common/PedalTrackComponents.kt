package com.affonso.pedaltrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.affonso.pedaltrack.ui.theme.PedalPrimary
import com.affonso.pedaltrack.ui.theme.PedalSecondary

/** Vivid red for destructive actions — Material3's theme "error" color is too desaturated in dark mode to read as a clear warning. */
val DeleteRed = Color(0xFFEF4444)

/** Small pill-shaped label used for duration/calories/heart-rate stats on a session card. */
@Composable
fun InfoChip(text: String, tint: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = tint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Violet-to-cyan gradient badge with a bike icon, used as a leading accent on session cards. */
@Composable
fun GradientBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(
                Brush.linearGradient(listOf(PedalPrimary, PedalSecondary)),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.DirectionsBike,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun durationChipTint() = MaterialTheme.colorScheme.primary

@Composable
fun caloriesChipTint() = MaterialTheme.colorScheme.secondary

@Composable
fun heartRateChipTint() = MaterialTheme.colorScheme.tertiary
