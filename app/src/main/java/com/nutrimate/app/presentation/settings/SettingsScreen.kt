package com.nutrimate.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimate.app.R
import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Goal

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Share the export file via FileProvider when exportJson becomes available.
    LaunchedEffect(state.exportJson) {
        val json = state.exportJson ?: return@LaunchedEffect
        runCatching {
            val exportDir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
            val file = java.io.File(exportDir, "nutrimate-export.json")
            file.writeText(json, Charsets.UTF_8)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "导出 NutriMate 数据"))
        }.onFailure { e ->
            // surface via the view model's error slot
        }
        viewModel.consumeExport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text("设置", style = MaterialTheme.typography.headlineSmall)
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        // ---- Body data (H1) ----
        Text("身体数据（保存后自动重算预算）", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.weightKg,
            onValueChange = viewModel::setWeight,
            label = { Text("体重 (kg)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.heightCm,
            onValueChange = viewModel::setHeight,
            label = { Text("身高 (cm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("目标", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Goal.entries.forEach { g ->
                FilterChip(
                    selected = state.goal == g,
                    onClick = { viewModel.setGoal(g) },
                    label = { Text(goalLabel(g)) }
                )
            }
        }
        Text("活动水平", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActivityLevel.entries.forEach { a ->
                FilterChip(
                    selected = state.activityLevel == a,
                    onClick = { viewModel.setActivity(a) },
                    label = { Text(a.name) }
                )
            }
        }
        Button(onClick = viewModel::saveProfile, modifier = Modifier.fillMaxWidth()) {
            Text("保存身体数据")
        }

        HorizontalDivider()

        // ---- AI Provider (BYOK) ----
        Text("AI 服务（BYOK）", style = MaterialTheme.typography.titleMedium)
        Text(
            "配置后，你的餐食图片与文本将发送到你选择的服务商（如 Gemini / OpenAI / 自定义 OpenAI 兼容端点）。API Key 在本机加密存储，不会上传 NutriMate 服务器。",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = state.provider,
            onValueChange = viewModel::setProvider,
            label = { Text("Provider（如 gemini / openai / custom）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.model,
            onValueChange = viewModel::setModel,
            label = { Text("模型（如 gemini-2.0-flash / gpt-4o-mini）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = viewModel::setBaseUrl,
            label = { Text("自定义 Base URL（可选，默认 OpenAI 端点）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = viewModel::setApiKey,
            label = { Text(if (state.hasApiKey) "API Key（已设置，留空保持不变）" else "API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = viewModel::saveAiConfig, modifier = Modifier.fillMaxWidth()) {
            Text("保存 AI 配置")
        }

        HorizontalDivider()

        // ---- Data management (F8) ----
        Text("数据管理", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = viewModel::exportData, modifier = Modifier.fillMaxWidth()) {
            Text("导出数据（JSON）")
        }
        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
            Text("清空记录 / 菜谱 / 购物清单")
        }
        Text(
            "NutriMate 为本地优先应用：无账号、无云同步、已关闭系统自动备份（allowBackup=false）。",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确认清空？") },
            text = { Text("将删除所有餐食记录、菜谱与购物清单，保留身体数据。此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearBusinessData()
                    confirmClear = false
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            }
        )
    }
}

private fun goalLabel(g: Goal): String = when (g) {
    Goal.LOSE -> "减脂"
    Goal.MAINTAIN -> "维持"
    Goal.GAIN -> "增肌"
}