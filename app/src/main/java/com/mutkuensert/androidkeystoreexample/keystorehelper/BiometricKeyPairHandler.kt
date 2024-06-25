package com.mutkuensert.androidkeystoreexample.keystorehelper

import androidx.fragment.app.FragmentActivity
import java.security.KeyPair

class BiometricKeyPairHandler(alias: String) {
    private val keyStoreHelper = KeyStoreHelper(
        alias = alias,
        requireBiometricAuth = true
    )

    fun generateHardwareBackedKeyPair(): KeyPair? {
        return keyStoreHelper.generateHardwareBackedKeyPair()
    }

    fun deleteKeyPair(): Boolean {
        return keyStoreHelper.deleteKeyStoreEntry()
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
}