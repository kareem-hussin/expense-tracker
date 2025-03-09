package com.example.expense_tracker

data class Transaction(
    val id: Int,
    val value: Double,
    val type: String,
    val category: String,
    val date: String,
    val description: String
)