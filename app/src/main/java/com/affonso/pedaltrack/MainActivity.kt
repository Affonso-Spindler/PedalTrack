package com.affonso.pedaltrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.affonso.pedaltrack.data.healthconnect.HealthConnectManagerImpl
import com.affonso.pedaltrack.data.local.PedalTrackDatabase
import com.affonso.pedaltrack.repository.CyclingRepositoryImpl
import com.affonso.pedaltrack.ui.navigation.PedalTrackNavHost
import com.affonso.pedaltrack.ui.theme.PedalTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManagerImpl
    private lateinit var repository: CyclingRepositoryImpl
    private var permissionsGranted by mutableStateOf<Boolean?>(null)

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        lifecycleScope.launch { permissionsGranted = healthConnectManager.hasAllPermissions() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = HealthConnectClient.getOrCreate(applicationContext)
        healthConnectManager = HealthConnectManagerImpl(client)
        val dao = PedalTrackDatabase.getInstance(applicationContext).cyclingSessionDao()
        repository = CyclingRepositoryImpl(dao, healthConnectManager)

        lifecycleScope.launch { permissionsGranted = healthConnectManager.hasAllPermissions() }

        setContent {
            PedalTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (permissionsGranted) {
                        true -> PedalTrackNavHost(repository = repository)
                        false -> PermissionRequestScreen(
                            onRequestClick = { requestPermissions.launch(healthConnectManager.permissions()) }
                        )
                        null -> {}
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { permissionsGranted = healthConnectManager.hasAllPermissions() }
    }
}

@Composable
private fun PermissionRequestScreen(onRequestClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("PedalTrack precisa de permissão para ler seus treinos de bike indoor e escrever a distância no Health Connect.")
        Button(onClick = onRequestClick, modifier = Modifier.padding(top = 16.dp)) {
            Text("Conceder permissão")
        }
    }
}
