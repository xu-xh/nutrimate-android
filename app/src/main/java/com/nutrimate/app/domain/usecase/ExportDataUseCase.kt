package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.GroceryItem
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.GroceryRepository
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Builds the user's complete local data as a JSON document (PRD F8 "导出 JSON").
 * Pure mapping is exposed as [buildJson] so it is unit-testable; the suspend
 * [export] variant collects the repositories.
 */
class ExportDataUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val foodLogRepository: FoodLogRepository,
    private val recipeRepository: RecipeRepository,
    private val groceryRepository: GroceryRepository
) {

    suspend fun exportJson(): String {
        val profile = profileRepository.observeProfile().first()
        val today = java.time.LocalDate.now().toEpochDay()
        val foodLogs = foodLogRepository.getDailyTotals(today - 365, today).let { totals ->
            // detailed entries come via repository by date; simplify: expose aggregates
            totals.map { DailyTotalExport(it.dateEpochDay, it.calories, it.protein, it.carbs, it.fat) }
        }
        val grocery = groceryRepository.observeAll().first()
        return buildJson(
            profile = profile,
            dailyTotals = foodLogs,
            groceryItems = grocery
        )
    }

    /** Pure builder — the tested core. */
    fun buildJson(
        profile: UserProfile?,
        dailyTotals: List<DailyTotalExport>,
        groceryItems: List<GroceryItem>
    ): String {
        val doc = ExportDocument(
            app = "NutriMate",
            version = 1,
            exportedAt = java.time.Instant.now().toString(),
            profile = profile?.let { p ->
                ProfileExport(
                    gender = p.gender.name,
                    birthdayEpochDay = p.birthdayEpochDay,
                    heightCm = p.heightCm,
                    weightKg = p.weightKg,
                    goal = p.goal.name,
                    activityLevel = p.activityLevel.name,
                    tastePreferences = p.tastePreferences.map { it.name },
                    allergens = p.allergens.map { it.name }
                )
            },
            dailyTotals = dailyTotals,
            groceryItems = groceryItems.map {
                GroceryItemExport(it.name, it.amount, it.unit, it.checked)
            }
        )
        return Json { prettyPrint = true; encodeDefaults = true }.encodeToString(doc)
    }
}

@Serializable
data class DailyTotalExport(
    val dateEpochDay: Long,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Serializable
private data class ExportDocument(
    val app: String,
    val version: Int,
    val exportedAt: String,
    val profile: ProfileExport?,
    val dailyTotals: List<DailyTotalExport>,
    val groceryItems: List<GroceryItemExport>
)

@Serializable
private data class ProfileExport(
    val gender: String,
    val birthdayEpochDay: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val tastePreferences: List<String>,
    val allergens: List<String>
)

@Serializable
private data class GroceryItemExport(
    val name: String,
    val amount: Double,
    val unit: String,
    val checked: Boolean
)