package com.mutkuensert.androidsignatureexample.signaturehelper.signature.algorithm

/**
 * See [Signature Algorithms](https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms)
 *
 * See [Elliptic Curve Names](https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#parameterspec-names)
 *
 * See why [P-384](https://github.com/OWASP/owasp-mastg/blob/master/Document/0x04g-Testing-Cryptography.md#identifying-insecure-andor-deprecated-cryptographic-algorithms)
 */
object DsaAlgorithms {
    val SHA384_WITH_ECDSA = EcdsaAlgorithm("SHA384withECDSA", Curve("secp384r1"))
}
