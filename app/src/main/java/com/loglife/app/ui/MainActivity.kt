package com.loglife.app.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.loglife.app.R
import com.loglife.app.LogLifeApp
import com.loglife.app.data.PreferencesManager
import com.loglife.app.databinding.ActivityMainBinding
import com.loglife.app.service.RecordingService
import com.loglife.app.service.SyncWorker
import com.loglife.app.service.VolumeButtonAccessibilityService
import com.loglife.app.util.GoogleAuthManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), VolumeButtonAccessibilityService.ServiceStatusListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var authManager: GoogleAuthManager
    
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = authManager.handleSignInResult(result.data)
        if (account != null) {
            prefsManager.googleAccountEmail = account.email
            updateUI()
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.error_google_sign_in, Toast.LENGTH_SHORT).show()
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            updateUI()
        } else {
            Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_SHORT).show()
        }
    }
    
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
    
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra(RecordingService.EXTRA_STATUS)
            updateStatus(status)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager.getInstance(this)
        authManager = GoogleAuthManager.getInstance(this)
        
        setupUI()
        requestPermissions()
        
        VolumeButtonAccessibilityService.addServiceStatusListener(this)
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
        
        val filter = IntentFilter(RecordingService.ACTION_STATUS_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        VolumeButtonAccessibilityService.removeServiceStatusListener(this)
    }
    
    private fun setupUI() {
        // Sign in button
        binding.btnSignIn.setOnClickListener {
            if (authManager.isSignedIn) {
                // Sign out
                lifecycleScope.launch {
                    authManager.signOut()
                    prefsManager.googleAccountEmail = null
                    updateUI()
                }
            } else {
                signInLauncher.launch(authManager.signInIntent)
            }
        }
        
        // Select document button
        binding.btnSelectDoc.setOnClickListener {
            if (!authManager.isSignedIn) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, DrivePickerActivity::class.java)
            docPickerLauncher.launch(intent)
        }
        
        // Enable accessibility button
        binding.btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        // Download model button
        binding.btnDownloadModel.setOnClickListener {
            downloadWhisperModel()
        }
        
        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // View queue button
        binding.btnViewQueue.setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun updateUI() {
        // Account status
        val isSignedIn = authManager.isSignedIn
        val email = prefsManager.googleAccountEmail
        
        binding.textAccountStatus.text = if (isSignedIn && email != null) {
            email
        } else {
            "Not signed in"
        }
        binding.btnSignIn.text = if (isSignedIn) {
            getString(R.string.btn_sign_out)
        } else {
            getString(R.string.btn_sign_in)
        }
        binding.checkSignIn.visibility = if (isSignedIn) View.VISIBLE else View.GONE
        
        // Document status
        val docName = prefsManager.selectedDocName
        binding.textDocStatus.text = docName ?: "No document selected"
        binding.btnSelectDoc.text = if (docName != null) {
            getString(R.string.btn_change_doc)
        } else {
            getString(R.string.btn_select_doc)
        }
        binding.checkDoc.visibility = if (docName != null) View.VISIBLE else View.GONE
        
        // Accessibility service status
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        binding.textAccessibilityStatus.text = if (isAccessibilityEnabled) {
            "Enabled"
        } else {
            "Not enabled"
        }
        binding.btnAccessibility.visibility = if (isAccessibilityEnabled) View.GONE else View.VISIBLE
        binding.checkAccessibility.visibility = if (isAccessibilityEnabled) View.VISIBLE else View.GONE
        
        // Model status
        val whisperManager = LogLifeApp.instance.whisperManager
        val isModelDownloaded = whisperManager.isModelDownloaded()
        binding.textModelStatus.text = if (isModelDownloaded) {
            "${prefsManager.whisperModel.displayName} - Downloaded"
        } else {
            "${prefsManager.whisperModel.displayName} - Not downloaded"
        }
        binding.btnDownloadModel.visibility = if (isModelDownloaded) View.GONE else View.VISIBLE
        binding.checkModel.visibility = if (isModelDownloaded) View.VISIBLE else View.GONE
        
        // Overall status
        val isReady = isSignedIn && docName != null && isAccessibilityEnabled && isModelDownloaded
        binding.statusIndicator.setBackgroundResource(
            if (isReady) R.drawable.status_ready else R.drawable.status_not_ready
        )
        binding.textStatus.text = when {
            !isSignedIn -> "Sign in with Google to continue"
            docName == null -> "Select a Google Doc"
            !isAccessibilityEnabled -> "Enable accessibility service"
            !isModelDownloaded -> "Download Whisper model"
            else -> getString(R.string.status_ready)
        }
        
        // Queue count
        lifecycleScope.launch {
            val count = LogLifeApp.instance.database.pendingNoteDao().getCount()
            binding.btnViewQueue.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.btnViewQueue.text = getString(R.string.btn_view_queue, count)
        }
    }
    
    private fun updateStatus(status: String?) {
        binding.textStatus.text = when (status) {
            RecordingService.STATUS_RECORDING -> getString(R.string.status_recording)
            RecordingService.STATUS_TRANSCRIBING -> getString(R.string.status_transcribing)
            RecordingService.STATUS_SYNCING -> getString(R.string.status_syncing)
            RecordingService.STATUS_DONE -> getString(R.string.status_done)
            RecordingService.STATUS_OFFLINE -> getString(R.string.status_offline)
            else -> getString(R.string.status_ready)
        }
        
        binding.statusIndicator.setBackgroundResource(
            when (status) {
                RecordingService.STATUS_RECORDING -> R.drawable.status_recording
                RecordingService.STATUS_DONE -> R.drawable.status_ready
                RecordingService.STATUS_ERROR -> R.drawable.status_not_ready
                else -> R.drawable.status_ready
            }
        )
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
    
    private fun downloadWhisperModel() {
        val whisperManager = LogLifeApp.instance.whisperManager
        
        binding.btnDownloadModel.isEnabled = false
        binding.btnDownloadModel.text = "Downloading..."
        binding.progressModel.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = whisperManager.downloadModel { progress ->
                binding.progressModel.progress = (progress * 100).toInt()
            }
            
            result.onSuccess {
                Toast.makeText(this@MainActivity, "Model downloaded!", Toast.LENGTH_SHORT).show()
                updateUI()
            }.onFailure { e ->
                Toast.makeText(
                    this@MainActivity,
                    "Download failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            binding.btnDownloadModel.isEnabled = true
            binding.btnDownloadModel.text = getString(R.string.btn_download_model)
            binding.progressModel.visibility = View.GONE
        }
    }
    
    override fun onServiceStatusChanged(isRunning: Boolean) {
        runOnUiThread {
            updateUI()
        }
    }
}
