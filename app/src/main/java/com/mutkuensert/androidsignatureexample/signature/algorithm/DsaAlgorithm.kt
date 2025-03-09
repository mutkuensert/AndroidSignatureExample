package com.mutkuensert.androidsignatureexample.signature.algorithm

interface DsaAlgorithm {
    val name: String
    val keystoreKey: String
    val digest: String
}