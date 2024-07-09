package com.mutkuensert.androidsignatureexample.signaturehelper

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val Tag = "BiometricAuthHelper"

object BiometricAuthHelper {

    fun isStrongBiometricAuthAvailable(activity: Activity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(Tag, "App can authenticate using biometrics.")
                true
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e(Tag, "No biometric features available on this device.")
                false
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e(Tag, "Biometric features are currently unavailable.")
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e(Tag, "Biometric features are not enrolled.")
                false
            }

            else -> false
        }
    }

    fun authenticate(
        fragmentActivity: FragmentActivity,
        onAuthenticationError: (
            errorCode: Int,
            errString: CharSequence
        ) -> Unit = { _, _ -> },
        onAuthenticationSucceeded: (BiometricPrompt.AuthenticationResult) -> Unit = {},
        onAuthenticationFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(fragmentActivity.applicationContext)
        val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        fragmentActivity.applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT
                    )
                        .show()
                    onAuthenticationError.invoke(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onAuthenticationSucceeded.invoke(result)
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        fragmentActivity.applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        fragmentActivity.applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    onAuthenticationFailed.invoke()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
