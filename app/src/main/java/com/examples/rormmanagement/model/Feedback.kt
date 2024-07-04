package com.examples.rormmanagement.model

data class Feedback(
    val feedbackId: String? = null,
    val userId: String? = null,
    val restaurantId: String? = null,
    val reservationId: String? = null,
    val rating: Int = 0,
    val review: String? = null,
    val timestamp: Long = 0L,
    var formattedDate: String? = null
)
