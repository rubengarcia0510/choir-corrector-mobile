package com.rubengarcia.choircorrector.api

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisStatusResponse(
    val jobId: String,
    val status: String
)

@Serializable
data class AnalysisResultResponse(
    val jobId: String,
    val status: String,
    val segments: List<ChromaIntonationSegment>
)

@Serializable
data class ChromaIntonationSegment(
    val startReferenceTimestampSec: Double,
    val endReferenceTimestampSec: Double,
    val startPerformanceTimestampSec: Double,
    val endPerformanceTimestampSec: Double,
    val maxDeviationCents: Double,
    val meanDeviationCents: Double,
    val severity: String
)
