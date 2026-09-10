package com.nutrimate.app.data.ai

import android.util.Base64
import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.repository.AiConfigRepository
import com.nutrimate.app.domain.repository.FoodAnalysisResult
import com.nutrimate.app.domain.repository.NutritionAiService
import com.nutrimate.app.domain.time.DayClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BYOK nutrition AI service with two modes:
 *  - Mock: deterministic offline responses (no account needed; the default
 *    until the user configures an AI provider in Settings).
 *  - OpenAI-compatible: real "chat completions" calls to the configured
 *    provider with structured JSON parsing, timeouts, and limited retries.
 *
 * Domain contract notes (see docs/01): the API key is read via
 * [AiConfigRepository] (KeyStore-backed); only the user-chosen provider sees
 * the photo bytes / prompt text (disclosed once during configuration).
 */
@Singleton
class DefaultNutritionAiService @Inject constructor(
    private val aiConfigRepository: AiConfigRepository,
    private val clock: DayClock
) : NutritionAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeFoodImage(imageBytes: ByteArray): FoodAnalysisResult =
        withContext(Dispatchers.IO) {
            val key = aiConfigRepository.readApiKey()
            if (key.isNullOrBlank()) mockAnalysis() else realAnalysis(key, imageBytes)
        }

    override suspend fun generateRecipes(
        profile: UserProfile,
        remainingCalories: Int,
        targetMeal: String,
        count: Int
    ): List<RecipeRecommendation> =
        withContext(Dispatchers.IO) {
            val key = aiConfigRepository.readApiKey()
            if (key.isNullOrBlank()) {
                mockRecipes(profile, remainingCalories, targetMeal, count)
            } else {
                realRecipes(key, profile, remainingCalories, targetMeal, count)
            }
        }

    // --------------------------------------------------------------- mock

    private fun mockAnalysis(): FoodAnalysisResult = FoodAnalysisResult(
        foodName = "示例餐食（模拟数据）",
        calories = 450.0,
        protein = 25.0,
        carbs = 50.0,
        fat = 15.0,
        confidence = 0.6,
        referenceServingG = 300.0,
        servingTip = "模拟模式：在设置中配置 AI Key 后启用真实识别"
    )

    private fun mockRecipes(
        profile: UserProfile,
        remainingCalories: Int,
        targetMeal: String,
        count: Int
    ): List<RecipeRecommendation> {
        val meal = MealType.valueOf(targetMeal)
        val base = remainingCalories.coerceIn(150, 700)
        val templates = listOf(
            Triple("鸡胸肉蔬菜碗", 0.9, 25),
            Triple("三文鱼糙米饭", 0.95, 30),
            Triple("番茄牛肉意面", 1.0, 28)
        )
        return templates.take(count).map { (name, ratio, proteinPer100) ->
            val cal = (base * ratio).toInt()
            RecipeRecommendation(
                dateEpochDay = clock.todayEpochDay(),
                targetMeal = meal,
                name = name,
                calories = cal,
                proteinGram = (cal * proteinPer100 / 100).coerceAtLeast(10),
                ingredients = listOf(
                    Ingredient("主食/主料", 150.0, "g"),
                    Ingredient("时蔬", 200.0, "g")
                ),
                steps = listOf("备菜", "烹制主料", "装盘"),
                tips = if (profile.allergens.isNotEmpty()) "已避开过敏原（模拟）" else null,
                createdAtEpochMillis = clock.nowEpochMillis()
            )
        }
    }

    // ------------------------------------------------------------- real API

    private suspend fun realAnalysis(apiKey: String, imageBytes: ByteArray): FoodAnalysisResult {
        val (model, baseUrl) = transport()
        val payload = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put("type", "text")
                                        .put(
                                            "text",
                                            ANALYSIS_PROMPT
                                        )
                                )
                                .put(
                                    JSONObject()
                                        .put("type", "image_url")
                                        .put(
                                            "image_url",
                                            JSONObject().put(
                                                "url",
                                                "data:image/jpeg;base64," +
                                                    Base64.encodeToString(
                                                        imageBytes,
                                                        Base64.NO_WRAP
                                                    )
                                            )
                                        )
                                )
                        )
                )
            )
        val content = postChat(baseUrl, apiKey, model, payload)
        return parseAnalysis(content)
    }

    private suspend fun realRecipes(
        apiKey: String,
        profile: UserProfile,
        remainingCalories: Int,
        targetMeal: String,
        count: Int
    ): List<RecipeRecommendation> {
        val (model, baseUrl) = transport()
        val allergens = profile.allergens.joinToString(",") { it.name }.ifBlank { "无" }
        val prefs = profile.tastePreferences.joinToString(",") { it.name }.ifBlank { "均衡" }
        val payload = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put(
                        "content",
                        "为 $targetMeal 生成 $count 个菜谱。偏好：$prefs，忌口：$allergens，" +
                            "剩余热量预算约 $remainingCalories kcal。" +
                            "返回严格 JSON 数组 [{\"name\":\"...\",\"calories\":0,\"proteinG\":0," +
                            "\"ingredients\":[{\"name\":\"...\",\"amount\":0,\"unit\":\"g\"}]," +
                            "\"steps\":[\"...\"],\"tips\":\"...\"}]"
                    )
                )
            )
        val content = postChat(baseUrl, apiKey, model, payload)
        return parseRecipes(content, targetMeal)
    }

    private suspend fun transport(): Pair<String, String> {
        val config = aiConfigRepository.observeConfig().first()
        val baseUrl = config.baseUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: "https://api.openai.com/v1"
        val model = config.model.ifBlank { "gpt-4o-mini" }
        return model to baseUrl.trimEnd('/')
    }

    private fun postChat(baseUrl: String, apiKey: String, model: String, payload: JSONObject): String {
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("AI HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IllegalStateException("AI empty body")
            return JSONObject(body).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    // ------------------------------------------------------------- parsing

    private fun parseAnalysis(content: String): FoodAnalysisResult {
        val json = extractJson(content)
        return FoodAnalysisResult(
            foodName = json.optString("foodName", "未知食物"),
            calories = json.optDouble("calories", 0.0),
            protein = json.optDouble("proteinG", 0.0),
            carbs = json.optDouble("carbG", 0.0),
            fat = json.optDouble("fatG", 0.0),
            confidence = json.optDouble("confidence", 0.5),
            referenceServingG = json.optDouble("referenceServingG", 0.0),
            servingTip = json.optString("servingTip")
        )
    }

    private fun parseRecipes(content: String, targetMeal: String): List<RecipeRecommendation> {
        val json = extractJson(content)
        // Accept either a bare array or {"recipes": [...]}
        val arr = if (json.has("recipes")) {
            json.getJSONArray("recipes")
        } else {
            JSONArray(content.substring(content.indexOf('['), content.lastIndexOf(']') + 1))
        }
        val out = mutableListOf<RecipeRecommendation>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val ingredients = mutableListOf<Ingredient>()
            obj.optJSONArray("ingredients")?.let { ing ->
                for (j in 0 until ing.length()) {
                    val o = ing.getJSONObject(j)
                    ingredients += Ingredient(
                        name = o.optString("name"),
                        amount = o.optDouble("amount", 0.0),
                        unit = o.optString("unit", "g")
                    )
                }
            }
            val steps = mutableListOf<String>()
            obj.optJSONArray("steps")?.let { st ->
                for (j in 0 until st.length()) steps += st.getString(j)
            }
            out += RecipeRecommendation(
                dateEpochDay = clock.todayEpochDay(),
                targetMeal = MealType.valueOf(targetMeal),
                name = obj.optString("name", "菜谱"),
                calories = obj.optInt("calories", 0),
                proteinGram = obj.optInt("proteinG", 0),
                ingredients = ingredients,
                steps = steps,
                tips = obj.optString("tips").ifBlank { null },
                createdAtEpochMillis = clock.nowEpochMillis()
            )
        }
        return out
    }

    private fun extractJson(content: String): JSONObject {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        return if (start >= 0 && end > start) JSONObject(content.substring(start, end + 1))
        else JSONObject()
    }

    private companion object {
        const val ANALYSIS_PROMPT =
            "Analyze this food photo. Return strict JSON: " +
                "{\"foodName\":\"...\",\"calories\":0,\"proteinG\":0,\"carbG\":0,\"fatG\":0," +
                "\"confidence\":0.0,\"referenceServingG\":0,\"servingTip\":\"...\"}"
    }
}