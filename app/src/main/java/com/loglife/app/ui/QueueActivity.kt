package com.loglife.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.loglife.app.R
import com.loglife.app.LogLifeApp
import com.loglife.app.data.PendingNote
import com.loglife.app.databinding.ActivityQueueBinding
import com.loglife.app.databinding.ItemPendingNoteBinding
import com.loglife.app.service.SyncWorker
import com.loglife.app.util.NetworkUtils
import kotlinx.coroutines.launch

class QueueActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQueueBinding
    private lateinit var adapter: PendingNoteAdapter
    private val database by lazy { LogLifeApp.instance.database }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadPendingNotes()
    }
    
    private fun setupUI() {
        // Toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // RecyclerView
        adapter = PendingNoteAdapter { note ->
            showDeleteNoteDialog(note)
        }
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
        
        // Sync now button
        binding.btnSyncNow.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                SyncWorker.enqueueSync(this)
                Toast.makeText(this, "Sync started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Clear all button
        binding.btnClearAll.setOnClickListener {
            showClearAllDialog()
        }
    }
    
    private fun loadPendingNotes() {
        lifecycleScope.launch {
            val notes = database.pendingNoteDao().getAll()
            
            if (notes.isEmpty()) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerNotes.visibility = View.GONE
                binding.btnSyncNow.visibility = View.GONE
                binding.btnClearAll.visibility = View.GONE
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerNotes.visibility = View.VISIBLE
                binding.btnSyncNow.visibility = View.VISIBLE
                binding.btnClearAll.visibility = View.VISIBLE
                adapter.submitList(notes)
            }
        }
    }
    
    private fun showDeleteNoteDialog(note: PendingNote) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note?")
            .setMessage("This note will be permanently deleted and won't be synced.")
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                deleteNote(note)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    private fun deleteNote(note: PendingNote) {
        lifecycleScope.launch {
            database.pendingNoteDao().delete(note)
            loadPendingNotes()
        }
    }
    
    private fun showClearAllDialog() {
        val count = adapter.itemCount
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_queue_title)
            .setMessage(getString(R.string.dialog_delete_queue_message, count))
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                clearAllNotes()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    private fun clearAllNotes() {
        lifecycleScope.launch {
            database.pendingNoteDao().deleteAll()
            loadPendingNotes()
            Toast.makeText(this@QueueActivity, "All pending notes cleared", Toast.LENGTH_SHORT).show()
        }
    }
}

class PendingNoteAdapter(
    private val onNoteClick: (PendingNote) -> Unit
) : ListAdapter<PendingNote, PendingNoteAdapter.NoteViewHolder>(NoteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemPendingNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class NoteViewHolder(
        private val binding: ItemPendingNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(note: PendingNote) {
            binding.textTimestamp.text = note.formattedTime
            binding.textTranscription.text = note.transcription
            
            if (note.retryCount > 0) {
                binding.textError.visibility = View.VISIBLE
                binding.textError.text = "Retries: ${note.retryCount}"
            } else {
                binding.textError.visibility = View.GONE
            }
            
            binding.root.setOnClickListener {
                onNoteClick(note)
            }
        }
    }
    
    class NoteDiffCallback : DiffUtil.ItemCallback<PendingNote>() {
        override fun areItemsTheSame(oldItem: PendingNote, newItem: PendingNote): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: PendingNote, newItem: PendingNote): Boolean {
            return oldItem == newItem
        }
    }
}
