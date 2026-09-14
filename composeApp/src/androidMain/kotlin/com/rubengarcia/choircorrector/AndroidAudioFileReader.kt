package com.rubengarcia.choircorrector

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import io.ktor.utils.io.streams.asInput

/**
 * Android implementation of AudioStreamProvider.
 *
 * The underlying ContentResolver InputStream is opened only when the
 * multipart upload asks for it. The audio contents are never loaded
 * completely into memory.
 */
class AndroidAudioStreamProvider(
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : AudioStreamProvider {

    override fun openStream(): io.ktor.utils.io.core.Input {
        return contentResolver.openInputStream(uri)
            ?.asInput()
            ?: throw IllegalStateException(
                "Unable to open stream for audio file: $uri"
            )
    }
}

class AndroidAudioFileReader(
    private val contentResolver: ContentResolver
) : AudioFileReader {

    override suspend fun read(uri: String): AudioFile {
        val parsedUri = Uri.parse(uri)

        val fileName = getFileName(parsedUri)
        val sizeBytes = getFileSize(parsedUri)

        return AudioFile(
            fileName = fileName,
            sizeBytes = sizeBytes,
            streamProvider = AndroidAudioStreamProvider(
                contentResolver,
                parsedUri
            )
        )
    }

    private fun getFileName(uri: Uri): String {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
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
    }

    private fun getFileSize(uri: Uri): Long? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    val size = cursor.getLong(sizeIndex)
                    if (size > 0L) size else null
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
}
