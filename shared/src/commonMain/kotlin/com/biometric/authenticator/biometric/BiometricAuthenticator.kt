package com.biometric.authenticator.biometric

import androidx.compose.runtime.Composable
import com.biometric.authenticator.model.BiometricAuthResult
import com.biometric.authenticator.model.BiometricStatus
import com.biometric.authenticator.model.BiometricType

interface BiometricAuthenticator {
    fun isBiometricAvailable(): BiometricStatus
    fun getBiometricType(): BiometricType
    suspend fun authenticate(
        title: String = "Biometric Authentication",
        subtitle: String = "Fingerprint / Biometric Verification",
        description: String = "Please authenticate using your fingerprint or biometric credential",
        negativeButtonText: String = "Cancel"
    ): BiometricAuthResult
}

@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
