# Android Signature Example

https://github.com/mutkuensert/AndroidKeyStoreExample/assets/97624869/551f58b1-a1da-4960-a878-28fc408d3c37

## Overview

AndroidSignatureExample is a demonstration project that shows how to implement secure digital signatures in Android applications using the Android KeyStore system. This project provides a complete implementation of generating cryptographic key pairs, signing data, and verifying signatures with support for hardware-backed keys and biometric authentication.

## Features

- **Hardware-backed Key Pair Generation:** Create cryptographically secure key pairs stored in Android's hardware security module
- **Biometric Authentication:** Restrict access to private keys with strong biometric authentication
- **ECDSA Digital Signatures:** Implement industry-standard signature algorithms (SHA384/SHA256 with ECDSA)
- **Signature Verification:** Verify signatures using stored or externally provided public keys
- **StrongBox Support:** Optional secure hardware integration where available
- **Emulator Compatibility:** Graceful handling of hardware security features in emulator environments

## Technical Details

This project demonstrates several best practices for implementing cryptographic operations in Android:

- **Android KeyStore:** Uses the system-provided KeyStore for secure key storage
- **Hardware Security:** Generates keys in hardware security modules where available
- **ECDSA with P-384/P-256:** Implements elliptic curves
- **Biometric Prompt API:** Integrates with the latest biometric authentication APIs
- **Jetpack Compose UI:** Modern UI implementation with full state management

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 35 or higher (minimum SDK 24)
- Device with biometric hardware or emulator

### Setup

1. Clone the repository
2. Open the project in Android Studio
3. Choose either the 'emulator' or 'production' build variant
4. Build and run the application

## Usage Example

The application provides a simple UI to demonstrate key concepts:

1. **Create Key Pair:** Generates a cryptographic key pair (public/private) secured in hardware and protected with biometric authentication
2. **Sign Data:** Enter data and create a digital signature using the private key
3. **Verify Signature:** Verify that a signature is valid for given data using a provided public key

Example code for key generation:

```kotlin
val keyPairManager = BiometricAuthRestrictedKeyPairManager("myKeyAlias")
val keyPair = keyPairManager.generateHardwareBackedKeyPair(activity)
val publicKey = keyPair.public.base64Encoded
```

Example code for signing data:

```kotlin
keyPairManager.authenticateAndSignData(
    "Data to be signed",
    activity
) { signedData ->
    // Use the signature in signedData.signature
}
```

## Project Structure

- `signature/`: Core cryptographic operations and key management
    - `KeyPairManager.kt`: Base class for managing cryptographic key pairs
    - `biometric/`: Biometric authentication integration
    - `algorithm/`: Signature algorithm implementations
- `ui/`: Jetpack Compose UI components

## Kdoc

Kdoc is available at [https://mutkuensert.github.io/AndroidSignatureExample/](https://mutkuensert.github.io/AndroidSignatureExample/)

The documentation includes API reference.

## Security Considerations

- This application follows OWASP recommendations for cryptographic implementations
- Uses P-384 curve by default for ECDSA operations (as recommended by security best practices)
- Properly handles key storage in hardware security modules
- Implements biometric authentication with strong security requirements
