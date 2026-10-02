package com.biometric.authenticator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform