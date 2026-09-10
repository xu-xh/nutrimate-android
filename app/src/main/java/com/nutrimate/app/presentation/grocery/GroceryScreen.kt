package com.nutrimate.app.presentation.grocery

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

/** Shopping list placeholder (M3 wires GroceryRepository). */
@Composable
fun GroceryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.placeholder_grocery),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.placeholder_grocery_note),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}