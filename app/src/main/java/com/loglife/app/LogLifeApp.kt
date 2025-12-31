package com.loglife.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.loglife.app.data.AppDatabase
import com.loglife.app.util.WhisperManager

class LogLifeApp : Application(), Configuration.Provider {
    
    lateinit var database: AppDatabase
        private set
    
    lateinit var whisperManager: WhisperManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        database = AppDatabase.getInstance(this)
        
        // Initialize Whisper manager
        whisperManager = WhisperManager(this)
        
        // Create notification channels
        createNotificationChannels()
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Recording channel
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when recording is in progress"
                setShowBadge(false)
            }
            
            // Sync status channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                getString(R.string.notification_channel_sync),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows sync status and errors"
            }
            
            notificationManager.createNotificationChannel(recordingChannel)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    companion object {
        const val CHANNEL_RECORDING = "recording_channel"
        const val CHANNEL_SYNC = "sync_channel"
        
        lateinit var instance: LogLifeApp
            private set
    }
}
