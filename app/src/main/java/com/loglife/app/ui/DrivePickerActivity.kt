package com.loglife.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.loglife.app.R
import com.loglife.app.databinding.ActivityDrivePickerBinding
import com.loglife.app.util.GoogleDocInfo
import com.loglife.app.util.GoogleDocsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DrivePickerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDrivePickerBinding
    private lateinit var docsManager: GoogleDocsManager
    private lateinit var adapter: DocListAdapter
    
    private var searchJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrivePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        docsManager = GoogleDocsManager.getInstance(this)
        
        setupUI()
        loadDocuments()
    }
    
    private fun setupUI() {
        // Toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // RecyclerView
        adapter = DocListAdapter { doc ->
            selectDocument(doc)
        }
        binding.recyclerDocs.layoutManager = LinearLayoutManager(this)
        binding.recyclerDocs.adapter = adapter
        
        // Search
        binding.editSearch.setOnEditorActionListener { _, _, _ ->
            searchDocuments(binding.editSearch.text.toString())
            true
        }
        
        // Swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadDocuments(binding.editSearch.text.toString().takeIf { it.isNotBlank() })
        }
    }
    
    private fun loadDocuments(query: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        binding.textEmpty.visibility = View.GONE
        binding.textError.visibility = View.GONE
        
        lifecycleScope.launch {
            val result = docsManager.listDocuments(query)
            
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            
            result.onSuccess { docs ->
                if (docs.isEmpty()) {
                    binding.textEmpty.visibility = View.VISIBLE
                    binding.recyclerDocs.visibility = View.GONE
                } else {
                    binding.textEmpty.visibility = View.GONE
                    binding.recyclerDocs.visibility = View.VISIBLE
                    adapter.submitList(docs)
                }
            }.onFailure { e ->
                binding.textError.visibility = View.VISIBLE
                binding.textError.text = "Error: ${e.message}"
                Toast.makeText(
                    this@DrivePickerActivity,
                    "Failed to load documents",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun searchDocuments(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300) // Debounce
            loadDocuments(query.takeIf { it.isNotBlank() })
        }
    }
    
    private fun selectDocument(doc: GoogleDocInfo) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_DOC_ID, doc.id)
            putExtra(EXTRA_DOC_NAME, doc.name)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    companion object {
        const val EXTRA_DOC_ID = "doc_id"
        const val EXTRA_DOC_NAME = "doc_name"
    }
}

// Adapter for the document list
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.loglife.app.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.*

class DocListAdapter(
    private val onDocSelected: (GoogleDocInfo) -> Unit
) : ListAdapter<GoogleDocInfo, DocListAdapter.DocViewHolder>(DocDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DocViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        
        fun bind(doc: GoogleDocInfo) {
            binding.textDocName.text = doc.name
            binding.textModified.text = doc.modifiedTime?.let {
                "Modified ${dateFormat.format(Date(it))}"
            } ?: ""
            
            binding.root.setOnClickListener {
                onDocSelected(doc)
            }
        }
    }
    
    class DocDiffCallback : DiffUtil.ItemCallback<GoogleDocInfo>() {
        override fun areItemsTheSame(oldItem: GoogleDocInfo, newItem: GoogleDocInfo): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: GoogleDocInfo, newItem: GoogleDocInfo): Boolean {
            return oldItem == newItem
        }
    }
}
