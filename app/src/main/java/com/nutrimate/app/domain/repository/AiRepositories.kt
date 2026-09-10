package com.nutrimate.app.domain.repository

import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** AI provider configuration (BYOK). The API key itself never leaves the device. */
data class AiConfig(
    val provider: String,
    val model: String,
    val baseUrl: String? = null,
    val hasApiKey: Boolean = false
)

interface AiConfigRepository {

    fun observeConfig(): Flow<AiConfig>

    /** Persist provider + model (API key stored separately via KeyStore). */
    suspend fun saveTransport(provider: String, model: String, baseUrl: String?)

    /** Encrypt and store the API key in Android Keystore. */
    suspend fun saveApiKey(key: String)

    /** Decrypt the stored API key; null if not configured or unreadable. */
    suspend fun readApiKey(): String?
}

/**
 * Thin domain abstraction over the AI provider network layer.
 * Keeps domain free of HTTP/serialization concerns; v0.1 includes a
 * deterministic mock so the app is fully usable offline.
 */
interface NutritionAiService {

    /** Analyze one compressed food photo. */
    suspend fun analyzeFoodImage(imageBytes: ByteArray): FoodAnalysisResult

    /** Generate up to [count] recipe candidates for the given context. */
    suspend fun generateRecipes(
        profile: UserProfile,
        remainingCalories: Int,
        targetMeal: String,
        count: Int
    ): List<RecipeRecommendation>
}

data class FoodAnalysisResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val confidence: Double,
    val referenceServingG: Double,
    val servingTip: String? = null
)