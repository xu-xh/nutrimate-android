package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.preferences.PreferencesDataSource
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.GenerateRecipesUseCase
import com.nutrimate.app.domain.usecase.RecipeGenerationCounter
import javax.inject.Inject
import javax.inject.Singleton

/** Daily recipe-generation quota backed by DataStore (resets when the day changes). */
@Singleton
class RecipeGenerationCounterImpl @Inject constructor(
    private val preferences: PreferencesDataSource,
    private val clock: DayClock
) : RecipeGenerationCounter {

    override suspend fun canGenerate(): Boolean =
        usedToday() < GenerateRecipesUseCase.DAILY_LIMIT

    override suspend fun usedToday(): Int =
        preferences.usedToday(dayKey())

    override suspend fun recordGenerated(dateEpochDay: Long) {
        val key = dayKey(dateEpochDay)
        val count = preferences.usedToday(key) + 1
        preferences.recordGeneration(key, count)
    }

    private fun dayKey(dateEpochDay: Long = clock.todayEpochDay()): String =
        dateEpochDay.toString()
}