package com.example.expense_tracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class AddExpense : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    // Example categories and icons (use your actual drawable resources)
    private val categories = listOf(
        "Food & Drink" to R.drawable.food_icon,
        "Shopping" to R.drawable.shopping_icon,
        "Transportation" to R.drawable.transport_icon,
        "Housing" to R.drawable.housing_icon,
        "Entertainment" to R.drawable.entertainment_icon,
        "Education" to R.drawable.education_icon,
        "Healthcare" to R.drawable.healthcare_icon,
        "Other" to R.drawable.other_icon
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val incomeTab = findViewById<TextView>(R.id.tab_income)
        incomeTab.setOnClickListener{
            val intent = Intent(this, AddIncome::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val reportButton = findViewById<ImageView>(R.id.report_icon)
        reportButton.setOnClickListener {
            val intent = Intent(this, ExpenseReport::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        val transactionsButton = findViewById<ImageView>(R.id.transactions_icon)
        transactionsButton.setOnClickListener {
            val intent = Intent(this, ExpenseTracker::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Initialize the database helper
        dbHelper = DatabaseHelper(this)

        // Save Button Click Listener
        findViewById<Button>(R.id.save_button).setOnClickListener {
            saveTransaction()
        }
    }

    // Method to save the transaction to the database
    private fun saveTransaction() {
        val amountEditText = findViewById<EditText>(R.id.amount)
        val categoryTextView = findViewById<TextView>(R.id.category)
        val dateTextView = findViewById<TextView>(R.id.date)
        val descriptionEditText = findViewById<EditText>(R.id.description)

        // Get values from the UI
        val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val category = categoryTextView.text.toString()
        val date = dateTextView.text.toString()
        val description = descriptionEditText.text.toString()


        // Validate inputs
        if (amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        // Ensure the date and category are not empty
        if (category.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please select a valid category and date.", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract the month from the selected date (Ensure proper formatting)
        val dateParts = date.split("-")

        if (dateParts.size < 3) {
            Toast.makeText(this, "Invalid date format.", Toast.LENGTH_SHORT).show()
            return
        }

        val monthString = dateParts[1] // Extract the month (MM) from the format "dd/MM/yyyy"

        Log.d("AddExpense", "Extracted Month: $monthString")

        // Query the database to check for income transactions in the same month
        val hasIncome = dbHelper.hasIncomeInMonth(monthString)
        Log.d("AddExpense", "Selected Month: $monthString")  // This should now correctly show the month (MM)

        if (!hasIncome) {
            // If no income transactions exist for the month, show a popup message
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("No Income Transaction Found")
                .setMessage("Please enter an income transaction for this month before adding an expense.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
            return
        }

        // If everything is valid, save the expense
        if (amount > 0 && category.isNotEmpty() && date.isNotEmpty()) {
            // Always set the type to "Expense"
            val type = "Expense"

            // Insert the data into the database, including the description
            dbHelper.insertTransaction(amount, type, category, date, description)

            // Reset the fields and navigate back to the ExpenseTracker
            amountEditText.text.clear()
            categoryTextView.text = "Category"
            dateTextView.text = "Select Date of Transaction"
            descriptionEditText.text.clear()

            // Navigate to the ExpenseTracker
            val intent = Intent(this, ExpenseTracker::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    // Function to open the date picker dialog and set the date format to "yyyy-MM-dd"
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

                // Create the format without the weekday (just "yyyy-MM-dd")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                Log.d("AddExpense", "Formatted Date: $formattedDate")

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
