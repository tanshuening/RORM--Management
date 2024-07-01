package com.examples.rormmanagement.model

data class Reservation(
    val reservationId: String = "",
    val restaurantId: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val timeSlot: String = "",
    val numOfPax: Int = 1,
    val specialRequest: String? = null,
    val bookingOccasion: String? = null,
    val bookingPhone: String? = null,
    val order: Order? = null
)
