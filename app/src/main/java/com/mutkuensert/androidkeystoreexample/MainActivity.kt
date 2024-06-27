package com.mutkuensert.androidkeystoreexample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mutkuensert.androidkeystoreexample.ui.theme.AndroidKeyStoreExampleTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel: MainViewModel by viewModels()

        setContent {
            AndroidKeyStoreExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val uiModel by viewModel.uiModel.collectAsState()

                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiModel = uiModel,
                        onClickCreateKeyPair = { viewModel.createKeyPair(this) },
                        onClickDeleteEntry = viewModel::deleteEntry,
                        onClickSignData = { viewModel.signData(this) },
                        onDataValueChange = viewModel::changeDataValue,
                        onExternalPublicKeyChange = viewModel::changeExternalPublicKeyValue,
                        onSignatureToBeVerifiedChange = viewModel::changeSignatureToBeVerified,
                        onDataToBeVerifiedChange = viewModel::changeDataToBeVerified,
                        onVerify = viewModel::verify
                    )

                    LaunchedEffect(Unit) { viewModel.init() }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    modifier: Modifier = Modifier,
    uiModel: MainUiModel,
    onClickCreateKeyPair: () -> Unit,
    onClickDeleteEntry: () -> Unit,
    onClickSignData: () -> Unit,
    onDataValueChange: (value: String) -> Unit,
    onExternalPublicKeyChange: (value: String) -> Unit,
    onDataToBeVerifiedChange: (value: String) -> Unit,
    onSignatureToBeVerifiedChange: (value: String) -> Unit,
    onVerify: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(onClick = onClickCreateKeyPair) {
            Text(text = "Create key pair")
        }

        Button(onClick = onClickDeleteEntry) {
            Text(text = "Delete entry")
        }

        Button(onClick = onClickSignData) {
            Text(text = "Sign data")
        }

        Text(text = "Alias", fontWeight = FontWeight.Bold)

        Text(text = uiModel.alias)

        Text(text = "Public key", fontWeight = FontWeight.Bold)

        SelectionContainer { Text(text = uiModel.publicKey) }

        Text(text = "Data", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiModel.data,
            onValueChange = onDataValueChange,
            placeholder = {
                Text(text = "Data", fontWeight = FontWeight.ExtraLight)
            })

        Text(text = "Signature", fontWeight = FontWeight.Bold)

        SelectionContainer { Text(text = uiModel.signature) }

        HorizontalDivider()

        Button(onClick = onVerify) {
            Text(text = "Verify with public key")
        }

        Text(text = "Public key", fontWeight = FontWeight.Bold)

        OutlinedTextField(value = uiModel.externalPublicKey,
            onValueChange = onExternalPublicKeyChange,
            placeholder = {
                Text(text = "Public key", fontWeight = FontWeight.ExtraLight)
            })

        Text(text = "Data to be verified", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiModel.dataToBeVerified,
            onValueChange = onDataToBeVerifiedChange,
            placeholder = {
                Text(text = "Data", fontWeight = FontWeight.ExtraLight)
            })

        Text(text = "Signature to be verified", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiModel.signatureToBeVerified,
            onValueChange = onSignatureToBeVerifiedChange,
            placeholder = {
                Text(text = "Signature", fontWeight = FontWeight.ExtraLight)
            })

        Text(text = "Is verified", fontWeight = FontWeight.Bold)

        Text(text = uiModel.isVerified)
    }
}