package com.rubengarcia.choircorrector.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class CorrectorCoroApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    @Serializable
    data class AnalysisStatus(val status: String)

    @Serializable
    data class AnalysisResult(val segments: List<JsonElement> = emptyList())

    private fun audioWavContentTypeString(): String = ContentType.parse("audio/wav").toString()
    
    suspend fun uploadReference(
        coroId: String,
        audioFile: File
    ): HttpResponse {
        val referenceBytes = audioFile.readBytes()
        return httpClient.post("$baseUrl/api/upload/reference") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("coro_id", coroId)
                        append(
                            key = "reference_audio",
                            value = referenceBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"reference_audio\"; filename=\"${audioFile.name}\""
                                )
                                append(HttpHeaders.ContentType, audioWavContentTypeString())
                            }
                        )
                    }
                )
            )
        }
    }
    
    suspend fun uploadRehearsal(
        coroId: String,
        audioFile: File
    ): String {
        val rehearsalBytes = audioFile.readBytes()
        val response = httpClient.post("$baseUrl/api/upload/rehearsal") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("coro_id", coroId)
                        append(
                            key = "rehearsal_audio",
                            value = rehearsalBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"rehearsal_audio\"; filename=\"${audioFile.name}\""
                                )
                                append(HttpHeaders.ContentType, audioWavContentTypeString())
                            }
                        )
                    }
                )
            )
        }

        val text = response.bodyAsText().trim()
        // Try to parse JSON with common keys, otherwise return raw text
        return try {
            val parsed = json.parseToJsonElement(text)
            if (parsed is JsonObject) {
                val obj = parsed.jsonObject
                val candidates = listOf("jobId", "job_id", "id", "analysisId", "analysis_id")
                for (c in candidates) {
                    if (obj.containsKey(c)) return obj[c].toString().trim('"')
                }
                // If object has a single field which is a string, return it
                val first = obj.entries.firstOrNull()
                if (first != null && first.value is JsonElement) {
                    return first.value.toString().trim('"')
                }
                text
            } else {
                text
            }
        } catch (e: Exception) {
            // Not JSON, just return the raw body
            text
        }
    }
    
    suspend fun uploadAudioFiles(
        referenceAudioFile: File,
        rehearsalAudioFile: File
    ): HttpResponse {
        val referenceBytes = referenceAudioFile.readBytes()
        val rehearsalBytes = rehearsalAudioFile.readBytes()
        return httpClient.post("$baseUrl/api/analyze") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "reference_audio",
                            value = referenceBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"reference_audio\"; filename=\"${referenceAudioFile.name}\""
                                )
                                append(HttpHeaders.ContentType, audioWavContentTypeString())
                            }
                        )
                        append(
                            key = "rehearsal_audio",
                            value = rehearsalBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"rehearsal_audio\"; filename=\"${rehearsalAudioFile.name}\""
                                )
                                append(HttpHeaders.ContentType, audioWavContentTypeString())
                            }
                        )
                    }
                )
            )
        }
    }
    
    suspend fun uploadAudioFileBytes(
        referenceAudioBytes: ByteArray,
        rehearsalAudioBytes: ByteArray,
        referenceFileName: String = "reference.wav",
        rehearsalFileName: String = "rehearsal.wav"
    ): HttpResponse {
        return httpClient.post("$baseUrl/api/analyze") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "reference_audio",
                            value = referenceAudioBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"reference_audio\"; filename=\"$referenceFileName\""
                                )
                                append(HttpHeaders.ContentType, audioWavContentTypeString())
                            }
                        )
                        append(
                            key = "rehearsal_audio",
                            value = rehearsalAudioBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"rehearsal_audio\"; filename=\"$rehearsalFileName\""
                                )
                                append(HttpHeaders.ContentType, audioWavContentTypeString())
                            }
                        )
                    }
                )
            )
        }
    }
    
    suspend fun getStatus(
        analysisId: String
    ): AnalysisStatus {
        val response = httpClient.post("$baseUrl/api/status/$analysisId") {
            // Empty body for GET-like status check
        }
        val text = response.bodyAsText()
        return try {
            json.decodeFromString(AnalysisStatus.serializer(), text)
        } catch (e: Exception) {
            // Fallback: return UNKNOWN status
            AnalysisStatus(status = text)
        }
    }
    
    suspend fun getResult(
        analysisId: String
    ): AnalysisResult {
        val response = httpClient.post("$baseUrl/api/result/$analysisId") {
            // Empty body for GET-like result retrieval
        }
        val text = response.bodyAsText()
        return try {
            json.decodeFromString(AnalysisResult.serializer(), text)
        } catch (e: Exception) {
            // Try to parse as generic JSON and extract segments
            try {
                val parsed = json.parseToJsonElement(text)
                if (parsed is JsonObject && parsed.jsonObject.containsKey("segments")) {
                    val segmentsElement = parsed.jsonObject["segments"]!!
                    // Use the JsonElement to construct AnalysisResult via decodeFromString
                    val segmentsJson = json.encodeToString(JsonElement.serializer(), segmentsElement)
                    val wrapper = "{\"segments\":$segmentsJson}"
                    json.decodeFromString(AnalysisResult.serializer(), wrapper)
                } else {
                    AnalysisResult()
                }
            } catch (ex: Exception) {
                AnalysisResult()
            }
        }
    }
}
