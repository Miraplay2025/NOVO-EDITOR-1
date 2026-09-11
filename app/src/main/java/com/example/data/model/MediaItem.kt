package com.example.data.model

data class MediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String,
    val isVideo: Boolean = false,
    val durationSec: Float = if (isVideo) 8.0f else 4.0f,
    val originalDurationSec: Float = if (isVideo) 8.0f else 4.0f,
    val orderIndex: Int = 1,
    val cameraMovementId: Int = 0,
    val transitionId: Int = 0
)

enum class AspectRatioType(val label: String, val ratio: Float, val description: String) {
    RATIO_16_9("16:9", 16f / 9f, "Widescreen Horizontal (YouTube, TV)"),
    RATIO_9_16("9:16", 9f / 16f, "Vertical Full Screen (Shorts, Reels, TikTok)"),
    RATIO_1_1("1:1", 1f, "Quadrado (Feed Instagram / Facebook)"),
    RATIO_4_5("4:5", 4f / 5f, "Vertical Retrato (Feed Instagram / Facebook)")
}

data class ExportConfig(
    val resolution: String = "1080p",
    val fps: Int = 30,
    val bitrate: String = "Médio",
    val subtitlesEnabled: Boolean = true,
    val subtitleStyleId: Int = 2,
    val wordsPerSubtitle: Int = 3,
    val subtitleTimingText: String = "00:01 Olá 00:02 bem-vindo 00:03 ao 00:04 editor 00:05 automático"
)
