package com.mutkuensert.androidsignatureexample.signature.algorithm

import android.security.keystore.KeyProperties

class EcdsaAlgorithm(
    override val name: String,
    val curve: Curve,
    override val digest: String
) : DsaAlgorithm {
    override val keystoreKey = KeyProperties.KEY_ALGORITHM_EC
}

@JvmInline
value class Curve(val name: String)