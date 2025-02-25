package com.kviv.ble.data

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.util.UUID

var bluetoothGatt: BluetoothGatt? = null
class BleGattCallBack(
    private val onConnected:  () -> Unit,
    private val onServiceDiscovered: suspend (BluetoothGatt) -> Unit,
    private val onError: suspend (String) -> Unit
): BluetoothGattCallback() {
    private val SERVICE_UUID = UUID.fromString("000018f0-0000-1000-8000-00805f9b34fb") // Replace with your printer's service UUID
    private val CHARACTERISTIC_UUID = UUID.fromString("00002af1-0000-1000-8000-00805f9b34fb") // Replace with your write characteristic UUID

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        val deviceAddress = gatt?.device?.address
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("Bluetooth", "Successfully connected to $deviceAddress")
                gatt?.requestMtu(512)
                bluetoothGatt = gatt
                Handler(Looper.getMainLooper()).post{
                    bluetoothGatt?.discoverServices()
                }
                onConnected()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLEDV", "Disconnected from printer")
                gatt?.close()
                runBlocking { onError("Disconnected") }
            }
        } else {
            Log.e("BLEDV", "GATT connection failed with status: $status")
            gatt?.close()
            runBlocking { onError("GATT connection failed with status: $status") }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        with(gatt) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic = gatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    enableNotification(characteristic)
                }
                runBlocking { gatt?.let { onServiceDiscovered(it) } }
            } else {
                runBlocking { onError("Service discovery failed") } // Instead of launching coroutine
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        Log.w("Bluetooth", "ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        with(characteristic) {
           Log.d("Bluetooth", "onCharacWrite Callback status: $status | characterist : $characteristic")
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.i("BluetoothGattCallback", "Wrote to characteristic $uuid }")
                }
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                    Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                }
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                    Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                }
                else -> {
                    Log.e("BluetoothGattCallback", "Characteristic write failed for $uuid, error: $status")
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Deprecated("Deprecated for Android 13+")
    @Suppress("DEPRECATION")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        with(characteristic) {
            Log.i("Bluetooth", "Characteristic $uuid changed | value: ${value.toHexString()}")
        }
    }
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val newValueHex = value.toHexString()
        with(characteristic) {
            Log.i("Bluetooth", "Characteristic $uuid changed | value: $newValueHex")
        }
    }
}
fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
    return properties and property != 0
}

fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)


@SuppressLint("MissingPermission")
fun writeDescriptor(descriptor: BluetoothGattDescriptor, payLoad: ByteArray) {
    bluetoothGatt?.let { gatt ->
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, payLoad)
        } else {
            gatt.legacyDescriptorWrite(descriptor, payLoad)
        }
    }
}

@SuppressLint("MissingPermission")
@TargetApi(Build.VERSION_CODES.S)
@Suppress("DEPRECATION")
private fun BluetoothGatt.legacyDescriptorWrite(
    descriptor: BluetoothGattDescriptor,
    value: ByteArray
): Boolean {
    descriptor.value = value
    return writeDescriptor(descriptor)
}

@SuppressLint("MissingPermission")
fun enableNotification(characteristic: BluetoothGattCharacteristic) {
    Log.d("Bluetooth", "We are enabling notifications")
    val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
    val payLoad = when {
        characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        else -> {
            Log.d("Bluetooth", "Doesn't support notifications")
            return
        }
    }

    Log.d("Bluetooth", "My payload is: $payLoad")
    characteristic.getDescriptor(cccdUuid)?.let { ccDescriptor ->
        if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
            Log.e("Bluetooth", "setCharacteristicNotification failed for ${characteristic.uuid}")
            return
        }
        writeDescriptor(ccDescriptor, payLoad)
    }?: Log.e("Bluetooth", "${characteristic.uuid} doesn't contain the CCC descriptor!")
}

@SuppressLint("MissingPermission")
fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
    if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
        Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
        return
    }

    val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
    characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
        if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
            Log.e("Bluetooth", "setCharacteristicNotification failed for ${characteristic.uuid}")
            return
        }
        writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
    } ?: Log.e("Bluetooth", "${characteristic.uuid} doesn't contain the CCC descriptor!")
}