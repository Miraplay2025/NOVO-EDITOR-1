package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AspectRatioType
import com.example.data.model.MediaItem
import com.example.engine.CameraMovement
import com.example.engine.VideoTransition
import com.example.ui.theme.StudioAccentGreen
import com.example.ui.theme.StudioAccentRed
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTertiary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

enum class BottomBarMode {
    DEFAULT,
    AUDIO_SELECTED,
    MEDIA_SELECTED
}

enum class ToolPanel {
    NONE,
    CAMERA_MOVEMENTS,
    TRANSITIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorWorkspaceScreen(
    initialMediaItems: List<MediaItem>,
    initialAudioUri: String,
    initialAudioTitle: String,
    initialAspectRatio: AspectRatioType,
    onBack: () -> Unit,
    onOpenAdvancedEdit: (List<MediaItem>, String, String, AspectRatioType) -> Unit
) {
    val mediaItems = remember {
        mutableStateListOf<MediaItem>().apply {
            addAll(initialMediaItems)
        }
    }

    var audioUri by remember { mutableStateOf(initialAudioUri) }
    var audioTitle by remember { mutableStateOf(initialAudioTitle) }
    var currentAspectRatio by remember { mutableStateOf(initialAspectRatio) }

    // Playback state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackSec by remember { mutableFloatStateOf(0f) }
    val totalDurationSec by remember {
        derivedStateOf {
            mediaItems.sumOf { it.durationSec.toDouble() }.toFloat().coerceAtLeast(1f)
        }
    }

    // Dynamic Bottom Bar Context
    var bottomBarMode by remember { mutableStateOf(BottomBarMode.DEFAULT) }
    var selectedMediaIndex by remember { mutableIntStateOf(-1) }

    // Sliding Tool Panels (5.1 Camera panel / 5.2 Transition panel)
    var activeToolPanel by remember { mutableStateOf(ToolPanel.NONE) }

    // Live preview camera animation and transition
    var activePreviewCameraId by remember { mutableIntStateOf(0) }
    var activePreviewTransitionId by remember { mutableIntStateOf(0) }

    // Single Media replacer launcher
    val singleMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedMediaIndex in 0 until mediaItems.size) {
            val uriStr = uri.toString()
            val isVid = uriStr.contains("video", ignoreCase = true)
            val old = mediaItems[selectedMediaIndex]
            mediaItems[selectedMediaIndex] = old.copy(
                uri = uriStr,
                isVideo = isVid,
                durationSec = if (isVid) 8f else 4f
            )
        }
    }

    // Audio replacer launcher
    val audioReplacerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri.toString()
            audioTitle = uri.lastPathSegment ?: "Nova Trilha Sonora.mp3"
        }
    }

    // Playback loop ticker
    LaunchedEffect(isPlaying, totalDurationSec) {
        if (isPlaying) {
            while (isPlaying) {
                delay(100)
                currentPlaybackSec += 0.1f
                if (currentPlaybackSec >= totalDurationSec) {
                    currentPlaybackSec = 0f
                }
            }
        }
    }

    // Calculate current active media based on playhead position
    val currentActiveMediaIndex by remember(currentPlaybackSec, mediaItems.size) {
        derivedStateOf {
            var acc = 0f
            var found = 0
            for ((idx, item) in mediaItems.withIndex()) {
                acc += item.durationSec
                if (currentPlaybackSec <= acc) {
                    found = idx
                    break
                }
            }
            found.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = StudioDarkBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = "Workspace de Edição",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                actions = {
                    // Botão "INICIAR EDIÇÃO" no canto superior direito
                    Button(
                        onClick = {
                            onOpenAdvancedEdit(
                                mediaItems.toList(),
                                audioUri,
                                audioTitle,
                                currentAspectRatio
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_start_editing_top")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_start_editing),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            // 4.3 Gestão Dinâmica de Áudio e Mídias (Barra Inferior Multifuncional)
            Surface(
                color = StudioSurface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (bottomBarMode) {
                    BottomBarMode.AUDIO_SELECTED -> {
                        // Ao clicar na faixa de Áudio: botão "Alterar Áudio" + Seta de Voltar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { bottomBarMode = BottomBarMode.DEFAULT }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Áudio Selecionado: $audioTitle",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StudioSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                maxLines = 1
                            )
                            Button(
                                onClick = { audioReplacerLauncher.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_change_audio")
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.change_audio), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    BottomBarMode.MEDIA_SELECTED -> {
                        // Ao clicar em uma Mídia específica: "Excluir Mídia" + "Alterar Mídia" + Seta de Voltar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                bottomBarMode = BottomBarMode.DEFAULT
                                selectedMediaIndex = -1
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "Mídia ${selectedMediaIndex + 1} selecionada",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (selectedMediaIndex in 0 until mediaItems.size) {
                                            mediaItems.removeAt(selectedMediaIndex)
                                            // Reindex remaining
                                            val reindexed = mediaItems.mapIndexed { idx, itm -> itm.copy(orderIndex = idx + 1) }
                                            mediaItems.clear()
                                            mediaItems.addAll(reindexed)
                                            bottomBarMode = BottomBarMode.DEFAULT
                                            selectedMediaIndex = -1
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_delete_selected_media")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.delete_media), fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { singleMediaLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_replace_selected_media")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.change_media), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    BottomBarMode.DEFAULT -> {
                        // Quick Studio Tool Triggers: Camera movements & Transitions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Camera Movements Button (Section 5.1)
                            OutlinedButton(
                                onClick = {
                                    activeToolPanel = if (activeToolPanel == ToolPanel.CAMERA_MOVEMENTS) ToolPanel.NONE else ToolPanel.CAMERA_MOVEMENTS
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (activeToolPanel == ToolPanel.CAMERA_MOVEMENTS) StudioPrimary else TextPrimary
                                ),
                                modifier = Modifier.testTag("btn_camera_movements_panel")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Câmera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Visual Transitions Button (Section 5.2)
                            OutlinedButton(
                                onClick = {
                                    activeToolPanel = if (activeToolPanel == ToolPanel.TRANSITIONS) ToolPanel.NONE else ToolPanel.TRANSITIONS
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (activeToolPanel == ToolPanel.TRANSITIONS) StudioSecondary else TextPrimary
                                ),
                                modifier = Modifier.testTag("btn_transitions_panel")
                            ) {
                                Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transições", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4.2 Top buttons for the 4 Aspect Ratios (can change aspect ratio anytime)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurface)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AspectRatioType.values().forEach { rType ->
                    val isCurrent = currentAspectRatio == rType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) StudioPrimary else Color.Transparent)
                            .clickable { currentAspectRatio = rType }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rType.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isCurrent) Color.White else TextSecondary
                        )
                    }
                }
            }

            // 4.2 Reprodutor de Pré-visualização (Player Central)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07080D)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF23263B), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activeMedia = mediaItems.getOrNull(currentActiveMediaIndex)

                    // Canvas Central with Aspect Ratio
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .aspectRatio(currentAspectRatio.ratio)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141624)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeMedia != null) {
                                MediaPreviewDisplay(
                                    mediaItem = activeMedia,
                                    cameraMovementId = activePreviewCameraId,
                                    transitionId = activePreviewTransitionId,
                                    isPlaying = isPlaying
                                )
                            } else {
                                Text("Nenhuma mídia", color = TextMuted)
                            }
                        }

                        // Controles de Reprodução: Botão central de Play/Pause sobre a mídia
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { isPlaying = !isPlaying }
                                .testTag("btn_play_pause_player"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Timecode badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%04.1f", currentPlaybackSec)}s / ${String.format(Locale.US, "%04.1f", totalDurationSec)}s",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // Indicador de Linha do Tempo (Playhead): Linha visual com agulha
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color(0xFF111320))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background ruler line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xFF2D314A))
                        )

                        // Progress filled line
                        val progressFraction = (currentPlaybackSec / totalDurationSec).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(2.dp)
                                .background(StudioSecondary)
                        )

                        // Playhead needle / arrow
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val needleOffset = maxWidth * progressFraction
                            Box(
                                modifier = Modifier
                                    .offset(x = needleOffset - 6.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(StudioSecondary)
                            )
                        }
                    }
                }
            }

            // Interactive Timeline Tracks (Mídias e Áudio)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Linha do Tempo Multitrack",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                // Track 1: Mídias
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(mediaItems, key = { _, itm -> itm.id }) { idx, item ->
                        val isCurrentActive = idx == currentActiveMediaIndex
                        val isSelected = selectedMediaIndex == idx && bottomBarMode == BottomBarMode.MEDIA_SELECTED

                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141624))
                                .border(
                                    width = if (isSelected) 2.dp else if (isCurrentActive) 1.5.dp else 1.dp,
                                    color = if (isSelected) StudioPrimary else if (isCurrentActive) StudioSecondary else Color(0xFF282B40),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedMediaIndex = idx
                                    bottomBarMode = BottomBarMode.MEDIA_SELECTED
                                }
                                .testTag("timeline_media_item_$idx")
                        ) {
                            if (item.uri.startsWith("content://") || item.uri.startsWith("file://")) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (item.isVideo) StudioSecondary else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${idx + 1} • ${item.durationSec.toInt()}s",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Track 2: Faixa de Áudio (Clicar abre controles na barra inferior)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (bottomBarMode == BottomBarMode.AUDIO_SELECTED) StudioSecondary.copy(alpha = 0.25f)
                            else Color(0xFF0F1A24)
                        )
                        .border(
                            width = if (bottomBarMode == BottomBarMode.AUDIO_SELECTED) 1.5.dp else 1.dp,
                            color = if (bottomBarMode == BottomBarMode.AUDIO_SELECTED) StudioSecondary else Color(0xFF1E3A4A),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            bottomBarMode = BottomBarMode.AUDIO_SELECTED
                            selectedMediaIndex = -1
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = StudioSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trilha Sonora: $audioTitle",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StudioSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // 5.1 Painel de Movimentos de Câmera (Quando ativo)
            AnimatedVisibility(visible = activeToolPanel == ToolPanel.CAMERA_MOVEMENTS) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = StudioPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "5.1 Movimentos de Câmera",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Aplica-se EXCLUSIVAMENTE a imagens, NUNCA a vídeos!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioAccentRed
                                    )
                                }
                            }
                            IconButton(onClick = { activeToolPanel = ToolPanel.NONE }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(CameraMovement.ALL) { _, movement ->
                                val isSelected = activePreviewCameraId == movement.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) StudioPrimary.copy(alpha = 0.3f) else Color(0xFF141624)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) StudioPrimary else Color(0xFF282B40),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            activePreviewCameraId = movement.id
                                        }
                                        .testTag("preview_camera_movement_${movement.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${movement.id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isSelected) StudioSecondary else Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = movement.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5.2 Painel de Transições Visuais Suaves (Quando ativo)
            AnimatedVisibility(visible = activeToolPanel == ToolPanel.TRANSITIONS) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Transform, contentDescription = null, tint = StudioSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "5.2 Catálogo de 15 Transições Suaves",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Cortes suaves, fades sutis e dissoluções",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioSecondary
                                    )
                                }
                            }
                            IconButton(onClick = { activeToolPanel = ToolPanel.NONE }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(VideoTransition.ALL) { _, transition ->
                                val isSelected = activePreviewTransitionId == transition.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) StudioSecondary.copy(alpha = 0.25f) else Color(0xFF141624)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(135.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) StudioSecondary else Color(0xFF282B40),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            activePreviewTransitionId = transition.id
                                        }
                                        .testTag("preview_transition_${transition.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${transition.id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isSelected) StudioSecondary else Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = transition.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaPreviewDisplay(
    mediaItem: MediaItem,
    cameraMovementId: Int,
    transitionId: Int,
    isPlaying: Boolean
) {
    // Animate camera movements exclusively for images
    val panAnim = remember { Animatable(0f) }
    val zoomAnim = remember { Animatable(1f) }

    LaunchedEffect(cameraMovementId, isPlaying) {
        if (!mediaItem.isVideo && isPlaying && cameraMovementId > 0) {
            when (cameraMovementId) {
                1 -> { // Pan Left
                    panAnim.animateTo(
                        targetValue = -30f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse)
                    )
                }
                2 -> { // Pan Right
                    panAnim.animateTo(
                        targetValue = 30f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse)
                    )
                }
                5 -> { // Zoom In
                    zoomAnim.animateTo(
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse)
                    )
                }
                6 -> { // Zoom Out
                    zoomAnim.snapTo(1.25f)
                    zoomAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse)
                    )
                }
                else -> {
                    panAnim.snapTo(0f)
                    zoomAnim.snapTo(1f)
                }
            }
        } else {
            panAnim.snapTo(0f)
            zoomAnim.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = panAnim.value
                scaleX = zoomAnim.value
                scaleY = zoomAnim.value
            },
        contentAlignment = Alignment.Center
    ) {
        if (mediaItem.uri.startsWith("content://") || mediaItem.uri.startsWith("file://")) {
            AsyncImage(
                model = mediaItem.uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Built-in studio sample display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            if (mediaItem.isVideo) listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                            else listOf(Color(0xFF064E3B), Color(0xFF047857))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (mediaItem.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (mediaItem.isVideo) "Mídia ${mediaItem.orderIndex} (Vídeo Original)" else "Mídia ${mediaItem.orderIndex} (Imagem 4s)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Overlay active transition name if applied
        if (transitionId > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Transição ${transitionId}",
                    color = StudioSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
