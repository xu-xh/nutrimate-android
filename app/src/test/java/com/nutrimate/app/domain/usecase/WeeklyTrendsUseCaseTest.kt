package com.nutrimate.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies WeeklyTrendsUseCase: 7 buckets always returned, missing days are 0,
 * and per-day aggregates map correctly.
 */
class WeeklyTrendsUseCaseTest {

    private val today = 20000L

    @Test
    fun `always returns exactly 7 days ending today`() = runTest {
        val useCase = WeeklyTrendsUseCase(
            foodLogRepository = com.nutrimate.app.domain.repository.FoodLogRepositoryStub(
                dailyTotals = emptyList()
            ),
            profileRepository = com.nutrimate.app.domain.repository.ProfileRepositoryStub(null),
            weightRepository = com.nutrimate.app.domain.repository.WeightRepositoryStub(),
            clock = object : com.nutrimate.app.domain.time.DayClock {
                override fun todayEpochDay(): Long = today
                override fun currentHour(): Int = 12
                override fun nowEpochMillis(): Long = 0L
            },
            calculatePlan = CalculateNutritionPlanUseCase()
        )

        val trends = useCase.last7Days()
        assertThat(trends).hasSize(7)
        assertThat(trends.first().dateEpochDay).isEqualTo(today - 6)
        assertThat(trends.last().dateEpochDay).isEqualTo(today)
        assertThat(trends.all { it.calories == 0.0 }).isTrue()
    }

    @Test
    fun `aggregates map into their buckets and zero-fill gaps`() = runTest {
        val useCase = WeeklyTrendsUseCase(
            foodLogRepository = com.nutrimate.app.domain.repository.FoodLogRepositoryStub(
                dailyTotals = listOf(
                    com.nutrimate.app.domain.repository.DailyTotalAggregate(today - 3, 1500.0, 90.0, 180.0, 40.0),
                    com.nutrimate.app.domain.repository.DailyTotalAggregate(today, 2000.0, 100.0, 220.0, 55.0)
                )
            ),
            profileRepository = com.nutrimate.app.domain.repository.ProfileRepositoryStub(null),
            weightRepository = com.nutrimate.app.domain.repository.WeightRepositoryStub(),
            clock = object : com.nutrimate.app.domain.time.DayClock {
                override fun todayEpochDay(): Long = today
                override fun currentHour(): Int = 12
                override fun nowEpochMillis(): Long = 0L
            },
            calculatePlan = CalculateNutritionPlanUseCase()
        )

        val trends = useCase.last7Days()
        assertThat(trends.first { it.dateEpochDay == today - 3 }.calories).isEqualTo(1500.0)
        assertThat(trends.first { it.dateEpochDay == today }.calories).isEqualTo(2000.0)
        // gaps are zero
        assertThat(trends.first { it.dateEpochDay == today - 1 }.calories).isEqualTo(0.0)
    }

    @Test
    fun `weight values are mapped per day and null when absent`() = runTest {
        val useCase = WeeklyTrendsUseCase(
            foodLogRepository = com.nutrimate.app.domain.repository.FoodLogRepositoryStub(),
            profileRepository = com.nutrimate.app.domain.repository.ProfileRepositoryStub(null),
            weightRepository = com.nutrimate.app.domain.repository.WeightRepositoryStub(
                records = listOf(
                    com.nutrimate.app.domain.model.WeightLogEntry(dateEpochDay = today - 2, weightKg = 70.5, createdAtEpochMillis = 0L),
                    com.nutrimate.app.domain.model.WeightLogEntry(dateEpochDay = today, weightKg = 69.8, createdAtEpochMillis = 0L)
                )
            ),
            clock = object : com.nutrimate.app.domain.time.DayClock {
                override fun todayEpochDay(): Long = today
                override fun currentHour(): Int = 12
                override fun nowEpochMillis(): Long = 0L
            },
            calculatePlan = CalculateNutritionPlanUseCase()
        )

        val trends = useCase.last7Days()
        assertThat(trends.first { it.dateEpochDay == today - 2 }.weightKg).isEqualTo(70.5)
        assertThat(trends.first { it.dateEpochDay == today }.weightKg).isEqualTo(69.8)
        assertThat(trends.first { it.dateEpochDay == today - 1 }.weightKg).isNull()
    }
}