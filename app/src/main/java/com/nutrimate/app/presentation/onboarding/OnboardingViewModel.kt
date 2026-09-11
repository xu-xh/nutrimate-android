package com.nutrimate.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Allergen
import com.nutrimate.app.domain.model.Gender
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.TastePreference
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** UI state for the 3-step onboarding wizard. */
data class OnboardingUiState(
    val step: Int = 1,
    // Step 1
    val gender: Gender? = null,
    val birthdayEpochDay: Long? = null,
    val heightCm: String = "170",
    val weightKg: String = "60",
    // Step 2
    val goal: Goal? = null,
    val activityLevel: ActivityLevel? = null,
    // Step 3
    val tastePreferences: Set<TastePreference> = emptySet(),
    val allergens: Set<Allergen> = emptySet(),
    val error: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val clock: DayClock
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    fun setGender(gender: Gender) = _state.update { it.copy(gender = gender, error = null) }

    fun setBirthday(date: LocalDate) = _state.update { it.copy(birthdayEpochDay = date.toEpochDay(), error = null) }

    fun setHeight(value: String) = _state.update { it.copy(heightCm = value, error = null) }

    fun setWeight(value: String) = _state.update { it.copy(weightKg = value, error = null) }

    fun setGoal(goal: Goal) = _state.update { it.copy(goal = goal, error = null) }

    fun setActivity(level: ActivityLevel) = _state.update { it.copy(activityLevel = level, error = null) }

    fun toggleTaste(pref: TastePreference) = _state.update { s ->
        val set = if (pref in s.tastePreferences) s.tastePreferences - pref else s.tastePreferences + pref
        s.copy(tastePreferences = set)
    }

    fun toggleAllergen(allergen: Allergen) = _state.update { s ->
        val set = if (allergen in s.allergens) s.allergens - allergen else s.allergens + allergen
        s.copy(allergens = set)
    }

    fun next() {
        val s = _state.value
        when (s.step) {
            1 -> {
                if (s.gender == null || s.birthdayEpochDay == null) {
                    _state.update { it.copy(error = "请选择性别与出生日期") }
                    return
                }
                val cm = s.heightCm.toDoubleOrNull()
                val kg = s.weightKg.toDoubleOrNull()
                if (cm == null || cm < 100 || cm > 250) { _state.update { it.copy(error = "身高需在 100-250 cm") }; return }
                if (kg == null || kg < 30 || kg > 300) { _state.update { it.copy(error = "体重需在 30-300 kg") }; return }
                _state.update { it.copy(step = 2) }
            }
            2 -> {
                if (s.goal == null || s.activityLevel == null) {
                    _state.update { it.copy(error = "请选择目标与活动水平") }
                    return
                }
                _state.update { it.copy(step = 3) }
            }
            3 -> finish()
        }
    }

    fun back() {
        if (_state.value.step > 1) _state.update { it.copy(step = it.step - 1, error = null) }
    }

    private fun finish() {
        val s = _state.value
        val birthday = s.birthdayEpochDay ?: return
        val cm = s.heightCm.toDoubleOrNull() ?: return
        val kg = s.weightKg.toDoubleOrNull() ?: return
        val gender = s.gender ?: return
        val goal = s.goal ?: return
        val activity = s.activityLevel ?: return

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val now = clock.nowEpochMillis()
                profileRepository.saveProfile(
                    UserProfile(
                        gender = gender,
                        birthdayEpochDay = birthday,
                        heightCm = cm,
                        weightKg = kg,
                        goal = goal,
                        activityLevel = activity,
                        tastePreferences = s.tastePreferences,
                        allergens = s.allergens,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now
                    )
                )
                profileRepository.setOnboardingDone(true)
            }.onSuccess {
                _state.update { it.copy(saving = false, done = true) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, error = "保存失败：${e.message}") }
            }
        }
    }
}