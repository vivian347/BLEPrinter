package com.kviv.ble.presentation

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kviv.ble.MainState
import com.kviv.ble.MainViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BluetoothScreenContent(
    viewModel: MainViewModel,
    multiplePermissionResultLauncher: ActivityResultLauncher<Array<String>>,
    permissionsToRequest: Array<String>,
    navController: NavController
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
            ) {
                BluetoothScreenTopBar(
                    onBackButtonClick = {
//                        navController.popBackStack()
                    }
                )
            }
        }
    ) { paddingValues ->
        val _state = viewModel.state.collectAsState()
        val state = _state.value
        when {
            state.isConnected -> {
                Log.d("Bluetooth", "Printing data-----------------------")
                navController.navigate("print")
//                viewModel.printData()
            }

            state.isConnecting -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Text(text = "Connecting")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    BluetoothDeviceList(
                        pairedDevices = state.pairedDevices,
                        scannedDevices = state.scannedDevices,
                        onClick = viewModel::connectToDevice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(
                            onClick = {
                                multiplePermissionResultLauncher.launch(permissionsToRequest)
                                viewModel.startScan()
                            },
                            enabled = !state.isScanning,
                            modifier = Modifier.padding(12.dp),
                            shape = RoundedCornerShape(40),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            if (state.isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "Start scan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.stopScanning() },
                            modifier = Modifier.padding(12.dp),
                            shape = RoundedCornerShape(40),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            Text(
                                text = "Stop scan",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
