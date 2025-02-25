package com.kviv.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kviv.ble.domain.PrintResult
import com.kviv.ble.presentation.BluetoothDeviceList
import com.kviv.ble.presentation.BluetoothScreenContent
import com.kviv.ble.presentation.BluetoothScreenTopBar
import com.kviv.ble.presentation.PrintScreen
import com.kviv.ble.ui.theme.BLETheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionsToRequest = if (SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }else {
        arrayOf( Manifest.permission.ACCESS_FINE_LOCATION)
    }
    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("Bluetooth", "Bluetooth enabled successfully")
        } else {
            Log.d("Bluetooth", "Bluetooth enable request denied")
        }
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasRequiredBluetoothPermission(): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    override fun onResume() {
        super.onResume()
        if (!isBluetoothEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            // Insufficient permission to prompt for Bluetooth enabling
            return
        }
        if (!isBluetoothEnabled) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                enableBluetoothLauncher.launch(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLETheme {
                val viewModel: MainViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()
                val printResult by viewModel.printResult.collectAsState()
                val dialogQueue = viewModel.visiblePermissionDialogQueue

                val context = LocalContext.current
                LaunchedEffect(printResult) {
                    when (printResult) {
                        is PrintResult.Success -> {
                            Toast.makeText(context, "Print successful", Toast.LENGTH_LONG).show()
                        }
                        is PrintResult.Error -> {
                            val errorMessage = (printResult as PrintResult.Error).message
                            Toast.makeText(context, "Print failed: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }

                LaunchedEffect(viewModel) {
                    if (state.isConnected) {
                        Log.d("Bluetooth", "Printing data-----------------------")
                        viewModel.printData()
                    }
                }

                val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        val allGranted = permissionsToRequest.all { perms[it] == true }
                        permissionsToRequest.forEach { permission ->
                            viewModel.onPermissionResult(
                                permission = permission,
                                isGranted = perms[permission] == true
                            )
                        }
                        if (allGranted) {
                            Log.d("Bluetooth", "All permissions granted")

                            // Turn on Bluetooth if it's not enabled
                            if (!isBluetoothEnabled) {
                                Log.d("Bluetooth", "Turning on Bluetooth")
                                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            } else {
                                Log.d("Bluetooth", "Bluetooth is already enabled")
                            }
                        } else {
                            Log.d("Bluetooth", "Some permissions were not granted")
                        }
                    }
                )

//                Scaffold(
//                    topBar = {
//                        Surface(
//                            shadowElevation = 3.dp,
//                        ){
//                            BluetoothScreenTopBar(
//                                onBackButtonClick = {
////                                    navController.popBackStack()
//                                }
//                            )
//                        }
//                    }
//                ) {paddingValues ->
//                    when {
//                        state.isConnected -> {
//                            Log.d("Bluetooth", "Printing data-----------------------")
//                            viewModel.printData()
//                        }
//                        state.isConnecting -> {
//                            Column(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(230.dp),
//                                horizontalAlignment = Alignment.CenterHorizontally,
//                                verticalArrangement = Arrangement.Center
//                            ) {
//                                Card(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(horizontal = 24.dp),
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = Color.LightGray.copy(alpha = 0.3f)
//                                    )
//                                ) {
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(vertical = 16.dp),
//                                        contentAlignment = Alignment.Center
//                                    ) {
//                                        CircularProgressIndicator(
//                                            modifier = Modifier.size(40.dp)
//                                        )
//                                        Text(text = "Connecting")
//                                    }
//
//                                }
//                            }
//                        }
//                        else -> {
//                            Column(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .padding(paddingValues)
//                            ) {
//                                BluetoothDeviceList(
//                                    pairedDevices = state.pairedDevices,
//                                    scannedDevices = state.scannedDevices,
//                                    onClick = viewModel::connectToDevice,
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .weight(1f)
//                                )
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceAround
//                                ) {
//                                    Button(
//                                        onClick = {
//                                            multiplePermissionResultLauncher.launch(permissionsToRequest)
//                                            viewModel.startScan()
//                                        },
//                                        enabled = !state.isScanning,
//                                        modifier = Modifier
//                                            .padding(12.dp),
//                                        shape = RoundedCornerShape(40),
//                                        colors = ButtonDefaults.buttonColors(
//                                            containerColor = Blue
//                                        )
//                                    ) {
//                                        if (state.isScanning) {
//                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                                        } else {
//                                            Text(
//                                                text = "Start scan",
//                                                style = MaterialTheme.typography.bodyMedium,
//                                                modifier = Modifier
//                                                    .padding(horizontal = 20.dp)
//                                            )
//                                        }
//                                    }
//                                    Button(
//                                        onClick = {
//                                            viewModel.stopScanning()
//                                        },
//                                        modifier = Modifier
//                                            .padding(12.dp),
//                                        shape = RoundedCornerShape(40),
//                                        colors = ButtonDefaults.buttonColors(
//                                            containerColor = Blue
//                                        )
//                                    ) {
//                                        Text(
//                                            text = "Stop scan",
//                                            style = MaterialTheme.typography.bodyMedium,
//                                            modifier = Modifier
//                                                .padding(horizontal = 20.dp)
//                                        )
//                                    }
//                                }
//
//                            }
//
//                        }
//                    }
//
//                }

                // Show permission dialogs if needed
                dialogQueue.reversed().forEach { permission ->
                    PermissionDialog(
                        permissionTextProvider = when (permission) {
                            Manifest.permission.BLUETOOTH_CONNECT -> BluetoothPermissionTextProvider()
                            Manifest.permission.BLUETOOTH_SCAN -> BluetoothPermissionTextProvider()
                            Manifest.permission.ACCESS_FINE_LOCATION -> LocationPermissionTextProvider()
                            else -> return@forEach
                        },
                        isPermanentlyDeclined = !shouldShowRequestPermissionRationale(permission),
                        onDismiss = viewModel::dismissDialog,
                        onOkClick = {
                            viewModel.dismissDialog()
                            multiplePermissionResultLauncher.launch(arrayOf(permission))
                        },
                        onGoToAppSettingsClick = ::openAppSettings
                    )
                }

                // ✅ Add your screens here inside BLETheme
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") { entry ->
                        val mainViewModel: MainViewModel? = navController
                            .currentBackStackEntry
                            ?.let { hiltViewModel(it) }

                        if (mainViewModel != null) {
                            BluetoothScreenContent(
                                navController = navController,
                                viewModel = mainViewModel,
                                multiplePermissionResultLauncher = multiplePermissionResultLauncher,
                                permissionsToRequest = permissionsToRequest
                            )
                        }
                    }
                    composable("print") { entry ->
                        val mainViewModel: MainViewModel? = navController
                            .currentBackStackEntry
                            ?.let { hiltViewModel(it) }
                        if (mainViewModel != null) {
                            PrintScreen(viewModel = mainViewModel)
                        }
                    }
                }
            }
        }
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
