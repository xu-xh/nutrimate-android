package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.model.GroceryItem

/**
 * Merges recipe ingredients into the grocery list.
 *  - identity key = normalized base name + unit
 *  - same name+unit -> accumulate amount
 *  - same name but different unit -> kept as separate rows (user reconciles)
 *
 * Pure logic object — no dependencies, trivially testable.
 */
object AddIngredientsToGroceryUseCase {

    suspend fun execute(ingredients: List<Ingredient>): List<GroceryItem> {
        require(ingredients.isNotEmpty()) { "recipe must have ingredients" }
        return merge(emptyList(), ingredients)
    }

    /** Pure function reused by tests: merges a list into an existing map. */
    fun merge(existing: List<GroceryItem>, incoming: List<Ingredient>): List<GroceryItem> {
        val byKey = existing.associateBy { key(it.name, it.unit) }.toMutableMap()
        for (ing in incoming) {
            val k = key(ing.name, ing.unit)
            val found = byKey[k]
            if (found == null) {
                byKey[k] = GroceryItem(
                    name = ing.name.trim(),
                    amount = ing.amount,
                    unit = ing.unit,
                    createdAtEpochMillis = 0L
                )
            } else {
                byKey[k] = found.copy(amount = found.amount + ing.amount)
            }
        }
        return byKey.values.toList()
    }

    private fun key(name: String, unit: String): String =
        normalize(name) + "|" + unit.trim().lowercase()

    fun normalize(name: String): String =
        name.trim().lowercase()
            .replace(Regex("\\s+"), " ")
}