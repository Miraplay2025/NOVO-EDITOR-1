package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AspectRatioType
import com.example.data.model.ExportConfig
import com.example.data.model.MediaItem
import com.example.data.model.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.engine.RenderStateManager
import com.example.service.VideoRenderService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = ProjectRepository(db.projectDao())

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val renderUiState = RenderStateManager.state

    // Current project being created or edited
    var currentProjectId: Long = 0L
    var currentProjectTitle: String = "Novo Projeto de Vídeo"
    var currentAspectRatio: AspectRatioType = AspectRatioType.RATIO_16_9
    var currentMediaItems: List<MediaItem> = emptyList()
    var currentAudioUri: String? = null
    var currentAudioTitle: String? = null

    fun loadProject(project: ProjectEntity) {
        currentProjectId = project.id
        currentProjectTitle = project.title
        currentAspectRatio = AspectRatioType.values().find { it.label == project.aspectRatio } ?: AspectRatioType.RATIO_16_9
        currentMediaItems = ProjectRepository.deserializeMediaItems(project.mediaItemsJson)
        currentAudioUri = project.audioUri
        currentAudioTitle = project.audioTitle
    }

    fun startNewProject(
        mediaItems: List<MediaItem>,
        audioUri: String,
        audioTitle: String,
        aspectRatio: AspectRatioType
    ) {
        currentProjectId = 0L
        currentProjectTitle = "Projeto Automático ${System.currentTimeMillis() % 1000}"
        currentMediaItems = mediaItems
        currentAudioUri = audioUri
        currentAudioTitle = audioTitle
        currentAspectRatio = aspectRatio
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun executeAutoEdit(
        selectedCameras: List<Int>,
        cameraPrompt: String,
        selectedTransitions: List<Int>,
        individualTimings: Map<Int, Float>,
        exportConfig: ExportConfig
    ) {
        val totalSec = currentMediaItems.sumOf {
            (individualTimings[it.orderIndex] ?: it.durationSec).toDouble()
        }.toFloat()

        // Persist/update project in Room
        viewModelScope.launch {
            val entity = ProjectEntity(
                id = currentProjectId,
                title = currentProjectTitle,
                aspectRatio = currentAspectRatio.label,
                mediaItemsJson = ProjectRepository.serializeMediaItems(currentMediaItems),
                audioUri = currentAudioUri,
                audioTitle = currentAudioTitle,
                selectedCameraIdsJson = ProjectRepository.serializeIntList(selectedCameras),
                cameraPromptText = cameraPrompt,
                selectedTransitionIdsJson = ProjectRepository.serializeIntList(selectedTransitions),
                subtitlesEnabled = exportConfig.subtitlesEnabled,
                subtitleStyleId = exportConfig.subtitleStyleId,
                wordsPerSubtitle = exportConfig.wordsPerSubtitle,
                resolution = exportConfig.resolution,
                fps = exportConfig.fps,
                bitrate = exportConfig.bitrate,
                totalDurationSec = totalSec,
                thumbnailUri = currentMediaItems.firstOrNull()?.uri,
                modifiedAt = System.currentTimeMillis()
            )
            val savedId = repository.saveProject(entity)
            currentProjectId = savedId

            // Launch native Android Foreground Service
            VideoRenderService.startRender(
                context = getApplication(),
                mediaItems = currentMediaItems,
                audioUri = currentAudioUri,
                selectedCameraIds = selectedCameras,
                cameraPromptText = cameraPrompt,
                selectedTransitionIds = selectedTransitions,
                individualTimings = individualTimings,
                exportConfig = exportConfig,
                projectId = savedId
            )
        }
    }
}
