package com.mutkuensert.androidkeystoreexample.keystorehelper

import androidx.fragment.app.FragmentActivity
import java.security.KeyPair

/**
 * BiometricKeyPairHandler manages the creation, deletion, and use of hardware-backed key pairs
 * with biometric authentication. This class utilizes KeyStoreHelper to interact with the Android KeyStore.
 *
 * @param alias The alias of the key entry in the KeyStore.
 */
class BiometricKeyPairHandler(alias: String) {
    private val keyStoreHelper = KeyStoreHelper(
        alias = alias,
        requireBiometricAuth = true
    )

    fun generateHardwareBackedKeyPair(activity: FragmentActivity): KeyPair? {
        if (!BiometricAuthHelper.isStrongBiometricAuthAvailable(activity)) {
            return null
        }

        return keyStoreHelper.generateHardwareBackedKeyPair()
    }

    fun deleteKeyPair(): Boolean {
        return keyStoreHelper.deleteKeyStoreEntry()
    }

    fun getPublicKeyBase64Encoded(keyPair: KeyPair): String {
        return keyStoreHelper.getPublicKeyBase64Encoded(keyPair)
    }

    fun verifyData(publicKey: String, data: String, signature: String): Boolean {
        return keyStoreHelper.verifyData(publicKey, data, signature)
    }

    /**
     * Authenticates the user via biometric authentication and signs the given data.
     * Calls the provided callback function with the signed data upon successful authentication.
     */
    fun authenticateAndSignData(
        data: String,
        activity: FragmentActivity,
        onAuthenticationSucceeded: (SignedData?) -> Unit
    ) {
        BiometricAuthHelper.authenticate(activity, onAuthenticationSucceeded = {
            onAuthenticationSucceeded(keyStoreHelper.signData(data))
        })
    }

    fun exists(): Boolean? {
        return keyStoreHelper.exists()
    }
}