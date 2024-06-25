package com.mutkuensert.androidkeystoreexample

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.mutkuensert.androidkeystoreexample.keystorehelper.BiometricKeyPairHandler
import com.mutkuensert.androidkeystoreexample.keystorehelper.KeyStoreHelper
import com.mutkuensert.androidkeystoreexample.keystorehelper.SignedData
import com.mutkuensert.androidkeystoreexample.keystorehelper.encodeBase64
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

    fun createKeyPair() {
        val keyPair = biometricKeyPairHandler.generateHardwareBackedKeyPair()

        if (keyPair != null) {
            preferences.edit {
                putString(KeyPublicKey, KeyStoreHelper.getPublicKeyBase64Encoded(keyPair))
            }
        }

        if (keyPair != null) {
            _uiModel.update {
                it.copy(
                    alias = alias,
                    publicKey = KeyStoreHelper.getPublicKeyBase64Encoded(keyPair)
                )
            }
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
                        it.copy(signature = signedData.value.encodeBase64())
                    }
                }
            })

    }

    fun changeDataValue(data: String) {
        _uiModel.update {
            it.copy(data = data)
        }
    }
}