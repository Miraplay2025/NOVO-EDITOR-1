package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AspectRatioType
import com.example.data.model.MediaItem
import com.example.ui.theme.StudioAccentGreen
import com.example.ui.theme.StudioAccentRed
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTertiary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    initialMedia: List<MediaItem> = emptyList(),
    initialAudioUri: String? = null,
    initialAudioTitle: String? = null,
    initialAspectRatio: AspectRatioType? = null,
    onBack: () -> Unit,
    onAdvanceToEditor: (List<MediaItem>, String, String, AspectRatioType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val mediaItems = remember {
        mutableStateListOf<MediaItem>().apply {
            addAll(initialMedia)
        }
    }

    var selectedAudioUri by remember { mutableStateOf<String?>(initialAudioUri) }
    var selectedAudioTitle by remember { mutableStateOf<String?>(initialAudioTitle) }
    var selectedAspectRatio by remember { mutableStateOf<AspectRatioType?>(initialAspectRatio) }

    // Media file picker (Photos and Videos)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val uriStr = uri.toString()
                val isVid = uriStr.contains("video", ignoreCase = true)
                val newItem = MediaItem(
                    uri = uriStr,
                    isVideo = isVid,
                    durationSec = if (isVid) 8.0f else 4.0f,
                    originalDurationSec = if (isVid) 8.0f else 4.0f,
                    orderIndex = mediaItems.size + 1
                )
                mediaItems.add(newItem)
            }
            // Auto detect aspect ratio from 1st media if not manually chosen
            if (selectedAspectRatio == null && mediaItems.isNotEmpty()) {
                selectedAspectRatio = AspectRatioType.RATIO_16_9
            }
        }
    }

    // Audio file picker (MP3, WAV, AAC, M4A)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri.toString()
            selectedAudioTitle = uri.lastPathSegment ?: "Trilha Sonora Selecionada.mp3"
        }
    }

    // Function to re-index all remaining items dynamically in real time
    fun reindexMediaItems() {
        val updated = mediaItems.mapIndexed { index, item ->
            item.copy(orderIndex = index + 1)
        }
        mediaItems.clear()
        mediaItems.addAll(updated)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        text = stringResource(R.string.step_import_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 3.3 Aspect Ratio Selectors on Top
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3.3 Proporção de Aspecto (Aspect Ratio)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = StudioSecondary
                        )
                        if (selectedAspectRatio == null) {
                            Text(
                                text = "Auto (1ª mídia)",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatioType.values().forEach { ratio ->
                            val isSelected = (selectedAspectRatio ?: AspectRatioType.RATIO_16_9) == ratio
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) StudioPrimary.copy(alpha = 0.25f) else Color(0xFF141622)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) StudioPrimary else Color(0xFF26293D),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedAspectRatio = ratio }
                                    .testTag("ratio_button_${ratio.label.replace(':', '_')}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ratio.label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) StudioPrimary else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (ratio) {
                                            AspectRatioType.RATIO_16_9 -> "Horizontal"
                                            AspectRatioType.RATIO_9_16 -> "Vertical"
                                            AspectRatioType.RATIO_1_1 -> "Quadrado"
                                            AspectRatioType.RATIO_4_5 -> "Retrato"
                                        },
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3.1 Botões de Upload Principais
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "3.1 Importação Obrigatória de Arquivos",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    // Botão 1: Carregar Mídias (Imagens e Vídeos)
                    Button(
                        onClick = {
                            mediaPickerLauncher.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_load_media")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Botão 1: Carregar Mídias (Imagens e Vídeos)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Botão 2: Carregar Áudio
                    Button(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAudioUri != null) Color(0xFF0F766E) else StudioSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_load_audio")
                    ) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedAudioUri != null) "Áudio Carregado: ${selectedAudioTitle ?: "Faixa 1"}" else "Botão 2: Carregar Áudio (MP3, WAV, AAC, M4A)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }

                    // Emulator Quick Presets (convenience bar to test instantly without waiting for gallery files)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ou use amostras de estúdio para teste rápido:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        TextButton(
                            onClick = {
                                if (mediaItems.isEmpty()) {
                                    mediaItems.addAll(
                                        listOf(
                                            MediaItem(uri = "sample_image_1", isVideo = false, durationSec = 4f, originalDurationSec = 4f, orderIndex = 1),
                                            MediaItem(uri = "sample_video_1", isVideo = true, durationSec = 6f, originalDurationSec = 6f, orderIndex = 2),
                                            MediaItem(uri = "sample_image_2", isVideo = false, durationSec = 4f, originalDurationSec = 4f, orderIndex = 3)
                                        )
                                    )
                                    reindexMediaItems()
                                }
                                if (selectedAudioUri == null) {
                                    selectedAudioUri = "sample_soundtrack_ambient.mp3"
                                    selectedAudioTitle = "Trilha Sonora Automática (Lo-Fi Beat).mp3"
                                }
                            }
                        ) {
                            Text("+ Amostras", color = StudioSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3.2 Visualização em Carrossel / Grid Horizontal com Numeração e Botão 'X'
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3.2 Mídias Selecionadas (${mediaItems.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (mediaItems.isNotEmpty()) {
                        TextButton(onClick = { mediaItems.clear() }) {
                            Text("Limpar Tudo", color = StudioAccentRed, fontSize = 12.sp)
                        }
                    }
                }

                if (mediaItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StudioCardBg)
                            .border(1.dp, Color(0xFF2A2D40), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma mídia carregada ainda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(mediaItems, key = { _, item -> item.id }) { index, item ->
                            MediaThumbnailCard(
                                item = item,
                                onRemove = {
                                    mediaItems.removeAt(index)
                                    reindexMediaItems()
                                }
                            )
                        }
                    }
                }
            }

            // Audio Status card
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedAudioUri != null) StudioAccentGreen.copy(alpha = 0.2f) else StudioCardBg
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (selectedAudioUri != null) StudioAccentGreen else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Faixa de Áudio",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = selectedAudioTitle ?: "Nenhum áudio selecionado (Obrigatório)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selectedAudioUri != null) TextPrimary else StudioAccentRed,
                            maxLines = 1
                        )
                    }

                    if (selectedAudioUri != null) {
                        IconButton(onClick = {
                            selectedAudioUri = null
                            selectedAudioTitle = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remover Áudio", tint = StudioAccentRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Avançar para o Editor (Validando rigorosamente regra obrigatória)
            Button(
                onClick = {
                    if (mediaItems.isEmpty() || selectedAudioUri.isNullOrBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "O carregamento de mídias e de áudio é estritamente OBRIGATÓRIO para avançar."
                            )
                        }
                    } else {
                        val finalRatio = selectedAspectRatio ?: AspectRatioType.RATIO_16_9
                        onAdvanceToEditor(
                            mediaItems.toList(),
                            selectedAudioUri!!,
                            selectedAudioTitle ?: "Trilha Sonora",
                            finalRatio
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mediaItems.isNotEmpty() && !selectedAudioUri.isNullOrBlank()) {
                        StudioPrimary
                    } else {
                        Color(0xFF374151)
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_advance_editor")
            ) {
                Text(
                    text = stringResource(R.string.btn_advance_editor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun MediaThumbnailCard(
    item: MediaItem,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF151722))
            .border(1.5.dp, Color(0xFF2A2D42), RoundedCornerShape(14.dp))
    ) {
        // Media representation
        if (item.uri.startsWith("content://") || item.uri.startsWith("file://") || item.uri.startsWith("http")) {
            AsyncImage(
                model = item.uri,
                contentDescription = "Mídia ${item.orderIndex}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Built-in sample representation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            if (item.isVideo) listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                            else listOf(Color(0xFF1E293B), Color(0xFF334155))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                    contentDescription = null,
                    tint = if (item.isVideo) StudioSecondary else Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Sequential Numeric Badge (1, 2, 3...) on corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioPrimary)
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${item.orderIndex}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Type badge (Video or Photo duration)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (item.isVideo) "VÍDEO" else "${item.durationSec.toInt()}s",
                color = if (item.isVideo) StudioSecondary else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 'X' Individual Removal Button in top corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(StudioAccentRed)
                .clickable { onRemove() }
                .testTag("btn_remove_media_${item.orderIndex}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remover Mídia ${item.orderIndex}",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
