package com.loglife.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives boot completed broadcast to ensure accessibility service
 * is ready after device restart
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed, scheduling sync")
            
            // Schedule periodic sync to catch any pending notes
            SyncWorker.schedulePeriodicSync(context)
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}
