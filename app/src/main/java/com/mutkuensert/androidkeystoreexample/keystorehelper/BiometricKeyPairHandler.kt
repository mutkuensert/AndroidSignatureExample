package com.mutkuensert.androidkeystoreexample.keystorehelper

import androidx.fragment.app.FragmentActivity
import java.security.KeyPair

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