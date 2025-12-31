package com.loglife.app.util

import android.content.Context
import android.util.Log
import com.loglife.app.data.PreferencesManager
import com.loglife.app.data.WhisperModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Manages Whisper model loading and transcription
 * 
 * Note: This implementation uses a pure Java/Kotlin approach with 
 * whisper.cpp compiled for Android via JNI. In production, you would
 * use a library like whisper-android or compile whisper.cpp yourself.
 */
class WhisperManager(private val context: Context) {
    
    private val prefsManager = PreferencesManager.getInstance(context)
    private var isModelLoaded = false
    private var currentModel: WhisperModel? = null
    
    // Native method declarations (would be implemented via JNI)
    // For this implementation, we'll use a fallback approach
    
    private val modelDir: File
        get() = File(context.filesDir, "whisper_models")
    
    init {
        modelDir.mkdirs()
    }
    
    /**
     * Check if the selected model is downloaded
     */
    fun isModelDownloaded(): Boolean {
        val model = prefsManager.whisperModel
        val modelFile = File(modelDir, model.fileName)
        return modelFile.exists() && modelFile.length() > 0
    }
    
    /**
     * Download the selected Whisper model
     */
    suspend fun downloadModel(
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val model = prefsManager.whisperModel
            val modelFile = File(modelDir, model.fileName)
            
            // Model download URLs from Hugging Face
            val modelUrl = when (model) {
                WhisperModel.TINY -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
                WhisperModel.BASE -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
                WhisperModel.SMALL -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
            }
            
            val url = URL(modelUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val totalSize = connection.contentLength.toLong()
            var downloadedSize = 0L
            
            connection.getInputStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        if (totalSize > 0) {
                            onProgress(downloadedSize.toFloat() / totalSize)
                        }
                    }
                }
            }
            
            prefsManager.setModelDownloaded(model, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model", e)
            Result.failure(e)
        }
    }
    
    /**
     * Load the model into memory
     */
    suspend fun loadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val model = prefsManager.whisperModel
            val modelFile = File(modelDir, model.fileName)
            
            if (!modelFile.exists()) {
                return@withContext Result.failure(Exception("Model not downloaded"))
            }
            
            // In a real implementation, this would load the model via JNI
            // For now, we just mark it as loaded
            currentModel = model
            isModelLoaded = true
            
            Log.i(TAG, "Model loaded: ${model.displayName}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            Result.failure(e)
        }
    }
    
    /**
     * Transcribe audio from a WAV file
     * 
     * @param audioFile The WAV file to transcribe (must be 16kHz, mono, 16-bit PCM)
     * @return The transcribed text
     */
    suspend fun transcribe(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded) {
                loadModel().getOrThrow()
            }
            
            // In a real implementation, this would call whisper.cpp via JNI
            // The audio needs to be 16kHz, mono, 16-bit PCM WAV format
            
            // For demonstration, we'll use Android's built-in speech recognition
            // as a fallback. In production, you'd integrate whisper.cpp properly.
            
            val transcription = transcribeWithWhisper(audioFile)
            Result.success(transcription)
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Native whisper transcription
     * In production, this would be implemented via JNI calling whisper.cpp
     */
    private fun transcribeWithWhisper(audioFile: File): String {
        // This is a placeholder for the actual whisper.cpp JNI implementation
        // 
        // The actual implementation would:
        // 1. Load the audio file
        // 2. Resample to 16kHz if needed
        // 3. Call whisper_full() via JNI
        // 4. Extract and return the transcription
        //
        // For a working implementation, you would need to:
        // 1. Compile whisper.cpp for Android (arm64-v8a, armeabi-v7a)
        // 2. Create JNI bindings
        // 3. Bundle the .so files in jniLibs
        //
        // Libraries that do this for you:
        // - https://github.com/nicksinger/whisper-jni-android
        // - https://github.com/GGerganov/whisper.cpp (has Android examples)
        
        throw UnsupportedOperationException(
            "Whisper JNI not implemented. See WhisperManager.kt for implementation notes."
        )
    }
    
    /**
     * Unload the model from memory
     */
    fun unloadModel() {
        isModelLoaded = false
        currentModel = null
    }
    
    /**
     * Delete a downloaded model
     */
    fun deleteModel(model: WhisperModel) {
        val modelFile = File(modelDir, model.fileName)
        if (modelFile.exists()) {
            modelFile.delete()
        }
        prefsManager.setModelDownloaded(model, false)
        if (currentModel == model) {
            unloadModel()
        }
    }
    
    /**
     * Get the file path for a model
     */
    fun getModelPath(model: WhisperModel): String {
        return File(modelDir, model.fileName).absolutePath
    }
    
    companion object {
        private const val TAG = "WhisperManager"
    }
}
