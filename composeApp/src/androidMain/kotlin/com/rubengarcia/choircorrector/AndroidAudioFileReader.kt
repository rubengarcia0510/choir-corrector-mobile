package com.rubengarcia.choircorrector

import android.content.ContentResolver
import android.net.Uri

class AndroidAudioFileReader(
    private val contentResolver: ContentResolver
) : AudioFileReader {

    override fun read(uri: String): AudioFile {
        val parsedUri = Uri.parse(uri)

        val bytes = contentResolver.openInputStream(parsedUri)?.use { input ->
            input.readBytes()
        } ?: error("Unable to read audio file: $uri")

        val fileName = contentResolver.query(
            parsedUri,
            arrayOf("_display_name"),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        } ?: "audio"

        return AudioFile(
            fileName = fileName,
            bytes = bytes
        )
    }
}
