package com.examples.rormmanagement.model

data class Order(
    val orderId: String = "",
    val orderItems: List<OrderItem> = listOf(),
    val totalAmount: Double = 0.0,
    val subtotalAmount: Double = 0.0,
    val orderStatus: String = "Pending",
    val orderDate: String = "",
    val specialRequests: String? = null,
    val paymentMethod: String = "",
    val promotionId: String = "",
    val reservationId: String = "",
    val restaurantId: String = "",
    val userId: String = "",
    val reservationDetails: Reservation? = null
)