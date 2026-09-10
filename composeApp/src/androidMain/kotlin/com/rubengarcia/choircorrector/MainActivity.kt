package com.rubengarcia.choircorrector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rubengarcia.choircorrector.api.CorrectorCoroApiClient

class MainActivity : ComponentActivity() {

    private lateinit var audioFilePicker: AndroidAudioFilePicker
    private lateinit var apiClient: CorrectorCoroApiClient
    private lateinit var audioFileReader: AndroidAudioFileReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioFilePicker = AndroidAudioFilePicker(this)
        audioFileReader = AndroidAudioFileReader(contentResolver)

        apiClient = CorrectorCoroApiClient(
            baseUrl = "http://127.0.0.1:8080",
            httpClient = createHttpClient()
        )

        setContent {
            App(
                audioFilePicker = audioFilePicker,
                audioFileReader = audioFileReader,
                apiClient = apiClient
            )
        }
    }
}
