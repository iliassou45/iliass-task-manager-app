package com.iliass.iliass.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.noteCard)
        val colorStrip: View = itemView.findViewById(R.id.colorStrip)
        val titleText: TextView = itemView.findViewById(R.id.noteTitle)
        val contentText: TextView = itemView.findViewById(R.id.noteContent)
        val dateText: TextView = itemView.findViewById(R.id.noteDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Set the color strip
        holder.colorStrip.setBackgroundColor(note.color)

        holder.titleText.text = note.title

        // Show preview of content
        val preview = if (note.content.length > 100) {
            note.content.substring(0, 100) + "..."
        } else {
            note.content
        }
        holder.contentText.text = preview

        // Format and display date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(note.updatedAt))

        holder.cardView.setOnClickListener {
            onNoteClick(note)
        }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes.sortedByDescending { it.updatedAt }
        notifyDataSetChanged()
    }
}