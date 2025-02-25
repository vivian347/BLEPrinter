package com.kviv.ble.domain

sealed interface PrintResult {
    data object Success : PrintResult
    data class Error(val message: String) : PrintResult
}