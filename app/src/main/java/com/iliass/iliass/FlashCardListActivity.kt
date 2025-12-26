// File: FlashCardListActivity.kt (Updated)
package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iliass.iliass.model.FlashCardFolder
import com.iliass.iliass.ui.adapter.FolderAdapter
import com.iliass.iliass.util.FlashCardManager

class FlashCardListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FolderAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var manager: FlashCardManager
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash_card_list)

        supportActionBar?.title = "Flashcard Folders"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        manager = FlashCardManager(this)
        setupViews()
        loadFolders()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.foldersRecyclerView)
        fabAdd = findViewById(R.id.fabAddFolder)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FolderAdapter(
            folders = mutableListOf(),
            onFolderClick = { folder -> openFolder(folder) },
            onFolderLongClick = { folder -> showFolderOptions(folder) }
        )
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun loadFolders() {
        val folders = manager.getFolders().toMutableList()
        adapter.updateFolders(folders)
    }

    private fun openFolder(folder: FlashCardFolder) {
        val intent = Intent(this, FolderDetailActivity::class.java)
        intent.putExtra("folder_id", folder.id)
        intent.putExtra("folder_name", folder.name)
        startActivity(intent)
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply {
            hint = "Folder name"
        }

        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    manager.createFolder(name)
                    loadFolders()
                    Toast.makeText(this, "Folder created!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFolderOptions(folder: FlashCardFolder) {
        val options = arrayOf("Open", "Rename", "Delete")
        AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFolder(folder)
                    1 -> showRenameDialog(folder)
                    2 -> showDeleteDialog(folder)
                }
            }
            .show()
    }

    private fun showRenameDialog(folder: FlashCardFolder) {
        val input = EditText(this).apply {
            setText(folder.name)
            selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle("Rename Folder")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    manager.updateFolder(folder.copy(name = newName))
                    loadFolders()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(folder: FlashCardFolder) {
        AlertDialog.Builder(this)
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete '${folder.name}'? Cards will be moved to unsorted.")
            .setPositiveButton("Delete") { _, _ ->
                manager.deleteFolder(folder.id)
                loadFolders()
                Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadFolders()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_flashcards, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_stats -> {
                showStats()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showStats() {
        val stats = manager.getStats()
        val message = """
            Total Cards: ${stats.totalCards}
            English: ${stats.englishCards}
            French: ${stats.frenchCards}
            Total Folders: ${manager.getFolders().size}
            Total Reviews: ${stats.totalReviews}
            Accuracy: ${"%.1f".format(stats.accuracy)}%
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Statistics")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}

