package com.mutkuensert.androidkeystoreexample

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.mutkuensert.androidkeystoreexample.keystorehelper.BiometricAuthHelper
import com.mutkuensert.androidkeystoreexample.keystorehelper.KeyStoreHelper
import com.mutkuensert.androidkeystoreexample.keystorehelper.SignedData
import com.mutkuensert.androidkeystoreexample.keystorehelper.encodeBase64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _uiModel = MutableStateFlow(MainUiModel.initial())
    val uiModel = _uiModel.asStateFlow()

    private val alias = "alias"
    private val keyStoreHelper = KeyStoreHelper(alias = alias, requireBiometricAuth = true)

    fun init() {
        _uiModel.update {
            it.copy(alias = alias)
        }
    }

    fun createKeyPair() {
        val keyPair = keyStoreHelper.generateHardwareBackedKeyPair()
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
                it.copy(alias = "", publicKey = "")
            } else {
                it
            }
        }
    }

    fun signData(activity: FragmentActivity) {
        var signedData: SignedData?
        BiometricAuthHelper.authenticate(activity, onAuthenticationSucceeded = {
            signedData = keyStoreHelper.signData(uiModel.value.data)

            if (signedData == null) return@authenticate

            val signature: String = signedData!!.value.encodeBase64()
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