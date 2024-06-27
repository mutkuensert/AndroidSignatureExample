package com.mutkuensert.androidkeystoreexample.keystorehelper

import android.util.Base64

internal fun String.decodeBase64(): ByteArray {
    return Base64.decode(this, Base64.NO_WRAP)
}

internal fun ByteArray.encodeBase64(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}