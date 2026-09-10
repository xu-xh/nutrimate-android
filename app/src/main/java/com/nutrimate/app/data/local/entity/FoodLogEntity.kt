package com.nutrimate.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.LogSource
import com.nutrimate.app.domain.model.MealType

@Entity(
    tableName = "food_logs",
    indices = [Index("dateEpochDay"), Index("deleted")]
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val mealType: String,
    val foodName: String,
    val source: String,
    val imageUri: String? = null,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingMultiplier: Double = 1.0,
    val referenceServingG: Double = 0.0,
    val confidence: Double = 1.0,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false
) {
    fun toDomain(): FoodLogEntry = FoodLogEntry(
        id = id,
        dateEpochDay = dateEpochDay,
        mealType = MealType.valueOf(mealType),
        foodName = foodName,
        source = LogSource.valueOf(source),
        imageUri = imageUri,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        servingMultiplier = servingMultiplier,
        referenceServingG = referenceServingG,
        confidence = confidence,
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
        deleted = deleted
    )

    companion object {
        fun fromDomain(e: FoodLogEntry): FoodLogEntity = FoodLogEntity(
            id = e.id,
            dateEpochDay = e.dateEpochDay,
            mealType = e.mealType.name,
            foodName = e.foodName,
            source = e.source.name,
            imageUri = e.imageUri,
            calories = e.calories,
            protein = e.protein,
            carbs = e.carbs,
            fat = e.fat,
            servingMultiplier = e.servingMultiplier,
            referenceServingG = e.referenceServingG,
            confidence = e.confidence,
            createdAt = e.createdAtEpochMillis,
            updatedAt = e.updatedAtEpochMillis,
            deleted = e.deleted
        )
    }
}