package com.mutkuensert.androidkeystoreexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutkuensert.androidkeystoreexample.ui.theme.AndroidKeyStoreExampleTheme

class MainActivity : ComponentActivity() {
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
                        onClickCreateKeyPair = viewModel::createKeyPair,
                        onClickDeleteEntry = {},
                        onClickSignData = {},
                        onDataValueChange = {},
                        )
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

) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = onClickCreateKeyPair) {
                Text(text = "Create key pair")
            }

            Button(onClick = onClickDeleteEntry) {
                Text(text = "Delete entry")
            }

            Button(onClick = onClickSignData) {
                Text(text = "Sign data")
            }
        }

        Text(text = "Alias")
        Text(text = uiModel.signature, fontWeight = FontWeight.Bold)

        Text(text = "Public key")
        Text(text = uiModel.signature, fontWeight = FontWeight.Bold)

        Text(text = "Data")
        OutlinedTextField(value = uiModel.data, onValueChange = onDataValueChange)

        Text(text = "Signature")
        Text(text = uiModel.signature, fontWeight = FontWeight.Bold)
    }
}