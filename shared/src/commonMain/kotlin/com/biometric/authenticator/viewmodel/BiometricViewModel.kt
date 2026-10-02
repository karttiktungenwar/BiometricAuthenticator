package com.biometric.authenticator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biometric.authenticator.biometric.BiometricAuthenticator
import com.biometric.authenticator.model.BiometricAuthResult
import com.biometric.authenticator.model.BiometricStatus
import com.biometric.authenticator.model.BiometricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Idle : AuthState
    object Authenticating : AuthState
    data class Success(val message: String) : AuthState
    data class Error(val message: String) : AuthState
}

data class AuthLogEntry(
    val id: String,
    val timeString: String,
    val status: String,
    val details: String,
    val isSuccess: Boolean
)

data class BiometricUiState(
    val biometricStatus: BiometricStatus = BiometricStatus.NotAvailable("Checking..."),
    val biometricType: BiometricType = BiometricType.NONE,
    val authState: AuthState = AuthState.Idle,
    val isAuthenticated: Boolean = false,
    val secretVaultData: String? = null,
    val authLogs: List<AuthLogEntry> = emptyList()
)

class BiometricViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricUiState())
    val uiState: StateFlow<BiometricUiState> = _uiState.asStateFlow()

    private var logCounter = 1

    fun checkBiometricAvailability(authenticator: BiometricAuthenticator) {
        val status = authenticator.isBiometricAvailable()
        val type = authenticator.getBiometricType()
        _uiState.update {
            it.copy(
                biometricStatus = status,
                biometricType = type
            )
        }
    }

    fun authenticate(authenticator: BiometricAuthenticator) {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Authenticating) }

            val result = authenticator.authenticate(
                title = "Biometric Security Check",
                subtitle = "Confirm your identity",
                description = "Touch fingerprint sensor or scan face to unlock vault",
                negativeButtonText = "Cancel"
            )

            val timeString = formatTime(logCounter)

            when (result) {
                is BiometricAuthResult.Success -> {
                    val log = AuthLogEntry(
                        id = "LOG-#$logCounter",
                        timeString = timeString,
                        status = "SUCCESS",
                        details = "Biometric match verified. Vault unlocked.",
                        isSuccess = true
                    )
                    logCounter++
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            authState = AuthState.Success("Successfully Authenticated!"),
                            secretVaultData = "AES-256 Key: 0x8F9A4B21-CONFIDENTIAL-SESSION-TOKEN",
                            authLogs = listOf(log) + it.authLogs
                        )
                    }
                }
                is BiometricAuthResult.Error -> {
                    val log = AuthLogEntry(
                        id = "LOG-#$logCounter",
                        timeString = timeString,
                        status = "ERROR",
                        details = result.message,
                        isSuccess = false
                    )
                    logCounter++
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Error(result.message),
                            authLogs = listOf(log) + it.authLogs
                        )
                    }
                }
                is BiometricAuthResult.Failed -> {
                    val log = AuthLogEntry(
                        id = "LOG-#$logCounter",
                        timeString = timeString,
                        status = "FAILED",
                        details = "Fingerprint / Face not recognized.",
                        isSuccess = false
                    )
                    logCounter++
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Error("Biometric not recognized. Please try again."),
                            authLogs = listOf(log) + it.authLogs
                        )
                    }
                }
                is BiometricAuthResult.Canceled -> {
                    val log = AuthLogEntry(
                        id = "LOG-#$logCounter",
                        timeString = timeString,
                        status = "CANCELED",
                        details = "User canceled authentication prompt.",
                        isSuccess = false
                    )
                    logCounter++
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Idle,
                            authLogs = listOf(log) + it.authLogs
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                secretVaultData = null,
                authState = AuthState.Idle
            )
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(authLogs = emptyList()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(authState = AuthState.Idle) }
    }

    private fun formatTime(attempt: Int): String {
        return "Attempt #$attempt"
    }
}
