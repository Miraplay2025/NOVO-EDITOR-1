package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExportConfig
import com.example.data.model.MediaItem
import com.example.engine.CameraMovement
import com.example.engine.CameraPromptParser
import com.example.engine.PromptValidationResult
import com.example.engine.SubtitleStyle
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
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedEditDialog(
    mediaItems: List<MediaItem>,
    audioUri: String?,
    onDismiss: () -> Unit,
    onStartAutoEdit: (
        selectedCameras: List<Int>,
        cameraPrompt: String,
        selectedTransitions: List<Int>,
        individualTimings: Map<Int, Float>,
        exportConfig: ExportConfig
    ) -> Unit
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    val imageItems = remember(mediaItems) { mediaItems.filter { !it.isVideo } }

    // 6.2 Camera Squares (one square per image item, defaults to 0)
    val cameraSquares = remember(imageItems.size) {
        mutableStateListOf<Int>().apply {
            repeat(imageItems.size.coerceAtLeast(1)) { add(0) }
        }
    }

    // 6.3 Camera Text Prompt
    var promptText by remember {
        mutableStateOf(
            if (imageItems.isNotEmpty()) {
                CameraPromptParser.generateTemplate(mediaItems)
            } else ""
        )
    }

    // Check mutual exclusion: squares > 0 vs prompt
    val hasNonZeroSquares by remember {
        derivedStateOf { cameraSquares.any { it > 0 } }
    }
    val hasCustomPrompt by remember {
        derivedStateOf { promptText.trim().isNotEmpty() }
    }

    // Dynamic instant validation of prompt
    val promptValidationResult by remember(promptText, mediaItems) {
        derivedStateOf {
            if (promptText.trim().isEmpty()) PromptValidationResult.Empty
            else CameraPromptParser.validateAndParse(promptText, mediaItems)
        }
    }

    // Mutual exclusion error
    val mutualExclusionError by remember {
        derivedStateOf {
            if (hasNonZeroSquares && hasCustomPrompt) {
                "BLOQUEIO DE USO DUPLO: Você preencheu os quadradinhos e o prompt de texto simultaneamente. Escolha apenas um dos métodos (limpe os quadradinhos para 0 ou apague o texto do prompt)."
            } else null
        }
    }

    // 6.4 Transition Squares
    val transitionSquares = remember(mediaItems.size) {
        val numTransitions = (mediaItems.size - 1).coerceAtLeast(1)
        mutableStateListOf<Int>().apply {
            repeat(numTransitions) { add(1) } // Default to 1 (Crossfade)
        }
    }

    // 6.5 Global Quality
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf(30) }
    var selectedBitrate by remember { mutableStateOf("Médio") }

    // 6.6 Manual Timing & Audio Sync
    val individualTimings = remember {
        mutableStateMapOf<Int, Float>().apply {
            mediaItems.forEach { put(it.orderIndex, it.durationSec) }
        }
    }
    var audioSyncWordsText by remember {
        mutableStateOf("00:01 Olá 00:02 bem-vindo 00:03 ao 00:04 editor 00:05 automático")
    }

    // 6.7 Subtitles Configuration
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var selectedSubtitleStyleId by remember { mutableStateOf(1) } // Default Neon Glow
    var wordsPerSubtitle by remember { mutableStateOf(3) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("advanced_edit_dialog"),
            color = StudioDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header with Close 'X'
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Configuração de Edição",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_advanced_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                // 6.1 Clickable Help Text: "NÃO SABE COMO CONFIGURAR?"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "NÃO SABE COMO CONFIGURAR?",
                        color = StudioSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { showHelpDialog = true }
                            .padding(4.dp)
                            .testTag("btn_help_config")
                    )
                }

                // Main Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Mutual Exclusion Alert if both are used
                    mutualExclusionError?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioAccentRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioAccentRed, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = StudioAccentRed)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = err,
                                    color = Color(0xFFFECACA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 6.2 Seleção de Câmeras por Quadradinhos Responsivos
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "6.2 Câmeras por Quadradinhos Responsivos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Valor padrão 0. Cada número escolhido (1 a 6) é exclusivo por quadradinho:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (imageItems.isEmpty()) {
                                Text(
                                    text = "Não há imagens no projeto (apenas vídeos). Movimentos de câmera ignorados.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    cameraSquares.forEachIndexed { sqIdx, currentValue ->
                                        val imgItem = imageItems.getOrNull(sqIdx)
                                        CameraSquareSelector(
                                            index = sqIdx + 1,
                                            label = "Img ${imgItem?.orderIndex ?: (sqIdx + 1)}",
                                            currentValue = currentValue,
                                            allSelectedValues = cameraSquares.toList(),
                                            onValueSelected = { newValue ->
                                                cameraSquares[sqIdx] = newValue
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6.3 Entrada por Prompt de Texto
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "6.3 OU FORNEÇA-NOS UM PROMPT DOS MOVIMENTOS DESEJADOS",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Formato estrito: Imagem1=MOVIMENTO 2, (Linha por linha)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                placeholder = {
                                    Text(
                                        text = "Imagem1=MOVIMENTO 2,\nImagem2=MOVIMENTO 5,\nImagem3=MOVIMENTO 1,",
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .testTag("input_camera_prompt"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioTertiary,
                                    unfocusedBorderColor = Color(0xFF2A2D40),
                                    focusedContainerColor = Color(0xFF0F101A),
                                    unfocusedContainerColor = Color(0xFF0F101A)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Instant validation feedback
                            when (val res = promptValidationResult) {
                                is PromptValidationResult.Error -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = StudioAccentRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = res.message,
                                            color = StudioAccentRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                is PromptValidationResult.Success -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = StudioAccentGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Prompt sintaticamente válido! Todas imagens mapeadas.",
                                            color = StudioAccentGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                PromptValidationResult.Empty -> {}
                            }
                        }
                    }

                    // 6.4 Seleção de Transições por Quadradinhos
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "6.4 SELECIONE AS TRANSIÇÕES DESEJADAS",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Transições suaves entre mídias (Opções 0 a 15):",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                transitionSquares.forEachIndexed { tIdx, currentTransId ->
                                    TransitionSquareSelector(
                                        index = tIdx + 1,
                                        label = "T${tIdx + 1}",
                                        currentValue = currentTransId,
                                        onValueSelected = { newTransId ->
                                            transitionSquares[tIdx] = newTransId
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 6.5 Configurações Globais de Qualidade de Exportação
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "6.5 Configurações Globais de Exportação",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Resolução
                            Text("Resolução:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("240p", "480p", "720p", "1080p").forEach { res ->
                                    val isSel = selectedResolution == res
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) StudioPrimary else Color(0xFF161824))
                                            .clickable { selectedResolution = res }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = res,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSel) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Taxa de Quadros (FPS)
                            Text("Taxa de Quadros (FPS):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(24, 30, 60).forEach { fpsVal ->
                                    val isSel = selectedFps == fpsVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) StudioSecondary else Color(0xFF161824))
                                            .clickable { selectedFps = fpsVal }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$fpsVal FPS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSel) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Taxa de Bits (Bitrate)
                            Text("Taxa de Bits (Bitrate):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Baixo", "Médio", "Alto").forEach { br ->
                                    val isSel = selectedBitrate == br
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) StudioTertiary else Color(0xFF161824))
                                            .clickable { selectedBitrate = br }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = br,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSel) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6.6 Mapeamento Manual de Temporização de Mídias e Sincronização de Legendas
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "6.6 Temporização e Sincronização Local",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Campo 1: Tempo de Mídia Individual
                            Text(
                                text = "Campo 1 (Tempo de Mídia Individual em segundos):",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                mediaItems.forEach { mItem ->
                                    val currentTime = individualTimings[mItem.orderIndex] ?: mItem.durationSec
                                    Column(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF141624))
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Mídia ${mItem.orderIndex}", fontSize = 10.sp, color = TextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", currentTime)}s",
                                            fontWeight = FontWeight.Bold,
                                            color = StudioPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Campo 2: Sincronização do Áudio / Palavras por Tempo
                            Text(
                                text = "Campo 2 (Sincronização do Áudio / Palavras por Tempo):",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = audioSyncWordsText,
                                onValueChange = { audioSyncWordsText = it },
                                placeholder = { Text("00:01 Olá 00:02 bem-vindo...", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioSecondary,
                                    unfocusedBorderColor = Color(0xFF2A2D40),
                                    focusedContainerColor = Color(0xFF0F101A),
                                    unfocusedContainerColor = Color(0xFF0F101A)
                                )
                            )
                        }
                    }

                    // 6.7 Configuração de Legendas Automáticas
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
                                Column {
                                    Text(
                                        text = "6.7 Legendas Automáticas",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "ATIVAR LEGENDAS AUTOMÁTICAS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (subtitlesEnabled) StudioAccentGreen else TextMuted
                                    )
                                }
                                Switch(
                                    checked = subtitlesEnabled,
                                    onCheckedChange = { subtitlesEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = StudioAccentGreen
                                    ),
                                    modifier = Modifier.testTag("toggle_auto_subtitles")
                                )
                            }

                            if (subtitlesEnabled) {
                                Spacer(modifier = Modifier.height(14.dp))

                                // Palavras por Vez na Tela (3 a 8 palavras, padrão 3)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Palavras por Vez na Tela:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "$wordsPerSubtitle palavras",
                                        fontWeight = FontWeight.Bold,
                                        color = StudioSecondary,
                                        fontSize = 14.sp
                                    )
                                }

                                Slider(
                                    value = wordsPerSubtitle.toFloat(),
                                    onValueChange = { wordsPerSubtitle = it.toInt() },
                                    valueRange = 3f..8f,
                                    steps = 4,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioSecondary,
                                        activeTrackColor = StudioSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "6 Estilos de Legendas (Apenas 1 ativo):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 6 Distinct Subtitle Style Cards
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SubtitleStyle.ALL.forEach { style ->
                                        val isSel = selectedSubtitleStyleId == style.id
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSel) StudioPrimary.copy(alpha = 0.2f) else Color(0xFF131522)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .width(130.dp)
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) StudioPrimary else Color(0xFF282B3E),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedSubtitleStyleId = style.id }
                                                .testTag("subtitle_style_${style.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.6f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = style.sampleText,
                                                        color = style.textColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = style.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSel) StudioPrimary else TextPrimary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Final Action: EDITAR AUTOMATICAMENTE
                Spacer(modifier = Modifier.height(12.dp))

                val canStart = mutualExclusionError == null &&
                        (promptValidationResult !is PromptValidationResult.Error)

                Button(
                    onClick = {
                        val exportConfig = ExportConfig(
                            resolution = selectedResolution,
                            fps = selectedFps,
                            bitrate = selectedBitrate,
                            subtitlesEnabled = subtitlesEnabled,
                            subtitleStyleId = selectedSubtitleStyleId,
                            wordsPerSubtitle = wordsPerSubtitle,
                            subtitleTimingText = audioSyncWordsText
                        )
                        onStartAutoEdit(
                            cameraSquares.toList(),
                            if (hasCustomPrompt) promptText else "",
                            transitionSquares.toList(),
                            individualTimings.toMap(),
                            exportConfig
                        )
                    },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canStart) StudioPrimary else Color(0xFF374151)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_execute_auto_edit")
                ) {
                    Icon(Icons.Default.MovieCreation, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "EDITAR AUTOMATICAMENTE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Mini Help Dialog ("NÃO SABE COMO CONFIGURAR?")
    if (showHelpDialog) {
        HelpConfigDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun CameraSquareSelector(
    index: Int,
    label: String,
    currentValue: Int,
    allSelectedValues: List<Int>,
    onValueSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (currentValue > 0) StudioPrimary.copy(alpha = 0.25f) else Color(0xFF151722))
                .border(
                    width = if (currentValue > 0) 1.5.dp else 1.dp,
                    color = if (currentValue > 0) StudioPrimary else Color(0xFF282C40),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(8.dp)
                .testTag("camera_square_$index")
        ) {
            Text(label, fontSize = 10.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currentValue",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentValue > 0) StudioSecondary else TextPrimary
            )
            Text(
                text = if (currentValue == 0) "Estático" else "Move $currentValue",
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(StudioSurface)
        ) {
            for (opt in 0..6) {
                // Rule: 0 is always allowed. 1..6 are exclusive (disabled if chosen elsewhere)
                val isAlreadyChosenElsewhere = opt > 0 && allSelectedValues.contains(opt) && opt != currentValue

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$opt - ${CameraMovement.getById(opt).name}",
                                color = if (isAlreadyChosenElsewhere) TextMuted else TextPrimary,
                                fontSize = 13.sp
                            )
                            if (isAlreadyChosenElsewhere) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(Em uso)", color = StudioAccentRed, fontSize = 10.sp)
                            }
                        }
                    },
                    enabled = !isAlreadyChosenElsewhere,
                    onClick = {
                        onValueSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TransitionSquareSelector(
    index: Int,
    label: String,
    currentValue: Int,
    onValueSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (currentValue > 0) StudioSecondary.copy(alpha = 0.2f) else Color(0xFF151722))
                .border(
                    width = 1.dp,
                    color = if (currentValue > 0) StudioSecondary else Color(0xFF282C40),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(8.dp)
                .testTag("transition_square_$index")
        ) {
            Text(label, fontSize = 10.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currentValue",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentValue > 0) StudioSecondary else TextPrimary
            )
            Text(
                text = if (currentValue == 0) "Corte" else "Trans $currentValue",
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(StudioSurface)
        ) {
            for (t in 0..15) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "$t - ${VideoTransition.getById(t).name}",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onValueSelected(t)
                        expanded = false
                    }
                )
            }
        }
    }
}
