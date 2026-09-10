package com.nutrimate.app.domain.model

/**
 * User profile snapshot used for nutrition plan calculation.
 *
 * @param birthdayEpochDay days since epoch (LocalDate.toEpochDay) - test-friendly, no java.time coupling
 * @param tastePreferences preference tags (may be empty)
 * @param allergens allergy/restriction flags (may be empty)
 */
data class UserProfile(
    val gender: Gender,
    val birthdayEpochDay: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: Goal,
    val activityLevel: ActivityLevel,
    val tastePreferences: Set<TastePreference> = emptySet(),
    val allergens: Set<Allergen> = emptySet(),
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

/** Daily nutrition targets derived from a profile. */
data class NutritionPlan(
    val bmr: Double,
    val tdee: Double,
    val caloriesBudget: Int,
    val proteinGram: Int,
    val carbGram: Int,
    val fatGram: Int,
    val hitSafetyFloor: Boolean
)

/**
 * A single logged meal entry (one food item).
 *
 * @param servingMultiplier portion multiplier relative to [referenceServingG]; 1.0 = reference serving
 * @param referenceServingG AI-provided reference serving weight in grams
 * @param confidence AI confidence in [0,1], 0 for manual entries
 */
data class FoodLogEntry(
    val id: Long = 0L,
    val dateEpochDay: Long,
    val mealType: MealType,
    val foodName: String,
    val source: LogSource,
    val imageUri: String? = null,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingMultiplier: Double = 1.0,
    val referenceServingG: Double = 0.0,
    val confidence: Double = 1.0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deleted: Boolean = false
)

/** Ingredient of a generated recipe. */
data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: String
)

/** A generated recipe recommendation. */
data class RecipeRecommendation(
    val id: Long = 0L,
    val dateEpochDay: Long,
    val targetMeal: MealType,
    val name: String,
    val calories: Int,
    val proteinGram: Int,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val tips: String? = null,
    val createdAtEpochMillis: Long
)

/** Grocery list item; name is the normalized base ingredient (no portion). */
data class GroceryItem(
    val id: Long = 0L,
    val name: String,
    val amount: Double,
    val unit: String,
    val checked: Boolean = false,
    val createdAtEpochMillis: Long
)

/** Aggregated daily nutrition summary for the Today screen. */
data class DailySummary(
    val dateEpochDay: Long,
    val eatenCalories: Double,
    val eatenProtein: Double,
    val eatenCarbs: Double,
    val eatenFat: Double,
    val remainingCalories: Double,
    val overBudget: Boolean,
    val meals: Map<MealType, List<FoodLogEntry>>
)