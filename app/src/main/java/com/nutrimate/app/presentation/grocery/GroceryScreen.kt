package com.nutrimate.app.presentation.grocery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimate.app.domain.model.GroceryItem

/** Shopping list tab: merged ingredients with check / delete / clear. */
@Composable
fun GroceryScreen(viewModel: GroceryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("购物清单", style = MaterialTheme.typography.headlineSmall)
            if (!state.empty) {
                TextButton(onClick = viewModel::clearAll) { Text("清空") }
            }
        }

        if (state.empty) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("清单是空的", style = MaterialTheme.typography.titleMedium)
                Text(
                    "去「今日吃什么」生成菜谱，食材会自动合并到这里",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    GroceryRow(item = item, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun GroceryRow(item: GroceryItem, viewModel: GroceryViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { viewModel.toggle(item) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    "${trimAmount(item.amount)} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { viewModel.remove(item) }) {
                Text("✕")
            }
        }
    }
}

private fun trimAmount(v: Double): String =
    if (v == Math.floor(v)) v.toInt().toString() else String.format("%.1f", v)