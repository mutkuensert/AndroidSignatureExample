package com.mutkuensert.androidsignatureexample.signature.biometric

import androidx.fragment.app.FragmentActivity
import com.mutkuensert.androidsignatureexample.BuildConfig
import com.mutkuensert.androidsignatureexample.biometricauth.BiometricAuth
import com.mutkuensert.androidsignatureexample.signature.KeyPairManager
import com.mutkuensert.androidsignatureexample.signature.SignedData
import java.security.KeyPair

/**
 * [BiometricAuthRestrictedKeyPairManager] is a [KeyPairManager] handles biometric authentication processes.
 *
 * @param alias Alias of key entry in [java.security.KeyStore].
 */
class BiometricAuthRestrictedKeyPairManager(alias: String) :
    KeyPairManager(alias, restrictToBiometricAuth = true) {
    private val biometricAuth = BiometricAuth()
    private var failedPromptCounter = 0

    /**
     * returns [KeyPair] if strong biometric is enrolled and
     * private key is inside secure hardware or device is emulator, otherwise null.
     */
    fun generateHardwareBackedKeyPair(activity: FragmentActivity): KeyPair? {
        if (!biometricAuth.isStrongBiometricEnrolled(activity)) {
            return null
        }

        return if (BuildConfig.IS_EMULATOR) {
            // Because emulators don't have Trusted Execution Environment
            generateKeyPair()
        } else {
            generateHardwareBackedKeyPair()
        }
    }

    /**
     * Authenticates the user via biometric authentication and signs [data].
     * Calls [onAuthenticationSucceeded] with [SignedData] if biometric authentication is successful.
     */
    fun authenticateAndSignData(
        data: String,
        activity: FragmentActivity,
        onAuthenticationSucceeded: (SignedData?) -> Unit
    ) {
        failedPromptCounter = 0

        biometricAuth.authenticate(
            activity,
            onAuthenticationSucceeded = {
                onAuthenticationSucceeded(signData(data))
            },
            onAuthenticationFailed = ::closePromptWhenFailedToLimit
        )
    }

    private fun closePromptWhenFailedToLimit() {
        val limit = 4
        failedPromptCounter++

        if (failedPromptCounter == limit) {
            biometricAuth.closePrompt()
        }
    }
}