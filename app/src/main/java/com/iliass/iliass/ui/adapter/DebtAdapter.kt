package com.iliass.iliass.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DebtType
import com.iliass.iliass.model.TransactionType
import com.iliass.iliass.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

class DebtAdapter(
    private var debts: List<Debt>,
    private val onDebtClick: (Debt) -> Unit,
    private val onDebtLongClick: ((Debt) -> Unit)? = null
) : RecyclerView.Adapter<DebtAdapter.DebtViewHolder>() {

    inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.debtCard)
        val personNameText: TextView = itemView.findViewById(R.id.personNameText)
        val reasonText: TextView = itemView.findViewById(R.id.reasonText)
        val amountText: TextView = itemView.findViewById(R.id.amountText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        val debt = debts[position]
        val remaining = debt.getRemainingAmount()

        holder.personNameText.text = debt.getDisplayName()
        holder.reasonText.text = debt.reason
        holder.amountText.text = CurrencyUtils.formatCurrency(remaining)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(debt.dateCreated))

        if (debt.transactions.isNotEmpty()) {
            val totalPaid = debt.transactions.filter { it.type == TransactionType.PAYMENT }
                .sumOf { it.amount }
            val totalAmount = debt.initialAmount +
                    debt.transactions.filter { it.type == TransactionType.ADDITIONAL_LOAN }
                        .sumOf { it.amount }
            val percentPaid = (totalPaid / totalAmount) * 100
            holder.statusText.text = "${String.format("%.0f", percentPaid)}% paid"
            holder.statusText.visibility = View.VISIBLE
        } else {
            holder.statusText.visibility = View.GONE
        }

        if (debt.type == DebtType.I_OWE) {
            holder.amountText.setTextColor(0xFFE53935.toInt())
            holder.cardView.setCardBackgroundColor(0xFFFCE4EC.toInt())
        } else {
            holder.amountText.setTextColor(0xFF43A047.toInt())
            holder.cardView.setCardBackgroundColor(0xFFF1F8E9.toInt())
        }

        // Regular click to view details
        holder.cardView.setOnClickListener {
            onDebtClick(debt)
        }

        // Long click to delete
        holder.cardView.setOnLongClickListener {
            onDebtLongClick?.invoke(debt)
            true
        }
    }

    override fun getItemCount() = debts.size

    fun updateDebts(newDebts: List<Debt>) {
        debts = newDebts
        notifyDataSetChanged()
    }
}