package com.kviv.ble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid

class BleScanner(
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var scanCallback: ScanCallback? = null
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private val printerServiceUuid = ParcelUuid.fromString("000018f0-0000-1000-8000-00805f9b34fb")
    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(printerServiceUuid)
        .build()



    @SuppressLint("MissingPermission")
    fun scanForPrinters(onDeviceFound:(BluetoothDevice) -> Unit) {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device
                device?.let { onDeviceFound(it) }
            }
        }
        scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        scanner?.stopScan(scanCallback)
    }
}