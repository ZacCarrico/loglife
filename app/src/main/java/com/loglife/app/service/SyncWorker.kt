package com.loglife.app.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.loglife.app.LogLifeApp
import com.loglife.app.util.GoogleDocsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for syncing pending notes to Google Docs
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val database = LogLifeApp.instance.database
    private val docsManager = GoogleDocsManager.getInstance(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting sync worker")
        
        try {
            val pendingNotes = database.pendingNoteDao().getAll()
            
            if (pendingNotes.isEmpty()) {
                Log.i(TAG, "No pending notes to sync")
                return@withContext Result.success()
            }
            
            Log.i(TAG, "Syncing ${pendingNotes.size} pending notes")
            
            var successCount = 0
            var failCount = 0
            
            // Process notes in chronological order (oldest first)
            for (note in pendingNotes.sortedBy { it.timestamp }) {
                try {
                    val noteText = "${note.formattedTime}\n${note.transcription}"
                    val result = docsManager.prependToDocument(note.targetDocId, noteText)
                    
                    result.onSuccess {
                        // Delete successfully synced note
                        database.pendingNoteDao().deleteById(note.id)
                        successCount++
                        Log.i(TAG, "Synced note ${note.id}")
                    }.onFailure { e ->
                        // Increment retry count
                        database.pendingNoteDao().incrementRetry(
                            note.id,
                            e.message ?: "Unknown error"
                        )
                        failCount++
                        Log.e(TAG, "Failed to sync note ${note.id}", e)
                    }
                } catch (e: Exception) {
                    database.pendingNoteDao().incrementRetry(
                        note.id,
                        e.message ?: "Unknown error"
                    )
                    failCount++
                    Log.e(TAG, "Exception syncing note ${note.id}", e)
                }
            }
            
            Log.i(TAG, "Sync complete: $successCount success, $failCount failed")
            
            if (failCount > 0) {
                // Some notes failed, retry later
                return@withContext Result.retry()
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed", e)
            Result.retry()
        }
    }
    
    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "sync_pending_notes"
        
        /**
         * Enqueue a one-time sync work
         */
        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            
            Log.i(TAG, "Sync work enqueued")
        }
        
        /**
         * Schedule periodic sync
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "${WORK_NAME}_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
        
        /**
         * Cancel all sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("${WORK_NAME}_periodic")
        }
    }
}
