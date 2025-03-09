package com.example.expense_tracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class ExpenseReport : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_reports)

        // Initialize PieChart
        pieChart = findViewById(R.id.category_pie_chart)

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Load and display data in the pie chart
        loadPieChartData()

        // Set up button listeners
        val addButton = findViewById<ImageView>(R.id.add_button)
        addButton.setOnClickListener {
            val intent = Intent(this, AddExpense::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        val transactionsButton = findViewById<ImageView>(R.id.transactions_icon)
        transactionsButton.setOnClickListener {
            val intent = Intent(this, ExpenseTracker::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        val incomeTab = findViewById<TextView>(R.id.tab_income)
        incomeTab.setOnClickListener {
            val intent = Intent(this, IncomeReport::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun loadPieChartData() {
        // Fetch categories and their corresponding amounts
        val categoryData = fetchCategoryData()

        // Check if there are no expense transactions
        if (categoryData.isEmpty()) {
            // No expense transactions, show the empty state views
            pieChart.visibility = View.GONE
            findViewById<ImageView>(R.id.no_transaction_image).visibility = View.VISIBLE
            findViewById<TextView>(R.id.no_transaction_text).visibility = View.VISIBLE
            return
        }

        // If there are expense transactions, proceed with displaying the pie chart
        pieChart.visibility = View.VISIBLE
        findViewById<ImageView>(R.id.no_transaction_image).visibility = View.GONE
        findViewById<TextView>(R.id.no_transaction_text).visibility = View.GONE

        // Convert the data to PieEntries
        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categoryData) {
            entries.add(PieEntry(amount, category))
        }

        // Create a PieDataSet
        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() // Use material colors for diversity
        dataSet.sliceSpace = 2f
        dataSet.valueTextSize = 12f

        // Create PieData and set it to the PieChart
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Expenses"
    }

    // This function fetches category data from the database and sums up amounts for each category
    @SuppressLint("Range")
    private fun fetchCategoryData(): Map<String, Float> {
        val db = dbHelper.readableDatabase
        val categoryData = mutableMapOf<String, Float>()

        // Fetch only expense transactions
        val cursor = db.rawQuery(
            "SELECT category, SUM(value) AS total FROM ${DatabaseHelper.TABLE_NAME} WHERE type = 'Expense' GROUP BY category",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(cursor.getColumnIndex("category"))
                val total = cursor.getFloat(cursor.getColumnIndex("total"))
                categoryData[category] = total
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categoryData
    }

}
