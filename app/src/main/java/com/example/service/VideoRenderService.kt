package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.ExportConfig
import com.example.data.repository.ProjectRepository
import com.example.engine.RenderEngine
import com.example.engine.RenderStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class VideoRenderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_RENDER) {
            val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1L)
            val mediaJson = intent.getStringExtra(EXTRA_MEDIA_JSON) ?: "[]"
            val audioUri = intent.getStringExtra(EXTRA_AUDIO_URI)
            val camerasJson = intent.getStringExtra(EXTRA_CAMERAS_JSON) ?: "[]"
            val cameraPrompt = intent.getStringExtra(EXTRA_CAMERA_PROMPT) ?: ""
            val transitionsJson = intent.getStringExtra(EXTRA_TRANSITIONS_JSON) ?: "[]"
            val timingsJson = intent.getStringExtra(EXTRA_TIMINGS_JSON) ?: "{}"
            val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"
            val fps = intent.getIntExtra(EXTRA_FPS, 30)
            val bitrate = intent.getStringExtra(EXTRA_BITRATE) ?: "Médio"
            val subtitlesEnabled = intent.getBooleanExtra(EXTRA_SUBTITLES_ENABLED, true)
            val subtitleStyleId = intent.getIntExtra(EXTRA_SUBTITLE_STYLE_ID, 1)
            val wordsPerSubtitle = intent.getIntExtra(EXTRA_WORDS_PER_SUBTITLE, 3)
            val subtitleTimingText = intent.getStringExtra(EXTRA_SUBTITLE_TIMING_TEXT) ?: ""

            startForegroundWithNotification("Iniciando renderização...", 0)

            val mediaItems = ProjectRepository.deserializeMediaItems(mediaJson)
            val cameraIds = ProjectRepository.deserializeIntList(camerasJson)
            val transitionIds = ProjectRepository.deserializeIntList(transitionsJson)
            val timingsMap = mutableMapOf<Int, Float>()
            try {
                val json = JSONObject(timingsJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    timingsMap[k.toInt()] = json.getDouble(k).toFloat()
                }
            } catch (_: Exception) {}

            val exportConfig = ExportConfig(
                resolution = resolution,
                fps = fps,
                bitrate = bitrate,
                subtitlesEnabled = subtitlesEnabled,
                subtitleStyleId = subtitleStyleId,
                wordsPerSubtitle = wordsPerSubtitle,
                subtitleTimingText = subtitleTimingText
            )

            serviceScope.launch {
                RenderStateManager.startRendering("Iniciando Foreground Service On-Device...")
                val engine = RenderEngine(applicationContext)

                val result = engine.executePipeline(
                    mediaItems = mediaItems,
                    audioUri = audioUri,
                    selectedCameraIds = cameraIds,
                    cameraPromptText = cameraPrompt,
                    selectedTransitionIds = transitionIds,
                    individualTimings = timingsMap,
                    exportConfig = exportConfig,
                    onProgressUpdate = { progress, stepText ->
                        val percent = (progress * 100).toInt()
                        RenderStateManager.updateProgress(progress, stepText)
                        updateNotification(stepText, percent)
                    }
                )

                result.onSuccess { outputFile ->
                    RenderStateManager.completeSuccess(
                        outputFilePath = outputFile.absolutePath,
                        finalMessage = "Vídeo final salvo em: ${outputFile.name}"
                    )
                    showCompletionNotification("Renderização Concluída!", "Vídeo salvo na Galeria com sucesso.")
                    stopSelf()
                }.onFailure { error ->
                    RenderStateManager.fail(error.message ?: "Erro desconhecido durante renderização")
                    showCompletionNotification("Falha na Renderização", error.message ?: "Erro inesperado")
                    stopSelf()
                }
            }
        } else if (action == ACTION_STOP_RENDER) {
            RenderStateManager.log("Renderização cancelada pelo usuário.", isError = true)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Renderização de Vídeo em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente durante a edição automática de vídeo"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Editor Automático On-Device")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundWithNotification(message: String, progress: Int) {
        val notification = buildNotification(message, progress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(message: String, progress: Int) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(message, progress))
    }

    private fun showCompletionNotification(title: String, body: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "auto_editor_render_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START_RENDER = "com.example.action.START_RENDER"
        const val ACTION_STOP_RENDER = "com.example.action.STOP_RENDER"

        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_MEDIA_JSON = "extra_media_json"
        const val EXTRA_AUDIO_URI = "extra_audio_uri"
        const val EXTRA_CAMERAS_JSON = "extra_cameras_json"
        const val EXTRA_CAMERA_PROMPT = "extra_camera_prompt"
        const val EXTRA_TRANSITIONS_JSON = "extra_transitions_json"
        const val EXTRA_TIMINGS_JSON = "extra_timings_json"
        const val EXTRA_RESOLUTION = "extra_resolution"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_SUBTITLES_ENABLED = "extra_subtitles_enabled"
        const val EXTRA_SUBTITLE_STYLE_ID = "extra_subtitle_style_id"
        const val EXTRA_WORDS_PER_SUBTITLE = "extra_words_per_subtitle"
        const val EXTRA_SUBTITLE_TIMING_TEXT = "extra_subtitle_timing_text"

        fun startRender(
            context: Context,
            mediaItems: List<com.example.data.model.MediaItem>,
            audioUri: String?,
            selectedCameraIds: List<Int>,
            cameraPromptText: String,
            selectedTransitionIds: List<Int>,
            individualTimings: Map<Int, Float>,
            exportConfig: ExportConfig,
            projectId: Long = 0L
        ) {
            val timingsObj = JSONObject()
            individualTimings.forEach { (k, v) -> timingsObj.put(k.toString(), v.toDouble()) }

            val intent = Intent(context, VideoRenderService::class.java).apply {
                action = ACTION_START_RENDER
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_MEDIA_JSON, ProjectRepository.serializeMediaItems(mediaItems))
                putExtra(EXTRA_AUDIO_URI, audioUri)
                putExtra(EXTRA_CAMERAS_JSON, ProjectRepository.serializeIntList(selectedCameraIds))
                putExtra(EXTRA_CAMERA_PROMPT, cameraPromptText)
                putExtra(EXTRA_TRANSITIONS_JSON, ProjectRepository.serializeIntList(selectedTransitionIds))
                putExtra(EXTRA_TIMINGS_JSON, timingsObj.toString())
                putExtra(EXTRA_RESOLUTION, exportConfig.resolution)
                putExtra(EXTRA_FPS, exportConfig.fps)
                putExtra(EXTRA_BITRATE, exportConfig.bitrate)
                putExtra(EXTRA_SUBTITLES_ENABLED, exportConfig.subtitlesEnabled)
                putExtra(EXTRA_SUBTITLE_STYLE_ID, exportConfig.subtitleStyleId)
                putExtra(EXTRA_WORDS_PER_SUBTITLE, exportConfig.wordsPerSubtitle)
                putExtra(EXTRA_SUBTITLE_TIMING_TEXT, exportConfig.subtitleTimingText)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
