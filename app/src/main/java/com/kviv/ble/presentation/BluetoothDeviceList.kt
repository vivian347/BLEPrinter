package com.kviv.ble.presentation

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceList(
    pairedDevices: List<android.bluetooth.BluetoothDevice>,
    scannedDevices: List<android.bluetooth.BluetoothDevice>,
    onClick: (android.bluetooth.BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "Paired Devices",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(pairedDevices) {device ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onClick(device) }
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Default.BluetoothConnected,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = device.name ?: "(No Name)",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
        item {
            Text(
                text = "Available Devices",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (scannedDevices.isEmpty()) {
            item {
                Text(
                    text = "No Devices Found",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()

                )
            }
        }

        items(scannedDevices) {device ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onClick(device) }
            ) {
                Row {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = device.name ?: "(No Name)",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }




    }
}