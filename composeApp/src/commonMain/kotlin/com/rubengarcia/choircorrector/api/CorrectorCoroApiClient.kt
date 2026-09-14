package com.rubengarcia.choircorrector.api

import com.rubengarcia.choircorrector.AudioFile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

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
                        appendInput(
                            key = "audio",
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"${audioFile.fileName}\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.parse("audio/wav").toString()
                                )
                            },
                            size = audioFile.sizeBytes
                        ) {
                            audioFile.streamProvider.openStream()
                        }
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
                        appendInput(
                            key = "audio",
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"${audioFile.fileName}\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.parse("audio/wav").toString()
                                )
                            },
                            size = audioFile.sizeBytes
                        ) {
                            audioFile.streamProvider.openStream()
                        }
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
