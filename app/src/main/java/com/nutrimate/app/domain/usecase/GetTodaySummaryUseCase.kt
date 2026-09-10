package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.DailySummary
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.time.DayClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Aggregates daily consumed nutrition and computes remaining budget.
 * Emits a fresh summary whenever entries or the profile change.
 */
class GetTodaySummaryUseCase @Inject constructor(
    private val foodLogRepository: FoodLogRepository,
    private val profileRepository: ProfileRepository,
    private val clock: DayClock,
    private val calculatePlan: CalculateNutritionPlanUseCase
) {

    /** Reactive summary for the current day (Today screen). */
    fun observeToday(): Flow<DailySummary?> =
        combine(
            foodLogRepository.observeByDate(clock.todayEpochDay()),
            profileRepository.observeProfile()
        ) { entries, profile ->
            buildDailySummary(entries, profile)
        }

    /** Single-shot read for non-reactive screens (e.g. history). */
    suspend fun forDate(dateEpochDay: Long): DailySummary {
        val entries = foodLogRepository.getByDate(dateEpochDay)
        val profile = profileRepository.observeProfile().first()
        return buildDailySummary(entries, profile)
    }

    private fun buildDailySummary(
        entries: List<FoodLogEntry>,
        profile: com.nutrimate.app.domain.model.UserProfile?
    ): DailySummary {
        val budget = profile?.let {
            calculatePlan.execute(it, clock.todayEpochDay()).caloriesBudget
        } ?: 0
        val visible = entries.filterNot { it.deleted }
        val totalCalories = visible.sumOf { it.calories }
        val grouped = visible.groupBy { it.mealType }
        return DailySummary(
            dateEpochDay = clock.todayEpochDay(),
            eatenCalories = totalCalories,
            eatenProtein = visible.sumOf { it.protein },
            eatenCarbs = visible.sumOf { it.carbs },
            eatenFat = visible.sumOf { it.fat },
            remainingCalories = budget - totalCalories,
            overBudget = totalCalories > budget,
            meals = MealType.entries.associateWith { m -> grouped[m] ?: emptyList() }
        )
    }
}