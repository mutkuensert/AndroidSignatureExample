package com.mutkuensert.androidkeystoreexample

data class MainUiModel(
    val alias: String,
    val data: String,
    val signature: String,
    val publicKey: String
) {
    companion object {
        fun initial(): MainUiModel {
            return MainUiModel(alias = "", data = "", signature = "", publicKey = "")
        }
    }
}