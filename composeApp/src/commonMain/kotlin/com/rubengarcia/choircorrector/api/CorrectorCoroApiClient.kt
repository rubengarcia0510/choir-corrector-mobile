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

class CorrectorCoroApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {

    suspend fun uploadReference(
        coroId: String,
        fileName: String,
        audioBytes: ByteArray
    ) {
        val response = httpClient.post("$baseUrl/coros/$coroId/referencia") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "audio",
                            audioBytes,
                            headers = io.ktor.http.Headers.build {
                                append(
                                    io.ktor.http.HttpHeaders.ContentDisposition,
                                    "form-data; name=\"audio\"; filename=\"$fileName\""
                                )
                                append(
                                    io.ktor.http.HttpHeaders.ContentType,
                                    ContentType.Audio.Any.toString()
                                )
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
        fileName: String,
        audioBytes: ByteArray
    ): String {
        val response = httpClient.post("$baseUrl/coros/$coroId/ensayos") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "audio",
                            audioBytes,
                            headers = io.ktor.http.Headers.build {
                                append(
                                    io.ktor.http.HttpHeaders.ContentDisposition,
                                    "form-data; name=\"audio\"; filename=\"$fileName\""
                                )
                                append(
                                    io.ktor.http.HttpHeaders.ContentType,
                                    ContentType.Audio.Any.toString()
                                )
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
