package com.kviv.ble

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kviv.ble.domain.BleController
import com.kviv.ble.domain.ConnectionResult
import com.kviv.ble.domain.PrintResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bleController: BleController
) : ViewModel() {
    val visiblePermissionDialogQueue = mutableStateListOf<String>()
    private val _state = MutableStateFlow(MainState())

    private var deviceConnectionJob: Job? = null
    private val _printResult = MutableStateFlow<PrintResult?>(null)
    val printResult: StateFlow<PrintResult?>
        get() = _printResult

    val state = combine(
        bleController.scannedDevices,
        bleController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun dismissDialog() {
        visiblePermissionDialogQueue.removeFirst()
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ){
        if (!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
            visiblePermissionDialogQueue.add(permission)
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            bleController.scanForDevices()
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            bleController.stopScanning()
            _state.update { it.copy(isScanning = false) }
        }
    }

    fun connectToDevice(device: BluetoothDevice){
        Log.d("Bluetooth", "Trying to connect to printer")
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true) }
            deviceConnectionJob?.cancel()
            deviceConnectionJob = bleController
                .connectToDevice(device)
                .listen()
        }
    }

    fun printData() {
        viewModelScope.launch {
            val data = "This is test data\n\n\n T\nE\nS\nT\n\n\nD\nA\nT\nA\n\n\n\n"
            delay(1000L)
            bleController.sendPrintData(data = data).collect{ result ->
                _printResult.value = result
            }
        }
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when(result) {
                is ConnectionResult.ConnectionEstablished -> {
                    Log.d("Bluetooth", "Connection established")
                    _state.update { it.copy(
                        isConnected = true,
                        isConnecting = false,
                        error = null
                    ) }
//                    bluetoothController.isConnected.value = true
                }
                is ConnectionResult.Error -> {
                    Log.e("Bluetooth", "Connection NOT established")
                    _state.update { it.copy(
                        isConnected = false,
                        isConnecting = false,
                        error = result.message
                    ) }
                }

                ConnectionResult.PairingInitiated -> {
                    // Update the state to reflect that pairing has started
                    Log.d("Bluetooth", "Pairing initiated")
                    _state.update { it.copy(
                        isConnecting = true,  // Still in the process of connecting
                        error = "Pairing with the device..."  // Optional: Show a message
                    ) }
                }
            }
        }.launchIn(viewModelScope)
    }
}