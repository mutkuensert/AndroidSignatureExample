package com.mutkuensert.androidkeystoreexample.keystorehelper

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.InvalidAlgorithmParameterException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

private const val Tag = "KeyStoreHelper"

/**
 * KeyStoreHelper provides utility methods to generate and manage key pairs in the Android KeyStore.
 * This class supports generating hardware-backed key pairs, signing data, verifying signatures, and managing KeyStore entries.
 *
 * @param alias The alias of the key entry in the KeyStore.
 * @param requireBiometricAuth Indicates if biometric authentication is required for accessing the key.
 * @param keyAlgorithm The algorithm to be used for key generation. Default is EC (Elliptic Curve).
 * @param signatureAlgorithm The algorithm to be used for signing data. Default is "SHA256withECDSA".
 * @param keyPairProvider The provider for the KeyStore. Default is "AndroidKeyStore".
 */
class KeyStoreHelper(
    val alias: String,
    val requireBiometricAuth: Boolean = false,
    val keyAlgorithm: String = KeyProperties.KEY_ALGORITHM_EC,
    val signatureAlgorithm: String = "SHA256withECDSA",
    val keyPairProvider: String = "AndroidKeyStore",
) {
    /**
     * Generates and returns key pair and if the pair is inside secure hardware or returns null and
     * removes the entry if the key pair isn't hardware backed or any error is occurred.
     */
    fun generateHardwareBackedKeyPair(): KeyPair? {
        val keyPair = generateKeyPair() ?: return null
        val publicKey: String = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

        return if (isInsideSecureHardware(keyPair) == true) {
            Log.i(Tag, "::${::generateHardwareBackedKeyPair.name}: Public Key: $publicKey")
            keyPair
        } else {
            deleteKeyStoreEntry()
            Log.w(
                Tag,
                "::${::generateHardwareBackedKeyPair.name}: Entry has been deleted because it's not hardware backed."
            )
            null
        }
    }

    /**
     * Generates a key pair.
     * @return Null if any error is occurred, otherwise the key pair.
     */
    fun generateKeyPair(): KeyPair? {
        if (requireBiometricAuth && Build.VERSION.SDK_INT < 30) {
            Log.w(
                Tag, "::${::generateKeyPair.name}: In api levels lower than 30, key pairs " +
                        "with authentication timeout greater than 0 are set with both device credential and biometric authentication parameter " +
                        "which is not as safe as biometric authentication."
            )
            return null
        }

        val kpg: KeyPairGenerator = try {
            KeyPairGenerator.getInstance(
                keyAlgorithm,
                keyPairProvider
            )
        } catch (exception: NoSuchAlgorithmException) {
            Log.e(Tag, exception.stackTraceToString())
            return null
        } catch (exception: NullPointerException) {
            Log.e(Tag, exception.stackTraceToString())
            return null
        }

        val parameterSpec: KeyGenParameterSpec = getKeyGenParameterSpec()

        val keyPair = try {
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        } catch (exception: InvalidAlgorithmParameterException) {
            Log.e(Tag, exception.stackTraceToString())
            null
        }

        return keyPair
    }

    private fun getKeyGenParameterSpec(): KeyGenParameterSpec {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )

        /* StrongBoxUnavailableException must be handled.
        if (Build.VERSION.SDK_INT >= 28) {
            spec.setIsStrongBoxBacked(true)
        }*/

        if (requireBiometricAuth) {
            spec.setBiometricAuthRequired()
        }

        spec.setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
        return spec.build()
    }

    @SuppressLint("NewApi")
    private fun KeyGenParameterSpec.Builder.setBiometricAuthRequired() {
        setUserAuthenticationRequired(true)
        setUserAuthenticationParameters(1, KeyProperties.AUTH_BIOMETRIC_STRONG)
        //if (Build.VERSION.SDK_INT < 30) setUserAuthenticationValidityDurationSeconds(1)
    }

    /**
     *  Checks if a key entry with the specified alias exists in the KeyStore.
     */
    fun exists(): Boolean? {
        return try {
            getKeyStore().containsAlias(alias)
        } catch (exception: KeyStoreException) {
            Log.e(Tag, "::${::exists.name}: " + exception.stackTraceToString())
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
        } catch (exception: KeyStoreException) {
            Log.e(
                Tag,
                "::${::deleteKeyStoreEntry.name}: KeyStore entry with alias: $alias couldn't be deleted."
                        + "\n" + exception.stackTraceToString()
            )
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
            Signature.getInstance(signatureAlgorithm).run {
                initSign(entry.privateKey)
                update(data.encodeToByteArray())
                sign()
            }
        } catch (exception: Exception) {
            Log.e(Tag, exception.stackTraceToString())
            return null
        }

        val signature: String = Base64.encodeToString(signatureBytes, Base64.DEFAULT)
        Log.i(Tag, "::${::signData.name}: Signature: $signature")

        return SignedData(signatureBytes, signature)
    }

    private fun getPrivateKeyEntry(): KeyStore.PrivateKeyEntry? {
        val ks = getKeyStore()

        val entry = try {
            ks.getEntry(alias, null)
        } catch (exception: Exception) {
            Log.e(Tag, exception.stackTraceToString())
            null
        }

        if (entry !is KeyStore.PrivateKeyEntry) {
            Log.w(
                Tag,
                "::${::getPrivateKeyEntry.name} Entry: $entry is not an instance of a PrivateKeyEntry"
            )
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
        val factory = KeyFactory.getInstance(keyAlgorithm, keyPairProvider)
        val keyInfo: KeyInfo
        var isHardwareBacked: Boolean? = null

        try {
            keyInfo = factory.getKeySpec(keyPair.private, KeyInfo::class.java)
            isHardwareBacked = keyInfo.isHardwareBacked()
        } catch (exception: InvalidKeySpecException) {
            Log.e(Tag, exception.stackTraceToString())
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
        val valid: Boolean = Signature.getInstance(signatureAlgorithm).run {
            initVerify(publicKey)
            update(data.toByteArray())
            verify(Base64.decode(signature, Base64.DEFAULT))
        }
        Log.i(Tag, "Signature: $signature is valid: $valid")

        return valid
    }

    /**
     * Converts a Base64 encoded public key string to a PublicKey object.
     */
    fun getPublicKeyFromString(publicKey: String): PublicKey? {
        val publicKeyBytes = try {
            Base64.decode(publicKey, Base64.NO_WRAP)
        } catch (exception: IllegalArgumentException) {
            Log.e(Tag, exception.stackTraceToString())
            return null
        }

        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance(keyAlgorithm)

        return try {
            keyFactory.generatePublic(keySpec)
        } catch (exception: InvalidKeySpecException) {
            Log.e(Tag, exception.stackTraceToString())
            null
        }
    }
}
