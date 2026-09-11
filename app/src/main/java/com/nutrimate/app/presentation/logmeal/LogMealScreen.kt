package com.nutrimate.app.presentation.logmeal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimate.app.R
import com.nutrimate.app.domain.model.MealType
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Full-screen "log a meal" flow: choose input → camera/gallery/manual →
 * AI preview (editable) → save. [onDone] returns to the Home tab.
 */
@Composable
fun LogMealScreen(
    onDone: () -> Unit,
    viewModel: LogMealViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // --- Gallery picker (system Photo Picker, no permission needed on modern Android) ---
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.analyzeImage(bytes, uri.toString())
            }
        }
    }

    // --- Camera permission request ---
    var cameraPermissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionAsked = true
        if (granted) {
            viewModel.chooseCapture()
        } else {
            viewModel.reset()
        }
    }

    LaunchedEffect(state.phase) {
        if (state.phase == LogPhase.CAPTURE && !wasCameraGranted(context)) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when (state.phase) {
        LogPhase.CHOOSE_INPUT -> ChooseInput(
            onCamera = {
                if (wasCameraGranted(context)) viewModel.chooseCapture() else permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onManual = viewModel::chooseManual
        )

        LogPhase.CAPTURE -> if (wasCameraGranted(context)) {
            CameraCaptureView(
                captureBitmapBytes = { bytes, uri ->
                    viewModel.analyzeImage(bytes, uri)
                },
                onBack = viewModel::reset
            )
        } else {
            PermissionDenied(
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onManual = viewModel::chooseManual,
                onSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            )
        }

        LogPhase.ANALYZING -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(stringResource(R.string.log_analyzing), modifier = Modifier.padding(top = 12.dp))
            }
        }

        LogPhase.PREVIEW -> PreviewForm(state, viewModel, onSave = { viewModel.saveAiResult() })

        LogPhase.MANUAL_FORM -> ManualForm(state, viewModel, onSave = { viewModel.saveManual() })

        LogPhase.SAVED -> SavedConfirmation(
            onDone = {
                viewModel.reset()
                onDone()
            }
        )
    }
}

@Composable
private fun ChooseInput(onCamera: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.log_choose), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.log_camera), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.log_gallery))
        }
        OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.log_manual))
        }
    }
}

@Composable
private fun CameraCaptureView(
    captureBitmapBytes: (ByteArray, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var surfaceProvider by remember { mutableStateOf<androidx.camera.core.Preview.SurfaceProvider?>(null) }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val provider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                surfaceProvider?.let { sp -> it.setSurfaceProvider(sp) }
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            cameraReady = true
        }.onFailure { cameraReady = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { v ->
                    surfaceProvider = v.surfaceProvider
                    // If the camera already bound, attach the provider now.
                    cameraReady
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) { Text("‹ 返回") }

        val capture = imageCapture
        Button(
            onClick = {
                if (capture != null) {
                    val buffer = ByteArrayOutputStream()
                    capture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val bytes = image.toJpegBytes()
                                image.close()
                                captureBitmapBytes(bytes, null)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                // stays on camera; user can retry
                            }
                        }
                    )
                }
            },
            enabled = cameraReady && capture != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) { Text(stringResource(R.string.log_shoot)) }
    }
}

private fun androidx.camera.core.ImageProxy.toJpegBytes(): ByteArray {
    val buffer = java.nio.ByteBuffer.allocate(width * height * 4)
    planes[0].buffer.rewind()
    planes[0].buffer.get(buffer.array(), 0, buffer.capacity().coerceAtMost(planes[0].buffer.remaining()))
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    val out = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
    bitmap.recycle()
    return out.toByteArray()
}

@Composable
private fun PermissionDenied(
    onGallery: () -> Unit,
    onManual: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.log_permission_denied))
        OutlinedButton(onClick = onGallery) { Text(stringResource(R.string.log_use_gallery)) }
        OutlinedButton(onClick = onManual) { Text(stringResource(R.string.log_manual)) }
        TextButton(onClick = onSettings) { Text(stringResource(R.string.log_go_settings)) }
    }
}

@Composable
private fun PreviewForm(
    state: LogMealUiState,
    viewModel: LogMealViewModel,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.log_preview), style = MaterialTheme.typography.titleLarge)
        if (state.confidence < 0.4) {
            Text(
                stringResource(R.string.log_low_confidence),
                color = Color(0xFFB45309),
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.foodName,
            onValueChange = viewModel::setFoodName,
            label = { Text(stringResource(R.string.log_food_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.calories,
            onValueChange = viewModel::setCalories,
            label = { Text(stringResource(R.string.log_kcal_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.protein,
                onValueChange = viewModel::setProtein,
                label = { Text(stringResource(R.string.log_protein_hint)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.carbs,
                onValueChange = viewModel::setCarbs,
                label = { Text(stringResource(R.string.log_carbs_hint)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.fat,
                onValueChange = viewModel::setFat,
                label = { Text(stringResource(R.string.log_fat_hint)) },
                modifier = Modifier.weight(1f)
            )
        }

        Text(stringResource(R.string.log_meal), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MealType.entries.forEach { m ->
                FilterChip(
                    selected = state.mealType == m,
                    onClick = { viewModel.setMealType(m) },
                    label = { Text(mealLabel(m)) }
                )
            }
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.log_save))
        }
    }
}

@Composable
private fun ManualForm(
    state: LogMealUiState,
    viewModel: LogMealViewModel,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.log_title), style = MaterialTheme.typography.titleLarge)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.foodName,
            onValueChange = viewModel::setFoodName,
            label = { Text(stringResource(R.string.log_food_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.calories,
            onValueChange = viewModel::setCalories,
            label = { Text(stringResource(R.string.log_kcal_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.protein,
                onValueChange = viewModel::setProtein,
                label = { Text(stringResource(R.string.log_protein_hint)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.carbs,
                onValueChange = viewModel::setCarbs,
                label = { Text(stringResource(R.string.log_carbs_hint)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.fat,
                onValueChange = viewModel::setFat,
                label = { Text(stringResource(R.string.log_fat_hint)) },
                modifier = Modifier.weight(1f)
            )
        }

        Text(stringResource(R.string.log_meal), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MealType.entries.forEach { m ->
                FilterChip(
                    selected = state.mealType == m,
                    onClick = { viewModel.setMealType(m) },
                    label = { Text(mealLabel(m)) }
                )
            }
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.log_save))
        }
    }
}

@Composable
private fun SavedConfirmation(onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.log_saved), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.log_done))
            }
        }
    }
}

private fun wasCameraGranted(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun mealLabel(m: MealType): String = when (m) {
    MealType.BREAKFAST -> "早餐"
    MealType.LUNCH -> "午餐"
    MealType.DINNER -> "晚餐"
    MealType.SNACK -> "加餐"
}