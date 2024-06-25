package com.mutkuensert.androidkeystoreexample

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.mutkuensert.androidkeystoreexample.keystorehelper.BiometricAuthHelper
import com.mutkuensert.androidkeystoreexample.keystorehelper.KeyStoreHelper
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
    private val keyStoreHelper = KeyStoreHelper(alias = alias, requireBiometricAuth = true)

    private val preferences =
        application.applicationContext.getSharedPreferences(PrefsKeyPair, Context.MODE_PRIVATE)

    fun init() {
        _uiModel.update {
            it.copy(alias = alias, publicKey = preferences.getString(KeyPublicKey, "")!!)
        }
    }

    fun createKeyPair() {
        val keyPair = keyStoreHelper.generateHardwareBackedKeyPair()

        if (keyPair != null) {
            preferences.edit {
                putString(KeyPublicKey, keyStoreHelper.getPublicKeyBase64Encoded(keyPair))
            }
        }

        _uiModel.update {
            if (keyPair != null) {
                it.copy(
                    alias = alias,
                    publicKey = keyStoreHelper.getPublicKeyBase64Encoded(keyPair) ?: ""
                )
            } else {
                it
            }
        }
    }

    fun deleteEntry() {
        val isDeleted = keyStoreHelper.deleteKeyStoreEntry()
        _uiModel.update {
            if (isDeleted) {
                preferences.edit { remove(KeyPublicKey) }

                it.copy(alias = "", publicKey = "")
            } else {
                it
            }
        }
    }

    fun signData(activity: FragmentActivity) {
        BiometricAuthHelper.authenticate(activity, onAuthenticationSucceeded = {
            val signedData = keyStoreHelper.signData(uiModel.value.data) ?: return@authenticate
            val signature: String = signedData.value.encodeBase64()

            _uiModel.update {
                it.copy(signature = signature)
            }
        })
    }

    fun changeDataValue(data: String) {
        _uiModel.update {
            it.copy(data = data)
        }
    }
}