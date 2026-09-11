package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.repository.DailyTotalAggregate
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.repository.WeightRepository
import com.nutrimate.app.domain.time.DayClock
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** One bar of the 7-day chart. */
data class TrendDay(
    val dateEpochDay: Long,
    val calories: Double,
    val budget: Int?,        // null when no profile yet
    val weightKg: Double?    // null when not measured that day
)

/**
 * Computes the last 7 days (including today) of calories vs budget
 * for the Home trend card. Days with no logs still appear with 0 kcal.
 */
class WeeklyTrendsUseCase @Inject constructor(
    private val foodLogRepository: FoodLogRepository,
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
    private val clock: DayClock,
    private val calculatePlan: CalculateNutritionPlanUseCase
) {

    suspend fun last7Days(): List<TrendDay> {
        val today = clock.todayEpochDay()
        val from = today - 6
        val totals = foodLogRepository.getDailyTotals(from, today)
            .associateBy { it.dateEpochDay }
        val weights = weightRepository.getRange(from, today).associateBy { it.dateEpochDay }
        val profile = profileRepository.observeProfile().first()

        return (from..today).map { day ->
            val aggregate: DailyTotalAggregate? = totals[day]
            TrendDay(
                dateEpochDay = day,
                calories = aggregate?.calories ?: 0.0,
                budget = profile?.let {
                    calculatePlan.execute(it, day).caloriesBudget
                },
                weightKg = weights[day]?.weightKg
            )
        }
    }
}