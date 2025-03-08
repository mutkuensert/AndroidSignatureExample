package com.mutkuensert.androidsignatureexample.signaturehelper.signature

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mutkuensert.androidsignatureexample.signaturehelper.signature.algorithm.DsaAlgorithm
import com.mutkuensert.androidsignatureexample.signaturehelper.signature.algorithm.DsaAlgorithms
import com.mutkuensert.androidsignatureexample.signaturehelper.signature.algorithm.EcdsaAlgorithm
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
 * [SignatureHelper] provides utility methods to generate and manage key pairs in the Android KeyStore.
 * This class supports generating hardware-backed key pairs, signing data, verifying signatures, and managing KeyStore entries.
 *
 * @param alias The alias of the key entry in the KeyStore.
 * @param requireBiometricAuth Indicates if strong biometric authentication is required for accessing the key.
 * @param dsaAlgorithm The algorithm to be used for signing data. Default is [DsaAlgorithms.SHA384_WITH_ECDSA].
 * @param keyPairProvider The provider for the KeyStore. Default is "AndroidKeyStore".
 */
@OptIn(ExperimentalStdlibApi::class)
class SignatureHelper(
    val alias: String,
    val requireBiometricAuth: Boolean = false,
    val dsaAlgorithm: DsaAlgorithm = DsaAlgorithms.SHA384_WITH_ECDSA,
    val keyPairProvider: String = "AndroidKeyStore",
) {

    /**
     * Generates and returns key pair and if the pair is inside secure hardware or returns null and
     * removes the entry if the key pair isn't hardware backed or any error is occurred.
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
        val kpg: KeyPairGenerator = try {
            KeyPairGenerator.getInstance(
                dsaAlgorithm.keystoreKey,
                keyPairProvider
            )
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            return null
        }

        var keyPair = try {
            kpg.initialize(getKeyGenParameterSpec(isStrongBoxEnabled = true))
            kpg.generateKeyPair()
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            null
        }

        if (keyPair == null) {
            keyPair = try {
                kpg.initialize(getKeyGenParameterSpec(isStrongBoxEnabled = false))
                kpg.generateKeyPair()
            } catch (exception: Exception) {
                Timber.e(exception.stackTraceToString())
                return null
            }
        }

        val publicKeyBase64: String =
            Base64.encodeToString(keyPair!!.public.encoded, Base64.NO_WRAP)
        Timber.i(
            "Public Key (Base64): $publicKeyBase64" +
                    "\nPublic Key (Hex): ${keyPair.public.encoded.toHexString()}"
        )

        return keyPair
    }

    private fun getKeyGenParameterSpec(isStrongBoxEnabled: Boolean): KeyGenParameterSpec {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )

        if (Build.VERSION.SDK_INT >= 28 && isStrongBoxEnabled) {
            spec.setIsStrongBoxBacked(true)
        }

        if (requireBiometricAuth) {
            spec.setBiometricAuthRequired()
        }

        if (dsaAlgorithm is EcdsaAlgorithm) {
            spec.setAlgorithmParameterSpec(ECGenParameterSpec(dsaAlgorithm.curve.name))
        }

        spec.setDigests(KeyProperties.DIGEST_SHA384)
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
     *  Checks if a key entry with the specified alias exists in the KeyStore.
     */
    fun exists(): Boolean? {
        return try {
            getKeyStore().containsAlias(alias)
        } catch (exception: Exception) {
            Timber.e(exception.stackTraceToString())
            false
        }
    }

    /**
     * Deletes the key entry with the specified alias from the KeyStore.
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
     * Signs the given data using the private key associated with the specified alias, returns null
     * if an error occurs.
     * If biometric authentication is required, it must be performed before signing the data.
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
        val ks = getKeyStore()

        val entry = try {
            ks.getEntry(alias, null)
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
     * Encodes the public key of the given key pair to a Base64 string.
     */
    fun getPublicKeyBase64Encoded(keyPair: KeyPair): String {
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    /**
     * Verifies the given signature using the provided public key and data.
     */
    fun verifyData(publicKey: String, data: String, signature: String): Boolean {
        val pubKey: PublicKey = getPublicKeyFromString(publicKey) ?: return false
        val valid: Boolean = verifyData(pubKey, data, signature)
        return valid
    }

    /**
     * Verifies the given signature using the provided public key and data.
     */
    fun verifyData(publicKey: PublicKey, data: String, signature: String): Boolean {
        val valid: Boolean = Signature.getInstance(dsaAlgorithm.name).run {
            initVerify(publicKey)
            update(data.toByteArray())
            verify(Base64.decode(signature, Base64.DEFAULT))
        }
        Timber.i("Signature $signature is valid: $valid")

        return valid
    }

    /**
     * Converts a Base64 encoded public key string to a PublicKey object.
     */
    fun getPublicKeyFromString(publicKey: String): PublicKey? {
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
