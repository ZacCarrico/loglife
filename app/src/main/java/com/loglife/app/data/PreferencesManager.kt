package com.loglife.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages app preferences with secure storage for sensitive data
 */
class PreferencesManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "voicetodoc_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val regularPrefs: SharedPreferences = context.getSharedPreferences(
        "voicetodoc_prefs",
        Context.MODE_PRIVATE
    )
    
    // Google account email (not sensitive, just for display)
    var googleAccountEmail: String?
        get() = regularPrefs.getString(KEY_GOOGLE_EMAIL, null)
        set(value) = regularPrefs.edit().putString(KEY_GOOGLE_EMAIL, value).apply()
    
    // Selected document ID
    var selectedDocId: String?
        get() = regularPrefs.getString(KEY_SELECTED_DOC_ID, null)
        set(value) = regularPrefs.edit().putString(KEY_SELECTED_DOC_ID, value).apply()
    
    // Selected document name (for display)
    var selectedDocName: String?
        get() = regularPrefs.getString(KEY_SELECTED_DOC_NAME, null)
        set(value) = regularPrefs.edit().putString(KEY_SELECTED_DOC_NAME, value).apply()
    
    // Whisper model selection
    var whisperModel: WhisperModel
        get() = WhisperModel.fromString(regularPrefs.getString(KEY_WHISPER_MODEL, WhisperModel.BASE.name))
        set(value) = regularPrefs.edit().putString(KEY_WHISPER_MODEL, value.name).apply()
    
    // Model downloaded status
    fun isModelDownloaded(model: WhisperModel): Boolean {
        return regularPrefs.getBoolean("${KEY_MODEL_DOWNLOADED}_${model.name}", false)
    }
    
    fun setModelDownloaded(model: WhisperModel, downloaded: Boolean) {
        regularPrefs.edit().putBoolean("${KEY_MODEL_DOWNLOADED}_${model.name}", downloaded).apply()
    }
    
    // First launch flag
    var isFirstLaunch: Boolean
        get() = regularPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = regularPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    
    // Accessibility service enabled reminder dismissed
    var accessibilityReminderDismissed: Boolean
        get() = regularPrefs.getBoolean(KEY_ACCESSIBILITY_DISMISSED, false)
        set(value) = regularPrefs.edit().putBoolean(KEY_ACCESSIBILITY_DISMISSED, value).apply()

    // Theme preference (dark mode is default)
    var themeMode: ThemeMode
        get() = ThemeMode.fromString(regularPrefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name))
        set(value) = regularPrefs.edit().putString(KEY_THEME_MODE, value.name).apply()
    
    fun clearAll() {
        securePrefs.edit().clear().apply()
        regularPrefs.edit().clear().apply()
    }
    
    companion object {
        private const val KEY_GOOGLE_EMAIL = "google_email"
        private const val KEY_SELECTED_DOC_ID = "selected_doc_id"
        private const val KEY_SELECTED_DOC_NAME = "selected_doc_name"
        private const val KEY_WHISPER_MODEL = "whisper_model"
        private const val KEY_MODEL_DOWNLOADED = "model_downloaded"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ACCESSIBILITY_DISMISSED = "accessibility_dismissed"
        private const val KEY_THEME_MODE = "theme_mode"
        
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

enum class WhisperModel(val displayName: String, val fileName: String, val sizeBytes: Long) {
    TINY("Tiny (~75MB)", "ggml-tiny.bin", 75_000_000L),
    BASE("Base (~150MB)", "ggml-base.bin", 150_000_000L),
    SMALL("Small (~500MB)", "ggml-small.bin", 500_000_000L);

    companion object {
        fun fromString(name: String?): WhisperModel {
            return values().find { it.name == name } ?: BASE
        }
    }
}

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default");

    companion object {
        fun fromString(name: String?): ThemeMode {
            return values().find { it.name == name } ?: DARK
        }
    }
}
