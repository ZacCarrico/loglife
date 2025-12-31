package com.loglife.app.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.DocsScopes
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Google OAuth authentication and API client creation
 */
class GoogleAuthManager(private val context: Context) {
    
    private val googleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_READONLY),
                Scope(DocsScopes.DOCUMENTS)
            )
            .build()
    }
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }
    
    val signInIntent: Intent
        get() = googleSignInClient.signInIntent
    
    val isSignedIn: Boolean
        get() = GoogleSignIn.getLastSignedInAccount(context) != null
    
    val currentAccount: GoogleSignInAccount?
        get() = GoogleSignIn.getLastSignedInAccount(context)
    
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            googleSignInClient.signOut().await()
        }
    }
    
    suspend fun revokeAccess() {
        withContext(Dispatchers.IO) {
            googleSignInClient.revokeAccess().await()
        }
    }
    
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.result
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Creates a Google Drive API service
     */
    fun getDriveService(): Drive? {
        val account = currentAccount ?: return null
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_READONLY)
        ).apply {
            selectedAccount = account.account
        }
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("LogLife")
            .build()
    }
    
    /**
     * Creates a Google Docs API service
     */
    fun getDocsService(): Docs? {
        val account = currentAccount ?: return null
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DocsScopes.DOCUMENTS)
        ).apply {
            selectedAccount = account.account
        }
        
        return Docs.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("LogLife")
            .build()
    }
    
    companion object {
        const val RC_SIGN_IN = 9001
        
        @Volatile
        private var INSTANCE: GoogleAuthManager? = null
        
        fun getInstance(context: Context): GoogleAuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = GoogleAuthManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
