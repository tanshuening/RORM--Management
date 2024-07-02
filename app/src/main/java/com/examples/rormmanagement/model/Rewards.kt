package com.examples.rormmanagement.model

data class Rewards(
    val rewardsId: String = "",
    val name: String = "",
    val description: String = "",
    val termsAndConditions: String = "",
    val points: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val image: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    var available: Boolean = true
)
