package com.nutrimate.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimate.app.R
import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Allergen
import com.nutrimate.app.domain.model.Gender
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.TastePreference
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

/**
 * 3-step onboarding wizard. Calls back [onDone] once the profile is saved.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.done) {
        onDone()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (state.step) {
                1 -> stringResource(R.string.ob_title_1)
                2 -> stringResource(R.string.ob_title_2)
                else -> stringResource(R.string.ob_title_3)
            },
            style = MaterialTheme.typography.headlineMedium
        )
        Text("第 ${state.step} / 3 步", style = MaterialTheme.typography.bodySmall)

        when (state.step) {
            1 -> StepBasic(state, viewModel)
            2 -> StepGoal(state, viewModel)
            3 -> StepPrefs(state, viewModel)
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.step > 1) {
                OutlinedButton(onClick = viewModel::back) {
                    Text(stringResource(R.string.ob_back))
                }
            }
            Button(
                onClick = viewModel::next,
                enabled = !state.saving
            ) {
                Text(
                    if (state.saving) stringResource(R.string.ob_calculating)
                    else if (state.step == 3) stringResource(R.string.ob_finish)
                    else stringResource(R.string.ob_next)
                )
            }
        }
    }
}

@Composable
private fun StepBasic(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.ob_gender), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { g ->
                FilterChip(
                    selected = state.gender == g,
                    onClick = { vm.setGender(g) },
                    label = {
                        Text(
                            stringResource(
                                if (g == Gender.MALE) R.string.ob_male else R.string.ob_female
                            )
                        )
                    }
                )
            }
        }

        Text(stringResource(R.string.ob_birthday), style = MaterialTheme.typography.titleSmall)
        val today = LocalDate.now(ZoneId.systemDefault())
        val defaultBirthday = LocalDate.of(1995, Month.JANUARY, 1)
        // Simple date picker: use year/month/day steppers to stay dependency-free.
        DateSteppers(
            initial = defaultBirthday,
            max = today,
            onChange = { vm.setBirthday(it) }
        )

        OutlinedTextField(
            value = state.heightCm,
            onValueChange = vm::setHeight,
            label = { Text(stringResource(R.string.ob_height)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.weightKg,
            onValueChange = vm::setWeight,
            label = { Text(stringResource(R.string.ob_weight)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepGoal(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.ob_goal), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach { g ->
                FilterChip(
                    selected = state.goal == g,
                    onClick = { vm.setGoal(g) },
                    label = {
                        Text(
                            stringResource(
                                when (g) {
                                    Goal.LOSE -> R.string.ob_goal_lose
                                    Goal.MAINTAIN -> R.string.ob_goal_maintain
                                    Goal.GAIN -> R.string.ob_goal_gain
                                }
                            )
                        )
                    }
                )
            }
        }

        Text(stringResource(R.string.ob_activity), style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevel.entries.forEach { level ->
                FilterChip(
                    selected = state.activityLevel == level,
                    onClick = { vm.setActivity(level) },
                    label = { Text(activityLabel(level)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepPrefs(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.ob_taste), style = MaterialTheme.typography.titleSmall)
        FlowChips(
            options = TastePreference.entries.toList(),
            selected = state.tastePreferences,
            label = ::tasteLabel,
            onToggle = vm::toggleTaste
        )
        Text(stringResource(R.string.ob_allergen), style = MaterialTheme.typography.titleSmall)
        FlowChips(
            options = Allergen.entries.toList(),
            selected = state.allergens,
            label = ::allergenLabel,
            onToggle = vm::toggleAllergen
        )
    }
}

@Composable
private fun <T> FlowChips(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    FilterChip(
                        selected = o in selected,
                        onClick = { onToggle(o) },
                        label = { Text(label(o)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSteppers(
    initial: LocalDate,
    max: LocalDate,
    onChange: (LocalDate) -> Unit
) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }

    fun emit() {
        val clipped = LocalDate.of(year, month, 1).withDayOfMonth(
            day.coerceAtMost(LocalDate.of(year, month, 1).lengthOfMonth())
        )
        onChange(clipped)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        NumberStepper("年", year, 1940..max.year) { year = it; emit() }
        NumberStepper("月", month, 1..12) { month = it; day = 1; emit() }
        NumberStepper("日", day, 1..31) { day = it; emit() }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            modifier = Modifier.width(88.dp)
        ) { Text("+") }
        Text("$label $value")
        OutlinedButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            modifier = Modifier.width(88.dp)
        ) { Text("−") }
    }
}

private fun activityLabel(level: ActivityLevel): String = when (level) {
    ActivityLevel.SEDENTARY -> "久坐（上班族，几乎不运动）"
    ActivityLevel.LIGHT -> "轻度（每周 1-3 次轻运动）"
    ActivityLevel.MODERATE -> "中度（每周 3-5 次运动）"
    ActivityLevel.ACTIVE -> "活跃（每周 6-7 次运动）"
    ActivityLevel.VERY_ACTIVE -> "非常活跃（高强度每天训练）"
    ActivityLevel.EXTREME -> "极活跃（重体力 + 每日训练）"
}

private fun tasteLabel(t: TastePreference): String = when (t) {
    TastePreference.LIGHT -> "清淡"
    TastePreference.HIGH_PROTEIN -> "高蛋白"
    TastePreference.LOW_CARB -> "低碳"
    TastePreference.SPICY -> "辣"
    TastePreference.VEGETARIAN -> "素食"
    TastePreference.QUICK -> "快手菜"
}

private fun allergenLabel(a: Allergen): String = when (a) {
    Allergen.PEANUT -> "花生"
    Allergen.DAIRY -> "乳制品"
    Allergen.SEAFOOD -> "海鲜"
    Allergen.GLUTEN -> "麸质"
    Allergen.EGG -> "鸡蛋"
    Allergen.SOY -> "大豆"
}