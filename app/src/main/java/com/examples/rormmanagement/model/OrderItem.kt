package com.examples.rormmanagement.model

data class OrderItem(
    val itemName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)