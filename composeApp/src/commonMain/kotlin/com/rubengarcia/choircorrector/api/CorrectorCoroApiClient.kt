package com.rubengarcia.choircorrector.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import com.rubengarcia.choircorrector.AudioFile
import io.ktor.utils.io.ByteReadChannel

class CorrectorCoroApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {

    suspend fun uploadReference(
        coroId: String,
        audioFile: AudioFile
    ) {
        val response = httpClient.post("$baseUrl/coros/$coroId/referencia") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        // Open stream and convert to ByteReadChannel for Ktor
                        val stream = audioFile.streamProvider.openStream()
                        val channel = try {
                            ByteReadChannel(stream)
                        } finally {
                            stream.close()
                        }
                        
                        append(
                            "audio",
                            channel,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"audio\"; filename=\"${audioFile.fileName}\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Audio.Any.toString()
                                )
                                // Include Content-Length if available
                                audioFile.sizeBytes?.let {
                                    append(HttpHeaders.ContentLength, it.toString())
                                }
                            }
                        )
                    }
                )
            )
        }

        check(response.status == HttpStatusCode.Created) {
            "Reference upload failed: ${response.status} ${response.bodyAsText()}"
        }
    }

    suspend fun uploadRehearsal(
        coroId: String,
        audioFile: AudioFile
    ): String {
        val response = httpClient.post("$baseUrl/coros/$coroId/ensayos") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        // Open stream and convert to ByteReadChannel for Ktor
                        val stream = audioFile.streamProvider.openStream()
                        val channel = try {
                            ByteReadChannel(stream)
                        } finally {
                            stream.close()
                        }
                        
                        append(
                            "audio",
                            channel,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"audio\"; filename=\"${audioFile.fileName}\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Audio.Any.toString()
                                )
                                // Include Content-Length if available
                                audioFile.sizeBytes?.let {
                                    append(HttpHeaders.ContentLength, it.toString())
                                }
                            }
                        )
                    }
                )
            )
        }

        check(response.status == HttpStatusCode.Accepted) {
            "Rehearsal upload failed: ${response.status} ${response.bodyAsText()}"
        }

        return response.bodyAsText()
    }

    suspend fun getStatus(jobId: String): AnalysisStatusResponse =
        httpClient.get("$baseUrl/ensayos/$jobId/estado").body()

    suspend fun getResult(jobId: String): AnalysisResultResponse =
        httpClient.get("$baseUrl/ensayos/$jobId/resultado").body()
}
