package com.example.expense_tracker

import TransactionAdapter
import TransactionHeader
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseTracker : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var noTransactionImage: ImageView
    private lateinit var noTransactionText: TextView
    private lateinit var incomeValueTextView: TextView
    private lateinit var expenseValueTextView: TextView
    private lateinit var totalBalanceTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense_tracker)

        // Initialize dbHelper first
        dbHelper = DatabaseHelper(this)

        // Initialize all UI components
        recyclerView = findViewById(R.id.transactions_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        noTransactionImage = findViewById(R.id.no_transaction_image)
        noTransactionText = findViewById(R.id.no_transaction_text)
        incomeValueTextView = findViewById(R.id.income_value)
        expenseValueTextView = findViewById(R.id.expense_value)
        totalBalanceTextView = findViewById(R.id.totalbalance)

        // Setup frequency selector for popup menu and default to "Daily"
        setupFrequencySelector()

        // Fetch transactions from the database and initialize view state
        val transactions = getTransactionsFromDatabase(sortById = true) // Default to sorting by ID (no frequency)

        if (transactions.isNotEmpty()) {
            // Show the RecyclerView and hide the empty state views
            transactionAdapter = TransactionAdapter(this, transactions)
            recyclerView.adapter = transactionAdapter
            recyclerView.visibility = View.VISIBLE
            noTransactionImage.visibility = View.GONE
            noTransactionText.visibility = View.GONE
        } else {
            // Show the empty state views and hide the RecyclerView
            showEmptyState()
        }

        // Setup buttons for additional functionality
        val addButton = findViewById<ImageView>(R.id.add_button)
        addButton.setOnClickListener {
            val intent = Intent(this, AddExpense::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val reportButton = findViewById<ImageView>(R.id.report_icon)
        reportButton.setOnClickListener {
            val intent = Intent(this, ExpenseReport::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val expenseReport = findViewById<View>(R.id.view3)
        expenseReport.setOnClickListener {
            val intent = Intent(this, AddExpense::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val incomeReport = findViewById<View>(R.id.view4)
        incomeReport.setOnClickListener {
            val intent = Intent(this, AddIncome::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val resetDateButton = findViewById<Button>(R.id.reset_date_button)
        val dateSelector = findViewById<TextView>(R.id.date_selector)
        dateSelector.setOnClickListener {
            showMonthYearPicker(dateSelector, resetDateButton)
            overridePendingTransition(0, 0)
        }


        // Retrieve the saved month and year from SharedPreferences
        val sharedPreferences = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE)
        val savedYear = sharedPreferences.getInt("selectedYear", -1)
        val savedMonth = sharedPreferences.getInt("selectedMonth", -1)


        if (savedYear != -1 && savedMonth != -1) {
            // If a value is saved, display it in the TextView and show the reset button
            val savedDate = "${getMonthName(savedMonth)} $savedYear"
            dateSelector.text = savedDate
            resetDateButton.visibility = View.VISIBLE
            filterTransactionsByMonthYear(savedYear, savedMonth)
        } else {
            // Default text if no value is saved and hide the reset button
            dateSelector.text = "Select Month"
            resetDateButton.visibility = View.GONE
        }

        // Set up the month-year picker
        dateSelector.setOnClickListener {
            showMonthYearPicker(dateSelector, resetDateButton)
        }

        // Reset button listener
        resetDateButton.setOnClickListener {
            with(sharedPreferences.edit()) {
                remove("selectedYear")
                remove("selectedMonth")
                apply()
            }

            // Reset the UI
            dateSelector.text = "Select Month"
            resetDateButton.visibility = View.GONE

            // Reload all transactions without filtering by date
            val transactions = getTransactionsFromDatabase()
            transactionAdapter = TransactionAdapter(this, transactions.toMutableList())
            recyclerView.adapter = transactionAdapter

            // Update totals
            displayTotals(transactions)

            // Show the RecyclerView and hide the empty state
            recyclerView.visibility = View.VISIBLE
            noTransactionImage.visibility = View.GONE
            noTransactionText.visibility = View.GONE
            dateSelector.setOnClickListener {
                showMonthYearPicker(dateSelector, resetDateButton)
            }
        }
    }


    fun showEmptyState() {
        recyclerView.visibility = View.GONE
        noTransactionImage.visibility = View.VISIBLE
        noTransactionText.visibility = View.VISIBLE

        // Clear income, expense, and balance TextViews
        incomeValueTextView.text = "P0.00"
        expenseValueTextView.text = "P0.00"
        totalBalanceTextView.text = "P0.00"
    }

    private fun getMonthName(month: Int): String {
        return DateFormatSymbols().shortMonths[month]
    }

    private fun showMonthYearPicker(dateSelector: TextView, resetDateButton: Button) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, _ ->
                // Format and display the selected month and year
                val selectedDate = "${getMonthName(selectedMonth)} $selectedYear"
                dateSelector.text = selectedDate

                // Save the selected date in SharedPreferences
                val sharedPreferences = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putInt("selectedYear", selectedYear)
                    putInt("selectedMonth", selectedMonth)
                    apply()
                }

                // Make the Reset Date button visible
                resetDateButton.visibility = View.VISIBLE

                filterTransactionsByMonthYear(selectedYear, selectedMonth)
            },
            Calendar.getInstance().get(Calendar.YEAR), // Provide the current year as default
            Calendar.getInstance().get(Calendar.MONTH), // Provide the current month as default
            1 // Default day set to 1
        )

        // Hide the day picker to show only month and year
        datePickerDialog.datePicker.findViewById<View>(
            resources.getIdentifier("android:id/day", null, null)
        )?.visibility = View.GONE

        datePickerDialog.show()
    }




    private fun setupFrequencySelector() {
        val frequencySelector = findViewById<TextView>(R.id.frequency_selector)
        val sharedPreferences = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE)

        frequencySelector.setOnClickListener {
            // Create a popup menu
            val popupMenu = PopupMenu(this, frequencySelector)
            popupMenu.menuInflater.inflate(R.menu.frequency_menu, popupMenu.menu)

            // Set a click listener for menu items
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.daily -> {
                        frequencySelector.text = "  Daily  "
                        groupTransactionsByFrequency("daily")
                        true
                    }
                    R.id.weekly -> {
                        frequencySelector.text = "  Weekly  "
                        groupTransactionsByFrequency("weekly")
                        true
                    }
                    R.id.none -> {
                        frequencySelector.text = "  Sort  "

                        // Get saved month and year from SharedPreferences
                        val selectedYear = sharedPreferences.getInt("selectedYear", -1)
                        val selectedMonth = sharedPreferences.getInt("selectedMonth", -1)

                        if (selectedYear != -1 && selectedMonth != -1) {
                            // Show all transactions for the selected month and year
                            filterTransactionsByMonthYear(selectedYear, selectedMonth)
                        } else {
                            // Show all transactions if no month-year is selected
                            val transactions = getTransactionsFromDatabase(sortById = true)
                            transactionAdapter = TransactionAdapter(this, transactions.toMutableList())
                            recyclerView.adapter = transactionAdapter
                            displayTotals(transactions)
                        }

                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }



    private fun groupTransactionsByFrequency(frequency: String) {
        val allTransactions = getTransactionsFromDatabase(sortById = false)

        // Use "yyyy-MM-dd" format for parsing
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Parse and sort transactions by date in descending order
        val sortedTransactions = allTransactions.sortedByDescending {
            try {
                dateFormat.parse(it.date)
            } catch (e: Exception) {
                Log.e("DateParseError", "Failed to parse date: ${it.date}", e)
                null
            }
        }

        // Group transactions by the desired frequency
        val groupedTransactions: Map<String, Pair<Double, List<Transaction>>> = when (frequency) {
            "daily" -> {
                sortedTransactions.groupBy { transaction ->
                    transaction.date // Group by the exact "yyyy-MM-dd" date
                }.mapValues { (_, transactions) ->
                    val totalBalance = transactions.sumOf { if (it.type == "Income") it.value else -it.value }
                    totalBalance to transactions
                }
            }
            "weekly" -> {
                sortedTransactions.groupBy { transaction ->
                    val transactionDate = dateFormat.parse(transaction.date)
                    getWeekHeader(transactionDate!!)
                }.mapValues { (_, transactions) ->
                    val totalBalance = transactions.sumOf { if (it.type == "Income") it.value else -it.value }
                    totalBalance to transactions
                }
            }
            else -> emptyMap()
        }

        // Flatten the grouped transactions for display
        val flattenedTransactions = groupedTransactions.flatMap { (group, balanceAndTransactions) ->
            val (totalBalance, transactions) = balanceAndTransactions
            listOf(TransactionHeader(group, totalBalance)) + transactions
        }

        transactionAdapter = TransactionAdapter(this, flattenedTransactions)
        recyclerView.adapter = transactionAdapter
    }

    private fun filterTransactionsByMonthYear(selectedYear: Int, selectedMonth: Int) {
        val allTransactions = getTransactionsFromDatabase()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Updated to "yyyy-MM-dd"

        // Filter transactions by selected year and month
        val filteredTransactions = allTransactions.filter { transaction ->
            try {
                val transactionDate = dateFormat.parse(transaction.date)
                val calendar = Calendar.getInstance().apply { time = transactionDate }
                calendar.get(Calendar.YEAR) == selectedYear && calendar.get(Calendar.MONTH) == selectedMonth
            } catch (e: ParseException) {
                Log.e("DateParseError", "Could not parse date: ${transaction.date}", e)
                false
            }
        }

        // Sort the filtered transactions by date in descending order
        val sortedTransactions = filteredTransactions.sortedByDescending {
            dateFormat.parse(it.date) // Sort by "yyyy-MM-dd"
        }

        // Update the RecyclerView or show empty state
        if (sortedTransactions.isEmpty()) {
            showEmptyState()
        } else {
            transactionAdapter = TransactionAdapter(this, sortedTransactions.toMutableList())
            recyclerView.adapter = transactionAdapter
            recyclerView.visibility = View.VISIBLE
            noTransactionImage.visibility = View.GONE
            noTransactionText.visibility = View.GONE
            displayTotals(sortedTransactions)
        }
    }

    // Helper function to get a formatted header for weekly grouping
    private fun getWeekHeader(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time
        return "Week of ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startOfWeek)}"
    }

    // Method to display the total income, expense, and balance
     fun displayTotals(transactions: List<Transaction>) {
        val totalIncome = transactions
            .filter { it.type == "Income" }
            .sumOf { it.value }

        val totalExpense = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.value }

        val totalBalance = totalIncome - totalExpense // Calculate balance as income - expense

        // Update the TextViews for income, expense, and total balance
        incomeValueTextView.text = "P$totalIncome"
        expenseValueTextView.text = "P$totalExpense"
        totalBalanceTextView.text = "P$totalBalance"

        Log.d("Totals", "Total Income: $totalIncome, Total Expense: $totalExpense, Total Balance: $totalBalance")
    }
    override fun onResume() {
        super.onResume()
        val transactions = getTransactionsFromDatabase()
        displayTotals(transactions)
    }

        // Query the database to get all transactions
        @SuppressLint("Range")
        private fun getTransactionsFromDatabase(sortById: Boolean = false): MutableList<Transaction> {
            val transactions = mutableListOf<Transaction>()
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_NAME}", null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))
                    val value = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_VALUE))
                    val type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE))
                    val category = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY))
                    val date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE))
                    val description = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DESCRIPTION))

                    transactions.add(Transaction(id, value, type, category, date, description))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()

            // Sort transactions by ID in descending order if sortById is true
            if (sortById) {
                transactions.sortByDescending { it.id }
            } else {
                // Otherwise, sort by date in descending order
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                transactions.sortByDescending {
                    try {
                        dateFormat.parse(it.date)
                    } catch (e: ParseException) {
                        Log.e("DateParseError", "Could not parse date: ${it.date}", e)
                        null
                    }
                }
            }

            return transactions
        }
}