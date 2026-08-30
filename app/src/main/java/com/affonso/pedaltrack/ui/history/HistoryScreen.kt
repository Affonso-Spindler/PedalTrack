package com.affonso.pedaltrack.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    sessions: List<CyclingSessionRecord>,
    onUpdate: (Long, Double, String?) -> Unit,
    onDelete: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<CyclingSessionRecord?>(null) }
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

    val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(sessions) { session ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { editing = session }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatter.format(session.startTime))
                        Text("${session.km} km · ${session.durationMin} min")
                        session.carga?.let { Text("Carga: $it") }
                    }
                    IconButton(onClick = { onDelete(session.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar")
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editar sessão")
        OutlinedTextField(
            value = km,
            onValueChange = { km = it },
            label = { Text("Km") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = carga,
            onValueChange = { carga = it },
            label = { Text("Carga (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = { km.toDoubleOrNull()?.let { onConfirm(it, carga.ifBlank { null }) } },
            modifier = Modifier.padding(top = 16.dp)
        ) { Text("Salvar") }
        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancelar") }
    }
}
