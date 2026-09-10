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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rubengarcia.choircorrector.api.CorrectorCoroApiClient
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class Screen {
    HOME,
    NEW_ANALYSIS
}

@Composable
fun App(
    audioFilePicker: AudioFilePicker,
    audioFileReader: AudioFileReader,
    apiClient: CorrectorCoroApiClient
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
                    audioFileReader = audioFileReader,
                    apiClient = apiClient,
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
    audioFileReader: AudioFileReader,
    apiClient: CorrectorCoroApiClient,
    onBack: () -> Unit
) {
    var referenceUri by remember { mutableStateOf<String?>(null) }
    var rehearsalUri by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var jobId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            onClick = {
                val reference = referenceUri
                val rehearsal = rehearsalUri

                if (reference == null || rehearsal == null) {
                    uploadMessage = "Seleccioná los dos audios."
                    return@Button
                }

                scope.launch {
                    uploading = true
                    uploadMessage = null

                    try {
                        val referenceFile = audioFileReader.read(reference)
                        val rehearsalFile = audioFileReader.read(rehearsal)

                        apiClient.uploadReference(
                            coroId = "demo",
                            fileName = referenceFile.fileName,
                            audioBytes = referenceFile.bytes
                        )

                        jobId = apiClient.uploadRehearsal(
                            coroId = "demo",
                            fileName = rehearsalFile.fileName,
                            audioBytes = rehearsalFile.bytes
                        )
                        uploadMessage = "Audios enviados correctamente."

                        var status = apiClient.getStatus(jobId!!)

                        while (status.status == "PENDIENTE" || status.status == "PROCESANDO") {
                            delay(2000)
                            status = apiClient.getStatus(jobId!!)
                        }

                        if (status.status == "LISTO") {
                            val result = apiClient.getResult(jobId!!)
                            uploadMessage =
                                "Análisis listo. Segmentos detectados: ${result.segments.size}"
                        } else {
                            uploadMessage = when (status.status) {
                                "ERROR" -> "Error durante el análisis."
                                else -> "Estado: ${status.status}"
                            }
                        }
                    } catch (e: Exception) {
                        uploadMessage = "Error al enviar los audios: ${e.message}"
                    } finally {
                        uploading = false
                    }
                }
            },
            enabled = !uploading,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(if (uploading) "Enviando..." else "Analizar")
        }

        uploadMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        jobId?.let {
            Text(
                text = "Job ID: $it",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Volver")
        }
    }
}
