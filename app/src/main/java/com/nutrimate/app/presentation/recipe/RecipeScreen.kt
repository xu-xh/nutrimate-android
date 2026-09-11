package com.nutrimate.app.presentation.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimate.app.R
import com.nutrimate.app.domain.model.RecipeRecommendation

/**
 * "今日吃什么" tab: generate 3 candidates within the remaining budget,
 * show details, and add ingredients to the grocery list.
 */
@Composable
fun RecipeScreen(viewModel: RecipeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("今日吃什么", style = MaterialTheme.typography.headlineSmall)
        Text(
            "今日剩余生成次数：${state.remainingGenerations} / 5",
            style = MaterialTheme.typography.bodySmall
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (!state.generatedToday) {
            if (state.loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = viewModel::generate,
                    enabled = state.remainingGenerations > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("基于剩余预算生成 3 个菜谱")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        added = recipe.id in state.addedToGrocery,
                        onClick = { viewModel.selectRecipe(recipe.id) }
                    )
                }
            }
        }
    }

    // Detail dialog
    val selected = state.recipes.firstOrNull { it.id == state.selectedRecipeId }
    if (selected != null) {
        RecipeDetailDialog(
            recipe = selected,
            added = selected.id in state.addedToGrocery,
            onDismiss = viewModel::dismissDetail,
            onAddToGrocery = { viewModel.addToGrocery(selected) }
        )
    }
}

@Composable
private fun RecipeCard(
    recipe: RecipeRecommendation,
    added: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                if (added) {
                    Text("✓ 已加入清单", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                "${recipe.calories} 千卡 · 蛋白质 ${recipe.proteinGram}g · ${mealLabel(recipe.targetMeal)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecipeDetailDialog(
    recipe: RecipeRecommendation,
    added: Boolean,
    onDismiss: () -> Unit,
    onAddToGrocery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipe.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${recipe.calories} 千卡 · 蛋白质 ${recipe.proteinGram}g",
                    style = MaterialTheme.typography.bodyMedium
                )
                "用料：".takeIf { recipe.ingredients.isNotEmpty() }?.let {
                    Text(
                        recipe.ingredients.joinToString("、") { ing ->
                            "${ing.name} ${trimAmount(ing.amount)}${ing.unit}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                "步骤：".takeIf { recipe.steps.isNotEmpty() }?.let {
                    Text(
                        recipe.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                recipe.tips?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    "AI 生成内容，请自行核对食材与过敏原",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(onClick = onAddToGrocery, enabled = !added) {
                Text(if (added) "已加入清单" else "加入购物清单")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun trimAmount(v: Double): String =
    if (v == Math.floor(v)) v.toInt().toString() else String.format("%.1f", v)

private fun mealLabel(type: com.nutrimate.app.domain.model.MealType): String = when (type) {
    com.nutrimate.app.domain.model.MealType.BREAKFAST -> "早餐"
    com.nutrimate.app.domain.model.MealType.LUNCH -> "午餐"
    com.nutrimate.app.domain.model.MealType.DINNER -> "晚餐"
    com.nutrimate.app.domain.model.MealType.SNACK -> "加餐"
}