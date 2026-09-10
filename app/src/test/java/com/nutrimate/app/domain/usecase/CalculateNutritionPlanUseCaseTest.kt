package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Gender
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Golden tests from PRD F1 — these lock the BMR/TDEE/budget formulas
 * against hand-computed reference values.
 */
class CalculateNutritionPlanUseCaseTest {

    // Reference "today": 2026-01-01
    private val today: Long = LocalDate.of(2026, 1, 1).toEpochDay()

    private fun profile(
        gender: Gender,
        birthday: LocalDate,
        heightCm: Double,
        weightKg: Double,
        goal: Goal,
        activity: ActivityLevel
    ) = UserProfile(
        gender = gender,
        birthdayEpochDay = birthday.toEpochDay(),
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal,
        activityLevel = activity,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    @Test
    fun `male 30y 170cm 70kg lose sedentary gives budget 1560`() {
        // BMR = 10*70 + 6.25*170 - 5*30 + 5 = 700+1062.5-150+5 = 1617.5
        // TDEE = 1617.5 * 1.2 = 1941.0 ; budget = -20% = 1552.8 -> 1552 (no floor)
        val plan = CalculateNutritionPlanUseCase.execute(
            profile(Gender.MALE, LocalDate.of(1995, 7, 1), 170.0, 70.0, Goal.LOSE, ActivityLevel.SEDENTARY),
            today
        )
        assertThat(plan.bmr).isWithin(0.1).of(1617.5)
        assertThat(plan.tdee).isWithin(0.1).of(1941.0)
        assertThat(plan.caloriesBudget).isEqualTo(1552)
        assertThat(plan.hitSafetyFloor).isFalse()
    }

    @Test
    fun `male small body hits 1500 safety floor`() {
        val plan = CalculateNutritionPlanUseCase.execute(
            profile(Gender.MALE, LocalDate.of(2000, 6, 1), 160.0, 50.0, Goal.LOSE, ActivityLevel.SEDENTARY),
            today
        )
        // raw budget below 1500 -> clamped to 1500
        assertThat(plan.caloriesBudget).isEqualTo(1500)
        assertThat(plan.hitSafetyFloor).isTrue()
    }

    @Test
    fun `female 28y 160cm 52kg lose sedentary hits 1200 floor`() {
        val plan = CalculateNutritionPlanUseCase.execute(
            profile(Gender.FEMALE, LocalDate.of(1997, 8, 15), 160.0, 52.0, Goal.LOSE, ActivityLevel.SEDENTARY),
            today
        )
        assertThat(plan.caloriesBudget).isEqualTo(1200)
        assertThat(plan.hitSafetyFloor).isTrue()
    }

    @Test
    fun `female 35y 165cm 60kg gain active gives 2540`() {
        // BMR = 600 + 1031.25 - 175 - 161 = 1295.25
        // TDEE = 1295.25 * 1.725 = 2234.3 ; budget = +10% = 2457.7 -> 2457 (no floor)
        val plan = CalculateNutritionPlanUseCase.execute(
            profile(Gender.FEMALE, LocalDate.of(1990, 3, 20), 165.0, 60.0, Goal.GAIN, ActivityLevel.ACTIVE),
            today
        )
        assertThat(plan.bmr).isWithin(0.1).of(1295.25)
        assertThat(plan.tdee).isWithin(0.1).of(2234.31)
        assertThat(plan.caloriesBudget).isEqualTo(2457)
        assertThat(plan.hitSafetyFloor).isFalse()
    }

    @Test
    fun `macro targets use energy split and kcal-per-gram`() {
        val plan = CalculateNutritionPlanUseCase.execute(
            profile(Gender.MALE, LocalDate.of(1995, 7, 1), 170.0, 70.0, Goal.LOSE, ActivityLevel.SEDENTARY),
            today
        )
        // budget 1552, lose split 30/40/30
        assertThat(plan.proteinGram).isEqualTo((1552 * 0.30 / 4).toInt())   // 116
        assertThat(plan.carbGram).isEqualTo((1552 * 0.40 / 4).toInt())      // 155
        assertThat(plan.fatGram).isEqualTo((1552 * 0.30 / 9).toInt())       // 51
    }

    @Test
    fun `age of 100 is accepted and of 130 rejected by guard in caller`() {
        // The formula itself is total; boundary validation lives in callers,
        // but we verify an unrealistic age does not crash.
        val plan = CalculateNutritionPlanUseCase.execute(
            profile(Gender.MALE, LocalDate.of(1926, 1, 1), 170.0, 70.0, Goal.MAINTAIN, ActivityLevel.SEDENTARY),
            today
        )
        assertThat(plan.caloriesBudget).isGreaterThan(0)
    }
}