package com.rubengarcia.choircorrector

import io.ktor.utils.io.core.Input

/**
 * Represents a streamable audio file without loading its contents into memory.
 */
data class AudioFile(
    val fileName: String,
    val sizeBytes: Long?,
    val streamProvider: AudioStreamProvider
)

/**
 * Platform-specific provider for opening audio file inputs.
 *
 * Each invocation must return a fresh input positioned at the beginning
 * of the audio file.
 */
interface AudioStreamProvider {
    fun openStream(): Input
}

interface AudioFileReader {
    suspend fun read(uri: String): AudioFile
}
