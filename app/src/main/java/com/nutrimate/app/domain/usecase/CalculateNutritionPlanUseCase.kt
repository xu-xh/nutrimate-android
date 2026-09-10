package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Gender
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.NutritionPlan
import com.nutrimate.app.domain.model.UserProfile
import java.time.LocalDate

/**
 * Computes BMR / TDEE / calorie budget / macro targets from a profile.
 *
 * Golden test values (see CalculateNutritionPlanUseCaseTest):
 *  - Male, 30y, 170cm, 70kg, LOSE, SEDENTARY -> budget 1560
 *  - Male, 25y, 160cm, 50kg, LOSE, SEDENTARY -> floor at 1500
 *  - Female, 28y, 160cm, 52kg, LOSE, SEDENTARY -> floor at 1200
 *  - Female, 35y, 165cm, 60kg, GAIN, ACTIVE -> budget 2540
 */
object CalculateNutritionPlanUseCase {

    /** Safety floor below which calorie budgets are clamped. */
    const val MALE_FLOOR_KCAL = 1500.0
    const val FEMALE_FLOOR_KCAL = 1200.0

    const val PROTEIN_KCAL_PER_G = 4.0
    const val CARB_KCAL_PER_G = 4.0
    const val FAT_KCAL_PER_G = 9.0

    /**
     * @param todayEpochDay reference date for age computation (injectable for tests)
     */
    fun execute(profile: UserProfile, todayEpochDay: Long): NutritionPlan {
        val age = computeAge(profile.birthdayEpochDay, todayEpochDay)
        val bmr = computeBmr(profile.gender, age, profile.heightCm, profile.weightKg)
        val tdee = bmr * profile.activityLevel.multiplier

        val rawBudget = tdee * (1.0 + profile.goal.adjustmentRatio)
        val floor = when (profile.gender) {
            Gender.MALE -> MALE_FLOOR_KCAL
            Gender.FEMALE -> FEMALE_FLOOR_KCAL
        }
        val hitFloor = rawBudget < floor
        val budget = if (hitFloor) floor else rawBudget

        val (proteinShare, carbShare, fatShare) = macroShares(profile.goal)
        val proteinGram = (budget * proteinShare / PROTEIN_KCAL_PER_G).toInt()
        val carbGram = (budget * carbShare / CARB_KCAL_PER_G).toInt()
        val fatGram = (budget * fatShare / FAT_KCAL_PER_G).toInt()

        return NutritionPlan(
            bmr = bmr,
            tdee = tdee,
            caloriesBudget = budget.toInt(),
            proteinGram = proteinGram,
            carbGram = carbGram,
            fatGram = fatGram,
            hitSafetyFloor = hitFloor
        )
    }

    /** Mifflin-St Jeor. Age in full years. */
    private fun computeBmr(gender: Gender, age: Int, heightCm: Double, weightKg: Double): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (gender) {
            Gender.MALE -> base + 5.0
            Gender.FEMALE -> base - 161.0
        }
    }

    private fun computeAge(birthdayEpochDay: Long, todayEpochDay: Long): Int {
        val birthday = LocalDate.ofEpochDay(birthdayEpochDay)
        val today = LocalDate.ofEpochDay(todayEpochDay)
        return java.time.Period.between(birthday, today).years
    }

    /** Energy-share splits per goal: (protein, carbs, fat) as fractions of calories. */
    private fun macroShares(goal: Goal): Triple<Double, Double, Double> = when (goal) {
        Goal.LOSE -> Triple(0.30, 0.40, 0.30)
        Goal.MAINTAIN -> Triple(0.25, 0.45, 0.30)
        Goal.GAIN -> Triple(0.30, 0.45, 0.25)
    }
}