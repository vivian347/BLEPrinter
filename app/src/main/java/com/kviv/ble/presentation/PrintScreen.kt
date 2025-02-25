package com.kviv.ble.presentation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.unit.dp
import com.kviv.ble.MainViewModel

@Composable
fun PrintScreen(
    viewModel: MainViewModel
) {
    Button(
        onClick = {
            viewModel.printData()
        },
        modifier = Modifier
            .padding(12.dp),
        shape = RoundedCornerShape(40),
        colors = ButtonDefaults.buttonColors(
            containerColor = Blue
        )
    ) {
        Text(
            text = "Print data",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 20.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreenTopBar(onBackButtonClick: () -> Unit){
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back"
                )
            }
        },
        title = { Text(
            text = "Configure Bluetooth",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        ) },
        colors = TopAppBarDefaults.topAppBarColors(
            MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .height(51.dp)

    )
}