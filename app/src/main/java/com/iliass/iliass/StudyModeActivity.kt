package com.iliass.iliass

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.iliass.iliass.model.FlashCard
import com.iliass.iliass.model.Language
import com.iliass.iliass.util.FlashCardManager

class StudyModeActivity : AppCompatActivity() {

    private lateinit var cardFront: CardView
    private lateinit var cardBack: CardView
    private lateinit var frontText: TextView
    private lateinit var backText: TextView
    private lateinit var flipButton: Button
    private lateinit var correctButton: Button
    private lateinit var incorrectButton: Button
    private lateinit var nextButton: Button
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var progressText: TextView
    private lateinit var manager: FlashCardManager

    private var cards: MutableList<FlashCard> = mutableListOf()
    private var currentIndex = 0
    private var isShowingFront = true
    private var correctCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_mode)

        supportActionBar?.title = "Study Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        manager = FlashCardManager(this)

        setupViews()
        loadCards()
        showCurrentCard()
    }

    private fun setupViews() {
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        frontText = findViewById(R.id.frontText)
        backText = findViewById(R.id.backText)
        flipButton = findViewById(R.id.flipButton)
        correctButton = findViewById(R.id.correctButton)
        incorrectButton = findViewById(R.id.incorrectButton)
        nextButton = findViewById(R.id.nextButton)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        flipButton.setOnClickListener { flipCard() }
        correctButton.setOnClickListener { markCorrect() }
        incorrectButton.setOnClickListener { markIncorrect() }
        nextButton.setOnClickListener { nextCard() }

        // Initially hide answer buttons
        correctButton.visibility = View.GONE
        incorrectButton.visibility = View.GONE
        nextButton.visibility = View.GONE
    }

    private fun loadCards() {
        val languageName = intent.getStringExtra("language")
        val language = if (languageName == Language.FRENCH.name) {
            Language.FRENCH
        } else {
            Language.ENGLISH
        }

        cards = manager.getCardsByLanguage(language).shuffled().toMutableList()
        progressBar.max = cards.size
    }

    private fun showCurrentCard() {
        if (currentIndex >= cards.size) {
            showResults()
            return
        }

        val card = cards[currentIndex]
        frontText.text = card.word
        backText.text = card.meaning

        // Reset to front
        isShowingFront = true
        cardFront.visibility = View.VISIBLE
        cardBack.visibility = View.GONE

        // Update progress
        progressBar.progress = currentIndex + 1
        progressText.text = "${currentIndex + 1} / ${cards.size}"

        // Reset buttons
        flipButton.visibility = View.VISIBLE
        correctButton.visibility = View.GONE
        incorrectButton.visibility = View.GONE
        nextButton.visibility = View.GONE
    }

    private fun flipCard() {
        val card = cards[currentIndex]

        if (isShowingFront) {
            // Flip to back
            cardFront.animate()
                .rotationY(90f)
                .setDuration(150)
                .withEndAction {
                    cardFront.visibility = View.GONE
                    cardBack.visibility = View.VISIBLE
                    cardBack.rotationY = -90f
                    cardBack.animate()
                        .rotationY(0f)
                        .setDuration(150)
                        .start()
                }
                .start()

            isShowingFront = false
            flipButton.visibility = View.GONE
            correctButton.visibility = View.VISIBLE
            incorrectButton.visibility = View.VISIBLE
        }
    }

    private fun markCorrect() {
        val card = cards[currentIndex]
        card.timesReviewed++
        card.correctCount++
        card.lastReviewed = System.currentTimeMillis()
        manager.updateFlashCard(card)

        correctCount++
        showNextButton()
    }

    private fun markIncorrect() {
        val card = cards[currentIndex]
        card.timesReviewed++
        card.lastReviewed = System.currentTimeMillis()
        manager.updateFlashCard(card)

        showNextButton()
    }

    private fun showNextButton() {
        correctButton.visibility = View.GONE
        incorrectButton.visibility = View.GONE
        nextButton.visibility = View.VISIBLE
    }

    private fun nextCard() {
        currentIndex++

        // Reset card rotation
        cardFront.rotationY = 0f
        cardBack.rotationY = 0f

        showCurrentCard()
    }

    private fun showResults() {
        val accuracy = if (cards.size > 0) {
            (correctCount.toFloat() / cards.size.toFloat() * 100).toInt()
        } else 0

        frontText.text = "Study Session Complete!"
        backText.text = """
            Cards Studied: ${cards.size}
            Correct: $correctCount
            Accuracy: $accuracy%
        """.trimIndent()

        cardFront.visibility = View.VISIBLE
        cardBack.visibility = View.GONE

        flipButton.visibility = View.GONE
        correctButton.visibility = View.GONE
        incorrectButton.visibility = View.GONE
        nextButton.text = "Finish"
        nextButton.visibility = View.VISIBLE
        nextButton.setOnClickListener { finish() }
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