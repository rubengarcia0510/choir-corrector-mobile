package com.rubengarcia.choircorrector

data class AudioFile(
    val fileName: String,
    val bytes: ByteArray
)

interface AudioFileReader {
    fun read(uri: String): AudioFile
}
