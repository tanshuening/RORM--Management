package com.examples.rormmanagement.model

data class Menu(
    var foodId: String = "",
    val foodName: String = "",
    val foodPrice: String = "",
    val foodDescription: String = "",
    val foodIngredients: String = "",
    val foodImage: String = "",
    var available: Boolean = true,
    val restaurantId: String = ""
)
