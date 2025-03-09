package com.mutkuensert.androidsignatureexample.signature

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mutkuensert.androidsignatureexample.signature.algorithm.DsaAlgorithm
import com.mutkuensert.androidsignatureexample.signature.algorithm.DsaAlgorithms
import com.mutkuensert.androidsignatureexample.signature.algorithm.EcdsaAlgorithm
import timber.log.Timber
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * [KeyPairManager] provides methods for managing key pairs and signing processes using Android [KeyStore].
 *
 * @param alias The alias of [KeyStore.PrivateKeyEntry] in [KeyStore].
 * @param restrictToBiometricAuth Restricts [KeyStore.PrivateKeyEntry] access to strong biometric authentication.
 * @param dsaAlgorithm The algorithm to be used for signing data. Default is [DsaAlgorithms.SHA384_WITH_ECDSA].
 */
@OptIn(ExperimentalStdlibApi::class)
abstract class KeyPairManager(
    val alias: String,
    val restrictToBiometricAuth: Boolean = false,
    val dsaAlgorithm: DsaAlgorithm = DsaAlgorithms.SHA384_WITH_ECDSA
) {
    private val keyPairProvider = "AndroidKeyStore"

    /**
     * Generates and returns key pair if private key entry is inside secure hardware otherwise
     * removes the entry and returns null
     */
    fun generateHardwareBackedKeyPair(): KeyPair? {
        val keyPair = generateKeyPair() ?: return null

        return if (isInsideSecureHardware(keyPair) == true) {
            keyPair
        } else {
            deleteKeyStoreEntry()
            Timber.w("Entry has been deleted because it's not hardware backed.")
            null
        }
    }

    /**
     * Generates a key pair.
     * @return Null if any error is occurred, otherwise the key pair.
     */
    fun generateKeyPair(): KeyPair? {
        val keyPairGenerator = try {
            KeyPairGenerator.getInstance(
                dsaAlgorithm.keystoreKey,
                keyPairProvider
            )
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            return null
        }

        var keyPair = generateKeyPairInStrongBox(keyPairGenerator)

        if (keyPair == null) {
            keyPair = generateKeyPair(keyPairGenerator) ?: return null
        } else {
            Timber.i("Private key is generated using StrongBox.")
        }

        val publicKeyBase64: String =
            Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        Timber.i(
            "Public Key (Base64): $publicKeyBase64" +
                    "\nPublic Key (Hex): ${keyPair.public.encoded.toHexString()}"
        )

        return keyPair
    }

    private fun generateKeyPair(keyPairGenerator: KeyPairGenerator): KeyPair? {
        return try {
            keyPairGenerator.initialize(getKeyGenParameterSpec(isStrongBoxEnabled = false))
            keyPairGenerator.generateKeyPair()
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            null
        }
    }

    private fun generateKeyPairInStrongBox(keyPairGenerator: KeyPairGenerator): KeyPair? {
        return try {
            keyPairGenerator.initialize(getKeyGenParameterSpec(isStrongBoxEnabled = true))
            keyPairGenerator.generateKeyPair()
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            null
        }
    }

    private fun getKeyGenParameterSpec(isStrongBoxEnabled: Boolean): KeyGenParameterSpec {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )

        if (Build.VERSION.SDK_INT >= 28 && isStrongBoxEnabled) {
            spec.setIsStrongBoxBacked(true)
        }

        if (restrictToBiometricAuth) {
            spec.setBiometricAuthRequired()
        }

        if (dsaAlgorithm is EcdsaAlgorithm) {
            spec.setAlgorithmParameterSpec(ECGenParameterSpec(dsaAlgorithm.curve.name))
        }

        spec.setDigests(dsaAlgorithm.digest)
        return spec.build()
    }

    private fun KeyGenParameterSpec.Builder.setBiometricAuthRequired() {
        setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= 30) {
            setUserAuthenticationParameters(1, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            setUserAuthenticationValidityDurationSeconds(1)
        }
    }

    /**
     *  Checks if [alias] exists in [KeyStore].
     */
    fun exists(): Boolean {
        return try {
            getKeyStore().containsAlias(alias)
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            false
        }
    }

    /**
     * Deletes [alias] from [KeyStore].
     */
    fun deleteKeyStoreEntry(): Boolean {
        return try {
            val keyStore = getKeyStore()
            keyStore.deleteEntry(alias)
            !keyStore.containsAlias(alias)
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            false
        }
    }

    /**
     * Signs [data] using private key, returns null if any error occurs.
     * If biometric authentication is required, it must be performed before signing [data].
     */
    fun signData(data: String): SignedData? {
        val entry = getPrivateKeyEntry() ?: return null

        val signatureBytes: ByteArray = try {
            Signature.getInstance(dsaAlgorithm.name).run {
                initSign(entry.privateKey)
                update(data.encodeToByteArray())
                sign()
            }
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            return null
        }

        val signature: String = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        Timber.i(
            "Signature (Base64): $signature" +
                    "\nSignature (Hex): ${signatureBytes.toHexString()}"
        )

        return SignedData(signatureBytes, signature)
    }

    private fun getPrivateKeyEntry(): KeyStore.PrivateKeyEntry? {
        val keyStore = getKeyStore()

        val entry = try {
            keyStore.getEntry(alias, null)
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            null
        }

        if (entry !is KeyStore.PrivateKeyEntry) {
            Timber.w("Entry {$entry} is not an instance of a PrivateKeyEntry")
            return null
        }

        return entry
    }

    private fun getKeyStore(): KeyStore {
        return KeyStore.getInstance(keyPairProvider).apply {
            load(null)
        }
    }

    private fun isInsideSecureHardware(keyPair: KeyPair): Boolean? {
        val factory = KeyFactory.getInstance(dsaAlgorithm.keystoreKey, keyPairProvider)
        val keyInfo: KeyInfo
        var isHardwareBacked: Boolean? = null

        try {
            keyInfo = factory.getKeySpec(keyPair.private, KeyInfo::class.java)
            isHardwareBacked = keyInfo.isHardwareBacked()
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
        }

        return isHardwareBacked
    }

    private fun KeyInfo.isHardwareBacked(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
                    || securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
        } else {
            @Suppress("DEPRECATION")
            isInsideSecureHardware
        }
    }

    /**
     * Verifies [signature] using base64 encoded [publicKey] and [data].
     */
    fun verifyData(publicKey: String, data: String, signature: String): Boolean {
        val pubKey: PublicKey = generatePublicKey(publicKey) ?: return false
        val valid: Boolean = verifyData(pubKey, data, signature)
        return valid
    }

    /**
     * Verifies [signature] using [publicKey] and [data].
     */
    private fun verifyData(publicKey: PublicKey, data: String, signature: String): Boolean {
        val valid: Boolean = Signature.getInstance(dsaAlgorithm.name).run {
            initVerify(publicKey)
            update(data.toByteArray())
            verify(Base64.decode(signature, Base64.DEFAULT))
        }
        Timber.i("Signature $signature is valid: $valid")

        return valid
    }

    /**
     * Generates [PublicKey] using Base64 encoded [publicKey].
     */
    private fun generatePublicKey(publicKey: String): PublicKey? {
        val publicKeyBytes = try {
            Base64.decode(publicKey, Base64.NO_WRAP)
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            return null
        }

        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance(dsaAlgorithm.keystoreKey)

        return try {
            keyFactory.generatePublic(keySpec)
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            null
        }
    }
}
