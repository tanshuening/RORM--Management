package com.examples.rormmanagement.model

data class Rewards(
    val rewardsId: String = "",
    val name: String = "",
    val description: String = "",
    val termsAndConditions: String = "",
    var points: Int = 0,  // Ensure this is correctly parsed from Firebase
    val startDate: String = "",
    val endDate: String = "",
    val image: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    var available: Boolean = true
) {
    // Ensure points is correctly parsed from String to Int
    fun parsePointsFromString(pointsString: String?): Int {
        return pointsString?.toIntOrNull() ?: 0
    }
}
