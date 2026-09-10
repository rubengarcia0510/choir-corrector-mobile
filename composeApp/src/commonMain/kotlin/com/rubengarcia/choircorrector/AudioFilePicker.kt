package com.rubengarcia.choircorrector

interface AudioFilePicker {
    fun pickAudio(onResult: (String?) -> Unit)
}
