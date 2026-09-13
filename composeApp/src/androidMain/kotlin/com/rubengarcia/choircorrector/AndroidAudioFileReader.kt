package com.rubengarcia.choircorrector

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

/**
 * Android implementation of AudioStreamProvider.
 * Opens fresh InputStreams from ContentResolver on demand without loading into memory.
 */
class AndroidAudioStreamProvider(
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : AudioStreamProvider {

    override suspend fun openStream(): InputStream {
        return contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open stream for audio file: $uri")
    }
}

class AndroidAudioFileReader(
    private val contentResolver: ContentResolver
) : AudioFileReader {

    override suspend fun read(uri: String): AudioFile {
        val parsedUri = Uri.parse(uri)

        // Get file name from ContentResolver metadata
        val fileName = getFileName(parsedUri)

        // Get file size from ContentResolver metadata (without loading entire file)
        val sizeBytes = getFileSize(parsedUri)

        // Create a stream provider that will open fresh streams on demand
        val streamProvider = AndroidAudioStreamProvider(contentResolver, parsedUri)

        return AudioFile(
            fileName = fileName,
            sizeBytes = sizeBytes,
            streamProvider = streamProvider
        )
    }

    /**
     * Query the file name from ContentResolver without loading the file.
     */
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

    /**
     * Query the file size from ContentResolver without loading the file.
     * Returns null if size cannot be determined.
     */
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
