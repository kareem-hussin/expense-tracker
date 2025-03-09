import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import com.example.expense_tracker.DatabaseHelper
import com.example.expense_tracker.ExpenseTracker
import com.example.expense_tracker.R
import com.example.expense_tracker.Transaction

// Assuming TransactionHeader is a data class with only a title property
data class TransactionHeader(val title: String, val totalBalance: Double)


class TransactionAdapter(private val context: Context, private val items: List<Any>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionHeader -> VIEW_TYPE_HEADER
            is Transaction -> VIEW_TYPE_TRANSACTION
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_TRANSACTION -> {
                val view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false)
                TransactionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val header = items[position] as TransactionHeader
                holder.bind(header)
            }
            is TransactionViewHolder -> {
                val transaction = items[position] as Transaction
                holder.bind(transaction)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTitle: TextView = itemView.findViewById(R.id.header_title)
        private val headerBalance: TextView = itemView.findViewById(R.id.header_balance)

        fun bind(header: TransactionHeader) {
            headerTitle.text = header.title
            headerBalance.text = "P${header.totalBalance}"
        }
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valueText: TextView = itemView.findViewById(R.id.transaction_value)
        private val categoryText: TextView = itemView.findViewById(R.id.transaction_category)
        private val dateText: TextView = itemView.findViewById(R.id.transaction_date)
        private val typeText: TextView = itemView.findViewById(R.id.transaction_type)
        private val iconImage: ImageView = itemView.findViewById(R.id.transaction_icon)
        private val descriptionText: TextView = itemView.findViewById(R.id.transaction_description)
        private val deleteButton: Button = itemView.findViewById(R.id.delete_button)

        fun bind(transaction: Transaction) {
            // Set the transaction details
            valueText.text = "P${transaction.value}"
            categoryText.text = transaction.category
            dateText.text = transaction.date
            typeText.text = transaction.type

            // Check if the description is empty and set visibility/appearance accordingly
            if (transaction.description.isNullOrEmpty()) {
                descriptionText.visibility = View.GONE
                valueText.textSize = 20f
                valueText.setTextColor(Color.BLACK)
            } else {
                descriptionText.visibility = View.VISIBLE
                descriptionText.text = transaction.description
                valueText.textSize = 18f
            }

            // Set the appropriate icon based on the type (Expense or Income)
            if (transaction.type == "Expense") {
                iconImage.setImageResource(R.drawable.minus_peso)
            } else {
                iconImage.setImageResource(R.drawable.plus_peso)
            }

            // Delete button click listener with confirmation dialog
            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(transaction)
            }
        }

        private fun showDeleteConfirmationDialog(transaction: Transaction) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete Transaction")
            builder.setMessage("Are you sure you want to delete this transaction?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                deleteTransaction(transaction)
                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }

        private fun deleteTransaction(transaction: Transaction) {
            val db = DatabaseHelper(context).writableDatabase
            db.delete(
                DatabaseHelper.TABLE_NAME,
                "${DatabaseHelper.COLUMN_ID} = ?",
                arrayOf(transaction.id.toString())
            )

            // Remove the transaction from the list and notify the adapter
            (items as MutableList).remove(transaction) // cast to MutableList to remove items
            notifyItemRemoved(adapterPosition)

            // If the list is empty, show empty state in the activity
            if (items.filterIsInstance<Transaction>().isEmpty()) {
                (context as? ExpenseTracker)?.showEmptyState()
            } else {
                // Update the totals after deletion
                // Filter out only Transaction items and recalculate totals
                (context as? ExpenseTracker)?.displayTotals(items.filterIsInstance<Transaction>())
            }
        }
    }
}
