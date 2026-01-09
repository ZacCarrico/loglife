package com.loglife.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.loglife.app.R
import com.loglife.app.data.PreferencesManager
import com.loglife.app.data.ThemeMode
import com.loglife.app.databinding.ActivitySettingsBinding
import com.loglife.app.util.GoogleAuthManager
import kotlinx.coroutines.launch

/**
 * Simplified MVP SettingsActivity
 * Shows basic app information and sign-out option
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var authManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        authManager = GoogleAuthManager.getInstance(this)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }

    private fun setupUI() {
        // Display current theme
        updateThemeDisplay()

        // Theme card click listener
        binding.cardTheme.setOnClickListener {
            showThemeDialog()
        }
    }

    private fun updateThemeDisplay() {
        val currentTheme = prefsManager.themeMode
        binding.textThemeMode.text = currentTheme.displayName
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark),
            getString(R.string.settings_theme_system)
        )

        val currentTheme = prefsManager.themeMode
        val checkedItem = when (currentTheme) {
            ThemeMode.LIGHT -> 0
            ThemeMode.DARK -> 1
            ThemeMode.SYSTEM -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_theme))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val newTheme = when (which) {
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    2 -> ThemeMode.SYSTEM
                    else -> ThemeMode.DARK
                }

                prefsManager.themeMode = newTheme
                applyTheme(newTheme)
                updateThemeDisplay()
                dialog.dismiss()
            }
            .show()
    }

    private fun applyTheme(theme: ThemeMode) {
        val mode = when (theme) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
