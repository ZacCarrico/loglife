package com.loglife.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.loglife.app.data.PreferencesManager
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
        // TODO: Update when activity_settings.xml layout is created
        // For now, just add sign out functionality
        // Sign out button will be added in the layout
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
