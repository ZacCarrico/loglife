package com.loglife.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.loglife.app.data.PreferencesManager
import com.loglife.app.data.ThemeMode

/**
 * Simplified MVP version of LogLifeApp
 * Removed: WorkManager, Room database, Whisper, notifications
 * MVP only needs: Basic app initialization
 */
class LogLifeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Apply saved theme preference (defaults to dark mode)
        applyThemePreference()

        // MVP: Minimal initialization
        // Future: Database, WorkManager, Whisper, notifications
    }

    private fun applyThemePreference() {
        val prefsManager = PreferencesManager.getInstance(this)
        val themeMode = prefsManager.themeMode

        val mode = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    companion object {
        lateinit var instance: LogLifeApp
            private set
    }
}
