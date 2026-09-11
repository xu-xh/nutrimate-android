package com.nutrimate.app.domain.repository

import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.model.WeightLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Minimal in-memory FoodLogRepository for unit tests. */
class FoodLogRepositoryStub(
    private val dailyTotals: List<DailyTotalAggregate> = emptyList(),
    private val entries: List<FoodLogEntry> = emptyList()
) : FoodLogRepository {
    override fun observeByDate(dateEpochDay: Long): Flow<List<FoodLogEntry>> =
        flowOf(entries.filter { it.dateEpochDay == dateEpochDay })

    override suspend fun getByDate(dateEpochDay: Long): List<FoodLogEntry> =
        entries.filter { it.dateEpochDay == dateEpochDay }

    override suspend fun getDailyTotals(fromDay: Long, toDay: Long): List<DailyTotalAggregate> =
        dailyTotals.filter { it.dateEpochDay in fromDay..toDay }

    override suspend fun insert(entry: FoodLogEntry): Long = 1L
    override suspend fun update(entry: FoodLogEntry) = Unit
    override suspend fun softDelete(id: Long) = Unit
}

/** Minimal ProfileRepository stub. */
class ProfileRepositoryStub(private val profile: UserProfile?) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = flowOf(profile)
    override suspend fun saveProfile(profile: UserProfile) = Unit
    override fun observeOnboardingDone(): Flow<Boolean> = flowOf(profile != null)
    override suspend fun setOnboardingDone(done: Boolean) = Unit
    override suspend fun clearBusinessData() = Unit
}

/** Grocery stub for export tests. */
class GroceryRepositoryStub(
    private val items: List<com.nutrimate.app.domain.model.GroceryItem> = emptyList()
) : GroceryRepository {
    override fun observeAll(): Flow<List<com.nutrimate.app.domain.model.GroceryItem>> = flowOf(items)
    override suspend fun addIngredients(ingredients: List<com.nutrimate.app.domain.model.Ingredient>) =
        items
    override suspend fun setChecked(id: Long, checked: Boolean) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun clearAll() = Unit
}

/** Weight repository stub for trend/weight tests. */
class WeightRepositoryStub(
    private val records: List<WeightLogEntry> = emptyList()
) : WeightRepository {
    override fun observeLatestFirst(): Flow<List<WeightLogEntry>> = flowOf(records)
    override suspend fun getRange(fromDay: Long, toDay: Long): List<WeightLogEntry> =
        records.filter { it.dateEpochDay in fromDay..toDay }
    override suspend fun recordWeight(dateEpochDay: Long, weightKg: Double) = Unit
    override suspend fun delete(id: Long) = Unit
}

/** Recipe stub for export tests (not used by export JSON v1 beyond totals). */
class RecipeRepositoryStub : RecipeRepository {
    override fun observeByDate(dateEpochDay: Long): Flow<List<RecipeRecommendation>> = flowOf(emptyList())
    override suspend fun insert(recipe: RecipeRecommendation): Long = recipe.id
}