package com.rubengarcia.choircorrector

/**
 * Metadata and streaming provider for an audio file.
 * Does NOT hold the file contents in memory; instead provides a way to open
 * a stream on demand for uploading.
 *
 * @param fileName Display name of the audio file
 * @param sizeBytes Total size in bytes (nullable if size cannot be determined)
 * @param openStream Suspend function that returns a platform-specific stream provider.
 *                   On Android, this returns an AndroidAudioStreamProvider.
 *                   Must be called fresh each time to get a new stream.
 */
data class AudioFile(
    val fileName: String,
    val sizeBytes: Long?,
    val openStream: suspend () -> Any?
)

interface AudioFileReader {
    suspend fun read(uri: String): AudioFile
}
