package com.nutrimate.app.domain.repository

import com.nutrimate.app.domain.model.Allergen
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.GroceryItem
import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the user profile (single-row table).
 */
interface ProfileRepository {

    /** Emits the current profile, or null before onboarding completes. */
    fun observeProfile(): Flow<UserProfile?>

    /** Persist a new/updated profile and recompute the nutrition plan. */
    suspend fun saveProfile(profile: UserProfile)

    /** True after onboarding has been completed at least once. */
    fun observeOnboardingDone(): Flow<Boolean>

    suspend fun setOnboardingDone(done: Boolean)

    /** Clear business data but keep profile (per PRD F8 "清空业务数据"). */
    suspend fun clearBusinessData()
}

/**
 * Repository for food log entries (per-day CRUD + aggregate).
 */
interface FoodLogRepository {

    fun observeByDate(dateEpochDay: Long): Flow<List<FoodLogEntry>>

    /** Non-deleted entries only. */
    suspend fun getByDate(dateEpochDay: Long): List<FoodLogEntry>

    /** Per-day totals over [fromDay]..[toDay] (inclusive), for trend charts. */
    suspend fun getDailyTotals(fromDay: Long, toDay: Long): List<DailyTotalAggregate>

    suspend fun insert(entry: FoodLogEntry): Long

    suspend fun update(entry: FoodLogEntry)

    /** Soft-delete: mark deleted=true so it stops counting. */
    suspend fun softDelete(id: Long)
}

/** Per-day nutrition totals (trend chart data point). */
data class DailyTotalAggregate(
    val dateEpochDay: Long,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * Repository for generated recipe history (used for caching + daily quota).
 */
interface RecipeRepository {

    fun observeByDate(dateEpochDay: Long): Flow<List<RecipeRecommendation>>

    suspend fun insert(recipe: RecipeRecommendation): Long
}

/**
 * Repository for the grocery list.
 */
interface GroceryRepository {

    fun observeAll(): Flow<List<GroceryItem>>

    /** Merge ingredients by normalized base name, accumulating amounts. */
    suspend fun addIngredients(ingredients: List<Ingredient>): List<GroceryItem>

    suspend fun setChecked(id: Long, checked: Boolean)

    suspend fun delete(id: Long)

    suspend fun clearAll()
}