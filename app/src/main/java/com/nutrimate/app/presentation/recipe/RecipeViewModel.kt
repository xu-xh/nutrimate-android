package com.nutrimate.app.presentation.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.repository.GroceryRepository
import com.nutrimate.app.domain.repository.RecipeRepository
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.GenerateRecipesUseCase
import com.nutrimate.app.domain.usecase.RecipeGenerationCounter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val recipes: List<RecipeRecommendation> = emptyList(),
    val remainingGenerations: Int = GenerateRecipesUseCase.DAILY_LIMIT,
    val generatedToday: Boolean = false,
    val addedToGrocery: Set<Long> = emptySet(),
    val selectedRecipeId: Long? = null
)

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val generateRecipesUseCase: GenerateRecipesUseCase,
    private val groceryRepository: GroceryRepository,
    private val recipeRepository: RecipeRepository,
    private val counter: RecipeGenerationCounter,
    private val clock: DayClock
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeUiState())
    val state: StateFlow<RecipeUiState> = _state

    init {
        viewModelScope.launch {
            _state.update { it.copy(remainingGenerations = counter.usedToday().let { u -> GenerateRecipesUseCase.DAILY_LIMIT - u }) }
            // restore today's generated recipes so the tab is not empty after recreation
            recipeRepository.observeByDate(clock.todayEpochDay()).collect { stored ->
                if (stored.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            recipes = stored,
                            generatedToday = true
                        )
                    }
                }
            }
        }
    }

    fun generate() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = generateRecipesUseCase.execute()) {
                is GenerateRecipesUseCase.Result.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            recipes = result.recipes,
                            generatedToday = true,
                            remainingGenerations = counter.usedToday().let { u -> GenerateRecipesUseCase.DAILY_LIMIT - u }
                        )
                    }
                }
                GenerateRecipesUseCase.Result.LimitReached -> _state.update {
                    it.copy(loading = false, error = "今日生成次数已用完（5 次/天）")
                }
                GenerateRecipesUseCase.Result.BudgetExhausted -> _state.update {
                    it.copy(loading = false, error = "今日营养已达标，可来点轻食或明日再看")
                }
                GenerateRecipesUseCase.Result.AiUnavailable -> _state.update {
                    it.copy(loading = false, error = "生成失败，请稍后重试（可在设置中配置 AI Key）")
                }
            }
        }
    }

    fun selectRecipe(id: Long) = _state.update { it.copy(selectedRecipeId = id) }

    fun dismissDetail() = _state.update { it.copy(selectedRecipeId = null) }

    /** Add this recipe's ingredients to the grocery list (idempotent merge). */
    fun addToGrocery(recipe: RecipeRecommendation) {
        viewModelScope.launch {
            runCatching {
                groceryRepository.addIngredients(recipe.ingredients)
            }.onSuccess {
                _state.update { s -> s.copy(addedToGrocery = s.addedToGrocery + recipe.id) }
            }.onFailure { e ->
                _state.update { it.copy(error = "加入清单失败：${e.message}") }
            }
        }
    }
}