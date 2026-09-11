package com.nutrimate.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.repository.AiConfigRepository
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.ExportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfile? = null,
    // body data editors
    val weightKg: String = "",
    val heightCm: String = "",
    val goal: Goal? = null,
    val activityLevel: ActivityLevel? = null,
    // AI provider
    val provider: String = "",
    val model: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val exportJson: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val aiConfigRepository: AiConfigRepository,
    private val clock: DayClock,
    private val exportDataUseCase: ExportDataUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        viewModelScope.launch {
            val profile = profileRepository.observeProfile().first()
            val ai = aiConfigRepository.observeConfig().first()
            _state.value = SettingsUiState(
                profile = profile,
                weightKg = profile?.weightKg?.toString() ?: "",
                heightCm = profile?.heightCm?.toString() ?: "",
                goal = profile?.goal,
                activityLevel = profile?.activityLevel,
                provider = ai.provider,
                model = ai.model,
                baseUrl = ai.baseUrl.orEmpty(),
                hasApiKey = ai.hasApiKey
            )
        }
    }

    fun setWeight(v: String) = _state.update { it.copy(weightKg = v, saved = false) }
    fun setHeight(v: String) = _state.update { it.copy(heightCm = v, saved = false) }
    fun setGoal(g: Goal) = _state.update { it.copy(goal = g, saved = false) }
    fun setActivity(a: ActivityLevel) = _state.update { it.copy(activityLevel = a, saved = false) }
    fun setProvider(v: String) = _state.update { it.copy(provider = v, saved = false) }
    fun setModel(v: String) = _state.update { it.copy(model = v, saved = false) }
    fun setBaseUrl(v: String) = _state.update { it.copy(baseUrl = v, saved = false) }
    fun setApiKey(v: String) = _state.update { it.copy(apiKey = v, saved = false) }

    /** Save body data (H1: any change recomputes budget/macros on save). */
    fun saveProfile() {
        val s = _state.value
        val current = s.profile ?: return
        val kg = s.weightKg.toDoubleOrNull()
        val cm = s.heightCm.toDoubleOrNull()
        if (kg == null || kg < 30 || kg > 300) { _state.update { it.copy(error = "体重需在 30-300 kg") }; return }
        if (cm == null || cm < 100 || cm > 250) { _state.update { it.copy(error = "身高需在 100-250 cm") }; return }
        val goal = s.goal ?: current.goal
        val activity = s.activityLevel ?: current.activityLevel
        viewModelScope.launch {
            runCatching {
                profileRepository.saveProfile(
                    current.copy(
                        weightKg = kg,
                        heightCm = cm,
                        goal = goal,
                        activityLevel = activity,
                        updatedAtEpochMillis = clock.nowEpochMillis()
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saved = true, error = null, message = "身体数据已保存，预算与宏量已重算") }
            }.onFailure { e ->
                _state.update { it.copy(error = "保存失败：${e.message}") }
            }
        }
    }

    /** Save AI provider transport + key (BYOK). Key goes to Keystore via repository. */
    fun saveAiConfig() {
        val s = _state.value
        if (s.provider.isBlank()) { _state.update { it.copy(error = "请填写 Provider 名称") }; return }
        if (s.model.isBlank()) { _state.update { it.copy(error = "请填写模型名") }; return }
        viewModelScope.launch {
            runCatching {
                aiConfigRepository.saveTransport(
                    provider = s.provider.trim(),
                    model = s.model.trim(),
                    baseUrl = s.baseUrl.trim().ifBlank { null }
                )
                if (s.apiKey.isNotBlank()) {
                    aiConfigRepository.saveApiKey(s.apiKey.trim())
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        saved = true,
                        error = null,
                        hasApiKey = true,
                        message = "AI 配置已保存（Key 已加密存储）。你的图片与文本将发送到你配置的服务商。"
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(error = "保存失败：${e.message}") }
            }
        }
    }

    /** Produce the full export JSON; the screen writes it to a shareable file. */
    fun exportData() {
        viewModelScope.launch {
            runCatching { exportDataUseCase.exportJson() }
                .onSuccess { json -> _state.update { it.copy(exportJson = json, error = null) } }
                .onFailure { e -> _state.update { it.copy(error = "导出失败：${e.message}") } }
        }
    }

    fun consumeExport() = _state.update { it.copy(exportJson = null) }

    /** Clear business data but keep profile (PRD F8). */
    fun clearBusinessData() {
        viewModelScope.launch {
            runCatching { profileRepository.clearBusinessData() }
                .onSuccess { _state.update { it.copy(message = "已清空记录、菜谱与购物清单（保留身体数据）") } }
                .onFailure { e -> _state.update { it.copy(error = "清空失败：${e.message}") } }
        }
    }
}