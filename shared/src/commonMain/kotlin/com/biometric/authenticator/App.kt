package com.biometric.authenticator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.biometric.authenticator.ui.BiometricAuthScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        BiometricAuthScreen()
    }
}
