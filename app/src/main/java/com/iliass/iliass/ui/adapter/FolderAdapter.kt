package com.iliass.iliass.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.FlashCardFolder

class FolderAdapter(
    private var folders: MutableList<FlashCardFolder>,
    private val onFolderClick: (FlashCardFolder) -> Unit,
    private val onFolderLongClick: (FlashCardFolder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.folderCard)
        val folderIcon: ImageView = itemView.findViewById(R.id.folderIcon)
        val folderName: TextView = itemView.findViewById(R.id.folderName)
        val folderDesc: TextView = itemView.findViewById(R.id.folderDesc)
        val cardCount: TextView = itemView.findViewById(R.id.cardCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]

        holder.folderName.text = folder.name
        holder.folderDesc.text = folder.description.ifEmpty { "No description" }
        holder.cardCount.text = "Tap to view cards"

        // Set folder icon background color
        holder.folderIcon.setColorFilter(
            android.graphics.Color.parseColor(folder.color),
            android.graphics.PorterDuff.Mode.SRC_IN
        )

        holder.cardView.setOnClickListener { onFolderClick(folder) }
        holder.cardView.setOnLongClickListener {
            onFolderLongClick(folder)
            true
        }
    }

    override fun getItemCount() = folders.size

    fun updateFolders(newFolders: List<FlashCardFolder>) {
        folders.clear()
        folders.addAll(newFolders)
        notifyDataSetChanged()
    }
}