package com.loglife.app.util

import android.content.Context
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest
import com.google.api.services.docs.v1.model.InsertTextRequest
import com.google.api.services.docs.v1.model.Location
import com.google.api.services.docs.v1.model.Request
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a Google Doc for display
 */
data class GoogleDocInfo(
    val id: String,
    val name: String,
    val modifiedTime: Long?
)

/**
 * Manages Google Docs operations
 */
class GoogleDocsManager(private val context: Context) {
    
    private val authManager = GoogleAuthManager.getInstance(context)
    
    /**
     * Lists Google Docs from user's Drive
     */
    suspend fun listDocuments(query: String? = null): Result<List<GoogleDocInfo>> = withContext(Dispatchers.IO) {
        try {
            val driveService = authManager.getDriveService()
                ?: return@withContext Result.failure(Exception("Not signed in"))
            
            val searchQuery = buildString {
                append("mimeType='application/vnd.google-apps.document'")
                append(" and trashed=false")
                if (!query.isNullOrBlank()) {
                    append(" and name contains '${query.replace("'", "\\'")}'")
                }
            }
            
            val result = driveService.files().list()
                .setQ(searchQuery)
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime)")
                .setPageSize(50)
                .setOrderBy("modifiedTime desc")
                .execute()
            
            val docs = result.files?.map { file ->
                GoogleDocInfo(
                    id = file.id,
                    name = file.name,
                    modifiedTime = file.modifiedTime?.value
                )
            } ?: emptyList()
            
            Result.success(docs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Prepends text to the beginning of a document
     */
    suspend fun prependToDocument(documentId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docsService = authManager.getDocsService()
                ?: return@withContext Result.failure(Exception("Not signed in"))
            
            // Use text as-is, already formatted by caller
            val formattedText = text
            
            // Insert at index 1 (beginning of document body, after the implicit start)
            val request = Request().apply {
                insertText = InsertTextRequest().apply {
                    this.text = formattedText
                    location = Location().apply {
                        index = 1
                    }
                }
            }
            
            val batchUpdate = BatchUpdateDocumentRequest().apply {
                requests = listOf(request)
            }
            
            docsService.documents()
                .batchUpdate(documentId, batchUpdate)
                .execute()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Formats timestamp for prepending to transcription
     * Format: 2025-12-22, Mon, 16:00
     */
    fun formatTimestamp(timestamp: Long = System.currentTimeMillis()): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd, EEE, HH:mm", Locale.US)
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Creates the full note text with timestamp and transcription
     */
    fun createNoteText(transcription: String, timestamp: Long = System.currentTimeMillis()): String {
        val formattedTime = formatTimestamp(timestamp)
        return "$formattedTime\n$transcription"
    }
    
    companion object {
        @Volatile
        private var INSTANCE: GoogleDocsManager? = null
        
        fun getInstance(context: Context): GoogleDocsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = GoogleDocsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
