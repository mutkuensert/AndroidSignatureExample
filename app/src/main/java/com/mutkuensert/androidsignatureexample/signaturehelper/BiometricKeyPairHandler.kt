package com.mutkuensert.androidsignatureexample.signaturehelper

import androidx.fragment.app.FragmentActivity
import java.security.KeyPair

/**
 * BiometricKeyPairHandler manages the creation, deletion, and use of hardware-backed key pairs
 * with biometric authentication. This class utilizes [SignatureHelper] to interact with the Android KeyStore.
 *
 * @param alias The alias of the key entry in the KeyStore.
 */
class BiometricKeyPairHandler(alias: String) {
    private val signatureHelper = SignatureHelper(
        alias = alias,
        requireBiometricAuth = true
    )

    /**
     * If strong biometric is available then returns
     * [SignatureHelper.generateHardwareBackedKeyPair] otherwise returns null.
     */
    fun generateHardwareBackedKeyPair(activity: FragmentActivity): KeyPair? {
        if (!BiometricAuthHelper.isStrongBiometricAuthAvailable(activity)) {
            return null
        }

        return signatureHelper.generateHardwareBackedKeyPair()
    }

    fun deleteKeyPair(): Boolean {
        return signatureHelper.deleteKeyStoreEntry()
    }

    fun getPublicKeyBase64Encoded(keyPair: KeyPair): String {
        return signatureHelper.getPublicKeyBase64Encoded(keyPair)
    }

    fun verifyData(publicKey: String, data: String, signature: String): Boolean {
        return signatureHelper.verifyData(publicKey, data, signature)
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
            onAuthenticationSucceeded(signatureHelper.signData(data))
        })
    }

    fun exists(): Boolean? {
        return signatureHelper.exists()
    }
}