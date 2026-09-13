package com.rubengarcia.choircorrector

/**
 * Represents a streamable audio file without loading its contents into memory.
 *
 * @param fileName Display name of the audio file
 * @param sizeBytes Total size in bytes (nullable if size cannot be determined)
 * @param streamProvider Platform-specific provider that opens a fresh stream on demand
 */
data class AudioFile(
    val fileName: String,
    val sizeBytes: Long?,
    val streamProvider: AudioStreamProvider
)

/**
 * Platform-specific provider for opening audio file streams.
 * Must be thread-safe and allow multiple invocations to open fresh streams.
 */
interface AudioStreamProvider {
    /**
     * Opens a fresh InputStream for the audio file.
     * Caller is responsible for closing the returned stream.
     *
     * @return A new InputStream positioned at the start of the file
     * @throws IOException if the stream cannot be opened
     */
    suspend fun openStream(): java.io.InputStream
}

interface AudioFileReader {
    suspend fun read(uri: String): AudioFile
}
