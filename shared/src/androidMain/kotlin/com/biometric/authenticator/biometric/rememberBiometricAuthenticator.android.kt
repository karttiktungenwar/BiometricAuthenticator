package com.biometric.authenticator.biometric

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.biometric.authenticator.model.BiometricAuthResult
import com.biometric.authenticator.model.BiometricStatus
import com.biometric.authenticator.model.BiometricType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidBiometricAuthenticator(
    private val context: Context
) : BiometricAuthenticator {

    private val biometricManager = BiometricManager.from(context)

    override fun isBiometricAvailable(): BiometricStatus {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricStatus.Available(getBiometricType())
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricStatus.NotAvailable("Security update required")
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                BiometricStatus.NotAvailable("Biometrics not supported on this device")
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricStatus.NotAvailable("Biometric status unknown")
            else -> BiometricStatus.NotAvailable("Biometric authentication unavailable")
        }
    }

    override fun getBiometricType(): BiometricType {
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else false

        return when {
            hasFingerprint && !hasFace -> BiometricType.FINGERPRINT
            hasFace && !hasFingerprint -> BiometricType.FACE
            hasFingerprint && hasFace -> BiometricType.FINGERPRINT
            else -> BiometricType.BIOMETRIC
        }
    }

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String
    ): BiometricAuthResult = withContext(Dispatchers.Main) {
        val activity = context.findFragmentActivity()
            ?: return@withContext BiometricAuthResult.Error(
                "Host Activity must be a FragmentActivity to display BiometricPrompt"
            )

        suspendCancellableCoroutine<BiometricAuthResult> { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(BiometricAuthResult.Success)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            continuation.resume(BiometricAuthResult.Canceled)
                        } else {
                            continuation.resume(BiometricAuthResult.Error(errString.toString(), errorCode))
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    if (continuation.isActive) {
                        continuation.resume(BiometricAuthResult.Failed)
                    }
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) {
        AndroidBiometricAuthenticator(context)
    }
}
