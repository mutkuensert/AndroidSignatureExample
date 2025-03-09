package com.mutkuensert.androidsignatureexample.signature.algorithm

import android.security.keystore.KeyProperties

/**
 * See [Signature Algorithms](https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms)
 *
 * See [Elliptic Curve Names](https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#parameterspec-names)
 *
 * See why [P-384](https://github.com/OWASP/owasp-mastg/blob/master/Document/0x04g-Testing-Cryptography.md#identifying-insecure-andor-deprecated-cryptographic-algorithms)
 */
object DsaAlgorithms {
    val SHA384_WITH_ECDSA = EcdsaAlgorithm(
        name = "SHA384withECDSA",
        Curve("secp384r1"),
        KeyProperties.DIGEST_SHA384
    )
    val SHA256_WITH_ECDSA = EcdsaAlgorithm(
        name = "SHA256withECDSA",
        Curve("secp256r1"),
        KeyProperties.DIGEST_SHA256
    )
}
