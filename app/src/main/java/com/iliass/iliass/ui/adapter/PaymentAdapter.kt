package com.iliass.iliass.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Payment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PaymentAdapter(
    private var payments: List<Payment>,
    private val onPaymentClick: ((Payment) -> Unit)? = null,
    private val onPaymentLongClick: ((Payment) -> Unit)? = null
) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.paymentCard)
        val amountText: TextView = itemView.findViewById(R.id.paymentAmountText)
        val dateText: TextView = itemView.findViewById(R.id.paymentDateText)
        val monthForText: TextView = itemView.findViewById(R.id.monthForText)
        val notesText: TextView = itemView.findViewById(R.id.paymentNotesText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        holder.amountText.text = currencyFormat.format(payment.amount)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(payment.paymentDate))

        if (payment.monthFor.isNotEmpty()) {
            holder.monthForText.text = "For: ${payment.monthFor}"
            holder.monthForText.visibility = View.VISIBLE
        } else {
            holder.monthForText.visibility = View.GONE
        }

        if (payment.notes.isNotEmpty()) {
            holder.notesText.text = payment.notes
            holder.notesText.visibility = View.VISIBLE
        } else {
            holder.notesText.visibility = View.GONE
        }

        holder.cardView.setOnClickListener {
            onPaymentClick?.invoke(payment)
        }

        holder.cardView.setOnLongClickListener {
            onPaymentLongClick?.invoke(payment)
            true
        }
    }

    override fun getItemCount() = payments.size

    fun updatePayments(newPayments: List<Payment>) {
        payments = newPayments
        notifyDataSetChanged()
    }
}
