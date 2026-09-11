package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.NutritionAiService
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.repository.RecipeRepository
import com.nutrimate.app.domain.time.DayClock
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * "今日吃什么" (What to eat today): picks the next meal slot from the current
 * hour + already-logged meals, then asks the AI for up to 3 candidates whose
 * calories fall within 60%..110% of the remaining budget.
 *
 * Daily generation quota (5/day) is enforced here via [RecipeGenerationCounter].
 */
class GenerateRecipesUseCase @Inject constructor(
    private val foodLogRepository: FoodLogRepository,
    private val profileRepository: ProfileRepository,
    private val recipeRepository: RecipeRepository,
    private val aiService: NutritionAiService,
    private val clock: DayClock,
    private val generationCounter: RecipeGenerationCounter,
    private val calculatePlan: CalculateNutritionPlanUseCase
) {

    companion object {
        const val DAILY_LIMIT = 5
        const val MIN_REMAINING_CALORIES = 150.0
        const val MIN_RATIO = 0.6
        const val MAX_RATIO = 1.1
    }

    sealed interface Result {
        data class Success(val recipes: List<RecipeRecommendation>) : Result
        data object LimitReached : Result
        data object BudgetExhausted : Result
        data object AiUnavailable : Result
    }

    suspend fun execute(): Result {
        if (!generationCounter.canGenerate()) return Result.LimitReached

        val profile = profileRepository.observeProfile().first()
            ?: return Result.AiUnavailable

        val today = clock.todayEpochDay()
        val entries: List<FoodLogEntry> = foodLogRepository.getByDate(today)
        val budget = calculatePlan.execute(profile, today).caloriesBudget
        val remaining = budget - entries.sumOf { it.calories }
        if (remaining < MIN_REMAINING_CALORIES) return Result.BudgetExhausted

        val targetMeal: MealType = inferNextMeal(
            currentHour = clock.currentHour(),
            loggedMeals = entries.map { it.mealType }.toSet()
        )

        val recipes = aiService
            .generateRecipes(
                profile = profile,
                remainingCalories = remaining.toInt(),
                targetMeal = targetMeal.name,
                count = 3
            )
            .filter { recipeInBudget(it, remaining) }

        if (recipes.isEmpty()) return Result.AiUnavailable

        recipes.forEach { r ->
            recipeRepository.insert(
                r.copy(
                    dateEpochDay = today,
                    targetMeal = targetMeal,
                    createdAtEpochMillis = clock.nowEpochMillis()
                )
            )
        }
        generationCounter.recordGenerated(today)
        return Result.Success(recipes)
    }

    /** A candidate is acceptable if its calories fall within 60%..110% of remaining budget. */
    internal fun recipeInBudget(recipe: RecipeRecommendation, remaining: Double): Boolean =
        recipe.calories in (remaining * MIN_RATIO).toInt()..(remaining * MAX_RATIO).toInt()

    /**
     * Next meal = the meal type that comes after the current hour, unless it was
     * already logged, in which case we advance to the next unlogged slot.
     */
    internal fun inferNextMeal(currentHour: Int, loggedMeals: Set<MealType>): MealType {
        val queue = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
        val current = MealType.fromHour(currentHour)
        val order = listOf(current) + queue.filterNot { it == current }
        for (m in order) if (m !in loggedMeals) return m
        return current
    }
}

/**
 * Persisted daily generation counter used by [GenerateRecipesUseCase].
 */
interface RecipeGenerationCounter {

    /** True if the user may generate once more today. */
    suspend fun canGenerate(): Boolean

    /** How many generations were used today (for UI display). */
    suspend fun usedToday(): Int

    /** Record one generation for the given day. */
    suspend fun recordGenerated(dateEpochDay: Long)
}