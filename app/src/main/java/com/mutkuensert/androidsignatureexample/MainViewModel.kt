package com.mutkuensert.androidsignatureexample

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.mutkuensert.androidsignatureexample.signaturehelper.BiometricKeyPairHandler
import com.mutkuensert.androidsignatureexample.signaturehelper.SignedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val PrefsKeyPair = "keyPairPreferences"
private const val KeyPublicKey = "publicKeyPrefsKey"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiModel = MutableStateFlow(MainUiModel.initial())
    val uiModel = _uiModel.asStateFlow()

    private val alias = "alias"
    private val biometricKeyPairHandler = BiometricKeyPairHandler(alias)

    private val preferences =
        application.applicationContext.getSharedPreferences(PrefsKeyPair, Context.MODE_PRIVATE)

    fun init() {
        _uiModel.update {
            it.copy(alias = alias, publicKey = preferences.getString(KeyPublicKey, "")!!)
        }
    }

    fun createKeyPair(activity: FragmentActivity) {
        val keyPair = biometricKeyPairHandler.generateHardwareBackedKeyPair(activity) ?: return
        val publicKey = biometricKeyPairHandler.getPublicKeyBase64Encoded(keyPair)

        preferences.edit {
            putString(KeyPublicKey, publicKey)
        }

        _uiModel.update {
            it.copy(
                alias = alias,
                publicKey = publicKey,
                externalPublicKey = publicKey
            )
        }
    }

    fun deleteEntry() {
        val isDeleted = biometricKeyPairHandler.deleteKeyPair()

        if (isDeleted) {
            _uiModel.update {
                preferences.edit { remove(KeyPublicKey) }
                MainUiModel.initial()
            }
        }
    }

    fun signData(activity: FragmentActivity) {
        biometricKeyPairHandler.authenticateAndSignData(
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
                isVerified = biometricKeyPairHandler.verifyData(
                    it.externalPublicKey,
                    it.dataToBeVerified,
                    it.signatureToBeVerified
                ).toString()
            )
        }
    }
}