package com.rubengarcia.choircorrector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubengarcia.choircorrector.api.AnalysisResultResponse
import com.rubengarcia.choircorrector.billing.RevenueCatManager
import com.revenuecat.purchases.kmp.Purchases
import com.rubengarcia.choircorrector.api.CorrectorCoroApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen {
    HOME,
    NEW_ANALYSIS
}

private enum class UploadPhase {
    IDLE,
    UPLOADING_REFERENCE,
    UPLOADING_REHEARSAL,
    ANALYZING,
    ANALYSIS_READY
}

@Composable
fun App(
    audioFilePicker: AudioFilePicker,
    audioFileReader: AudioFileReader,
    apiClient: CorrectorCoroApiClient
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val revenueCatManager = remember { RevenueCatManager() }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    revenueCatManager = revenueCatManager,
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
    revenueCatManager: RevenueCatManager,
    onNewAnalysis: () -> Unit
) {
    var isPro by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isPro = try {
            revenueCatManager.isProActive()
        } catch (_: Exception) {
            false
        }
    }

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

        when (isPro) {
            true -> Text(
                text = "Choir Corrector Pro",
                style = MaterialTheme.typography.titleMedium
            )

            false -> Text(
                text = "Free plan",
                style = MaterialTheme.typography.titleMedium
            )

            null -> Text(
                text = "Checking subscription...",
                style = MaterialTheme.typography.bodyMedium
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNewAnalysis,
            enabled = isPro != null
        ) {
            Text("New analysis")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    isPro = try {
                        revenueCatManager.restorePurchases()
                    } catch (_: Exception) {
                        false
                    }
                }
            }
        ) {
            Text("Restore Purchases")
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
    val scope = rememberCoroutineScope()
    var rehearsalUri by remember { mutableStateOf<String?>(null) }
    var uploadPhase by remember { mutableStateOf(UploadPhase.IDLE) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var jobId by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember { mutableStateOf<AnalysisResultResponse?>(null) }

    val isBusy = uploadPhase != UploadPhase.IDLE && uploadPhase != UploadPhase.ANALYSIS_READY

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
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
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select reference")
        }

        referenceUri?.let {
            Text(
                text = "Reference selected",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                audioFilePicker.pickAudio { uri ->
                    rehearsalUri = uri
                }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select rehearsal")
        }

        rehearsalUri?.let {
            Text(
                text = "Rehearsal selected",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        uploadMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (analysisResult == null) {
            Button(
                onClick = {
                    val reference = referenceUri
                    val rehearsal = rehearsalUri

                    if (reference == null || rehearsal == null) {
                        uploadMessage = "Select both audio files."
                        return@Button
                    }

                    scope.launch {
                        uploadPhase = UploadPhase.UPLOADING_REFERENCE
                        uploadMessage = null
                        jobId = null

                        try {
                            val referenceFile = audioFileReader.read(reference)
                            apiClient.uploadReference(
                                coroId = "demo",
                                audioFile = referenceFile
                            )

                            uploadPhase = UploadPhase.UPLOADING_REHEARSAL
                            val rehearsalFile = audioFileReader.read(rehearsal)
                            jobId = apiClient.uploadRehearsal(
                                coroId = "demo",
                                audioFile = rehearsalFile
                            )

                            uploadPhase = UploadPhase.ANALYZING
                            var status = apiClient.getStatus(jobId!!)

                            while (status.status == "PENDIENTE" || status.status == "PROCESANDO") {
                                delay(2000)
                                status = apiClient.getStatus(jobId!!)
                            }

                            if (status.status == "LISTO") {
                                analysisResult = apiClient.getResult(jobId!!)
                                uploadPhase = UploadPhase.ANALYSIS_READY
                                uploadMessage = "Analysis ready. ${analysisResult!!.segments.size} segments detected."
                            } else {
                                uploadPhase = UploadPhase.IDLE
                                uploadMessage = when (status.status) {
                                    "ERROR" -> "Analysis error."
                                    else -> "Status: ${status.status}"
                                }
                            }
                        } catch (e: Exception) {
                            uploadPhase = UploadPhase.IDLE
                            uploadMessage = "Error uploading audio files: ${e.message}"
                        }
                    }
                },
                enabled = !isBusy && referenceUri != null && rehearsalUri != null,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    when (uploadPhase) {
                        UploadPhase.IDLE -> "Analyze"
                        UploadPhase.UPLOADING_REFERENCE -> "Uploading reference..."
                        UploadPhase.UPLOADING_REHEARSAL -> "Uploading rehearsal..."
                        UploadPhase.ANALYZING -> "Analyzing..."
                        UploadPhase.ANALYSIS_READY -> "Analysis ready"
                    }
                )
            }
        }

        if (analysisResult != null) {
            AnalysisResultView(
                result = analysisResult!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 20.dp)
            )
        }

        if (jobId != null && analysisResult == null) {
            Text(
                text = "Job ID: $jobId",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
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
        modifier = modifier
    ) {
        Text(
            text = "Analysis results",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "${result.segments.size} segments",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(result.segments) { index, segment ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Segment ${index + 1}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentField(
                            label = "Reference",
                            value = "${formatOneDecimal(segment.startReferenceTimestampSec)} – ${formatOneDecimal(segment.endReferenceTimestampSec)} s"
                        )
                        SegmentField(
                            label = "Rehearsal",
                            value = "${formatOneDecimal(segment.startPerformanceTimestampSec)} – ${formatOneDecimal(segment.endPerformanceTimestampSec)} s"
                        )
                        SegmentField(
                            label = "Mean deviation",
                            value = "${formatOneDecimal(segment.meanDeviationCents)} cents"
                        )
                        SegmentField(
                            label = "Maximum deviation",
                            value = "${formatOneDecimal(segment.maxDeviationCents)} cents"
                        )
                        SegmentField(
                            label = "Severity",
                            value = segment.severity
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentField(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatOneDecimal(value: Double): String {
    return ((value * 10.0).toInt() / 10.0).toString()
}
