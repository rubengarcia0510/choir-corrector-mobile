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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                        append(
                            "audio",
                            audioFile.sizeBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"audio\"; filename=\"${audioFile.fileName}\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Audio.Any.toString()
                                )
                            }
                        ) {
                            // Open stream on-demand and write directly to multipart body
                            val stream = audioFile.streamProvider.openStream()
                            try {
                                stream.copyTo(this)
                            } finally {
                                stream.close()
                            }
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
                        append(
                            "audio",
                            audioFile.sizeBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"audio\"; filename=\"${audioFile.fileName}\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Audio.Any.toString()
                                )
                            }
                        ) {
                            // Open stream on-demand and write directly to multipart body
                            val stream = audioFile.streamProvider.openStream()
                            try {
                                stream.copyTo(this)
                            } finally {
                                stream.close()
                            }
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
