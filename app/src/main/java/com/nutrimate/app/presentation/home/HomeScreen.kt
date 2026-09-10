package com.nutrimate.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nutrimate.app.R

/**
 * Today screen shell (M1 placeholder).
 * M2 wires this to GetTodaySummaryUseCase + calorie ring + meal list.
 */
@Composable
fun HomeScreen(mode: String = "home") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (mode == "log_meal_placeholder") {
                stringResource(R.string.placeholder_log_meal)
            } else {
                stringResource(R.string.placeholder_home)
            },
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.placeholder_m1_note),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}