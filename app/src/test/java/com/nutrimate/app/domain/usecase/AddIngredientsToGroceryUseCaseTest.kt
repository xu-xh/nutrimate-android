package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.GroceryItem
import com.nutrimate.app.domain.model.Ingredient
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AddIngredientsToGroceryUseCaseTest {

    private val useCase = AddIngredientsToGroceryUseCase

    @Test
    fun `same ingredient same unit accumulates amount`() {
        val existing = emptyList<GroceryItem>()
        val merged = useCase.merge(
            existing,
            listOf(
                Ingredient("鸡胸肉", 200.0, "g"),
                Ingredient("鸡胸肉", 150.0, "g")
            )
        )
        assertThat(merged).hasSize(1)
        assertThat(merged[0].amount).isEqualTo(350.0)
        assertThat(merged[0].unit).isEqualTo("g")
    }

    @Test
    fun `normalization is case-insensitive and trims whitespace`() {
        val merged = useCase.merge(
            emptyList(),
            listOf(
                Ingredient("  Chicken Breast ", 100.0, "g"),
                Ingredient("chicken breast", 50.0, "g")
            )
        )
        assertThat(merged).hasSize(1)
        assertThat(merged[0].amount).isEqualTo(150.0)
    }

    @Test
    fun `different units are kept separate for user to reconcile`() {
        val merged = useCase.merge(
            emptyList(),
            listOf(
                Ingredient("鸡蛋", 2.0, "个"),
                Ingredient("鸡蛋", 100.0, "g")
            )
        )
        assertThat(merged).hasSize(2)
    }

    @Test
    fun `merging onto existing grocery list keeps ids immutable`() {
        val existing = listOf(
            GroceryItem(id = 7L, name = "鸡胸肉", amount = 200.0, unit = "g", createdAtEpochMillis = 100L)
        )
        val merged = useCase.merge(existing, listOf(Ingredient("鸡胸肉", 100.0, "g")))
        assertThat(merged).hasSize(1)
        assertThat(merged[0].amount).isEqualTo(300.0)
        assertThat(merged[0].createdAtEpochMillis).isEqualTo(100L)
    }
}