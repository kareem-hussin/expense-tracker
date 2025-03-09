package com.example.expense_tracker

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// SQLiteOpenHelper class to manage database creation and version management
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "expense_tracker.db"
        private const val DATABASE_VERSION = 2 // Incremented the version for the new column

        // Table name
        const val TABLE_NAME = "transactions"

        // Column names
        const val COLUMN_ID = "id"
        const val COLUMN_VALUE = "value"
        const val COLUMN_TYPE = "type"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_DATE = "date"
        const val COLUMN_DESCRIPTION = "description" // New column for description
    }

    // Create table SQL query with the new description column
    private val CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_VALUE REAL,
            $COLUMN_TYPE TEXT,
            $COLUMN_CATEGORY TEXT,
            $COLUMN_DATE TEXT,
            $COLUMN_DESCRIPTION TEXT
        )
    """

    // OnCreate method to create the table
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE)
    }

    // OnUpgrade method to handle database version upgrades
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add the description column if upgrading from version 1 to 2
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DESCRIPTION TEXT")
        }
    }

    // Insert data into the database, now with the description parameter
    fun insertTransaction(value: Double, type: String, category: String, date: String, description: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_VALUE, value)
            put(COLUMN_TYPE, type)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_DATE, date) // Ensure date is in the correct format
            put(COLUMN_DESCRIPTION, description)
        }

        // Log the date being inserted
        Log.d("DatabaseHelper", "Inserting Date: $date")

        db.insert(TABLE_NAME, null, values)
        db.close()
    }



    fun hasIncomeInMonth(month: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM transactions WHERE type = 'Income' AND strftime('%m', date) = ?"
        Log.d("DatabaseHelper", "Query: $query with month: $month")

        // Log the query before execution
        Log.d("DatabaseHelper", "Executing query: $query with month: $month")

        val cursor = db.rawQuery(query, arrayOf(month))

        var hasIncome = false
        if (cursor.moveToFirst()) {
            hasIncome = cursor.getInt(0) > 0
        }

        // Log the result of the query
        Log.d("DatabaseHelper", "Income Found: $hasIncome")

        cursor.close()
        db.close()
        return hasIncome
    }


}
