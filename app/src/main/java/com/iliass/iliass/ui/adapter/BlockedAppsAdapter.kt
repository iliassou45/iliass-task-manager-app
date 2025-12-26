package com.iliass.iliass.ui.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.BlockedApp
import com.iliass.iliass.R

class BlockedAppsAdapter(
    private val apps: List<BlockedApp>,
    private val onEdit: (BlockedApp, Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onToggle: (BlockedApp, Int, Boolean) -> Unit
) : RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appName)
        val blockStatus: TextView = view.findViewById(R.id.blockStatus)
        val remainingTime: TextView = view.findViewById(R.id.remainingTime)
        val toggleSwitch: Switch = view.findViewById(R.id.toggleSwitch)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        holder.appName.text = app.appName
        holder.remainingTime.text = app.getRemainingTime()

        // Set status text and color
        when {
            !app.isActive -> {
                holder.blockStatus.text = "Paused"
                holder.blockStatus.setTextColor(0xFF757575.toInt())
            }
            app.isExpired() -> {
                holder.blockStatus.text = "Expired"
                holder.blockStatus.setTextColor(0xFF757575.toInt())
            }
            else -> {
                holder.blockStatus.text = "Active"
                holder.blockStatus.setTextColor(0xFFE53935.toInt())
            }
        }

        // Set toggle switch
        holder.toggleSwitch.setOnCheckedChangeListener(null)
        holder.toggleSwitch.isChecked = app.isActive
        holder.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(app, position, isChecked)
        }

        // Set button listeners
        holder.editButton.setOnClickListener {
            onEdit(app, position)
        }

        holder.deleteButton.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount() = apps.size
}