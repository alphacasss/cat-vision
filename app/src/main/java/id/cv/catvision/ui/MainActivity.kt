package id.cv.catvision.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.cv.catvision.breed.BreedEstimator
import id.cv.catvision.breed.BreedMatch
import id.cv.catvision.breed.CatBreed
import id.cv.catvision.breed.CatBreeds
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: CatVisionViewModel by viewModels {
        CatVisionViewModel.factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatVisionTheme {
                CatVisionApp(viewModel)
            }
        }
    }
}

@Composable
private fun CatVisionApp(viewModel: CatVisionViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val camera by viewModel.cameraOptions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startCamera()
        } else {
            viewModel.showError("Izin kamera ditolak")
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::analyzeImage)
    }

    val activateCamera: () -> Unit = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val pickImage: () -> Unit = {
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Camera is manual by default; auto-start only when enabled in Settings.
    LaunchedEffect(Unit) {
        if (settings.autoStartCamera) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.startCamera()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraSurface(
            viewModel = viewModel,
            isRunning = uiState.isRunning,
            focusPoint = camera.focusPoint,
            isFocusing = camera.isFocusing
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                status = uiState.status,
                isModelReady = uiState.isModelReady,
                detections = uiState.detections,
                onSettings = viewModel::openSettings,
                onBreeds = viewModel::openBreeds,
                onPickImage = pickImage
            )
            Spacer(modifier = Modifier.weight(1f))
            DetectionPanel(status = uiState.status)
            ControlBar(
                uiState = uiState,
                camera = camera,
                hapticsEnabled = settings.hapticsEnabled,
                onPhoto = viewModel::takePhoto,
                onHistory = viewModel::openHistory,
                onTorch = viewModel::toggleTorch,
                onSwitchCamera = viewModel::switchCamera,
                onActivate = activateCamera,
                onStop = viewModel::stopCamera,
                onOpenPhoto = viewModel::openPhoto,
                onPickImage = pickImage,
                onZoom = viewModel::setZoom
            )
        }

        if (!uiState.isRunning && !uiState.isCameraStarting && uiState.status !is UiStatus.Error) {
            CameraOffOverlay(
                onActivate = activateCamera,
                onOpenSettings = viewModel::openSettings
            )
        }

        if (uiState.isCameraStarting) {
            StartingOverlay()
        }

        if (uiState.status is UiStatus.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                ErrorCard(
                    message = (uiState.status as UiStatus.Error).msg,
                    onRetry = activateCamera,
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }

        if (uiState.showHistory) {
            HistorySheet(
                items = uiState.history,
                onClose = viewModel::closeHistory,
                onClear = viewModel::clearHistory
            )
        }

        if (uiState.showSettings) {
            SettingsSheet(
                settings = settings,
                onClose = viewModel::closeSettings,
                onAutoStart = viewModel::setAutoStartCamera,
                onHaptics = viewModel::setHapticsEnabled,
                onSaveDetections = viewModel::setSaveDetections,
                onThreshold = viewModel::setDetectionThreshold,
                onPickImage = pickImage,
                onClearHistory = viewModel::clearHistory,
                onOpenBreeds = {
                    viewModel.closeSettings()
                    viewModel.openBreeds()
                }
            )
        }

        if (uiState.showBreeds) {
            BreedDatabaseSheet(onClose = viewModel::closeBreeds)
        }

        if (uiState.isAnalyzingImage || uiState.importedResult != null || uiState.importError != null) {
            ImportDialog(
                result = uiState.importedResult,
                analyzing = uiState.isAnalyzingImage,
                error = uiState.importError,
                onClose = viewModel::closeImportedResult
            )
        }

        uiState.lastPhotoPath?.let { path ->
            if (uiState.showPhoto) {
                PhotoDialog(path = path, onClose = viewModel::closePhoto)
            }
        }
    }
}

@Composable
private fun CameraSurface(
    viewModel: CatVisionViewModel,
    isRunning: Boolean,
    focusPoint: Pair<Float, Float>?,
    isFocusing: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f && isRunning) viewModel.zoomBy(zoom)
                }
                detectTapGestures { offset ->
                    if (isRunning) {
                        viewModel.focusAt(
                            offset.x / size.width,
                            offset.y / size.height
                        )
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also(viewModel::setPreviewView)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (focusPoint != null) {
            val scale by animateFloatAsState(
                targetValue = if (isFocusing) 0.65f else 1f,
                animationSpec = tween(220),
                label = "focusScale"
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * focusPoint.first - 24.dp,
                        y = maxHeight * focusPoint.second - 24.dp
                    )
                    .size(48.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .border(2.dp, catAccent, CircleShape)
            )
        }

        if (isRunning) {
            val transition = rememberInfiniteTransition(label = "scan")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanProgress"
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 28.dp.toPx()
                val stroke = 2.dp.toPx()
                val lineColor = catAccent.copy(alpha = 0.75f)
                val corner = 34.dp.toPx()
                val right = size.width - inset
                val bottom = size.height - inset

                drawLine(lineColor, Offset(inset, inset), Offset(inset + corner, inset), stroke)
                drawLine(lineColor, Offset(inset, inset), Offset(inset, inset + corner), stroke)
                drawLine(lineColor, Offset(right, inset), Offset(right - corner, inset), stroke)
                drawLine(lineColor, Offset(right, inset), Offset(right, inset + corner), stroke)
                drawLine(lineColor, Offset(inset, bottom), Offset(inset + corner, bottom), stroke)
                drawLine(lineColor, Offset(inset, bottom), Offset(inset, bottom - corner), stroke)
                drawLine(lineColor, Offset(right, bottom), Offset(right - corner, bottom), stroke)
                drawLine(lineColor, Offset(right, bottom), Offset(right, bottom - corner), stroke)

                val y = inset + (bottom - inset) * progress
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            catAccent.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        startX = inset,
                        endX = right
                    ),
                    start = Offset(inset, y),
                    end = Offset(right, y),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    status: UiStatus,
    isModelReady: Boolean,
    detections: Int,
    onSettings: () -> Unit,
    onBreeds: () -> Unit,
    onPickImage: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val statusColor = when (status) {
        is UiStatus.CatDetected -> catAccent
        is UiStatus.NotACat -> warningAmber
        is UiStatus.Error -> errorRed
        is UiStatus.Paused -> warningAmber
        is UiStatus.Running -> successGreen
        else -> Color(0xFF9AA4B2)
    }
    val statusLabel = when (status) {
        is UiStatus.CatDetected -> "Kucing terdeteksi"
        is UiStatus.NotACat -> "Bukan kucing"
        is UiStatus.Error -> "Error"
        is UiStatus.Paused -> "Kamera berhenti"
        is UiStatus.Running -> "Memantau kamera"
        else -> "Kamera nonaktif"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = catAccent.copy(alpha = 0.18f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Pets,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "CatVision",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.10f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (isModelReady) Icons.Filled.Bolt else Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = if (isModelReady) successGreen else warningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isModelReady) "AI siap" else "Memuat AI",
                        color = Color(0xFFE6EAF2),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onPickImage, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = "Pilih foto dari galeri",
                    tint = Color.White
                )
            }
            IconButton(onClick = onBreeds, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = "Database breed",
                    tint = Color.White
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = dotAlpha }
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedContent(
                targetState = statusLabel,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                },
                label = "statusText"
            ) { label ->
                Text(
                    text = label,
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (status is UiStatus.Error) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.msg,
                    color = Color(0xFFB7BFCC),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (detections > 0) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$detections deteksi",
                    color = Color(0xFFB7BFCC),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CameraOffOverlay(
    onActivate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(28.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = CircleShape, color = catAccent.copy(alpha = 0.14f)) {
                    Icon(
                        imageVector = Icons.Filled.Pets,
                        contentDescription = null,
                        tint = catAccent,
                        modifier = Modifier.padding(16.dp).size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Kamera nonaktif",
                    color = Color(0xFFF7F9FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aktifkan kamera saat dibutuhkan. Tidak perlu menutup aplikasi.",
                    color = Color(0xFF9AA4B2),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActivate,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = catAccent,
                        contentColor = Color(0xFF12151B)
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aktifkan Kamera", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onOpenSettings) {
                    Text("Pengaturan", color = Color(0xFFB7BFCC), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StartingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = catAccent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Menyalakan kamera…",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DetectionPanel(status: UiStatus) {
    val visible = status is UiStatus.CatDetected || status is UiStatus.NotACat
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        when (status) {
            is UiStatus.CatDetected -> DetectionCard(
                label = status.label,
                confidence = status.confidence,
                topClasses = status.topClasses,
                breedMatches = status.breedMatches,
                features = status.features,
                modelLabel = status.modelLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            is UiStatus.NotACat -> NotACatCard(
                label = status.label,
                confidence = status.confidence,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            else -> Unit
        }
    }
}

@Composable
private fun NotACatCard(
    label: String,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.94f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = warningAmber.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = warningAmber,
                    modifier = Modifier.padding(9.dp).size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bukan kucing",
                    color = Color(0xFFF7F9FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (confidence > 0f) {
                        "Objek terdeteksi: $label (${(confidence * 100).roundToInt()}%)"
                    } else {
                        "Tidak ada objek yang bisa dikenali"
                    },
                    color = Color(0xFFB7BFCC),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Arahkan kamera ke kucing untuk estimasi breed.",
                    color = Color(0xFF9AA4B2),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DetectionCard(
    label: String,
    confidence: Float,
    topClasses: List<Pair<String, Float>> = emptyList(),
    breedMatches: List<BreedMatch> = emptyList(),
    features: BreedEstimator.Features? = null,
    modelLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.94f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SentimentSatisfiedAlt,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = Color(0xFFF7F9FC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Estimasi breed dari warna & pola bulu",
                        color = Color(0xFF9AA4B2),
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "${(confidence * 100).roundToInt()}%",
                    color = catAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ConfidenceBar(confidence)

            if (breedMatches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "KANDIDAT BREED",
                    color = catAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                breedMatches.forEachIndexed { index, match ->
                    BreedCandidateRow(rank = index + 1, match = match)
                }
                breedMatches.firstOrNull()?.reasons?.firstOrNull()?.let { reason ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reason,
                        color = Color(0xFF9AA4B2),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val colorSummary = features?.colorSummary
            val pattern = features?.pattern?.label
            if (!colorSummary.isNullOrBlank() || pattern != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!colorSummary.isNullOrBlank()) {
                        FeatureChip("Warna: $colorSummary")
                    }
                    if (pattern != null) {
                        FeatureChip("Pola: $pattern")
                    }
                }
            }

            if (modelLabel != null && breedMatches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Model ImageNet: $modelLabel",
                    color = Color(0xFF7E8898),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BreedCandidateRow(rank: Int, match: BreedMatch) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = "$rank.",
            color = Color(0xFF7E8898),
            fontSize = 11.sp,
            modifier = Modifier.width(18.dp)
        )
        Text(
            text = match.breed.name,
            color = if (rank == 1) Color(0xFFF7F9FC) else Color(0xFFB7BFCC),
            fontWeight = if (rank == 1) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(match.confidence * 100).roundToInt()}%",
            color = if (rank == 1) catAccent else Color(0xFF9AA4B2),
            fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text = text,
            color = Color(0xFFB7BFCC),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ConfidenceBar(confidence: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(confidence.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(listOf(catAccentDark, catAccent)),
                    CircleShape
                )
        )
    }
}

@Composable
private fun ControlBar(
    uiState: UiState,
    camera: CameraOptions,
    hapticsEnabled: Boolean,
    onPhoto: () -> Unit,
    onHistory: () -> Unit,
    onTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onActivate: () -> Unit,
    onStop: () -> Unit,
    onOpenPhoto: () -> Unit,
    onPickImage: () -> Unit,
    onZoom: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (uiState.isRunning && camera.maxZoom > 1.05f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    val presets = buildList {
                        add(1f)
                        if (camera.maxZoom >= 1.9f) add(2f)
                        if (camera.maxZoom >= 2.9f) add(3f)
                    }
                    presets.forEach { preset ->
                        val selected = if (preset == 1f) {
                            camera.zoomRatio < 1.45f
                        } else {
                            abs(camera.zoomRatio - preset) < 0.45f
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (selected) catAccent else Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.clickable { onZoom(preset) }
                        ) {
                            Text(
                                text = "${preset.roundToInt()}x",
                                color = if (selected) Color(0xFF12151B) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallAction(
                    icon = Icons.Filled.History,
                    contentDescription = "Riwayat",
                    enabled = uiState.history.isNotEmpty(),
                    onClick = onHistory
                )
                SmallAction(
                    icon = if (camera.isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Senter",
                    enabled = uiState.isRunning && camera.hasFlash &&
                        camera.lensFacing == CameraSelector.LENS_FACING_BACK,
                    tint = if (camera.isTorchOn) catAccent else Color.White,
                    onClick = onTorch
                )

                ShutterButton(
                    enabled = uiState.isRunning,
                    icon = if (uiState.isRunning) Icons.Filled.CameraAlt else Icons.Filled.PlayArrow,
                    hapticsEnabled = hapticsEnabled,
                    onClick = {
                        if (uiState.isRunning) onPhoto() else onActivate()
                    }
                )

                SmallAction(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Ganti kamera",
                    enabled = uiState.isRunning,
                    onClick = onSwitchCamera
                )

                if (uiState.lastPhotoPath != null) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable(onClick = onOpenPhoto)
                    ) {
                        AsyncImage(
                            model = File(uiState.lastPhotoPath),
                            contentDescription = "Foto terakhir",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    SmallAction(
                        icon = Icons.Filled.PhotoLibrary,
                        contentDescription = "Pilih foto",
                        enabled = true,
                        onClick = onPickImage
                    )
                }
            }

            if (uiState.isRunning) {
                TextButton(
                    onClick = onStop,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        tint = Color(0xFFB7BFCC),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hentikan kamera",
                        color = Color(0xFFB7BFCC),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = Color.White.copy(alpha = 0.28f)
        ),
        modifier = Modifier
            .size(46.dp)
            .background(Color.White.copy(alpha = 0.10f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ShutterButton(
    enabled: Boolean,
    icon: ImageVector,
    hapticsEnabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = tween(120),
        label = "shutterScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(78.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(3.dp, Color.White.copy(alpha = if (enabled) 0.9f else 0.3f), CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(if (enabled) catAccent else Color.White.copy(alpha = 0.15f))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ) {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (enabled) "Ambil foto" else "Aktifkan kamera",
            tint = if (enabled) Color(0xFF12151B) else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = errorRed,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Terjadi masalah",
                color = Color(0xFFF7F9FC),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = Color(0xFFB7BFCC),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onRetry) {
                    Text("Coba lagi", color = catAccent)
                }
                TextButton(onClick = onOpenSettings) {
                    Text("Izin aplikasi", color = catAccent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: AppSettings,
    onClose: () -> Unit,
    onAutoStart: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onSaveDetections: (Boolean) -> Unit,
    onThreshold: (Float) -> Unit,
    onPickImage: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenBreeds: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan",
                    color = Color(0xFFF7F9FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFFB7BFCC)
                    )
                }
            }

            SectionLabel("Kamera")
            SettingSwitch(
                title = "Mulai kamera otomatis",
                subtitle = "Default mati — kamera dinyalakan manual",
                checked = settings.autoStartCamera,
                onCheckedChange = onAutoStart
            )
            SettingSwitch(
                title = "Getar saat memotret",
                subtitle = "Haptic feedback pada tombol shutter",
                checked = settings.hapticsEnabled,
                onCheckedChange = onHaptics
            )

            SettingsDivider()

            SectionLabel("Deteksi")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ambang keyakinan",
                        color = Color(0xFFF7F9FC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Minimal ${(settings.detectionThreshold * 100).roundToInt()}% untuk menampilkan estimasi breed",
                        color = Color(0xFF9AA4B2),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "${(settings.detectionThreshold * 100).roundToInt()}%",
                    color = catAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Slider(
                value = settings.detectionThreshold,
                onValueChange = onThreshold,
                valueRange = 0.20f..0.80f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = catAccent,
                    activeTrackColor = catAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )

            SettingsDivider()

            SectionLabel("Foto & data")
            SettingSwitch(
                title = "Simpan deteksi ke riwayat",
                subtitle = "Hasil kamera dan foto galeri",
                checked = settings.saveDetections,
                onCheckedChange = onSaveDetections
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih foto dari galeri", color = Color(0xFFF7F9FC))
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = errorRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus semua riwayat & foto", color = errorRed, fontSize = 13.sp)
            }

            SettingsDivider()

            SectionLabel("Breed kucing")
            OutlinedButton(
                onClick = onOpenBreeds,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka database breed", color = Color(0xFFF7F9FC))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${CatBreeds.all.size} breed populer & kontes, lengkap dengan ciri fisik, warna, dan perawatan.",
                color = Color(0xFF9AA4B2),
                fontSize = 11.sp
            )

            SettingsDivider()

            SectionLabel("Tentang")
            Text(
                text = "CatVision v1.1.0",
                color = Color(0xFFF7F9FC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Model MobileNetV2 + estimasi breed on-device",
                color = Color(0xFF9AA4B2),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreedDatabaseSheet(onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<CatBreed?>(null) }

    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            CatBreeds.all
        } else {
            CatBreeds.all.filter { breed ->
                breed.name.contains(q, ignoreCase = true) ||
                    breed.origin.contains(q, ignoreCase = true) ||
                    breed.temperament.contains(q, ignoreCase = true) ||
                    breed.traits.any { it.contains(q, ignoreCase = true) }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Database breed kucing",
                        color = Color(0xFFF7F9FC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "${CatBreeds.all.size} breed • ${CatBreeds.contestBreeds.size} sering ikut kontes",
                        color = Color(0xFF9AA4B2),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFFB7BFCC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Cari breed, warna, atau ciri fisik…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color(0xFF9AA4B2),
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF7F9FC),
                    unfocusedTextColor = Color(0xFFF7F9FC),
                    focusedBorderColor = catAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                    focusedPlaceholderColor = Color(0xFF9AA4B2),
                    unfocusedPlaceholderColor = Color(0xFF9AA4B2),
                    cursorColor = catAccent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { breed ->
                    BreedListRow(breed = breed, onClick = { selected = breed })
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "Breed tidak ditemukan. Coba kata kunci lain.",
                            color = Color(0xFF9AA4B2),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    selected?.let { breed ->
        BreedDetailDialog(breed = breed, onClose = { selected = null })
    }
}

@Composable
private fun BreedListRow(breed: CatBreed, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f))
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = catAccent.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Pets,
                    contentDescription = null,
                    tint = catAccent,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = breed.name,
                        color = Color(0xFFF7F9FC),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (breed.contest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catAccent.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "KONTES",
                                color = catAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "${breed.origin} • ${breed.size} • ${breed.weight}",
                    color = Color(0xFF9AA4B2),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = breed.traits.firstOrNull() ?: breed.temperament,
                    color = Color(0xFFB7BFCC),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${breed.popularity}/10",
                color = catAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BreedDetailDialog(breed: CatBreed, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = catAccent.copy(alpha = 0.14f)) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = null,
                            tint = catAccent,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = breed.name,
                            color = Color(0xFFF7F9FC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "${breed.origin} • ${breed.size}",
                            color = Color(0xFF9AA4B2),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFFB7BFCC)
                        )
                    }
                }

                if (breed.contest) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catAccent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "Sering diikutsertakan kontes • popularitas ${breed.popularity}/10",
                            color = catAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                InfoRow("Berat", breed.weight)
                InfoRow("Umur", breed.lifespan)
                InfoRow("Temperamen", breed.temperament)

                SectionLabel("Warna bulu")
                Text(
                    text = breed.colors.joinToString(", ") { it.label },
                    color = Color(0xFFB7BFCC),
                    fontSize = 12.sp
                )

                SectionLabel("Pola bulu")
                Text(
                    text = breed.patterns.joinToString(", ") { it.label },
                    color = Color(0xFFB7BFCC),
                    fontSize = 12.sp
                )

                SectionLabel("Ciri fisik")
                breed.traits.forEach { trait ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "•",
                            color = catAccent,
                            fontSize = 12.sp,
                            modifier = Modifier.width(14.dp)
                        )
                        Text(
                            text = trait,
                            color = Color(0xFFB7BFCC),
                            fontSize = 12.sp
                        )
                    }
                }

                SectionLabel("Perawatan")
                Text(
                    text = breed.care,
                    color = Color(0xFFB7BFCC),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF9AA4B2),
            fontSize = 12.sp,
            modifier = Modifier.width(92.dp)
        )
        Text(
            text = value,
            color = Color(0xFFF7F9FC),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        color = catAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFF7F9FC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF9AA4B2),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF12151B),
                checkedTrackColor = catAccent
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        color = Color.White.copy(alpha = 0.08f)
    )
}

@Composable
private fun ImportDialog(
    result: ImportedResult?,
    analyzing: Boolean,
    error: String?,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (result != null) {
                AsyncImage(
                    model = File(result.imagePath),
                    contentDescription = "Foto yang dianalisis",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    analyzing -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = catAccent,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Menganalisis foto…",
                                    color = Color(0xFFF7F9FC),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    error != null -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = errorRed,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    color = Color(0xFFB7BFCC),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    result != null -> {
                        if (result.isCat) {
                            DetectionCard(
                                label = result.label ?: "Kucing",
                                confidence = result.confidence,
                                topClasses = result.topClasses,
                                breedMatches = result.breedMatches,
                                features = result.features,
                                modelLabel = result.modelLabel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            NotACatCard(
                                label = result.modelLabel ?: "tidak dikenali",
                                confidence = result.confidence,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onClose) {
                    Text("Tutup", color = Color(0xFFB7BFCC))
                }
            }

            FilledIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    items: List<HistoryItem>,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    var selected by remember { mutableStateOf<HistoryItem?>(null) }
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Riwayat deteksi",
                    color = Color(0xFFF7F9FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = errorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus", color = errorRed, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFFB7BFCC)
                    )
                }
            }

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pets,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Belum ada riwayat deteksi",
                        color = Color(0xFF9AA4B2),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        HistoryRow(item = item, onClick = { selected = item })
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }

    selected?.let { item ->
        HistoryDetailDialog(item = item, onClose = { selected = null })
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    val time = remember(item.timestamp) {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
            .format(Date(item.timestamp))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.photoPath != null) {
                    AsyncImage(
                        model = File(item.photoPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Pets,
                        contentDescription = null,
                        tint = catAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    color = Color(0xFFF7F9FC),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = time,
                    color = Color(0xFF9AA4B2),
                    fontSize = 11.sp
                )
                if (item.confidence > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.confidence.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(catAccent, CircleShape)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (item.confidence > 0f) {
                    "${(item.confidence * 100).roundToInt()}%"
                } else {
                    "foto"
                },
                color = catAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Buka detail riwayat",
                tint = Color(0xFF7E8898),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Dialog detail untuk satu entri riwayat: foto, label, keyakinan, dan waktu. */
@Composable
private fun HistoryDetailDialog(item: HistoryItem, onClose: () -> Unit) {
    var showFullPhoto by remember { mutableStateOf(false) }
    val time = remember(item.timestamp) {
        SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss", Locale("id", "ID"))
            .format(Date(item.timestamp))
    }
    val confidencePercent = "${"%.1f".format(item.confidence * 100f)}%"
    val photoFile = remember(item.photoPath) { item.photoPath?.let { File(it) } }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = catAccent.copy(alpha = 0.14f)) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = catAccent,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detail deteksi",
                            color = Color(0xFFF7F9FC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (photoFile != null) {
                                "Ketuk foto untuk melihat ukuran penuh"
                            } else {
                                "Deteksi tanpa foto tersimpan"
                            },
                            color = Color(0xFF9AA4B2),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFFB7BFCC)
                        )
                    }
                }

                if (photoFile != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = photoFile,
                        contentDescription = "Foto riwayat",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showFullPhoto = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.label,
                    color = Color(0xFFF7F9FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (item.confidence > 0f) {
                        "Keyakinan $confidencePercent"
                    } else {
                        "Tanpa skor keyakinan"
                    },
                    color = catAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.confidence > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfidenceBar(item.confidence)
                }

                Spacer(modifier = Modifier.height(16.dp))
                InfoRow("Waktu", time)
                InfoRow("Label", item.label)
                if (item.confidence > 0f) InfoRow("Keyakinan", confidencePercent)
                InfoRow(
                    "Sumber",
                    if (photoFile != null) "Foto tersimpan" else "Kamera (tanpa foto)"
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Catatan: kandidat breed dihitung saat deteksi dan tidak disimpan di riwayat.",
                    color = Color(0xFF9AA4B2),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (showFullPhoto) {
        photoFile?.let { file ->
            PhotoDialog(path = file.absolutePath, onClose = { showFullPhoto = false })
        }
    }
}

@Composable
private fun PhotoDialog(path: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onClose)
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = "Foto",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
            FilledIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup")
            }
        }
    }
}

private val catAccent = Color(0xFFFF7A1A)
private val catAccentDark = Color(0xFFE06010)
private val cardBg = Color(0xFF1A1D24)
private val successGreen = Color(0xFF4ADE80)
private val warningAmber = Color(0xFFFBBF24)
private val errorRed = Color(0xFFF87171)
