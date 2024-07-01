package com.examples.rormmanagement.model

data class Promotion(
    val promotionId: String = "",
    val name: String = "",
    val description: String = "",
    val termsAndConditions: String = "",
    val discount: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val image: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    var available: Boolean = true
)
