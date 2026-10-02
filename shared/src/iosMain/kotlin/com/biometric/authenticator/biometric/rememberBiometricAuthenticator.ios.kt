package com.biometric.authenticator.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.biometric.authenticator.model.BiometricAuthResult
import com.biometric.authenticator.model.BiometricStatus
import com.biometric.authenticator.model.BiometricType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LABiometryTypeNone
import platform.LocalAuthentication.LABiometryTypeOpticID
import platform.LocalAuthentication.LABiometryTypeTouchID
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

class IOSBiometricAuthenticator : BiometricAuthenticator {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun isBiometricAvailable(): BiometricStatus {
        val laContext = LAContext()
        return memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val canEvaluate = laContext.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                errorPtr.ptr
            )

            if (canEvaluate) {
                val type = getBiometricTypeForContext(laContext)
                BiometricStatus.Available(type)
            } else {
                val nsError = errorPtr.value
                val errorCode = nsError?.code
                when (errorCode) {
                    LAErrorBiometryNotEnrolled -> BiometricStatus.NotEnrolled
                    LAErrorBiometryNotAvailable -> BiometricStatus.HardwareUnavailable
                    else -> {
                        val message = nsError?.localizedDescription ?: "Biometric authentication not available"
                        BiometricStatus.NotAvailable(message)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getBiometricType(): BiometricType {
        val laContext = LAContext()
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            laContext.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                errorPtr.ptr
            )
        }
        return getBiometricTypeForContext(laContext)
    }

    private fun getBiometricTypeForContext(laContext: LAContext): BiometricType {
        return when (laContext.biometryType) {
            LABiometryTypeTouchID -> BiometricType.FINGERPRINT
            LABiometryTypeFaceID -> BiometricType.FACE
            LABiometryTypeOpticID -> BiometricType.FACE
            LABiometryTypeNone -> BiometricType.NONE
            else -> BiometricType.BIOMETRIC
        }
    }

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String
    ): BiometricAuthResult = suspendCancellableCoroutine { continuation ->
        val laContext = LAContext()
        laContext.localizedCancelTitle = negativeButtonText

        laContext.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = description
        ) { success, nsError ->
            if (continuation.isActive) {
                if (success) {
                    continuation.resume(BiometricAuthResult.Success)
                } else if (nsError != null) {
                    if (nsError.code == LAErrorUserCancel) {
                        continuation.resume(BiometricAuthResult.Canceled)
                    } else {
                        continuation.resume(
                            BiometricAuthResult.Error(
                                nsError.localizedDescription,
                                nsError.code.toInt()
                            )
                        )
                    }
                } else {
                    continuation.resume(BiometricAuthResult.Failed)
                }
            }
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    return remember { IOSBiometricAuthenticator() }
}
