package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.iliass.iliass.model.Language
import com.iliass.iliass.util.FlashCardManager
import java.text.SimpleDateFormat
import java.util.*

class FlashCardDetailActivity : AppCompatActivity() {

    private lateinit var wordCard: MaterialCardView
    private lateinit var wordText: TextView
    private lateinit var languageText: TextView
    private lateinit var meaningCard: MaterialCardView
    private lateinit var meaningText: TextView
    private lateinit var statsCard: MaterialCardView
    private lateinit var timesReviewedText: TextView
    private lateinit var correctCountText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var createdAtText: TextView
    private lateinit var lastReviewedText: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var manager: FlashCardManager

    private var cardId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash_card_detail)

        supportActionBar?.title = "Card Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        manager = FlashCardManager(this)

        setupViews()
        loadCardData()
    }

    private fun setupViews() {
        wordCard = findViewById(R.id.wordCard)
        wordText = findViewById(R.id.wordText)
        languageText = findViewById(R.id.languageText)
        meaningCard = findViewById(R.id.meaningCard)
        meaningText = findViewById(R.id.meaningText)
        statsCard = findViewById(R.id.statsCard)
        timesReviewedText = findViewById(R.id.timesReviewedText)
        correctCountText = findViewById(R.id.correctCountText)
        accuracyText = findViewById(R.id.accuracyText)
        createdAtText = findViewById(R.id.createdAtText)
        lastReviewedText = findViewById(R.id.lastReviewedText)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)

        editButton.setOnClickListener { editCard() }
        deleteButton.setOnClickListener { showDeleteDialog() }
    }

    private fun loadCardData() {
        cardId = intent.getStringExtra("card_id")

        cardId?.let { id ->
            val card = manager.getFlashCards().find { it.id == id }

            card?.let {
                wordText.text = it.word
                languageText.text = when (it.language) {
                    Language.ENGLISH -> "🇬🇧 English"
                    Language.FRENCH -> "🇫🇷 French"
                }

                meaningText.text = it.meaning

                timesReviewedText.text = it.timesReviewed.toString()
                correctCountText.text = it.correctCount.toString()

                val accuracy = if (it.timesReviewed > 0) {
                    ((it.correctCount.toFloat() / it.timesReviewed.toFloat()) * 100).toInt()
                } else 0
                accuracyText.text = "$accuracy%"

                val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                createdAtText.text = dateFormat.format(Date(it.createdAt))

                lastReviewedText.text = if (it.lastReviewed > 0) {
                    dateFormat.format(Date(it.lastReviewed))
                } else {
                    "Never"
                }
            } ?: run {
                finish()
            }
        } ?: run {
            finish()
        }
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

    private fun editCard() {
        cardId?.let { id ->
            val card = manager.getFlashCards().find { it.id == id }
            card?.let {
                val intent = Intent(this, AddFlashCardActivity::class.java)
                intent.putExtra("card_id", it.id)
                intent.putExtra("word", it.word)
                intent.putExtra("meaning", it.meaning)
                intent.putExtra("language", it.language.name)
                startActivity(intent)
            }
        }
    }

    private fun showDeleteDialog() {
        cardId?.let { id ->
            val card = manager.getFlashCards().find { it.id == id }
            card?.let {
                AlertDialog.Builder(this)
                    .setTitle("Delete Card")
                    .setMessage("Are you sure you want to delete '${it.word}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        manager.deleteFlashCard(id)
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCardData()
    }
}