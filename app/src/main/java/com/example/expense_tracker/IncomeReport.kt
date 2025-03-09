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

class IncomeReport : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_report)

        // Initialize PieChart and DatabaseHelper
        pieChart = findViewById(R.id.category_pie_chart)
        dbHelper = DatabaseHelper(this)

        // Load and display data for income categories
        loadPieChartData()

        // Set up button listeners
        val addButton = findViewById<ImageView>(R.id.add_button)
        addButton.setOnClickListener {
            val intent = Intent(this, AddIncome::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        val transactionsButton = findViewById<ImageView>(R.id.transactions_icon)
        transactionsButton.setOnClickListener {
            val intent = Intent(this, ExpenseTracker::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        val expenseTab = findViewById<TextView>(R.id.tab_expense)
        expenseTab.setOnClickListener {
            val intent = Intent(this, ExpenseReport::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun loadPieChartData() {
        val categoryData = fetchCategoryData()

        if (categoryData.isEmpty()) {
            pieChart.visibility = View.GONE
            findViewById<ImageView>(R.id.no_transaction_image).visibility = View.VISIBLE
            findViewById<TextView>(R.id.no_transaction_text).visibility = View.VISIBLE
            return
        }

        pieChart.visibility = View.VISIBLE
        findViewById<ImageView>(R.id.no_transaction_image).visibility = View.GONE
        findViewById<TextView>(R.id.no_transaction_text).visibility = View.GONE

        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categoryData) {
            val pieEntry = PieEntry(amount, category)
            entries.add(pieEntry)
        }

        val dataSet = PieDataSet(entries, "Income Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 2f
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Income"
    }

    // This function fetches income data from the database and sums up amounts for each category
    @SuppressLint("Range")
    private fun fetchCategoryData(): Map<String, Float> {
        val db = dbHelper.readableDatabase
        val categoryData = mutableMapOf<String, Float>()

        // Select only "Income" transactions
        val cursor = db.rawQuery(
            "SELECT category, SUM(value) AS total FROM ${DatabaseHelper.TABLE_NAME} WHERE type = 'Income' GROUP BY category",
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
