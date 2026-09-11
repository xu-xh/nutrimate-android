package com.nutrimate.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimate.app.domain.model.DailySummary
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.NutritionPlan
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.CalculateNutritionPlanUseCase
import com.nutrimate.app.domain.usecase.GetTodaySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val dateEpochDay: Long = 0L,
    val summary: DailySummary? = null,
    val plan: NutritionPlan? = null,
    val presentDay: Boolean = false,
    val loading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryUseCase: GetTodaySummaryUseCase,
    private val foodLogRepository: FoodLogRepository,
    private val profileRepository: ProfileRepository,
    private val clock: DayClock,
    private val calculatePlan: CalculateNutritionPlanUseCase
) : ViewModel() {

    private val _date = MutableStateFlow(clock.todayEpochDay())
    val date: StateFlow<Long> = _date

    private val _state = MutableStateFlow(
        HomeUiState(dateEpochDay = clock.todayEpochDay())
    )
    val state: StateFlow<HomeUiState> = _state

    init {
        viewModelScope.launch {
            combine(
                _date,
                profileRepository.observeProfile()
            ) { day, profile ->
                val profileForDay = profile
                val summary = summaryUseCase.forDate(day)
                val plan = profileForDay?.let {
                    calculatePlan.execute(it, day)
                }
                HomeUiState(
                    dateEpochDay = day,
                    summary = summary,
                    plan = plan,
                    presentDay = day == clock.todayEpochDay(),
                    loading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun previousDay() {
        _date.update { it - 1 }
    }

    fun nextDay() {
        _date.update { it + 1 }
    }

    fun today() {
        _date.value = clock.todayEpochDay()
    }

    fun deleteEntry(entry: FoodLogEntry) {
        viewModelScope.launch {
            foodLogRepository.softDelete(entry.id)
            _state.update { s ->
                s.copy(summary = summaryUseCase.forDate(s.dateEpochDay))
            }
        }
    }

    fun updateEntry(entry: FoodLogEntry, multiplier: Double) {
        viewModelScope.launch {
            val scaled = entry.copy(
                calories = round1(entry.calories * multiplier / entry.servingMultiplier.coerceAtLeast(0.01)),
                protein = round1(entry.protein * multiplier / entry.servingMultiplier.coerceAtLeast(0.01)),
                carbs = round1(entry.carbs * multiplier / entry.servingMultiplier.coerceAtLeast(0.01)),
                fat = round1(entry.fat * multiplier / entry.servingMultiplier.coerceAtLeast(0.01)),
                servingMultiplier = multiplier
            )
            foodLogRepository.update(scaled)
            _state.update { s -> s.copy(summary = summaryUseCase.forDate(s.dateEpochDay)) }
        }
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0
}