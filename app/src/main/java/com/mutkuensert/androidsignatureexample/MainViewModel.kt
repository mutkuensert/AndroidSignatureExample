package com.mutkuensert.androidsignatureexample

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.mutkuensert.androidsignatureexample.signature.SignedData
import com.mutkuensert.androidsignatureexample.signature.base64Encoded
import com.mutkuensert.androidsignatureexample.signature.biometric.BiometricAuthRestrictedKeyPairManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val KeyPairPreferencesName = "keyPairPreferences"
private const val PublicKeyPrefsKey = "publicKeyPrefsKey"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiModel = MutableStateFlow(MainUiModel.initial())
    val uiModel = _uiModel.asStateFlow()

    private val alias = "alias"
    private val keyPairManager = BiometricAuthRestrictedKeyPairManager(alias)

    private val preferences =
        application.applicationContext.getSharedPreferences(
            KeyPairPreferencesName,
            Context.MODE_PRIVATE
        )

    fun init() {
        _uiModel.update {
            it.copy(
                alias = alias,
                originalPublicKey = preferences.getString(PublicKeyPrefsKey, "")!!
            )
        }
    }

    fun createKeyPair(activity: FragmentActivity) {
        val keyPair = keyPairManager.generateHardwareBackedKeyPair(activity) ?: return
        val publicKey = keyPair.public.base64Encoded

        preferences.edit {
            putString(PublicKeyPrefsKey, publicKey)
        }

        _uiModel.update {
            it.copy(
                alias = alias,
                originalPublicKey = publicKey,
                externalPublicKey = publicKey
            )
        }
    }

    fun deleteEntry() {
        val isDeleted = keyPairManager.deleteKeyStoreEntry()

        if (isDeleted) {
            _uiModel.update {
                preferences.edit { remove(PublicKeyPrefsKey) }
                MainUiModel.initial()
            }
        }
    }

    fun signData(activity: FragmentActivity) {
        keyPairManager.authenticateAndSignData(
            uiModel.value.data,
            activity,
            onAuthenticationSucceeded = { signedData: SignedData? ->
                if (signedData != null) {
                    _uiModel.update {
                        it.copy(
                            signature = signedData.signature,
                            signatureToBeVerified = signedData.signature
                        )
                    }
                }
            })

    }

    fun changeDataValue(data: String) {
        _uiModel.update {
            it.copy(data = data)
        }
    }

    fun changeExternalPublicKeyValue(value: String) {
        _uiModel.update {
            it.copy(externalPublicKey = value)
        }
    }

    fun changeDataToBeVerified(value: String) {
        _uiModel.update {
            it.copy(dataToBeVerified = value)
        }
    }

    fun changeSignatureToBeVerified(value: String) {
        _uiModel.update {
            it.copy(signatureToBeVerified = value)
        }
    }

    fun verify() {
        _uiModel.update {
            it.copy(
                isVerified = keyPairManager.verifyData(
                    it.externalPublicKey,
                    it.dataToBeVerified,
                    it.signatureToBeVerified
                ).toString()
            )
        }
    }
}