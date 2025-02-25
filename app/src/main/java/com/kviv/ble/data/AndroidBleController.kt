package com.kviv.ble.data

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kviv.ble.domain.BleController
import com.kviv.ble.domain.ConnectionResult
import com.kviv.ble.domain.PrintResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"


@SuppressLint("MissingPermission")
class AndroidBleController(
    private val context: Context
): BleController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var gatt: BluetoothGatt? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDevice>>
        get() = _pairedDevices.asStateFlow()


    private fun addScannedDevice(device: BluetoothDevice) {
        _scannedDevices.update { devices ->
            if (devices.none{ it.address == device.address}) {
                devices + device
            } else{
                devices
            }
        }
    }

    private fun addPairedDevice(device: BluetoothDevice) {
        _pairedDevices.update { pairedDevices ->
            if (pairedDevices.none { it.address == device.address }) {
                pairedDevices + device
            } else {
                pairedDevices
            }
        }

        _scannedDevices.update { devices ->
            val scannedList = devices.toMutableList()

            scannedList.removeAll { it.address == device.address }

            scannedList
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun BluetoothGattCharacteristic.isWritable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse() = containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun BluetoothGattDescriptor.isWritable(): Boolean =
        containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE) ||
                containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) ||
                containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) ||
                containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) ||
                containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM)

    fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
        permissions and permission != 0



    override fun scanForDevices() {
        if (SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        } else {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return
        }
        bluetoothAdapter?.let {
            BleScanner(it).scanForPrinters { device ->
                if (device.bondState == BluetoothDevice.BOND_BONDED) {
                    addPairedDevice(device)
                } else {
                    addScannedDevice(device)
                }
            }
        }
    }

    override fun stopScanning() {
        bluetoothAdapter?.let { BleScanner(it).stopScanning() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun bondDevice(device: BluetoothDevice): Boolean {
        return suspendCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                        val bondedDevice: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)

                        if (bondedDevice?.address == device.address) {
                            when (bondState) {
                                BluetoothDevice.BOND_BONDED -> {
                                    Log.d("BLEDV", "Device successfully bonded.")
                                    context?.unregisterReceiver(this)
                                    continuation.resume(true)
                                }
                                BluetoothDevice.BOND_NONE -> {
                                    Log.e("BLEDV", "Bonding failed.")
                                    context?.unregisterReceiver(this)
                                    continuation.resume(false)
                                }
                            }
                        }
                    }
                }
            }

            val intentFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            context.registerReceiver(receiver, intentFilter)

            val bondingStarted = device.createBond()
            if (!bondingStarted) {
                Log.e("BLEDV", "Failed to start bonding process.")
                context.unregisterReceiver(receiver)
                continuation.resume(false)
            }
        }
    }

    private suspend fun retryConnection(device: BluetoothDevice, retries: Int = 3, delayMs: Long = 1000): Boolean {
        repeat(retries) { attempt ->
            Log.d("BLEDV", "Retrying connection... Attempt ${attempt + 1}/$retries")
            gatt?.close()
            gatt = null
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                Log.d("BLEDV", "Device is not bonded. Initiating bonding...")
                if (!bondDevice(device)) {
                    Log.e("BLEDV", "Bonding failed. Cannot proceed with connection.")
                    return false
                }
            }
            val result = suspendCoroutine<Boolean> { continuation ->
                val gattCallback = BleGattCallBack(
                    onConnected = {
                        _isConnected.update { true }
                        continuation.resume(true)
                    },
                    onServiceDiscovered = { gatt = it },
                    onError = { errorMessage ->
                        _isConnected.update { false }
                        continuation.resume(false)
                    }
                )
                val gatt = device.connectGatt(null, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                if (gatt == null) continuation.resume(false)
            }
            if (result) return true
            delay(delayMs) // Wait before retrying
        }
        return false
    }

    override fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult> = channelFlow {
        stopScanning()
        if (!retryConnection(device)) {
            send(ConnectionResult.Error("Failed to connect to device after retries"))
            close()
        } else{
            send(ConnectionResult.ConnectionEstablished)
        }

        awaitClose {
            gatt?.close()
            _isConnected.update { false }
        }
    }.flowOn(Dispatchers.IO)


    override fun sendPrintData(data: String): Flow<PrintResult> = flow<PrintResult> {
        val characteristic = bluetoothGatt?.services?.flatMap { service ->
            service.characteristics.map { characteristic ->
                characteristic
            }
        }?.find { it.uuid.toString().equals("00002af1-0000-1000-8000-00805f9b34fb", ignoreCase = true) }


        if (characteristic != null) {
            Log.d("Bluetooth", "Characteristic settled on: ${characteristic.uuid}")
        }

        val payload = data.toByteArray()
        val writeType = when {
            characteristic?.isWritable() == true -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic?.isWritableWithoutResponse() == true -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> {
                emit(PrintResult.Error("Connected device cannot print"))
                return@flow
            }
        }

        bluetoothGatt?.let { gatt ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, payload, writeType)
            } else {
                // Fall back to deprecated version of writeCharacteristic for Android <13
                gatt.legacyCharacteristicWrite(characteristic, payload, writeType)
            }
        } ?: error("Not connected to a Bluetooth device!")
    }.flowOn(Dispatchers.IO)

    @TargetApi(Build.VERSION_CODES.S)
    @Suppress("DEPRECATION")
    private fun BluetoothGatt.legacyCharacteristicWrite(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int
    ) {
        characteristic.writeType = writeType
        characteristic.value = value
        writeCharacteristic(characteristic)
    }

}