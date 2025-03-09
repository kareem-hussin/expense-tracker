package com.example.expense_tracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncome : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    // Example categories and icons (use your actual drawable resources)
    private val categories = listOf(
        "Salary" to R.drawable.salary_icon,
        "Investments" to R.drawable.investments_icon,
        "Allowance" to R.drawable.allowance_icon,
        "Bonus" to R.drawable.bonus_icon,
        "Other" to R.drawable.other_in_icon
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_income)

        // Initialize the DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Switch to AddExpense when Expense tab is clicked
        val expenseTab = findViewById<TextView>(R.id.tab_expense)
        expenseTab.setOnClickListener {
            val intent = Intent(this, AddExpense::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Navigate to other activities
        val reportButton = findViewById<ImageView>(R.id.report_icon)
        reportButton.setOnClickListener {
            val intent = Intent(this, IncomeReport::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val transactionsButton = findViewById<ImageView>(R.id.transactions_icon)
        transactionsButton.setOnClickListener {
            val intent = Intent(this, ExpenseTracker::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Set up Save button
        findViewById<Button>(R.id.save_button).setOnClickListener {
            saveTransaction()
        }
    }

    // Method to save the income transaction to the database
    private fun saveTransaction() {
        val amountEditText = findViewById<EditText>(R.id.amount)
        val categoryTextView = findViewById<TextView>(R.id.category)
        val dateTextView = findViewById<TextView>(R.id.date)
        val descriptionEditText = findViewById<EditText>(R.id.description) // New description field

        // Get values from the UI
        val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val category = categoryTextView.text.toString()
        val date = dateTextView.text.toString()
        val description = descriptionEditText.text.toString() // Get the description

        // Validate inputs
        if (amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (category.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please select a valid category and date.", Toast.LENGTH_SHORT).show()
            return
        }

        // Always set the type to "Income"
        val type = "Income"

        // Insert the data into the database, including the description
        dbHelper.insertTransaction(amount, type, category, date, description)

        // Show a confirmation message or reset the form
        // For now, just clear the fields
        amountEditText.text.clear()
        categoryTextView.text = "Category"
        dateTextView.text = "Select Date of Transaction"
        descriptionEditText.text.clear() // Clear the description field

        // Redirect to ExpenseTracker
        val intent = Intent(this, ExpenseTracker::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    fun openDatePicker(view: View) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the date to "yyyy-MM-dd"
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }

                // Create the format
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                // Update the TextView with the formatted date
                val dateTextView = findViewById<TextView>(R.id.date)
                dateTextView.text = formattedDate
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    // Open category dialog
    fun openCategoryDialog(view: View) {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        // Set up the categories list in the dialog
        val categoryList = dialogView.findViewById<LinearLayout>(R.id.category_list)

        categories.forEach { (categoryName, iconResId) ->
            val categoryView = LayoutInflater.from(this).inflate(R.layout.dialog_category_item, null)
            val categoryText = categoryView.findViewById<TextView>(R.id.category_text)
            val categoryIcon = categoryView.findViewById<ImageView>(R.id.category_icon)

            categoryText.text = categoryName
            categoryIcon.setImageResource(iconResId)

            // Set click listener to select the category
            categoryView.setOnClickListener {
                // Update the TextView in the main layout with the selected category
                val categoryTextView = findViewById<TextView>(R.id.category)
                categoryTextView.text = categoryName

                // Update the ImageView with the selected category icon
                val categoryImageView = findViewById<ImageView>(R.id.category_icon)
                categoryImageView.setImageResource(iconResId)

                // Dismiss the dialog
                dialog.dismiss()
            }

            // Add each category to the dialog's category list view
            categoryList.addView(categoryView)
        }

        dialog.show()
    }
}
