package com.example.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val message: String,
    val isError: Boolean = false
) {
    val formatted: String
        get() = "[$timestamp] $message"
}

data class RenderUiState(
    val isRendering: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val logs: List<LogEntry> = emptyList(),
    val isCompleted: Boolean = false,
    val outputFilePath: String? = null,
    val errorMessage: String? = null
)
