package com.loglife.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Records audio in WAV format suitable for Whisper transcription
 * (16kHz, mono, 16-bit PCM)
 */
class AudioRecorder(private val context: Context) {
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var rawDataFile: File? = null
    
    // Whisper requires 16kHz sample rate
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    private val bufferSize: Int by lazy {
        val minSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        maxOf(minSize, sampleRate * 2) // At least 1 second buffer
    }
    
    /**
     * Start recording audio
     * @return The file where audio will be saved
     */
    suspend fun startRecording(): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!hasRecordPermission()) {
                return@withContext Result.failure(SecurityException("Microphone permission not granted"))
            }
            
            if (isRecording) {
                stopRecording()
            }
            
            // Create temp files
            val cacheDir = File(context.cacheDir, "recordings")
            cacheDir.mkdirs()
            
            val timestamp = System.currentTimeMillis()
            outputFile = File(cacheDir, "recording_$timestamp.wav")
            rawDataFile = File(cacheDir, "recording_$timestamp.raw")
            
            // Initialize AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("Failed to initialize AudioRecord"))
            }
            
            isRecording = true
            audioRecord?.startRecording()
            
            // Start recording thread
            recordingThread = Thread {
                writeAudioDataToFile()
            }.apply { start() }
            
            Log.i(TAG, "Recording started: ${outputFile?.absolutePath}")
            Result.success(outputFile!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            Result.failure(e)
        }
    }
    
    /**
     * Stop recording and finalize the WAV file
     */
    suspend fun stopRecording(): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!isRecording) {
                return@withContext Result.failure(Exception("Not recording"))
            }
            
            isRecording = false
            
            // Wait for recording thread to finish
            recordingThread?.join(2000)
            recordingThread = null
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            // Convert raw to WAV
            val wavFile = outputFile ?: return@withContext Result.failure(Exception("No output file"))
            val rawFile = rawDataFile ?: return@withContext Result.failure(Exception("No raw data file"))
            
            convertRawToWav(rawFile, wavFile)
            
            // Clean up raw file
            rawFile.delete()
            
            Log.i(TAG, "Recording stopped: ${wavFile.absolutePath}, size: ${wavFile.length()}")
            Result.success(wavFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            cleanup()
            Result.failure(e)
        }
    }
    
    /**
     * Cancel recording without saving
     */
    fun cancelRecording() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        cleanup()
    }
    
    private fun writeAudioDataToFile() {
        val buffer = ShortArray(bufferSize / 2)
        
        try {
            FileOutputStream(rawDataFile).use { output ->
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // Convert shorts to bytes
                        val byteBuffer = ByteBuffer.allocate(readSize * 2)
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until readSize) {
                            byteBuffer.putShort(buffer[i])
                        }
                        output.write(byteBuffer.array())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing audio data", e)
        }
    }
    
    private fun convertRawToWav(rawFile: File, wavFile: File) {
        val rawData = rawFile.readBytes()
        val dataLength = rawData.size
        val totalLength = dataLength + 36
        
        FileOutputStream(wavFile).use { output ->
            // WAV header
            output.write("RIFF".toByteArray()) // ChunkID
            output.write(intToByteArray(totalLength)) // ChunkSize
            output.write("WAVE".toByteArray()) // Format
            output.write("fmt ".toByteArray()) // Subchunk1ID
            output.write(intToByteArray(16)) // Subchunk1Size (16 for PCM)
            output.write(shortToByteArray(1)) // AudioFormat (1 for PCM)
            output.write(shortToByteArray(1)) // NumChannels (1 for mono)
            output.write(intToByteArray(sampleRate)) // SampleRate
            output.write(intToByteArray(sampleRate * 2)) // ByteRate
            output.write(shortToByteArray(2)) // BlockAlign
            output.write(shortToByteArray(16)) // BitsPerSample
            output.write("data".toByteArray()) // Subchunk2ID
            output.write(intToByteArray(dataLength)) // Subchunk2Size
            output.write(rawData) // Audio data
        }
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }
    
    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte()
        )
    }
    
    private fun cleanup() {
        outputFile?.delete()
        rawDataFile?.delete()
        outputFile = null
        rawDataFile = null
    }
    
    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    companion object {
        private const val TAG = "AudioRecorder"
    }
}
