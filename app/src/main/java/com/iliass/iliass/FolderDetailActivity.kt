// File: FolderDetailActivity.kt
package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iliass.iliass.model.FlashCard
import com.iliass.iliass.ui.adapter.FlashCardAdapter
import com.iliass.iliass.util.FlashCardManager

class FolderDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FlashCardAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var manager: FlashCardManager
    private var folderId: String? = null
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_detail)

        folderId = intent.getStringExtra("folder_id")
        val folderName = intent.getStringExtra("folder_name")

        supportActionBar?.title = folderName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        manager = FlashCardManager(this)
        setupViews()
        loadCards()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.cardsRecyclerView)
        fabAdd = findViewById(R.id.fabAddCard)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FlashCardAdapter(
            cards = mutableListOf(),
            onCardClick = { card -> viewCardDetails(card) },
            onCardLongClick = { card -> showCardOptions(card) }
        )
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddFlashCardActivity::class.java)
            intent.putExtra("folder_id", folderId)
            startActivity(intent)
        }
    }

    private fun loadCards() {
        val cards = if (searchQuery.isBlank()) {
            manager.getCardsByFolder(folderId)
        } else {
            manager.searchCards(searchQuery).filter { it.folderId == folderId }
        }
        adapter.updateCards(cards)
    }

    private fun viewCardDetails(card: FlashCard) {
        val intent = Intent(this, FlashCardDetailActivity::class.java)
        intent.putExtra("card_id", card.id)
        startActivity(intent)
    }

    private fun showCardOptions(card: FlashCard) {
        val options = arrayOf("View Details", "Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(card.word)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewCardDetails(card)
                    1 -> editCard(card)
                    2 -> deleteCard(card)
                }
            }
            .show()
    }

    private fun editCard(card: FlashCard) {
        val intent = Intent(this, AddFlashCardActivity::class.java)
        intent.putExtra("card_id", card.id)
        intent.putExtra("word", card.word)
        intent.putExtra("meaning", card.meaning)
        intent.putExtra("language", card.language.name)
        intent.putExtra("folder_id", folderId)
        startActivity(intent)
    }

    private fun deleteCard(card: FlashCard) {
        AlertDialog.Builder(this)
            .setTitle("Delete Card")
            .setMessage("Are you sure you want to delete '${card.word}'?")
            .setPositiveButton("Delete") { _, _ ->
                manager.deleteFlashCard(card.id)
                loadCards()
                Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadCards()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_flashcards, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                loadCards()
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}