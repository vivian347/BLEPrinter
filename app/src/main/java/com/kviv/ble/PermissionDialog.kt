package com.kviv.ble

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Permission required")
        },
        text = {
            Text(text = permissionTextProvider.getDescription(isPermanentlyDeclined = isPermanentlyDeclined))
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider()
                Text(
                    text = if (isPermanentlyDeclined) {
                        "Grant Permission"
                    } else { "OK" },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPermanentlyDeclined) {
                                onGoToAppSettingsClick()
                            } else {
                                onOkClick()
                            }
                        }
                        .padding(16.dp)
                )
            }
        },
        modifier = modifier
    )

}

interface PermissionTextProvider{
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class BluetoothPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined bluetooth permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "To connect to your Bluetooth printer and scale, this app needs permission to scan and communicate with nearby Bluetooth devices.\n" +
                    "\n" +
                    "We only use Bluetooth for device connections and do not access or store any personal data.\n" +
                    "\n" +
                    "Would you like to grant permission?."
        }
    }
}

class LocationPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined location permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "To connect to your Bluetooth printer and scale, this app needs access to your location. This is required by Android to enable Bluetooth scanning and ensure a seamless connection to your work devices.\n" +
                    "\n" +
                    "We do not track or store your location—this is only used for Bluetooth functionality.\n" +
                    "\n" +
                    "Would you like to grant permission?"
        }
    }
}