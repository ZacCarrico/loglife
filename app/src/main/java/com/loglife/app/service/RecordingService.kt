package com.loglife.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.loglife.app.R
import com.loglife.app.LogLifeApp
import com.loglife.app.data.PendingNote
import com.loglife.app.data.PreferencesManager
import com.loglife.app.ui.MainActivity
import com.loglife.app.util.AudioRecorder
import com.loglife.app.util.GoogleDocsManager
import com.loglife.app.util.NetworkUtils
import kotlinx.coroutines.*
import java.io.File

/**
 * Foreground service for recording audio, transcribing, and syncing to Google Docs
 */
class RecordingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var audioRecorder: AudioRecorder? = null
    private var isRecording = false
    private var recordingFile: File? = null
    
    private val prefsManager by lazy { PreferencesManager.getInstance(this) }
    private val docsManager by lazy { GoogleDocsManager.getInstance(this) }
    private val whisperManager by lazy { LogLifeApp.instance.whisperManager }
    private val database by lazy { LogLifeApp.instance.database }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        audioRecorder = AudioRecorder(this)
        Log.i(TAG, "Recording service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        return START_NOT_STICKY
    }
    
    private fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        // Check if document is selected
        val docId = prefsManager.selectedDocId
        if (docId.isNullOrBlank()) {
            Log.e(TAG, "No document selected")
            notifyError(getString(R.string.error_no_document))
            stopSelf()
            return
        }
        
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createRecordingNotification())
        
        isRecording = true
        broadcastStatus(STATUS_RECORDING)
        
        serviceScope.launch {
            try {
                val result = audioRecorder?.startRecording()
                result?.onSuccess { file ->
                    recordingFile = file
                    Log.i(TAG, "Recording to: ${file.absolutePath}")
                }?.onFailure { e ->
                    Log.e(TAG, "Failed to start recording", e)
                    notifyError(e.message ?: "Recording failed")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting recording", e)
                notifyError(e.message ?: "Recording failed")
                stopSelf()
            }
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            stopSelf()
            return
        }
        
        isRecording = false
        
        serviceScope.launch {
            try {
                // Stop recording
                val result = audioRecorder?.stopRecording()
                result?.onSuccess { audioFile ->
                    processRecording(audioFile)
                }?.onFailure { e ->
                    Log.e(TAG, "Failed to stop recording", e)
                    notifyError(e.message ?: "Recording failed")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception stopping recording", e)
                notifyError(e.message ?: "Recording failed")
                stopSelf()
            }
        }
    }
    
    private suspend fun processRecording(audioFile: File) {
        val timestamp = System.currentTimeMillis()
        val docId = prefsManager.selectedDocId ?: run {
            notifyError("No document selected")
            stopSelf()
            return
        }
        
        // Update notification
        updateNotification(getString(R.string.notification_transcribing))
        broadcastStatus(STATUS_TRANSCRIBING)
        
        try {
            // Transcribe audio
            val transcriptionResult = whisperManager.transcribe(audioFile)
            val transcription = transcriptionResult.getOrThrow()
            
            if (transcription.isBlank()) {
                Log.w(TAG, "Empty transcription")
                notifyError("No speech detected")
                stopSelf()
                return
            }
            
            Log.i(TAG, "Transcription: $transcription")
            
            // Create note text
            val noteText = docsManager.createNoteText(transcription, timestamp)
            val formattedTime = docsManager.formatTimestamp(timestamp)
            
            // Try to sync immediately if online
            if (NetworkUtils.isNetworkAvailable(this@RecordingService)) {
                broadcastStatus(STATUS_SYNCING)
                updateNotification(getString(R.string.status_syncing))
                
                val syncResult = docsManager.prependToDocument(docId, noteText)
                syncResult.onSuccess {
                    Log.i(TAG, "Note synced successfully")
                    notifySuccess()
                    broadcastStatus(STATUS_DONE)
                }.onFailure { e ->
                    Log.e(TAG, "Sync failed, queuing note", e)
                    queueNote(timestamp, formattedTime, transcription, docId)
                }
            } else {
                // Queue for later sync
                Log.i(TAG, "Offline, queuing note")
                queueNote(timestamp, formattedTime, transcription, docId)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            notifyError(e.message ?: "Processing failed")
        } finally {
            // Clean up audio file
            audioFile.delete()
            stopSelf()
        }
    }
    
    private suspend fun queueNote(
        timestamp: Long,
        formattedTime: String,
        transcription: String,
        docId: String
    ) {
        val note = PendingNote(
            timestamp = timestamp,
            formattedTime = formattedTime,
            transcription = transcription,
            targetDocId = docId
        )
        database.pendingNoteDao().insert(note)
        
        notifyQueued()
        broadcastStatus(STATUS_OFFLINE)
        
        // Schedule sync worker
        SyncWorker.enqueueSync(this)
    }
    
    private fun createRecordingNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, LogLifeApp.CHANNEL_RECORDING)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(getString(R.string.notification_recording_text))
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, LogLifeApp.CHANNEL_RECORDING)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .build()
        
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun notifySuccess() {
        showSyncNotification(getString(R.string.notification_synced))
    }
    
    private fun notifyQueued() {
        showSyncNotification(getString(R.string.notification_queued))
    }
    
    private fun notifyError(message: String) {
        showSyncNotification(getString(R.string.notification_sync_failed, message))
    }
    
    private fun showSyncNotification(text: String) {
        val notification = NotificationCompat.Builder(this, LogLifeApp.CHANNEL_SYNC)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_doc)
            .setAutoCancel(true)
            .build()
        
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(SYNC_NOTIFICATION_ID, notification)
    }
    
    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioRecorder?.cancelRecording()
        Log.i(TAG, "Recording service destroyed")
    }
    
    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val SYNC_NOTIFICATION_ID = 1002
        
        const val ACTION_START_RECORDING = "com.voicetodoc.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.voicetodoc.STOP_RECORDING"
        const val ACTION_STATUS_UPDATE = "com.voicetodoc.STATUS_UPDATE"
        
        const val EXTRA_STATUS = "status"
        
        const val STATUS_RECORDING = "recording"
        const val STATUS_TRANSCRIBING = "transcribing"
        const val STATUS_SYNCING = "syncing"
        const val STATUS_DONE = "done"
        const val STATUS_OFFLINE = "offline"
        const val STATUS_ERROR = "error"
    }
}
