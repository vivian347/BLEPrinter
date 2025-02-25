package com.kviv.ble.domain

sealed interface ConnectionResult {
    data object ConnectionEstablished: ConnectionResult
    data class Error(val message: String): ConnectionResult
    data object PairingInitiated: ConnectionResult
}