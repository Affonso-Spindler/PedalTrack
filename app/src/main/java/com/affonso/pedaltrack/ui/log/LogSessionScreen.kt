@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.affonso.pedaltrack.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.ui.common.GradientBadge
import com.affonso.pedaltrack.ui.common.InfoChip
import com.affonso.pedaltrack.ui.common.applyDigitEdit
import com.affonso.pedaltrack.ui.common.caloriesChipTint
import com.affonso.pedaltrack.ui.common.formatKmDigits
import com.affonso.pedaltrack.ui.common.heartRateChipTint
import com.affonso.pedaltrack.ui.common.kmDigitsToDouble
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogSessionScreen(
    uiState: LogSessionUiState,
    onSubmit: (HealthConnectSession, Double, String?) -> Unit,
    onDismissError: () -> Unit = {}
) {
    var selected by remember { mutableStateOf<HealthConnectSession?>(null) }

    if (uiState.loading) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }
        return
    }

    val current = selected
    if (current != null) {
        LogSessionForm(
            session = current,
            onCancel = { selected = null },
            onConfirm = { km, carga ->
                onSubmit(current, km, carga)
                selected = null
            }
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
                onClick = onDismissError
            ) {
                Text(
                    "Erro ao carregar treinos: $error (toque para dispensar)",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (uiState.loggableSessions.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { Text("Nenhum treino novo nos últimos 30 dias") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                items(uiState.loggableSessions) { session ->
                    SessionCard(session = session, onClick = { selected = session })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: HealthConnectSession, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM · HH:mm").withZone(ZoneId.systemDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientBadge()
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "${session.durationMin} min",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatter.format(session.startTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChip("🔥 ${session.calories?.let { "%.0f".format(it) } ?: "—"} kcal", caloriesChipTint())
                    InfoChip("♥ ${session.avgHeartRate?.toString() ?: "—"} bpm", heartRateChipTint())
                }
            }
        }
    }
}

@Composable
private fun LogSessionForm(
    session: HealthConnectSession,
    onCancel: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var kmDigits by remember { mutableStateOf("") }
    var carga by remember { mutableStateOf("") }
    var kmError by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM · HH:mm").withZone(ZoneId.systemDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.offset(x = (-8).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                GradientBadge()
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        formatter.format(session.startTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("${session.durationMin} min", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip("🔥 ${session.calories?.let { "%.0f".format(it) } ?: "—"} kcal", caloriesChipTint())
                        InfoChip("♥ ${session.avgHeartRate?.toString() ?: "—"} bpm", heartRateChipTint())
                    }
                }
            }
        }

        Text("Km rodado", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
        val kmDisplay = formatKmDigits(kmDigits)
        OutlinedTextField(
            value = TextFieldValue(kmDisplay, TextRange(kmDisplay.length)),
            onValueChange = { field -> kmDigits = applyDigitEdit(kmDigits, field.text); kmError = false },
            placeholder = { Text("0,0") },
            isError = kmError,
            supportingText = { if (kmError) Text("Km inválido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = carga,
            onValueChange = { carga = it },
            label = { Text("Carga (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        Button(
            onClick = {
                val parsedKm = kmDigitsToDouble(kmDigits)
                if (parsedKm == null) {
                    kmError = true
                } else {
                    onConfirm(parsedKm, carga.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Salvar", fontSize = 16.sp) }
    }
}
