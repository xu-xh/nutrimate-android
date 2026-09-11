package com.nutrimate.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Verifies the export JSON document shape (PRD F8): profile present,
 * daily totals and grocery items included, valid JSON parseable as object.
 */
class ExportDataUseCaseTest {

    private val export = ExportDataUseCase(
        profileRepository = com.nutrimate.app.domain.repository.ProfileRepositoryStub(null),
        foodLogRepository = com.nutrimate.app.domain.repository.FoodLogRepositoryStub(),
        recipeRepository = com.nutrimate.app.domain.repository.RecipeRepositoryStub(),
        groceryRepository = com.nutrimate.app.domain.repository.GroceryRepositoryStub()
    )

    @Test
    fun `buildJson always emits parseable JSON root object`() {
        val json = export.buildJson(
            profile = null,
            dailyTotals = emptyList(),
            groceryItems = emptyList()
        )
        val root = Json.parseToJsonElement(json).jsonObject
        assertThat(root["app"]!!.jsonPrimitive.content).isEqualTo("NutriMate")
        assertThat(root["dailyTotals"]!!.jsonArray).isEmpty()
        assertThat(root["groceryItems"]!!.jsonArray).isEmpty()
    }

    @Test
    fun `profile fields round-trip into JSON`() {
        val profile = com.nutrimate.app.domain.model.UserProfile(
            gender = com.nutrimate.app.domain.model.Gender.FEMALE,
            birthdayEpochDay = 10000L,
            heightCm = 165.0,
            weightKg = 55.0,
            goal = com.nutrimate.app.domain.model.Goal.LOSE,
            activityLevel = com.nutrimate.app.domain.model.ActivityLevel.LIGHT,
            tastePreferences = setOf(com.nutrimate.app.domain.model.TastePreference.LIGHT),
            allergens = setOf(com.nutrimate.app.domain.model.Allergen.SEAFOOD),
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L
        )
        val json = export.buildJson(
            profile = profile,
            dailyTotals = listOf(DailyTotalExport(20000L, 1500.0, 90.0, 180.0, 40.0)),
            groceryItems = listOf(
                com.nutrimate.app.domain.model.GroceryItem(
                    name = "鸡胸肉", amount = 300.0, unit = "g", createdAtEpochMillis = 1L
                )
            )
        )
        val root = Json.parseToJsonElement(json).jsonObject
        assertThat(root["profile"]!!.jsonObject["goal"]!!.jsonPrimitive.content).isEqualTo("LOSE")
        assertThat(root["profile"]!!.jsonObject["allergens"]!!.jsonArray.size).isEqualTo(1)
        assertThat(root["dailyTotals"]!!.jsonArray[0].jsonObject["calories"]!!.jsonPrimitive.content)
            .isEqualTo("1500.0")
        assertThat(root["groceryItems"]!!.jsonArray[0].jsonObject["name"]!!.jsonPrimitive.content)
            .isEqualTo("鸡胸肉")
    }
}