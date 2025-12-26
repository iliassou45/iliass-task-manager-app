package com.iliass.iliass.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iliass.iliass.R
import com.iliass.iliass.model.Phone
import com.iliass.iliass.util.TimeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhoneAdapter(
    private var phones: List<Phone>,
    private val onPhoneClick: (Phone) -> Unit
) : RecyclerView.Adapter<PhoneAdapter.PhoneViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    inner class PhoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.phoneCard)
        val phoneImage: ImageView = itemView.findViewById(R.id.phoneImage)
        val phoneName: TextView = itemView.findViewById(R.id.phoneName)
        val phoneImei: TextView = itemView.findViewById(R.id.phoneImei)
        val shopName: TextView = itemView.findViewById(R.id.shopName)
        val dateRegistered: TextView = itemView.findViewById(R.id.dateRegistered)
        val relativeTime: TextView = itemView.findViewById(R.id.relativeTime)
        val warrantyStatus: TextView = itemView.findViewById(R.id.warrantyStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_phone, parent, false)
        return PhoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhoneViewHolder, position: Int) {
        val phone = phones[position]

        holder.phoneName.text = phone.name
        holder.phoneImei.text = "IMEI: ${phone.imei}"
        holder.shopName.text = "🪧 ${phone.shopName}"

        // Show both relative time and exact date
        val relativeTimeStr = TimeUtils.getRelativeTimeString(phone.dateRegistered)
        holder.relativeTime.text = "⏱️ $relativeTimeStr"
        holder.dateRegistered.text = "📅 ${dateFormat.format(Date(phone.dateRegistered))}"

        // Show warranty status
        val warrantyStatus = TimeUtils.getWarrantyStatus(phone.dateRegistered, 12)
        if (warrantyStatus.isActive) {
            holder.warrantyStatus.text = "✅ ${warrantyStatus.message}"
            holder.warrantyStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        } else {
            holder.warrantyStatus.text = "⚠️ ${warrantyStatus.message}"
            holder.warrantyStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
        }

        // Load image if exists
        phone.imagePath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                holder.phoneImage.setImageBitmap(bitmap)
            } else {
                holder.phoneImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } ?: run {
            holder.phoneImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.cardView.setOnClickListener {
            onPhoneClick(phone)
        }
    }

    override fun getItemCount() = phones.size

    fun updatePhones(newPhones: List<Phone>) {
        phones = newPhones
        notifyDataSetChanged()
    }
}