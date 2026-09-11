package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.RenderStateManager
import com.example.engine.RenderUiState
import com.example.ui.theme.StudioAccentGreen
import com.example.ui.theme.StudioAccentRed
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RenderProgressDialog(
    renderState: RenderUiState,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll logs to bottom on new entries
    LaunchedEffect(renderState.logs.size) {
        if (renderState.logs.isNotEmpty()) {
            listState.animateScrollToItem(renderState.logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!renderState.isRendering) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !renderState.isRendering,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("render_progress_dialog"),
            color = StudioDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        if (renderState.isCompleted) listOf(StudioAccentGreen, Color(0xFF059669))
                                        else if (renderState.errorMessage != null) listOf(StudioAccentRed, Color(0xFF991B1B))
                                        else listOf(StudioPrimary, StudioSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (renderState.isCompleted) Icons.Default.CheckCircle
                                else if (renderState.errorMessage != null) Icons.Default.Error
                                else Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Renderização On-Device",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (renderState.isRendering) "Processando em Segundo Plano..."
                                else if (renderState.isCompleted) "Processamento Finalizado!"
                                else if (renderState.errorMessage != null) "Falha Detectada"
                                else "Pronto",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (renderState.isCompleted) StudioAccentGreen
                                else if (renderState.errorMessage != null) StudioAccentRed
                                else StudioSecondary
                            )
                        }
                    }

                    // Ícone de Lixeira no canto superior do console de logs
                    IconButton(
                        onClick = { RenderStateManager.clearLogs() },
                        modifier = Modifier.testTag("btn_clear_logs")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpar logs",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Bar Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
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
                                text = renderState.statusMessage.ifBlank { "Aguardando início..." },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = "${(renderState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { renderState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (renderState.isCompleted) StudioAccentGreen else StudioPrimary,
                            trackColor = Color(0xFF1E2135)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Console de Logs Detalhados em Tempo Real
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Console de Logs Detalhados (${renderState.logs.size} eventos)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF090A10))
                        .border(1.dp, Color(0xFF202334), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    if (renderState.logs.isEmpty()) {
                        Text(
                            text = "Nenhum log registrado.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(renderState.logs, key = { it.id }) { log ->
                                Text(
                                    text = log.formatted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    color = if (log.isError) StudioAccentRed
                                    else if (log.message.contains("concluído", ignoreCase = true) || log.message.contains("sucesso", ignoreCase = true)) StudioAccentGreen
                                    else if (log.message.contains("Passo", ignoreCase = true)) StudioSecondary
                                    else TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Success or Error banners & Action Buttons
                Spacer(modifier = Modifier.height(16.dp))

                if (renderState.isCompleted) {
                    // Success Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioAccentGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Vídeo Compilado com Sucesso!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Arquivo salvo na Galeria / Armazenamento do dispositivo.",
                                    color = Color(0xFFD1FAE5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (renderState.errorMessage != null) {
                    // Error Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = StudioAccentRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = renderState.errorMessage,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Botão "Voltar" para fechar a tela (como especificado em 7.3 Passo 5)
                Button(
                    onClick = onDismiss,
                    enabled = !renderState.isRendering,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (renderState.isCompleted) StudioAccentGreen else StudioPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_return_from_render")
                ) {
                    Text(
                        text = if (renderState.isRendering) "Processando em Segundo Plano..." else "Voltar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
