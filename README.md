

https://github.com/mutkuensert/AndroidKeyStoreExample/assets/97624869/551f58b1-a1da-4960-a878-28fc408d3c37

```kotlin
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
        // Method implementation
    }

    /**
     * Generates a key pair.
     * @return Null if any error is occurred, otherwise the key pair.
     */
    fun generateKeyPair(): KeyPair? {
        // Method implementation
    }

    /**
     *  Checks if a key entry with the specified alias exists in the KeyStore.
     */
    fun exists(): Boolean? {
        // Method implementation
    }

    /**
     * Deletes the key entry with the specified alias from the KeyStore.
     */
    fun deleteKeyStoreEntry(): Boolean {
        // Method implementation
    }

    /**
     * Signs the given data using the private key associated with the specified alias, returns null
     * if an error occurs.
     * If biometric authentication is required, it must be performed before signing the data.
     */
    fun signData(data: String): SignedData? {
        // Method implementation
    }

    /**
     * Encodes the public key of the given key pair to a Base64 string.
     */
    fun getPublicKeyBase64Encoded(keyPair: KeyPair): String {
        // Method implementation
    }

    /**
     * Verifies the given signature using the provided public key and data.
     */
    fun verifyData(publicKey: String, data: String, signature: String): Boolean {
        // Method implementation
    }

    /**
     * Verifies the given signature using the provided public key and data.
     */
    fun verifyData(publicKey: PublicKey, data: String, signature: String): Boolean {
        // Method implementation
    }

    /**
     * Converts a Base64 encoded public key string to a PublicKey object.
     */
    fun getPublicKeyFromString(publicKey: String): PublicKey? {
        // Method implementation
    }

    // Private methods and other internal logic
}

```
