package com.nutrimate.app.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimate.app.R
import com.nutrimate.app.domain.model.DailySummary
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.model.MealType
import com.nutrimate.app.domain.model.NutritionPlan
import java.time.LocalDate

/**
 * Today screen: calorie ring, macro progress, per-meal list, date navigation.
 * [onLogMeal] opens the full-screen log flow; [onEatWhat] jumps to the recipe tab.
 */
@Composable
fun HomeScreen(
    onLogMeal: () -> Unit,
    onEatWhat: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<FoodLogEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Date navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousDay) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        LocalDate.ofEpochDay(date).toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!state.presentDay) {
                        TextButton(onClick = viewModel::today) {
                            Text(stringResource(R.string.home_today_btn))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::nextDay) { Text("›", style = MaterialTheme.typography.headlineMedium) }
                    IconButton(onClick = onOpenSettings) { Text("⚙") }
                }
            }

            CalorieRing(summary = state.summary, plan = state.plan)

            MacroBars(summary = state.summary, plan = state.plan)

            // 今日吃什么 entry (M3)
            Button(onClick = onEatWhat, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_eat_what))
            }

            HorizontalDivider()

            MealLists(
                summary = state.summary,
                onEdit = { editing = it },
                onDelete = viewModel::deleteEntry
            )

            Spacer(Modifier.height(80.dp))
        }

        // FAB
        FloatingActionButton(
            onClick = onLogMeal,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Text(stringResource(R.string.home_log_fab))
        }
    }

    editing?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { multiplier ->
                viewModel.updateEntry(entry, multiplier)
                editing = null
            },
            onDelete = {
                viewModel.deleteEntry(entry)
                editing = null
            }
        )
    }
}

@Composable
private fun CalorieRing(summary: DailySummary?, plan: NutritionPlan?) {
    val budget = plan?.caloriesBudget ?: 2000
    val eaten = summary?.eatenCalories ?: 0.0
    val over = summary?.overBudget ?: false
    val ratio = (eaten / budget).coerceIn(0.0, 1.0)
    val ringColor = if (over) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color(0xFFE2E7EE),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * ratio.toFloat(),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${eaten.toInt()} / $budget",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                if (over) stringResource(R.string.home_over)
                else stringResource(R.string.home_remaining),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MacroBars(summary: DailySummary?, plan: NutritionPlan?) {
    @Composable
    fun row(label: String, eaten: Double, target: Int?, fraction: Float, color: Color) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${eaten.toInt()}${stringResource(R.string.home_gram)} / ${target ?: "-"}${stringResource(R.string.home_gram)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = Color(0xFFE2E7EE)
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        row(
            stringResource(R.string.home_protein),
            summary?.eatenProtein ?: 0.0,
            plan?.proteinGram,
            fraction = plan?.proteinGram?.let { t ->
                (summary?.eatenProtein ?: 0.0).div(t.toDouble()).toFloat().coerceIn(0f, 1f)
            } ?: 0f,
            color = Color(0xFF2E7D32)
        )
        row(
            stringResource(R.string.home_carbs),
            summary?.eatenCarbs ?: 0.0,
            plan?.carbGram,
            fraction = plan?.carbGram?.let { t ->
                (summary?.eatenCarbs ?: 0.0).div(t.toDouble()).toFloat().coerceIn(0f, 1f)
            } ?: 0f,
            color = Color(0xFFF59E0B)
        )
        row(
            stringResource(R.string.home_fat),
            summary?.eatenFat ?: 0.0,
            plan?.fatGram,
            fraction = plan?.fatGram?.let { t ->
                (summary?.eatenFat ?: 0.0).div(t.toDouble()).toFloat().coerceIn(0f, 1f)
            } ?: 0f,
            color = Color(0xFF2563EB)
        )
    }
}

@Composable
private fun MealLists(
    summary: DailySummary?,
    onEdit: (FoodLogEntry) -> Unit,
    onDelete: (FoodLogEntry) -> Unit
) {
    val meals = summary?.meals ?: emptyMap()
    val hasAny = meals.values.any { it.isNotEmpty() }

    if (!hasAny) {
        Text(
            stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    MealType.entries.forEach { type ->
        val entries = meals[type].orEmpty()
        if (entries.isNotEmpty()) {
            MealSection(type = type, entries = entries, onEdit = onEdit)
        }
    }
}

@Composable
private fun MealSection(
    type: MealType,
    entries: List<FoodLogEntry>,
    onEdit: (FoodLogEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(mealName(type), style = MaterialTheme.typography.titleSmall)
        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(entry) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.foodName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "P ${entry.protein.toInt()}g · C ${entry.carbs.toInt()}g · F ${entry.fat.toInt()}g",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("${entry.calories.toInt()} 千卡", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EditEntryDialog(
    entry: FoodLogEntry,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var multiplierText by remember { mutableStateOf(entry.servingMultiplier.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.foodName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${entry.calories.toInt()} 千卡 · P ${entry.protein.toInt()}g · C ${entry.carbs.toInt()}g · F ${entry.fat.toInt()}g"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_serving) + " × ")
                    androidx.compose.material3.OutlinedTextField(
                        value = multiplierText,
                        onValueChange = { multiplierText = it },
                        singleLine = true,
                        modifier = Modifier.size(110.dp, 56.dp)
                    )
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.home_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = multiplierText.toDoubleOrNull() ?: 1.0
                onSave(m)
            }) { Text(stringResource(R.string.home_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_cancel)) }
        }
    )
}

private fun mealName(type: MealType): String = when (type) {
    MealType.BREAKFAST -> "早餐"
    MealType.LUNCH -> "午餐"
    MealType.DINNER -> "晚餐"
    MealType.SNACK -> "加餐"
}