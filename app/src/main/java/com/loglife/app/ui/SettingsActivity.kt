package com.loglife.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.loglife.app.R
import com.loglife.app.LogLifeApp
import com.loglife.app.data.PreferencesManager
import com.loglife.app.data.WhisperModel
import com.loglife.app.databinding.ActivitySettingsBinding
import com.loglife.app.service.VolumeButtonAccessibilityService
import com.loglife.app.util.GoogleAuthManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var authManager: GoogleAuthManager
    
    private val docPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val docId = data.getStringExtra(DrivePickerActivity.EXTRA_DOC_ID)
                val docName = data.getStringExtra(DrivePickerActivity.EXTRA_DOC_NAME)
                if (docId != null && docName != null) {
                    prefsManager.selectedDocId = docId
                    prefsManager.selectedDocName = docName
                    updateUI()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager.getInstance(this)
        authManager = GoogleAuthManager.getInstance(this)
        
        setupUI()
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
    
    private fun setupUI() {
        // Toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Account section
        binding.cardAccount.setOnClickListener {
            if (authManager.isSignedIn) {
                showSignOutDialog()
            }
        }
        
        // Document section
        binding.cardDocument.setOnClickListener {
            if (authManager.isSignedIn) {
                val intent = Intent(this, DrivePickerActivity::class.java)
                docPickerLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Model section
        binding.cardModel.setOnClickListener {
            showModelSelectionDialog()
        }
        
        // Accessibility section
        binding.cardAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        // Pending sync section
        binding.cardPendingSync.setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }
    }
    
    private fun updateUI() {
        // Account
        val email = prefsManager.googleAccountEmail
        binding.textAccountEmail.text = email ?: "Not signed in"
        binding.textAccountAction.text = if (email != null) "Tap to sign out" else ""
        
        // Document
        val docName = prefsManager.selectedDocName
        binding.textDocName.text = docName ?: "No document selected"
        binding.textDocAction.text = if (docName != null) "Tap to change" else "Tap to select"
        
        // Model
        val model = prefsManager.whisperModel
        val whisperManager = LogLifeApp.instance.whisperManager
        val isDownloaded = whisperManager.isModelDownloaded()
        binding.textModelName.text = model.displayName
        binding.textModelStatus.text = if (isDownloaded) "Downloaded" else "Not downloaded"
        
        // Accessibility
        val isEnabled = isAccessibilityServiceEnabled()
        binding.textAccessibilityStatus.text = if (isEnabled) "Enabled" else "Disabled"
        binding.textAccessibilityAction.text = if (isEnabled) "" else "Tap to enable"
        
        // Pending sync
        lifecycleScope.launch {
            val count = LogLifeApp.instance.database.pendingNoteDao().getCount()
            binding.textPendingCount.text = "$count pending"
            binding.cardPendingSync.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }
    
    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_sign_out_title)
            .setMessage(R.string.dialog_sign_out_message)
            .setPositiveButton(R.string.btn_sign_out) { _, _ ->
                lifecycleScope.launch {
                    authManager.signOut()
                    prefsManager.googleAccountEmail = null
                    updateUI()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    private fun showModelSelectionDialog() {
        val models = WhisperModel.values()
        val modelNames = models.map { it.displayName }.toTypedArray()
        val currentIndex = models.indexOf(prefsManager.whisperModel)
        
        AlertDialog.Builder(this)
            .setTitle("Select Whisper Model")
            .setSingleChoiceItems(modelNames, currentIndex) { dialog, which ->
                val selectedModel = models[which]
                prefsManager.whisperModel = selectedModel
                dialog.dismiss()
                updateUI()
                
                // Check if model needs download
                if (!LogLifeApp.instance.whisperManager.isModelDownloaded()) {
                    showDownloadModelDialog(selectedModel)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    private fun showDownloadModelDialog(model: WhisperModel) {
        val sizeStr = when {
            model.sizeBytes >= 1_000_000_000 -> "${model.sizeBytes / 1_000_000_000}GB"
            model.sizeBytes >= 1_000_000 -> "${model.sizeBytes / 1_000_000}MB"
            else -> "${model.sizeBytes / 1_000}KB"
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_model_download_title)
            .setMessage(getString(R.string.dialog_model_download_message, model.displayName, sizeStr))
            .setPositiveButton(R.string.dialog_download) { _, _ ->
                downloadModel()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    private fun downloadModel() {
        val whisperManager = LogLifeApp.instance.whisperManager
        
        binding.progressModel.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = whisperManager.downloadModel { progress ->
                binding.progressModel.progress = (progress * 100).toInt()
            }
            
            binding.progressModel.visibility = View.GONE
            
            result.onSuccess {
                Toast.makeText(this@SettingsActivity, "Model downloaded!", Toast.LENGTH_SHORT).show()
                updateUI()
            }.onFailure { e ->
                Toast.makeText(
                    this@SettingsActivity,
                    "Download failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "${packageName}/${VolumeButtonAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Find 'LogLife' and enable it",
            Toast.LENGTH_LONG
        ).show()
    }
}
