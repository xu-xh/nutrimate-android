package com.nutrimate.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.model.RecipeRecommendation
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "recipe_logs",
    indices = [Index("dateEpochDay")]
)
data class RecipeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val targetMeal: String,
    val name: String,
    val calories: Int,
    val proteinGram: Int,
    val ingredientsJson: String,
    val stepsJson: String,
    val tips: String? = null,
    val createdAt: Long
) {
    fun toDomain(): RecipeRecommendation = RecipeRecommendation(
        id = id,
        dateEpochDay = dateEpochDay,
        targetMeal = MealType.valueOf(targetMeal),
        name = name,
        calories = calories,
        proteinGram = proteinGram,
        ingredients = Json.decodeFromString<IngredientsDto>(ingredientsJson).items
            .map { Ingredient(it.name, it.amount, it.unit) },
        steps = Json.decodeFromString<StepsDto>(stepsJson).items,
        tips = tips,
        createdAtEpochMillis = createdAt
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDomain(r: RecipeRecommendation): RecipeLogEntity = RecipeLogEntity(
            dateEpochDay = r.dateEpochDay,
            targetMeal = r.targetMeal.name,
            name = r.name,
            calories = r.calories,
            proteinGram = r.proteinGram,
            ingredientsJson = json.encodeToString(
                IngredientsDto(r.ingredients.map { IngredientDto(it.name, it.amount, it.unit) })
            ),
            stepsJson = json.encodeToString(StepsDto(r.steps)),
            tips = r.tips,
            createdAt = r.createdAtEpochMillis
        )
    }
}

@Serializable
internal data class IngredientDto(val name: String, val amount: Double, val unit: String)

@Serializable
internal data class IngredientsDto(val items: List<IngredientDto>)

@Serializable
internal data class StepsDto(val items: List<String>)