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
import androidx.lifecycle.lifecycleScope
import com.affonso.pedaltrack.data.local.PedalTrackDatabase
import com.affonso.pedaltrack.data.samsunghealth.SamsungHealthManagerImpl
import com.affonso.pedaltrack.repository.CyclingRepositoryImpl
import com.affonso.pedaltrack.ui.navigation.PedalTrackNavHost
import com.affonso.pedaltrack.ui.theme.PedalTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: SamsungHealthManagerImpl
    private lateinit var repository: CyclingRepositoryImpl
    private var permissionsGranted by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = SamsungHealthManagerImpl(applicationContext)
        val dao = PedalTrackDatabase.getInstance(applicationContext).cyclingSessionDao()
        repository = CyclingRepositoryImpl(dao, healthConnectManager)

        lifecycleScope.launch { permissionsGranted = healthConnectManager.hasAllPermissions() }

        setContent {
            PedalTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (permissionsGranted) {
                        true -> PedalTrackNavHost(repository = repository)
                        false -> PermissionRequestScreen(
                            onRequestClick = {
                                lifecycleScope.launch {
                                    permissionsGranted = healthConnectManager.requestPermissions(this@MainActivity)
                                }
                            }
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
        Text("PedalTrack precisa de permissão para ler seus treinos de bike indoor no Samsung Health.")
        Button(onClick = onRequestClick, modifier = Modifier.padding(top = 16.dp)) {
            Text("Conceder permissão")
        }
    }
}
