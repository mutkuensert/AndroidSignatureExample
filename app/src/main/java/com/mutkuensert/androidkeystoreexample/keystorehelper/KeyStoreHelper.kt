package com.mutkuensert.androidkeystoreexample.keystorehelper

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
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
private const val KeyAlgorithm = KeyProperties.KEY_ALGORITHM_EC
private const val KeyPairProvider = "AndroidKeyStore"
private const val SignatureAlgorithm = "SHA256withECDSA"

/**
 * @param alias Keystore alias of entry.
 * @param requireBiometricAuth True if biometric auth is wanted for the key.
 */
class KeyStoreHelper(
    val alias: String,
    val requireBiometricAuth: Boolean = false
) {
    /**
     * Generates hardware backed key pair and writes private key into AndroidKeyStore.
     * @return Null if the private key isn't hardware backed or any error is occurred.
     */
    fun generateHardwareBackedKeyPair(): KeyPair? {
        val keyPair = generateKeyPair() ?: return null

        return if (isInsideSecureHardware(keyPair) == true) {
            Log.i(
                Tag,
                "::${::generateHardwareBackedKeyPair.name}: Public Key: ${keyPair.public.encoded.encodeBase64()}"
            )
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
     * Generates key pair and writes private key into AndroidKeyStore.
     * @return Null if any error is occurred.
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
                KeyAlgorithm,
                KeyPairProvider
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

    fun exists(): Boolean? {
        return try {
            getKeyStore().containsAlias(alias)
        } catch (exception: KeyStoreException) {
            Log.e(Tag, "::${::exists.name}: " + exception.stackTraceToString())
            false
        }
    }

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
     * If [requireBiometricAuth] is true authenticate user via biometric authentication
     * before signing data.
     */
    fun signData(data: String): SignedData? {
        val entry = getPrivateKeyEntry() ?: return null

        val signature: ByteArray = try {
            Signature.getInstance(SignatureAlgorithm).run {
                initSign(entry.privateKey)
                update(data.encodeToByteArray())
                sign()
            }
        } catch (exception: Exception) {
            Log.e(Tag, exception.stackTraceToString())
            return null
        }

        Log.i(Tag, "::${::signData.name}: Signature: ${signature.encodeBase64()}")

        return SignedData(signature)
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
        return KeyStore.getInstance(KeyPairProvider).apply {
            load(null)
        }
    }

    private fun isInsideSecureHardware(keyPair: KeyPair): Boolean? {
        val factory = KeyFactory.getInstance(KeyAlgorithm, KeyPairProvider)
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

    companion object {
        fun getPublicKeyBase64Encoded(keyPair: KeyPair): String {
            return keyPair.public.encoded.encodeBase64()
        }

        fun verifyData(publicKey: String, data: String, signature: String): Boolean {
            val pubKey: PublicKey = getPublicKeyFromString(publicKey) ?: return false

            val valid: Boolean = Signature.getInstance(SignatureAlgorithm).run {
                initVerify(pubKey)
                update(data.encodeToByteArray())
                verify(signature.decodeBase64())
            }

            return valid
        }

        fun verifyData(publicKey: PublicKey, data: String, signedData: SignedData): Boolean {
            val valid: Boolean = Signature.getInstance(SignatureAlgorithm).run {
                initVerify(publicKey)
                update(data.encodeToByteArray())
                verify(signedData.value)
            }

            return valid
        }

        fun getPublicKeyFromString(value: String): PublicKey? {
            val publicBytes = try {
                value.decodeBase64()
            } catch (exception: IllegalArgumentException) {
                Log.e(Tag, exception.stackTraceToString())
                return null
            }

            val keySpec = X509EncodedKeySpec(publicBytes)
            val keyFactory = KeyFactory.getInstance(KeyAlgorithm)
            return keyFactory.generatePublic(keySpec)
        }
    }
}
