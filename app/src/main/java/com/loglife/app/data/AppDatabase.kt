package com.loglife.app.data

import android.content.Context
import androidx.room.*

/**
 * Entity representing a queued transcription that hasn't been synced yet
 */
@Entity(tableName = "pending_notes")
data class PendingNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "formatted_time")
    val formattedTime: String,
    
    @ColumnInfo(name = "transcription")
    val transcription: String,
    
    @ColumnInfo(name = "target_doc_id")
    val targetDocId: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "last_error")
    val lastError: String? = null
)

/**
 * DAO for pending notes
 */
@Dao
interface PendingNoteDao {
    @Query("SELECT * FROM pending_notes ORDER BY timestamp DESC")
    suspend fun getAll(): List<PendingNote>
    
    @Query("SELECT * FROM pending_notes WHERE target_doc_id = :docId ORDER BY timestamp ASC")
    suspend fun getByDocId(docId: String): List<PendingNote>
    
    @Query("SELECT COUNT(*) FROM pending_notes")
    suspend fun getCount(): Int
    
    @Insert
    suspend fun insert(note: PendingNote): Long
    
    @Update
    suspend fun update(note: PendingNote)
    
    @Delete
    suspend fun delete(note: PendingNote)
    
    @Query("DELETE FROM pending_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM pending_notes")
    suspend fun deleteAll()
    
    @Query("UPDATE pending_notes SET retry_count = retry_count + 1, last_error = :error WHERE id = :id")
    suspend fun incrementRetry(id: Long, error: String)
}

/**
 * Room Database
 */
@Database(
    entities = [PendingNote::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingNoteDao(): PendingNoteDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voicetodoc_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
