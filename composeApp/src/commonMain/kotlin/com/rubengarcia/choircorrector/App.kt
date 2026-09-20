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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubengarcia.choircorrector.api.AnalysisResultResponse
import com.rubengarcia.choircorrector.api.ChromaIntonationSegment
import com.rubengarcia.choircorrector.api.CorrectorCoroApiClient
import com.rubengarcia.choircorrector.billing.RevenueCatManager
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
    val scope = rememberCoroutineScope()

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

        if (isPro == false) {
            Button(
                onClick = {
                    scope.launch {
                        isPro = try {
                            revenueCatManager.purchasePro()
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
            ) {
                Text("Upgrade to Pro")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    val active = try {
                        revenueCatManager.isProActive()
                    } catch (_: Exception) {
                        false
                    }

                    isPro = active

                    if (active) {
                        onNewAnalysis()
                    }
                }
            },
            enabled = isPro == true
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
    var uploadPhase by remember { mutableStateOf(UploadPhase.IDLE) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember { mutableStateOf<AnalysisResultResponse?>(null) }

    val scope = rememberCoroutineScope()

    val isBusy = uploadPhase != UploadPhase.IDLE &&
        uploadPhase != UploadPhase.ANALYSIS_READY

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = "New analysis",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Compare a reference recording with a rehearsal.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "1. Reference audio",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "The recording used as the musical reference.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            audioFilePicker.pickAudio { uri ->
                                referenceUri = uri
                                uploadMessage = null
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (referenceUri == null) {
                                "Select reference"
                            } else {
                                "Change reference"
                            }
                        )
                    }

                    if (referenceUri != null) {
                        Text(
                            text = "✓ Reference selected",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "2. Rehearsal audio",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "The choir recording you want to evaluate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            audioFilePicker.pickAudio { uri ->
                                rehearsalUri = uri
                                uploadMessage = null
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (rehearsalUri == null) {
                                "Select rehearsal"
                            } else {
                                "Change rehearsal"
                            }
                        )
                    }

                    if (rehearsalUri != null) {
                        Text(
                            text = "✓ Rehearsal selected",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "3. Analysis",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Upload both recordings and compare them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnalysisProgress(
                        phase = uploadPhase,
                        message = uploadMessage
                    )

                    if (analysisResult == null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val reference = referenceUri
                                val rehearsal = rehearsalUri

                                if (reference == null || rehearsal == null) {
                                    uploadMessage = "Select both audio files before starting."
                                    return@Button
                                }

                                scope.launch {
                                    uploadPhase = UploadPhase.UPLOADING_REFERENCE
                                    uploadMessage = null

                                    try {
                                        val referenceFile =
                                            audioFileReader.read(reference)

                                        apiClient.uploadReference(
                                            coroId = "demo",
                                            audioFile = referenceFile
                                        )

                                        uploadPhase = UploadPhase.UPLOADING_REHEARSAL

                                        val rehearsalFile =
                                            audioFileReader.read(rehearsal)

                                        val jobId = apiClient.uploadRehearsal(
                                            coroId = "demo",
                                            audioFile = rehearsalFile
                                        )

                                        uploadPhase = UploadPhase.ANALYZING

                                        var status =
                                            apiClient.getStatus(jobId)

                                        while (
                                            status.status == "PENDIENTE" ||
                                            status.status == "PROCESANDO"
                                        ) {
                                            delay(2000)
                                            status = apiClient.getStatus(jobId)
                                        }

                                        if (status.status == "LISTO") {
                                            val result =
                                                apiClient.getResult(jobId)

                                            analysisResult = result
                                            uploadPhase =
                                                UploadPhase.ANALYSIS_READY
                                            uploadMessage =
                                                "Analysis completed successfully."
                                        } else {
                                            uploadPhase = UploadPhase.IDLE

                                            uploadMessage =
                                                when (status.status) {
                                                    "ERROR" ->
                                                        "The backend reported an analysis error."

                                                    else ->
                                                        "Analysis finished with status: ${status.status}"
                                                }
                                        }
                                    } catch (e: Exception) {
                                        uploadPhase = UploadPhase.IDLE
                                        uploadMessage =
                                            "We couldn't complete the analysis. Check your connection and try again."
                                    }
                                }
                            },
                            enabled = !isBusy &&
                                referenceUri != null &&
                                rehearsalUri != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when (uploadPhase) {
                                    UploadPhase.ANALYSIS_READY ->
                                        "Analysis ready"

                                    else ->
                                        "Start analysis"
                                }
                            )
                        }
                    }
                }
            }
        }

        if (analysisResult != null) {
            item {
                AnalysisResultSummary(
                    result = analysisResult!!
                )
            }

            item {
                Text(
                    text = "Detected segments",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            itemsIndexed(analysisResult!!.segments) { index, segment ->
                SegmentCard(
                    index = index,
                    segment = segment
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onBack,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnalysisProgress(
    phase: UploadPhase,
    message: String?
) {
    when (phase) {
        UploadPhase.IDLE -> {
            Text(
                text = message ?: "Ready to analyze.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (message != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        UploadPhase.UPLOADING_REFERENCE -> {
            ProgressRow(
                title = "Uploading reference",
                detail = "Sending the reference recording..."
            )
        }

        UploadPhase.UPLOADING_REHEARSAL -> {
            ProgressRow(
                title = "Uploading rehearsal",
                detail = "Sending the rehearsal recording..."
            )
        }

        UploadPhase.ANALYZING -> {
            ProgressRow(
                title = "Analyzing",
                detail = "Comparing pitch and timing..."
            )
        }

        UploadPhase.ANALYSIS_READY -> {
            Text(
                text = "✓ Analysis ready",
                style = MaterialTheme.typography.bodyLarge
            )

            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(end = 16.dp)
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AnalysisResultSummary(
    result: AnalysisResultResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Analysis results",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Your rehearsal has been compared with the reference.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            SummaryMetric(
                label = "Detected segments",
                value = result.segments.size.toString()
            )

            if (result.segments.isNotEmpty()) {
                val averageDeviation =
                    result.segments
                        .map { it.meanDeviationCents }
                        .average()

                val maximumDeviation =
                    result.segments
                        .maxOf { it.maxDeviationCents }

                SummaryMetric(
                    label = "Average deviation",
                    value = "${formatOneDecimal(averageDeviation)} cents"
                )

                SummaryMetric(
                    label = "Maximum deviation",
                    value = "${formatOneDecimal(maximumDeviation)} cents"
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun SegmentCard(
    index: Int,
    segment: ChromaIntonationSegment
) {
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
                value = "${formatOneDecimal(segment.startReferenceTimestampSec)} – " +
                    "${formatOneDecimal(segment.endReferenceTimestampSec)} s"
            )

            SegmentField(
                label = "Rehearsal",
                value = "${formatOneDecimal(segment.startPerformanceTimestampSec)} – " +
                    "${formatOneDecimal(segment.endPerformanceTimestampSec)} s"
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
