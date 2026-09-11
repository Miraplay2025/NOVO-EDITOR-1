package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.DashboardScreen
import com.example.ui.EditorViewModel
import com.example.ui.EditorWorkspaceScreen
import com.example.ui.MediaPickerScreen
import com.example.ui.SplashScreen
import com.example.ui.dialogs.AdvancedEditDialog
import com.example.ui.dialogs.RenderProgressDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioDarkBg

enum class ScreenState {
    SPLASH,
    DASHBOARD,
    MEDIA_PICKER,
    EDITOR_WORKSPACE
}

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioDarkBg
                ) {
                    AutoEditorApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun AutoEditorApp(viewModel: EditorViewModel) {
    var currentScreen by remember { mutableStateOf(ScreenState.SPLASH) }

    val projects by viewModel.allProjects.collectAsState()
    val renderState by viewModel.renderUiState.collectAsState()

    var showAdvancedEditDialog by remember { mutableStateOf(false) }
    var showRenderProgressDialog by remember { mutableStateOf(false) }

    // Intercept back presses based on active screen
    BackHandler(enabled = currentScreen != ScreenState.DASHBOARD && currentScreen != ScreenState.SPLASH) {
        when (currentScreen) {
            ScreenState.MEDIA_PICKER -> currentScreen = ScreenState.DASHBOARD
            ScreenState.EDITOR_WORKSPACE -> currentScreen = ScreenState.DASHBOARD
            else -> {}
        }
    }

    when (currentScreen) {
        ScreenState.SPLASH -> {
            SplashScreen(
                onInitialized = {
                    currentScreen = ScreenState.DASHBOARD
                }
            )
        }

        ScreenState.DASHBOARD -> {
            DashboardScreen(
                projects = projects,
                onCreateNewProject = {
                    currentScreen = ScreenState.MEDIA_PICKER
                },
                onOpenProject = { project ->
                    viewModel.loadProject(project)
                    currentScreen = ScreenState.EDITOR_WORKSPACE
                },
                onDeleteProject = { id ->
                    viewModel.deleteProject(id)
                }
            )
        }

        ScreenState.MEDIA_PICKER -> {
            MediaPickerScreen(
                initialMedia = viewModel.currentMediaItems,
                initialAudioUri = viewModel.currentAudioUri,
                initialAudioTitle = viewModel.currentAudioTitle,
                initialAspectRatio = viewModel.currentAspectRatio,
                onBack = { currentScreen = ScreenState.DASHBOARD },
                onAdvanceToEditor = { mediaItems, audioUri, audioTitle, ratio ->
                    viewModel.startNewProject(mediaItems, audioUri, audioTitle, ratio)
                    currentScreen = ScreenState.EDITOR_WORKSPACE
                }
            )
        }

        ScreenState.EDITOR_WORKSPACE -> {
            EditorWorkspaceScreen(
                initialMediaItems = viewModel.currentMediaItems,
                initialAudioUri = viewModel.currentAudioUri ?: "",
                initialAudioTitle = viewModel.currentAudioTitle ?: "Trilha Sonora",
                initialAspectRatio = viewModel.currentAspectRatio,
                onBack = { currentScreen = ScreenState.DASHBOARD },
                onOpenAdvancedEdit = { updatedMedia, updatedAudio, updatedAudioTitle, updatedRatio ->
                    viewModel.currentMediaItems = updatedMedia
                    viewModel.currentAudioUri = updatedAudio
                    viewModel.currentAudioTitle = updatedAudioTitle
                    viewModel.currentAspectRatio = updatedRatio
                    showAdvancedEditDialog = true
                }
            )
        }
    }

    // 6. Pop-up Avançado de Configuração de Edição ("INICIAR EDIÇÃO")
    if (showAdvancedEditDialog) {
        AdvancedEditDialog(
            mediaItems = viewModel.currentMediaItems,
            audioUri = viewModel.currentAudioUri,
            onDismiss = { showAdvancedEditDialog = false },
            onStartAutoEdit = { selectedCameras, cameraPrompt, selectedTransitions, individualTimings, exportConfig ->
                showAdvancedEditDialog = false
                showRenderProgressDialog = true
                viewModel.executeAutoEdit(
                    selectedCameras = selectedCameras,
                    cameraPrompt = cameraPrompt,
                    selectedTransitions = selectedTransitions,
                    individualTimings = individualTimings,
                    exportConfig = exportConfig
                )
            }
        )
    }

    // 7. Pipeline Local de Renderização em Segundo Plano (Foreground Service)
    if (showRenderProgressDialog || renderState.isRendering) {
        RenderProgressDialog(
            renderState = renderState,
            onDismiss = {
                showRenderProgressDialog = false
            }
        )
    }
}
