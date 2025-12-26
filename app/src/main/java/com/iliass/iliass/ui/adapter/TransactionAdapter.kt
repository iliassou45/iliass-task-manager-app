package com.iliass.iliass.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DebtTransaction
import com.iliass.iliass.model.TransactionType
import com.iliass.iliass.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<DebtTransaction>,
    private val onTransactionLongClick: ((DebtTransaction) -> Unit)? = null
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeText: TextView = view.findViewById(R.id.transactionTypeText)
        val amountText: TextView = view.findViewById(R.id.transactionAmountText)
        val dateText: TextView = view.findViewById(R.id.transactionDateText)
        val noteText: TextView = view.findViewById(R.id.transactionNoteText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Set transaction type
        when (transaction.type) {
            TransactionType.PAYMENT -> {
                holder.typeText.text = "💳 Payment"
                holder.amountText.text = "- ${CurrencyUtils.formatCurrency(transaction.amount)}"
                holder.amountText.setTextColor(0xFF43A047.toInt()) // Green for payments
            }
            TransactionType.ADDITIONAL_LOAN -> {
                holder.typeText.text = "➕ Additional Loan"
                holder.amountText.text = "+ ${CurrencyUtils.formatCurrency(transaction.amount)}"
                holder.amountText.setTextColor(0xFFE53935.toInt()) // Red for additional loans
            }
        }

        // Set date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(transaction.date))

        // Set note if available
        if (transaction.note.isNotEmpty()) {
            holder.noteText.visibility = View.VISIBLE
            holder.noteText.text = "📝 ${transaction.note}"
        } else {
            holder.noteText.visibility = View.GONE
        }

        // Long click to delete
        holder.itemView.setOnLongClickListener {
            onTransactionLongClick?.invoke(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<DebtTransaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}