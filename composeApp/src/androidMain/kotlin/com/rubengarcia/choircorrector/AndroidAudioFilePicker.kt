package com.rubengarcia.choircorrector

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class AndroidAudioFilePicker(
    private val activity: ComponentActivity
) : AudioFilePicker {

    private var onResultCallback: ((String?) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        onResultCallback?.invoke(uri?.toString())
        onResultCallback = null
    }

    override fun pickAudio(onResult: (String?) -> Unit) {
        onResultCallback = onResult

        launcher.launch(
            arrayOf(
                "audio/*"
            )
        )
    }
}
