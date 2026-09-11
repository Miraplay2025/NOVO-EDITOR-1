package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val aspectRatio: String = "16:9",
    val mediaItemsJson: String = "[]",
    val audioUri: String? = null,
    val audioTitle: String? = null,
    val selectedCameraIdsJson: String = "[]",
    val cameraPromptText: String = "",
    val selectedTransitionIdsJson: String = "[]",
    val subtitlesEnabled: Boolean = true,
    val subtitleStyleId: Int = 1,
    val wordsPerSubtitle: Int = 3,
    val individualTimingsJson: String = "{}",
    val subtitleTimingText: String = "",
    val resolution: String = "1080p",
    val fps: Int = 30,
    val bitrate: String = "Médio",
    val totalDurationSec: Float = 0f,
    val thumbnailUri: String? = null,
    val modifiedAt: Long = System.currentTimeMillis()
)
