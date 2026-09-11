package com.nutrimate.app.presentation.logmeal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.repository.FoodAnalysisResult
import com.nutrimate.app.domain.repository.NutritionAiService
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.LogFoodUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Phase of the log-meal flow. */
enum class LogPhase {
    CHOOSE_INPUT,   // pick camera / gallery / manual
    CAPTURE,        // camera preview live
    ANALYZING,      // AI call in flight
    PREVIEW,        // editable analysis result
    MANUAL_FORM,    // manual entry form
    SAVED           // done, show confirmation
}

data class LogMealUiState(
    val phase: LogPhase = LogPhase.CHOOSE_INPUT,
    val analyzing: Boolean = false,
    val error: String? = null,
    // AI preview fields (editable)
    val foodName: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val confidence: Double = 1.0,
    val referenceServingG: Double = 0.0,
    val servingMultiplier: Double = 1.0,
    val manualMode: Boolean = false,
    val mealType: MealType = MealType.LUNCH,
    val lastImageUri: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class LogMealViewModel @Inject constructor(
    private val aiService: NutritionAiService,
    private val logFoodUseCase: LogFoodUseCase,
    private val clock: DayClock
) : ViewModel() {

    private val _state = MutableStateFlow(LogMealUiState())
    val state: StateFlow<LogMealUiState> = _state

    init {
        // Default meal type follows the current hour.
        _state.update { it.copy(mealType = MealType.fromHour(clock.currentHour())) }
    }

    fun chooseCapture() = _state.update { it.copy(phase = LogPhase.CAPTURE, error = null) }

    fun chooseManual() = _state.update {
        it.copy(phase = LogPhase.MANUAL_FORM, manualMode = true, error = null)
    }

    fun reset() {
        _state.value = LogMealUiState(
            mealType = MealType.fromHour(clock.currentHour())
        )
    }

    fun setMealType(type: MealType) = _state.update { it.copy(mealType = type) }

    fun setServingMultiplier(m: Double) = _state.update { it.copy(servingMultiplier = m) }

    fun setFoodName(v: String) = _state.update { it.copy(foodName = v) }
    fun setCalories(v: String) = _state.update { it.copy(calories = v) }
    fun setProtein(v: String) = _state.update { it.copy(protein = v) }
    fun setCarbs(v: String) = _state.update { it.copy(carbs = v) }
    fun setFat(v: String) = _state.update { it.copy(fat = v) }

    /** Compress + analyze a captured image (from camera or gallery). */
    fun analyzeImage(imageBytes: ByteArray, imageUri: String?) {
        _state.update { it.copy(phase = LogPhase.ANALYZING, analyzing = true, error = null) }
        viewModelScope.launch {
            runCatching {
                // Compress client-side: max long edge 1024, JPEG <= 80% (PRD F3)
                val compressed = compressForAnalysis(imageBytes)
                aiService.analyzeFoodImage(compressed)
            }.onSuccess { result: FoodAnalysisResult ->
                _state.update {
                    it.copy(
                        phase = LogPhase.PREVIEW,
                        analyzing = false,
                        foodName = result.foodName,
                        calories = trim(result.calories),
                        protein = trim(result.protein),
                        carbs = trim(result.carbs),
                        fat = trim(result.fat),
                        confidence = result.confidence,
                        referenceServingG = result.referenceServingG,
                        servingMultiplier = 1.0,
                        manualMode = false,
                        lastImageUri = imageUri
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        phase = LogPhase.PREVIEW,
                        analyzing = false,
                        error = "识别失败：${e.message ?: "未知错误"}，可改为手动输入"
                    )
                }
            }
        }
    }

    /** Save the (possibly edited) AI preview result.
     *  Preview fields always hold the TOTAL for the selected serving; the user
     *  either edits the numbers directly or picks a multiplier that re-scales
     *  the reference serving in the UI. We persist the totals as-is. */
    fun saveAiResult(onSaved: () -> Unit = {}) {
        val s = _state.value
        if (s.foodName.isBlank()) { _state.update { it.copy(error = "食物名称不能为空") }; return }
        viewModelScope.launch {
            runCatching {
                logFoodUseCase.logAiPhoto(
                    foodName = s.foodName,
                    mealType = s.mealType,
                    caloriesPerServing = s.calories.toDoubleOrNull() ?: 0.0,
                    proteinPerServing = s.protein.toDoubleOrNull() ?: 0.0,
                    carbPerServing = s.carbs.toDoubleOrNull() ?: 0.0,
                    fatPerServing = s.fat.toDoubleOrNull() ?: 0.0,
                    servingMultiplier = 1.0,
                    referenceServingG = s.referenceServingG,
                    confidence = s.confidence,
                    imageUri = s.lastImageUri,
                    manualCalories = s.calories.toDoubleOrNull() ?: 0.0,
                    manualProtein = s.protein.toDoubleOrNull() ?: 0.0,
                    manualCarbs = s.carbs.toDoubleOrNull() ?: 0.0,
                    manualFat = s.fat.toDoubleOrNull() ?: 0.0
                )
            }.onSuccess {
                _state.update { it.copy(phase = LogPhase.SAVED, saved = true, error = null) }
                onSaved()
            }.onFailure { e ->
                _state.update { it.copy(error = "保存失败：${e.message}") }
            }
        }
    }

    /** Save a fully manual entry. */
    fun saveManual(onSaved: () -> Unit = {}) {
        val s = _state.value
        if (s.foodName.isBlank()) { _state.update { it.copy(error = "食物名称不能为空") }; return }
        val cal = s.calories.toDoubleOrNull()
        if (cal == null || cal < 0 || cal > 5000) { _state.update { it.copy(error = "热量需在 0-5000 kcal") }; return }
        viewModelScope.launch {
            runCatching {
                logFoodUseCase.logManual(
                    foodName = s.foodName,
                    mealType = s.mealType,
                    calories = cal,
                    protein = s.protein.toDoubleOrNull() ?: 0.0,
                    carbs = s.carbs.toDoubleOrNull() ?: 0.0,
                    fat = s.fat.toDoubleOrNull() ?: 0.0
                )
            }.onSuccess {
                _state.update { it.copy(phase = LogPhase.SAVED, saved = true, error = null) }
                onSaved()
            }.onFailure { e ->
                _state.update { it.copy(error = "保存失败：${e.message}") }
            }
        }
    }

    /** Round-trip JPEG compression with max long edge 1024px. */
    private fun compressForAnalysis(bytes: ByteArray): ByteArray {
        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return bytes
        val maxDim = maxOf(bmp.width, bmp.height)
        if (maxDim <= 1024) {
            val out = java.io.ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            bmp.recycle()
            return out.toByteArray()
        }
        val scale = 1024f / maxDim
        val scaled = android.graphics.Bitmap.createScaledBitmap(
            bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
        )
        bmp.recycle()
        val out = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        scaled.recycle()
        return out.toByteArray()
    }

    private fun trim(v: Double): String =
        if (v == Math.floor(v) && !v.isInfinite()) v.toInt().toString()
        else String.format("%.1f", v)
}