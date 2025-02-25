package com.kviv.ble.domain

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val pairedDevices: StateFlow<List<BluetoothDevice>>

    fun scanForDevices()
    fun stopScanning()
    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>
    fun sendPrintData(data: String): Flow<PrintResult>
}