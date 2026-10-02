package com.biometric.authenticator.model

enum class BiometricType(val displayName: String) {
    FINGERPRINT("Fingerprint"),
    FACE("Face Recognition"),
    BIOMETRIC("Biometric Authentication"),
    NONE("None Available")
}

sealed interface BiometricStatus {
    data class Available(val type: BiometricType) : BiometricStatus
    data class NotAvailable(val reason: String) : BiometricStatus
    object NotEnrolled : BiometricStatus
    object HardwareUnavailable : BiometricStatus
}

sealed interface BiometricAuthResult {
    object Success : BiometricAuthResult
    data class Error(val message: String, val errorCode: Int = 0) : BiometricAuthResult
    object Failed : BiometricAuthResult
    object Canceled : BiometricAuthResult
}
