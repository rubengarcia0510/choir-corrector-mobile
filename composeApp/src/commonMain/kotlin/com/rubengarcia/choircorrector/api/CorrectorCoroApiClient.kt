package com.rubengarcia.choircorrector.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.io.File

class CorrectorCoroApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient()
) {
    
    suspend fun uploadReference(
        referenceAudioFile: File
    ): HttpResponse {
        val referenceBytes = referenceAudioFile.readBytes()
        
        return httpClient.post("$baseUrl/api/upload/reference") {
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV.toString())
                            }
                        )
                    }
                )
            )
        }
    }
    
    suspend fun uploadRehearsal(
        rehearsalAudioFile: File
    ): HttpResponse {
        val rehearsalBytes = rehearsalAudioFile.readBytes()
        
        return httpClient.post("$baseUrl/api/upload/rehearsal") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "rehearsal_audio",
                            value = rehearsalBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"rehearsal_audio\"; filename=\"${rehearsalAudioFile.name}\""
                                )
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV.toString())
                            }
                        )
                    }
                )
            )
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV.toString())
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV.toString())
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV.toString())
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV.toString())
                            }
                        )
                    }
                )
            )
        }
    }
    
    suspend fun getStatus(
        analysisId: String
    ): HttpResponse {
        return httpClient.post("$baseUrl/api/status/$analysisId") {
            // Empty body for GET-like status check
        }
    }
    
    suspend fun getResult(
        analysisId: String
    ): HttpResponse {
        return httpClient.post("$baseUrl/api/result/$analysisId") {
            // Empty body for GET-like result retrieval
        }
    }
}
