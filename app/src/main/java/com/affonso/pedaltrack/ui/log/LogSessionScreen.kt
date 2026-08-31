package com.affonso.pedaltrack.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.affonso.pedaltrack.domain.HealthConnectSession
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
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault()) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Text(formatter.format(session.startTime))
            Text("${session.durationMin} min")
            Text(
                "Calorias: ${session.calories?.let { "%.0f".format(it) } ?: "—"} · " +
                    "FC média: ${session.avgHeartRate?.toString() ?: "—"} bpm"
            )
        }
    }
}

@Composable
private fun LogSessionForm(
    session: HealthConnectSession,
    onCancel: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var km by remember { mutableStateOf("") }
    var carga by remember { mutableStateOf("") }
    var kmError by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sessão de ${session.durationMin} min")
        OutlinedTextField(
            value = km,
            onValueChange = { km = it; kmError = false },
            label = { Text("Km") },
            isError = kmError,
            supportingText = { if (kmError) Text("Km inválido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = carga,
            onValueChange = { carga = it },
            label = { Text("Carga (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
            modifier = Modifier.padding(top = 16.dp)
        ) { Text("Salvar") }
        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancelar") }
    }
}
