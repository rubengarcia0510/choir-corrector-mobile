package com.rubengarcia.choircorrector.api

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.MultiPartFormDataContent
import io.ktor.http.content.PartData
import io.ktor.http.content.formData
import java.io.File

class CorrectorCoroApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient()
) {
    
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV)
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV)
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV)
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
                                append(HttpHeaders.ContentType, ContentType.Audio.WAV)
                            }
                        )
                    }
                )
            )
        }
    }
}
