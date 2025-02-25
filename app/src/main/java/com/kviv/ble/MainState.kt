package com.kviv.ble

import android.bluetooth.BluetoothDevice

data class MainState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isScanning: Boolean = false,
    val error: String? = null
)