package com.loglife.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.loglife.app.R
import com.loglife.app.data.PreferencesManager
import com.loglife.app.databinding.ActivityMainBinding
import com.loglife.app.util.GoogleAuthManager
import com.loglife.app.util.GoogleDocsManager
import com.loglife.app.util.NetworkUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simplified MVP MainActivity
 * Features: Text box + Log button + Google Docs integration
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var authManager: GoogleAuthManager
    private lateinit var docsManager: GoogleDocsManager

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = authManager.handleSignInResult(result.data)
        if (account != null) {
            prefsManager.googleAccountEmail = account.email
            updateUI()
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val docPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        authManager = GoogleAuthManager.getInstance(this)
        docsManager = GoogleDocsManager.getInstance(this)

        setupUI()
        updateUI()
    }

    private fun setupUI() {
        // Log button
        binding.buttonLog.setOnClickListener {
            handleLogClick()
        }

        // Sign in button
        binding.buttonSignIn.setOnClickListener {
            signInLauncher.launch(authManager.signInIntent)
        }

        // Pick document button
        binding.buttonPickDoc.setOnClickListener {
            val intent = Intent(this, DrivePickerActivity::class.java)
            docPickerLauncher.launch(intent)
        }

        // Settings button
        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateUI() {
        val isSignedIn = authManager.isSignedIn
        val hasDoc = prefsManager.selectedDocId != null

        // Show/hide sign-in button
        binding.buttonSignIn.visibility = if (isSignedIn) View.GONE else View.VISIBLE
        binding.layoutMain.visibility = if (isSignedIn) View.VISIBLE else View.GONE

        // Update document info
        if (hasDoc) {
            binding.textDocName.text = prefsManager.selectedDocName ?: "Selected document"
            binding.buttonLog.isEnabled = true
        } else {
            binding.textDocName.text = "No document selected"
            binding.buttonLog.isEnabled = false
        }

        // Update account info
        if (isSignedIn) {
            binding.textAccountInfo.text = "Signed in as ${prefsManager.googleAccountEmail}"
        }
    }

    private fun handleLogClick() {
        val text = binding.editTextLog.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter some text to log", Toast.LENGTH_SHORT).show()
            return
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            return
        }

        val documentId = prefsManager.selectedDocId
        if (documentId == null) {
            Toast.makeText(this, "No document selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.buttonLog.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Create timestamped entry
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())
                val logEntry = "[$timestamp]\n$text\n\n"

                // Prepend to Google Doc
                val result = docsManager.prependToDocument(documentId, logEntry)

                if (result.isSuccess) {
                    // Clear text box and show success
                    binding.editTextLog.text.clear()
                    Toast.makeText(this@MainActivity, "Logged successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error logging: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.buttonLog.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
