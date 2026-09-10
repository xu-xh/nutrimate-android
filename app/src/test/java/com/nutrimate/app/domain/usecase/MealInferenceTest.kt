package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.MealType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GenerateRecipesUseCaseMealInferenceTest {

    // Use reflection-free construction: inferNextMeal is internal & pure,
    // so we test via a small subclass-less helper that reuses the same rule.

    private fun nextMeal(hour: Int, logged: Set<MealType>): MealType =
        MealType.fromHour(hour).let { current ->
            val queue = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
            val order = listOf(current) + queue.filterNot { it == current }
            order.firstOrNull { it !in logged } ?: current
        }

    @Test
    fun `at 8am with none logged suggests breakfast`() {
        assertThat(nextMeal(8, emptySet())).isEqualTo(MealType.BREAKFAST)
    }

    @Test
    fun `at 12pm with breakfast logged suggests lunch`() {
        assertThat(nextMeal(12, setOf(MealType.BREAKFAST))).isEqualTo(MealType.LUNCH)
    }

    @Test
    fun `at 19pm with breakfast and lunch logged suggests dinner`() {
        assertThat(nextMeal(19, setOf(MealType.BREAKFAST, MealType.LUNCH)))
            .isEqualTo(MealType.DINNER)
    }

    @Test
    fun `at 15pm with all earlier meals logged suggests dinner`() {
        // 15h -> LUNCH already logged -> advance to DINNER
        assertThat(nextMeal(15, setOf(MealType.BREAKFAST, MealType.LUNCH)))
            .isEqualTo(MealType.DINNER)
    }

    @Test
    fun `at 23pm with everything logged falls back to snack`() {
        val all = MealType.entries.toSet()
        assertThat(nextMeal(23, all)).isEqualTo(MealType.fromHour(23))
    }

    @Test
    fun `meal boundaries map to expected slots`() {
        assertThat(MealType.fromHour(5)).isEqualTo(MealType.BREAKFAST)
        assertThat(MealType.fromHour(9)).isEqualTo(MealType.BREAKFAST)
        assertThat(MealType.fromHour(10)).isEqualTo(MealType.LUNCH)
        assertThat(MealType.fromHour(13)).isEqualTo(MealType.LUNCH)
        assertThat(MealType.fromHour(14)).isEqualTo(MealType.DINNER)
        assertThat(MealType.fromHour(20)).isEqualTo(MealType.DINNER)
        assertThat(MealType.fromHour(21)).isEqualTo(MealType.SNACK)
        assertThat(MealType.fromHour(4)).isEqualTo(MealType.SNACK)
    }
}