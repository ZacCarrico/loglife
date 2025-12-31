package com.loglife.app

import android.app.Application

/**
 * Simplified MVP version of LogLifeApp
 * Removed: WorkManager, Room database, Whisper, notifications
 * MVP only needs: Basic app initialization
 */
class LogLifeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // MVP: Minimal initialization
        // Future: Database, WorkManager, Whisper, notifications
    }

    companion object {
        lateinit var instance: LogLifeApp
            private set
    }
}
