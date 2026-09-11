package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.Gender
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.repository.DailyTotalAggregate
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.NutritionAiService
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.repository.RecipeRepository
import com.nutrimate.app.domain.time.DayClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies "今日吃什么" business rules:
 *  - daily quota (5) -> LimitReached
 *  - exhausted budget (< 150 kcal) -> BudgetExhausted
 *  - candidate calories filtered into 60%..110% of remaining budget
 *  - AI failure / empty result -> AiUnavailable
 */
class GenerateRecipesUseCaseTest {

    private val today = 20000L

    private val profile = UserProfile(
        gender = Gender.MALE,
        birthdayEpochDay = 10000L,
        heightCm = 175.0,
        weightKg = 70.0,
        goal = Goal.LOSE,
        activityLevel = ActivityLevel.MODERATE,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    private val fakeClock = object : DayClock {
        override fun todayEpochDay(): Long = today
        override fun currentHour(): Int = 12
        override fun nowEpochMillis(): Long = 0L
    }

    private fun aiService(recipes: List<RecipeRecommendation>) = object : NutritionAiService {
        override suspend fun analyzeFoodImage(imageBytes: ByteArray) =
            com.nutrimate.app.domain.repository.FoodAnalysisResult("x", 100.0, 5.0, 10.0, 2.0, 0.9, 100.0, null)

        override suspend fun generateRecipes(
            profile: UserProfile,
            remainingCalories: Int,
            targetMeal: String,
            count: Int
        ): List<RecipeRecommendation> = recipes
    }

    private fun recipe(id: Long, calories: Int) = RecipeRecommendation(
        id = id,
        dateEpochDay = today,
        targetMeal = MealType.LUNCH,
        name = "R$id",
        calories = calories,
        proteinGram = 20,
        ingredients = listOf(Ingredient("鸡胸肉", 100.0, "g")),
        steps = listOf("做"),
        createdAtEpochMillis = 0L
    )

    private fun profileRepo() = object : ProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = flowOf(profile)
        override suspend fun saveProfile(profile: UserProfile) = Unit
        override fun observeOnboardingDone(): Flow<Boolean> = flowOf(true)
        override suspend fun setOnboardingDone(done: Boolean) = Unit
        override suspend fun clearBusinessData() = Unit
    }

    private fun foodLogRepo(entries: List<FoodLogEntry>) = object : FoodLogRepository {
        override fun observeByDate(dateEpochDay: Long): Flow<List<FoodLogEntry>> = flowOf(entries)
        override suspend fun getByDate(dateEpochDay: Long): List<FoodLogEntry> = entries
        override suspend fun getDailyTotals(fromDay: Long, toDay: Long): List<DailyTotalAggregate> =
            emptyList()
        override suspend fun insert(entry: FoodLogEntry): Long = 1L
        override suspend fun update(entry: FoodLogEntry) = Unit
        override suspend fun softDelete(id: Long) = Unit
    }

    private fun recipeRepo() = object : RecipeRepository {
        private val store = mutableListOf<RecipeRecommendation>()
        override fun observeByDate(dateEpochDay: Long): Flow<List<RecipeRecommendation>> =
            MutableStateFlow(store).let { it }
        override suspend fun insert(recipe: RecipeRecommendation): Long {
            store += recipe
            return recipe.id
        }
    }

    private class FakeCounter(var used: Int) : RecipeGenerationCounter {
        override suspend fun canGenerate(): Boolean = used < GenerateRecipesUseCase.DAILY_LIMIT
        override suspend fun usedToday(): Int = used
        override suspend fun recordGenerated(dateEpochDay: Long) { used += 1 }
    }

    private fun useCase(
        counter: RecipeGenerationCounter,
        ai: NutritionAiService,
        entries: List<FoodLogEntry> = emptyList()
    ) = GenerateRecipesUseCase(
        foodLogRepository = foodLogRepo(entries),
        profileRepository = profileRepo(),
        recipeRepository = recipeRepo(),
        aiService = ai,
        clock = fakeClock,
        generationCounter = counter,
        calculatePlan = CalculateNutritionPlanUseCase()
    )

    @Test
    fun `limit reached blocks generation`() = runTest {
        val u = useCase(
            counter = FakeCounter(GenerateRecipesUseCase.DAILY_LIMIT),
            ai = aiService(listOf(recipe(1, 400)))
        )
        assertThat(u.execute()).isInstanceOf(GenerateRecipesUseCase.Result.LimitReached::class.java)
    }

    @Test
    fun `exhausted budget returns BudgetExhausted`() = runTest {
        // Profile budget ≈ BMR(≈1664)*1.55*0.8 ≈ 2063 kcal (see CalculateNutritionPlanUseCase).
        // Eating > budget leaves remaining < 150 -> exhausted.
        val eaten = FoodLogEntry(
            dateEpochDay = today, mealType = MealType.LUNCH, foodName = "x",
            source = com.nutrimate.app.domain.model.LogSource.MANUAL,
            calories = 3500.0, protein = 50.0, carbs = 200.0, fat = 60.0,
            createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L
        )
        val u = useCase(
            counter = FakeCounter(0),
            ai = aiService(listOf(recipe(1, 400))),
            entries = listOf(eaten)
        )
        assertThat(u.execute()).isInstanceOf(GenerateRecipesUseCase.Result.BudgetExhausted::class.java)
    }

    @Test
    fun `candidates outside 60-110 percent budget are filtered out`() = runTest {
        // remaining budget will be profile budget (LOSE) minus nothing.
        // budget = BMR*1.55*0.8 for 175/70/30-ish male; whatever it is, craft
        // recipes with absurd calories to force empty -> AiUnavailable.
        val u = useCase(
            counter = FakeCounter(0),
            ai = aiService(listOf(recipe(1, 9999)))
        )
        // 9999 kcal can never be within 110% of any sane budget.
        assertThat(u.execute()).isInstanceOf(GenerateRecipesUseCase.Result.AiUnavailable::class.java)
    }

    @Test
    fun `valid candidates produce Success and are persisted`() = runTest {
        val counter = FakeCounter(0)
        // budget ≈ 2063; 60%..110% ≈ 1238..2270 — pick in-band candidates.
        val u = useCase(
            counter = counter,
            ai = aiService(listOf(recipe(1, 1400), recipe(2, 1600), recipe(3, 1900)))
        )
        val result = u.execute()
        assertThat(result).isInstanceOf(GenerateRecipesUseCase.Result.Success::class.java)
        val recipes = (result as GenerateRecipesUseCase.Result.Success).recipes
        assertThat(recipes).hasSize(3)
        assertThat(counter.usedToday()).isEqualTo(1)
    }

    @Test
    fun `recipeInBudget respects ratio bounds`() {
        val u = useCase(
            counter = FakeCounter(0),
            ai = aiService(emptyList())
        )
        // remaining 500 -> 60%..110% = 300..550
        assertThat(u.recipeInBudget(recipe(1, 300), 500.0)).isTrue()
        assertThat(u.recipeInBudget(recipe(1, 550), 500.0)).isTrue()
        assertThat(u.recipeInBudget(recipe(1, 299), 500.0)).isFalse()
        assertThat(u.recipeInBudget(recipe(1, 551), 500.0)).isFalse()
    }
}