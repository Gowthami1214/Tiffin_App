package com.example.tiffinapp.data.model

import kotlinx.serialization.Serializable

/**
 * Domain model for a home kitchen.
 *
 * @property id           Unique identifier.
 * @property name         Display name of the kitchen.
 * @property cuisineType  Cuisine style (e.g. "Punjabi", "South Indian").
 * @property pricePerTiffin Price in INR per tiffin.
 * @property isVeg        Whether the kitchen is purely vegetarian.
 * @property rating       Rating out of 5.0.
 * @property weeklyMenu   Map of day name → meal description (Monday–Sunday).
 */
@Serializable
data class Kitchen(
    val id: String,
    val name: String,
    val cuisineType: String,
    val pricePerTiffin: Int,
    val isVeg: Boolean,
    val rating: Double,
    val weeklyMenu: Map<String, String>
)

/** UI state for the kitchen list. */
sealed interface KitchenState {
    data object Loading : KitchenState
    data class  Success(val kitchens: List<Kitchen>) : KitchenState
    data object Empty   : KitchenState
    data class  Error(val message: String) : KitchenState
}
