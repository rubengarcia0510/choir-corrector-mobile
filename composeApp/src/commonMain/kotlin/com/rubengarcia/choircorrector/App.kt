package com.rubengarcia.choircorrector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rubengarcia.choircorrector.api.AnalysisResultResponse
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
            Text("New analysis")
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
    var analysisResult by remember {
        mutableStateOf<AnalysisResultResponse?>(null)
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "New analysis",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Select both audio files.",
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
            Text("Select reference")
        }

        referenceUri?.let {
            Text(
                text = "Reference selected",
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
            Text("Select rehearsal")
        }

        rehearsalUri?.let {
            Text(
                text = "Rehearsal selected",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                val reference = referenceUri
                val rehearsal = rehearsalUri

                if (reference == null || rehearsal == null) {
                    uploadMessage = "Select both audio files."
                    return@Button
                }

                scope.launch {
                    uploading = true
                    uploadMessage = null
                    analysisResult = null

                    try {
                        // Read reference metadata (no file content loaded yet)
                        val referenceFile = audioFileReader.read(reference)

                        // Upload reference file with streaming (opens stream only during upload)
                        apiClient.uploadReference(
                            coroId = "demo",
                            audioFile = referenceFile
                        )

                        // Read rehearsal metadata (no file content loaded yet)
                        val rehearsalFile = audioFileReader.read(rehearsal)

                        // Upload rehearsal file with streaming (opens stream only during upload)
                        jobId = apiClient.uploadRehearsal(
                            coroId = "demo",
                            audioFile = rehearsalFile
                        )
                        uploadMessage = "Audio files uploaded successfully."

                        var status = apiClient.getStatus(jobId!!)

                        while (status.status == "PENDIENTE" || status.status == "PROCESANDO") {
                            delay(2000)
                            status = apiClient.getStatus(jobId!!)
                        }

                        if (status.status == "LISTO") {
                            analysisResult = apiClient.getResult(jobId!!)
                            uploadMessage = "Analysis ready."
                        } else {
                            uploadMessage = when (status.status) {
                                "ERROR" -> "Analysis error."
                                else -> "Status: ${status.status}"
                            }
                        }
                    } catch (e: Exception) {
                        uploadMessage = "Error uploading audio files: ${e.message}"
                    } finally {
                        uploading = false
                    }
                }
            },
            enabled = !uploading,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(if (uploading) "Uploading..." else "Analyze")
        }

        uploadMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        analysisResult?.let { result ->
            AnalysisResultView(
                result = result,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .weight(1f)
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
            Text("Back")
        }
    }
}


@Composable
private fun AnalysisResultView(
    result: AnalysisResultResponse,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Analysis results",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Segments detected: ${result.segments.size}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(result.segments) { index, segment ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Segment ${index + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Reference: ${
                            segment.startReferenceTimestampSec.roundToTenths()
                        } - ${
                            segment.endReferenceTimestampSec.roundToTenths()
                        } s"
                    )

                    Text(
                        text = "Rehearsal: ${
                            segment.startPerformanceTimestampSec.roundToTenths()
                        } - ${
                            segment.endPerformanceTimestampSec.roundToTenths()
                        } s"
                    )

                    Text(
                        text = "Mean deviation: ${
                            segment.meanDeviationCents.roundToTenths()
                        } cents"
                    )

                    Text(
                        text = "Maximum deviation: ${
                            segment.maxDeviationCents.roundToTenths()
                        } cents"
                    )

                    Text(
                        text = "Severity: ${segment.severity}"
                    )
                }
            }
        }
    }
}

private fun Double.roundToTenths(): String =
    ((this * 10.0).toLong() / 10.0).toString()
