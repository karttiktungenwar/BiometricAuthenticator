package com.biometric.authenticator

import com.biometric.authenticator.biometric.BiometricAuthenticator
import com.biometric.authenticator.model.BiometricAuthResult
import com.biometric.authenticator.model.BiometricStatus
import com.biometric.authenticator.model.BiometricType
import com.biometric.authenticator.viewmodel.AuthState
import com.biometric.authenticator.viewmodel.BiometricViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeBiometricAuthenticator(
    var status: BiometricStatus = BiometricStatus.Available(BiometricType.FINGERPRINT),
    var type: BiometricType = BiometricType.FINGERPRINT,
    var result: BiometricAuthResult = BiometricAuthResult.Success
) : BiometricAuthenticator {

    override fun isBiometricAvailable(): BiometricStatus = status

    override fun getBiometricType(): BiometricType = type

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String
    ): BiometricAuthResult = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCheckBiometricAvailability() {
        val viewModel = BiometricViewModel()
        val fakeAuth = FakeBiometricAuthenticator(
            status = BiometricStatus.Available(BiometricType.FINGERPRINT),
            type = BiometricType.FINGERPRINT
        )

        viewModel.checkBiometricAvailability(fakeAuth)

        val state = viewModel.uiState.value
        assertTrue(state.biometricStatus is BiometricStatus.Available)
        assertEquals(BiometricType.FINGERPRINT, state.biometricType)
    }

    @Test
    fun testAuthenticationSuccess() = runTest {
        val viewModel = BiometricViewModel()
        val fakeAuth = FakeBiometricAuthenticator(result = BiometricAuthResult.Success)

        viewModel.authenticate(fakeAuth)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertTrue(state.authState is AuthState.Success)
        assertNotNull(state.secretVaultData)
        assertEquals(1, state.authLogs.size)
        assertTrue(state.authLogs[0].isSuccess)
    }

    @Test
    fun testAuthenticationFailure() = runTest {
        val viewModel = BiometricViewModel()
        val fakeAuth = FakeBiometricAuthenticator(result = BiometricAuthResult.Error("Access Denied", 101))

        viewModel.authenticate(fakeAuth)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertTrue(state.authState is AuthState.Error)
        assertNull(state.secretVaultData)
        assertEquals(1, state.authLogs.size)
        assertFalse(state.authLogs[0].isSuccess)
    }

    @Test
    fun testLogoutClearsVault() = runTest {
        val viewModel = BiometricViewModel()
        val fakeAuth = FakeBiometricAuthenticator(result = BiometricAuthResult.Success)

        viewModel.authenticate(fakeAuth)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.logout()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertNull(state.secretVaultData)
        assertEquals(AuthState.Idle, state.authState)
    }

    @Test
    fun testClearLogs() = runTest {
        val viewModel = BiometricViewModel()
        val fakeAuth = FakeBiometricAuthenticator(result = BiometricAuthResult.Success)

        viewModel.authenticate(fakeAuth)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.authLogs.size)

        viewModel.clearLogs()

        assertEquals(0, viewModel.uiState.value.authLogs.size)
    }
}
