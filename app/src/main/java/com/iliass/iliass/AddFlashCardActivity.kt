// File: AddFlashCardActivity.kt (Updated)
package com.iliass.iliass

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iliass.iliass.model.FlashCard
import com.iliass.iliass.model.Language
import com.iliass.iliass.util.FlashCardManager

class AddFlashCardActivity : AppCompatActivity() {

    private lateinit var wordInput: EditText
    private lateinit var meaningInput: EditText
    private lateinit var languageGroup: RadioGroup
    private lateinit var radioEnglish: RadioButton
    private lateinit var radioFrench: RadioButton
    private lateinit var saveButton: Button
    private lateinit var manager: FlashCardManager

    private var editingCardId: String? = null
    private var folderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_flash_card)

        manager = FlashCardManager(this)

        // Get folder_id from intent if coming from FolderDetailActivity
        folderId = intent.getStringExtra("folder_id")

        setupViews()
        loadCardData()
    }

    private fun setupViews() {
        wordInput = findViewById(R.id.wordInput)
        meaningInput = findViewById(R.id.meaningInput)
        languageGroup = findViewById(R.id.languageRadioGroup)
        radioEnglish = findViewById(R.id.radioEnglish)
        radioFrench = findViewById(R.id.radioFrench)
        saveButton = findViewById(R.id.saveButton)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        saveButton.setOnClickListener {
            saveFlashCard()
        }
    }

    private fun loadCardData() {
        val cardId = intent.getStringExtra("card_id")

        if (cardId != null) {
            supportActionBar?.title = "Edit Flashcard"
            editingCardId = cardId

            wordInput.setText(intent.getStringExtra("word"))
            meaningInput.setText(intent.getStringExtra("meaning"))

            val language = intent.getStringExtra("language")
            if (language == Language.FRENCH.name) {
                radioFrench.isChecked = true
            } else {
                radioEnglish.isChecked = true
            }
        } else {
            supportActionBar?.title = "Add Flashcard"
        }
    }

    private fun saveFlashCard() {
        val word = wordInput.text.toString().trim()
        val meaning = meaningInput.text.toString().trim()

        if (word.isEmpty()) {
            wordInput.error = "Word is required"
            return
        }

        if (meaning.isEmpty()) {
            meaningInput.error = "Meaning is required"
            return
        }

        val language = when (languageGroup.checkedRadioButtonId) {
            R.id.radioFrench -> Language.FRENCH
            else -> Language.ENGLISH
        }

        if (editingCardId != null) {
            val existingCard = manager.getFlashCards().find { it.id == editingCardId }
            if (existingCard != null) {
                val updatedCard = existingCard.copy(
                    word = word,
                    meaning = meaning,
                    language = language,
                    folderId = folderId ?: existingCard.folderId
                )
                manager.updateFlashCard(updatedCard)
                Toast.makeText(this, "Card updated!", Toast.LENGTH_SHORT).show()
            }
        } else {
            val card = FlashCard(
                word = word,
                meaning = meaning,
                language = language,
                folderId = folderId
            )
            manager.addFlashCard(card)
            Toast.makeText(this, "Card added!", Toast.LENGTH_SHORT).show()
        }

        finish()
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