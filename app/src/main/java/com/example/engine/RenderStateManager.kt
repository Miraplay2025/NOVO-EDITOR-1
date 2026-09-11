package com.example.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RenderStateManager {
    private val _state = MutableStateFlow(RenderUiState())
    val state: StateFlow<RenderUiState> = _state.asStateFlow()

    fun startRendering(initialMessage: String = "Iniciando pipeline de renderização...") {
        _state.value = RenderUiState(
            isRendering = true,
            progress = 0.05f,
            statusMessage = initialMessage,
            logs = listOf(LogEntry(message = initialMessage)),
            isCompleted = false,
            errorMessage = null,
            outputFilePath = null
        )
    }

    fun updateProgress(progress: Float, message: String) {
        _state.update { current ->
            val newLogs = current.logs + LogEntry(message = message)
            current.copy(
                progress = progress.coerceIn(0f, 1f),
                statusMessage = message,
                logs = newLogs
            )
        }
    }

    fun log(message: String, isError: Boolean = false) {
        _state.update { current ->
            current.copy(
                logs = current.logs + LogEntry(message = message, isError = isError)
            )
        }
    }

    fun completeSuccess(outputFilePath: String, finalMessage: String = "Vídeo compilado e salvo com sucesso na Galeria!") {
        _state.update { current ->
            val newLogs = current.logs + LogEntry(message = finalMessage)
            current.copy(
                isRendering = false,
                progress = 1.0f,
                statusMessage = finalMessage,
                logs = newLogs,
                isCompleted = true,
                outputFilePath = outputFilePath,
                errorMessage = null
            )
        }
    }

    fun fail(errorMsg: String) {
        _state.update { current ->
            val newLogs = current.logs + LogEntry(message = "ERRO: $errorMsg", isError = true)
            current.copy(
                isRendering = false,
                statusMessage = "Falha no processamento.",
                errorMessage = errorMsg,
                logs = newLogs,
                isCompleted = false
            )
        }
    }

    fun clearLogs() {
        _state.update { current ->
            current.copy(logs = emptyList())
        }
    }

    fun reset() {
        _state.value = RenderUiState()
    }
}
