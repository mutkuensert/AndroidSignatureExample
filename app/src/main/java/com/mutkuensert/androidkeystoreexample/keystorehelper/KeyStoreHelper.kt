package com.mutkuensert.androidkeystoreexample.keystorehelper

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
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

private const val Tag = "KeyStoreHelper"

/**
 * @param alias Keystore alias of entry.
 * @param requireBiometricAuth True if biometric auth is wanted for the key.
 */
class KeyStoreHelper(
    val alias: String,
    val requireBiometricAuth: Boolean = false
) {
    private val keyAlgorithm = KeyProperties.KEY_ALGORITHM_EC
    private val keyPairProvider = "AndroidKeyStore"
    private val signatureAlgorithm = "SHA256withECDSA"

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
        val kpg: KeyPairGenerator = try {
            KeyPairGenerator.getInstance(
                keyAlgorithm,
                keyPairProvider
            )
        } catch (e: NoSuchAlgorithmException) {
            Log.e(Tag, e.stackTraceToString())
            return null
        } catch (e: NullPointerException) {
            Log.e(Tag, e.stackTraceToString())
            return null
        }

        val parameterSpec: KeyGenParameterSpec = getKeyGenParameterSpec()

        val keyPair = try {
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        } catch (e: InvalidAlgorithmParameterException) {
            Log.e(Tag, e.stackTraceToString())
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


    private fun KeyGenParameterSpec.Builder.setBiometricAuthRequired() {
        setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= 30) {
            setUserAuthenticationParameters(1, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            setUserAuthenticationValidityDurationSeconds(1)
        }
    }

    fun deleteKeyStoreEntry(): Boolean {
        return try {
            val keyStore = getKeyStore()
            keyStore.deleteEntry(alias)
            !keyStore.containsAlias(alias)
        } catch (e: KeyStoreException) {
            Log.e(
                Tag,
                "::${::deleteKeyStoreEntry.name}: KeyStore entry with alias: $alias couldn't be deleted."
                        + "\n" + e.stackTraceToString()
            )
            false
        }
    }

    companion object {
        fun getPublicKeyBase64Encoded(keyPair: KeyPair): String {
            return keyPair.public.encoded.encodeBase64()
        }
    }

    /**
     * If [requireBiometricAuth] is true authenticate user via biometric authentication
     * before signing data.
     */
    fun signData(data: String): SignedData? {
        val entry = getPrivateKeyEntry() ?: return null

        val signature: ByteArray = try {
            Signature.getInstance(signatureAlgorithm).run {
                initSign(entry.privateKey)
                update(data.decodeBase64())
                sign()
            }
        } catch (e: SignatureException) {
            Log.e(Tag, e.stackTraceToString())
            return null
        }

        Log.i(Tag, "::${::signData.name}: Signature: ${signature.encodeBase64()}")

        return SignedData(signature)
    }

    private fun getPrivateKeyEntry(): KeyStore.PrivateKeyEntry? {
        val ks = getKeyStore()

        val entry = try {
            ks.getEntry(alias, null)
        } catch (e: Exception) {
            Log.e(Tag, e.stackTraceToString())
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

    fun verifyData(publicKey: PublicKey, data: String, signedData: SignedData): Boolean {
        val valid: Boolean = Signature.getInstance(signatureAlgorithm).run {
            initVerify(publicKey)
            update(data.decodeBase64())
            verify(signedData.value)
        }

        return valid
    }

    fun getPublicKeyFromString(value: String): PublicKey {
        val publicBytes = value.decodeBase64()
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance(keyAlgorithm)
        return keyFactory.generatePublic(keySpec)
    }

    private fun isInsideSecureHardware(keyPair: KeyPair): Boolean? {
        val factory = KeyFactory.getInstance(keyAlgorithm, keyPairProvider)
        val keyInfo: KeyInfo
        var isHardwareBacked: Boolean? = null

        try {
            keyInfo = factory.getKeySpec(keyPair.private, KeyInfo::class.java)
            isHardwareBacked = keyInfo.isHardwareBacked()
        } catch (e: InvalidKeySpecException) {
            Log.e(Tag, e.stackTraceToString())
        }

        return isHardwareBacked
    }

    private fun KeyInfo.isHardwareBacked(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
                    || securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
        } else {
            isInsideSecureHardware
        }
    }
}
