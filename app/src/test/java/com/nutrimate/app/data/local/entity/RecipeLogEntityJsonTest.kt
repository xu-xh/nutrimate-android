package com.nutrimate.app.data.local.entity

import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifies the JSON ingredient/steps serialization used by RECIPE_LOG
 * survives a round trip (and tolerates unknown fields).
 */
class RecipeLogEntityJsonTest {

    @Test
    fun `recipe round-trips through JSON columns`() {
        val recipe = RecipeRecommendation(
            dateEpochDay = 20000L,
            targetMeal = MealType.DINNER,
            name = "三文鱼糙米饭",
            calories = 480,
            proteinGram = 32,
            ingredients = listOf(
                Ingredient("三文鱼", 150.0, "g"),
                Ingredient("糙米", 80.0, "g")
            ),
            steps = listOf("腌制", "煎制", "装盘"),
            tips = null,
            createdAtEpochMillis = 123L
        )

        val entity = RecipeLogEntity.fromDomain(recipe)
        val restored = entity.toDomain()

        assertThat(restored.name).isEqualTo("三文鱼糙米饭")
        assertThat(restored.calories).isEqualTo(480)
        assertThat(restored.ingredients).hasSize(2)
        assertThat(restored.ingredients[0].name).isEqualTo("三文鱼")
        assertThat(restored.ingredients[0].amount).isEqualTo(150.0)
        assertThat(restored.steps).containsExactly("腌制", "煎制", "装盘").inOrder()
        assertThat(restored.targetMeal).isEqualTo(MealType.DINNER)
    }

    @Test
    fun `empty ingredients produce empty list`() {
        val recipe = RecipeRecommendation(
            dateEpochDay = 1L,
            targetMeal = MealType.SNACK,
            name = "无配料",
            calories = 100,
            proteinGram = 1,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAtEpochMillis = 1L
        )
        val restored = RecipeLogEntity.fromDomain(recipe).toDomain()
        assertThat(restored.ingredients).isEmpty()
        assertThat(restored.steps).isEmpty()
    }
}