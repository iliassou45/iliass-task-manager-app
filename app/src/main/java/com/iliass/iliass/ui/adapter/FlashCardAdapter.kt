// File: FlashCardAdapter.kt (Fixed)
package com.iliass.iliass.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.FlashCard
import com.iliass.iliass.model.Language

class FlashCardAdapter(
    private var cards: MutableList<FlashCard>,
    private val onCardClick: (FlashCard) -> Unit,
    private val onCardLongClick: (FlashCard) -> Unit
) : RecyclerView.Adapter<FlashCardAdapter.FlashCardViewHolder>() {

    inner class FlashCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.flashCardView)
        val wordText: TextView = itemView.findViewById(R.id.wordText)
        val meaningText: TextView = itemView.findViewById(R.id.meaningText)
        val languageText: TextView = itemView.findViewById(R.id.languageText)
        val statsText: TextView = itemView.findViewById(R.id.statsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flashcard, parent, false)
        return FlashCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlashCardViewHolder, position: Int) {
        val card = cards[position]

        holder.wordText.text = card.word

        // Show full meaning without truncation, and handle line breaks properly
        holder.meaningText.text = card.meaning

        holder.languageText.text = when (card.language) {
            Language.ENGLISH -> "🇬🇧 English"
            Language.FRENCH -> "🇫🇷 French"
        }

        val accuracy = if (card.timesReviewed > 0) {
            ((card.correctCount.toFloat() / card.timesReviewed.toFloat()) * 100).toInt()
        } else 0

        holder.statsText.text = "Reviewed: ${card.timesReviewed} times • $accuracy% accuracy"

        holder.cardView.setOnClickListener { onCardClick(card) }
        holder.cardView.setOnLongClickListener {
            onCardLongClick(card)
            true
        }
    }

    override fun getItemCount() = cards.size

    fun updateCards(newCards: List<FlashCard>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged()
    }
}