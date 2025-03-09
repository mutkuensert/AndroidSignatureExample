package com.mutkuensert.androidsignatureexample.signature

import android.util.Base64
import java.security.PublicKey

val PublicKey.base64Encoded: String
    get() {
        return Base64.encodeToString(encoded, Base64.NO_WRAP)
    }