package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.LogSource
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.time.DayClock
import javax.inject.Inject

/**
 * Persist a food entry and update the daily aggregate.
 * Portion scaling: nutrition values are stored already-scaled
 * (calories/macros = per-1x-values * servingMultiplier).
 */
class LogFoodUseCase @Inject constructor(
    private val foodLogRepository: FoodLogRepository,
    private val clock: DayClock
) {

    suspend fun logManual(
        foodName: String,
        mealType: MealType,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double
    ): Long {
        require(foodName.isNotBlank()) { "food name required" }
        require(calories in 0.0..5000.0) { "calories out of range" }
        require(listOf(protein, carbs, fat).all { it in 0.0..1000.0 }) { "macro out of range" }

        return foodLogRepository.insert(
            FoodLogEntry(
                dateEpochDay = clock.todayEpochDay(),
                mealType = mealType,
                foodName = foodName.trim(),
                source = LogSource.MANUAL,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                createdAtEpochMillis = clock.nowEpochMillis(),
                updatedAtEpochMillis = clock.nowEpochMillis()
            )
        )
    }

    /** Persist an AI photo analysis result with optional portion scaling. */
    suspend fun logAiPhoto(
        foodName: String,
        mealType: MealType,
        caloriesPerServing: Double,
        proteinPerServing: Double,
        carbPerServing: Double,
        fatPerServing: Double,
        servingMultiplier: Double,
        referenceServingG: Double,
        confidence: Double,
        imageUri: String?,
        manualCalories: Double? = null,
        manualProtein: Double? = null,
        manualCarbs: Double? = null,
        manualFat: Double? = null
    ): Long {
        // 用户修正优先（预览页可编辑）
        val cal = manualCalories ?: (caloriesPerServing * servingMultiplier)
        val prot = manualProtein ?: (proteinPerServing * servingMultiplier)
        val carb = manualCarbs ?: (carbPerServing * servingMultiplier)
        val f = manualFat ?: (fatPerServing * servingMultiplier)

        return foodLogRepository.insert(
            FoodLogEntry(
                dateEpochDay = clock.todayEpochDay(),
                mealType = mealType,
                foodName = foodName.trim(),
                source = LogSource.AI_PHOTO,
                imageUri = imageUri,
                calories = cal,
                protein = prot,
                carbs = carb,
                fat = f,
                servingMultiplier = servingMultiplier,
                referenceServingG = referenceServingG,
                confidence = confidence,
                createdAtEpochMillis = clock.nowEpochMillis(),
                updatedAtEpochMillis = clock.nowEpochMillis()
            )
        )
    }

    suspend fun editEntry(entry: FoodLogEntry) {
        foodLogRepository.update(entry.copy(updatedAtEpochMillis = clock.nowEpochMillis()))
    }

    suspend fun softDelete(id: Long) {
        foodLogRepository.softDelete(id)
    }
}