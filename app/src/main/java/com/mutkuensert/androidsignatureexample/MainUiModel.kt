package com.mutkuensert.androidsignatureexample

data class MainUiModel(
    val alias: String,
    val data: String,
    val signature: String,
    val publicKey: String,
    val externalPublicKey: String,
    val dataToBeVerified: String,
    val signatureToBeVerified: String,
    val isVerified: String,
) {
    companion object {
        fun initial(): MainUiModel {
            return MainUiModel(
                alias = "",
                data = "",
                signature = "",
                publicKey = "",
                externalPublicKey = "",
                dataToBeVerified = "",
                signatureToBeVerified = "",
                isVerified = ""
            )
        }
    }
}