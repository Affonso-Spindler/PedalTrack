@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.affonso.pedaltrack.ui.history

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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.ui.common.DeleteRed
import com.affonso.pedaltrack.ui.common.GradientBadge
import com.affonso.pedaltrack.ui.common.InfoChip
import com.affonso.pedaltrack.ui.common.caloriesChipTint
import com.affonso.pedaltrack.ui.common.durationChipTint
import com.affonso.pedaltrack.ui.common.heartRateChipTint
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    sessions: List<CyclingSessionRecord>,
    onUpdate: (Long, Double, String?) -> Unit,
    onDelete: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<CyclingSessionRecord?>(null) }
    var pendingDelete by remember { mutableStateOf<CyclingSessionRecord?>(null) }
    val currentlyEditing = editing

    if (currentlyEditing != null) {
        EditSessionForm(
            session = currentlyEditing,
            onCancel = { editing = null },
            onConfirm = { km, carga ->
                onUpdate(currentlyEditing.id, km, carga)
                editing = null
            }
        )
        return
    }

    val formatter = DateTimeFormatter.ofPattern("dd/MM · HH:mm").withZone(ZoneId.systemDefault())

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Apagar sessão?") },
            text = {
                Text(
                    "A sessão de ${"%.1f".format(toDelete.km)} km em ${formatter.format(toDelete.startTime)} " +
                        "será removida e não pode ser recuperada."
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(toDelete.id); pendingDelete = null }) {
                    Text("Apagar", color = DeleteRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(sessions) { session ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { editing = session },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GradientBadge()
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(
                                    formatter.format(session.startTime),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${"%.1f".format(session.km)} km",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { pendingDelete = session }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Apagar", tint = DeleteRed)
                        }
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip("⏱ ${session.durationMin} min", durationChipTint())
                        InfoChip("🔥 ${session.calories?.let { "%.0f".format(it) } ?: "—"} kcal", caloriesChipTint())
                        InfoChip("♥ ${session.avgHeartRate?.toString() ?: "—"} bpm", heartRateChipTint())
                    }
                    session.carga?.let {
                        Text(
                            "Carga: $it",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSessionForm(
    session: CyclingSessionRecord,
    onCancel: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var km by remember { mutableStateOf(session.km.toString()) }
    var carga by remember { mutableStateOf(session.carga.orEmpty()) }
    var kmError by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM · HH:mm").withZone(ZoneId.systemDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.offset(x = (-8).dp)) {
            Text("← Cancelar")
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
        OutlinedTextField(
            value = km,
            onValueChange = { km = it; kmError = false },
            placeholder = { Text("0,0") },
            isError = kmError,
            supportingText = { if (kmError) Text("Km inválido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                val parsedKm = km.replace(',', '.').toDoubleOrNull()
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
