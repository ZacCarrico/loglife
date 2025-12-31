package com.loglife.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Accessibility Service that detects volume down button presses
 * to trigger voice recording.
 */
class VolumeButtonAccessibilityService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isVolumeDownPressed = AtomicBoolean(false)
    private var recordingJob: Job? = null
    
    // Minimum hold duration before starting recording (ms)
    private val holdThreshold = 300L
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        isServiceRunning = true
        
        // Notify listeners that service is ready
        serviceStatusListeners.forEach { it.onServiceStatusChanged(true) }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We only care about key events, not accessibility events
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }
    
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isVolumeDownPressed.getAndSet(true)) {
                        Log.d(TAG, "Volume down pressed")
                        onVolumeDownPressed()
                    }
                    // Consume the event to prevent volume change
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeDownPressed.getAndSet(false)) {
                        Log.d(TAG, "Volume down released")
                        onVolumeDownReleased()
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }
    
    private fun onVolumeDownPressed() {
        recordingJob = serviceScope.launch {
            // Wait for hold threshold
            delay(holdThreshold)
            
            if (isVolumeDownPressed.get()) {
                // Start recording
                startRecordingService()
            }
        }
    }
    
    private fun onVolumeDownReleased() {
        recordingJob?.cancel()
        recordingJob = null
        
        // Stop recording if it was started
        stopRecordingService()
    }
    
    private fun startRecordingService() {
        Log.i(TAG, "Starting recording service")
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START_RECORDING
        }
        startForegroundService(intent)
    }
    
    private fun stopRecordingService() {
        Log.i(TAG, "Stopping recording service")
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        startService(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isServiceRunning = false
        serviceStatusListeners.forEach { it.onServiceStatusChanged(false) }
        Log.i(TAG, "Accessibility service destroyed")
    }
    
    interface ServiceStatusListener {
        fun onServiceStatusChanged(isRunning: Boolean)
    }
    
    companion object {
        private const val TAG = "VolumeButtonService"
        
        var isServiceRunning = false
            private set
        
        private val serviceStatusListeners = mutableListOf<ServiceStatusListener>()
        
        fun addServiceStatusListener(listener: ServiceStatusListener) {
            serviceStatusListeners.add(listener)
        }
        
        fun removeServiceStatusListener(listener: ServiceStatusListener) {
            serviceStatusListeners.remove(listener)
        }
    }
}
