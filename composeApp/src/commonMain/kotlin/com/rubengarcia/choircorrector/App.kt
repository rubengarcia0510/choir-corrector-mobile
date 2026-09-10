package com.rubengarcia.choircorrector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class Screen {
    HOME,
    NEW_ANALYSIS
}

@Composable
fun App(
    audioFilePicker: AudioFilePicker
) {
    var screen by remember { mutableStateOf(Screen.HOME) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onNewAnalysis = {
                        screen = Screen.NEW_ANALYSIS
                    }
                )

                Screen.NEW_ANALYSIS -> NewAnalysisScreen(
                    audioFilePicker = audioFilePicker,
                    onBack = {
                        screen = Screen.HOME
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onNewAnalysis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choir Corrector",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Análisis de afinación y tempo para coros",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = onNewAnalysis
        ) {
            Text("Nuevo análisis")
        }
    }
}

@Composable
private fun NewAnalysisScreen(
    audioFilePicker: AudioFilePicker,
    onBack: () -> Unit
) {
    var referenceUri by remember { mutableStateOf<String?>(null) }
    var rehearsalUri by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nuevo análisis",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Seleccioná los dos audios.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = {
                audioFilePicker.pickAudio { uri ->
                    referenceUri = uri
                }
            }
        ) {
            Text("Seleccionar referencia")
        }

        referenceUri?.let {
            Text(
                text = "Referencia seleccionada",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                audioFilePicker.pickAudio { uri ->
                    rehearsalUri = uri
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Seleccionar ensayo")
        }

        rehearsalUri?.let {
            Text(
                text = "Ensayo seleccionado",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Volver")
        }
    }
}
