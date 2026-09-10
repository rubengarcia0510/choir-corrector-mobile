package com.rubengarcia.choircorrector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    private lateinit var audioFilePicker: AndroidAudioFilePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioFilePicker = AndroidAudioFilePicker(this)

        setContent {
            App(
                audioFilePicker = audioFilePicker
            )
        }
    }
}
