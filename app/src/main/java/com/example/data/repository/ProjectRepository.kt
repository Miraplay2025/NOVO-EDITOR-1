package com.example.data.repository

import com.example.data.dao.ProjectDao
import com.example.data.model.MediaItem
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val dao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun getProject(id: Long): ProjectEntity? = dao.getProjectById(id)

    suspend fun saveProject(project: ProjectEntity): Long {
        return if (project.id == 0L) {
            dao.insertProject(project)
        } else {
            dao.updateProject(project)
            project.id
        }
    }

    suspend fun deleteProject(id: Long) {
        dao.deleteProject(id)
    }

    companion object {
        fun serializeMediaItems(items: List<MediaItem>): String {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("uri", item.uri)
                    put("isVideo", item.isVideo)
                    put("durationSec", item.durationSec.toDouble())
                    put("originalDurationSec", item.originalDurationSec.toDouble())
                    put("orderIndex", item.orderIndex)
                    put("cameraMovementId", item.cameraMovementId)
                    put("transitionId", item.transitionId)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun deserializeMediaItems(json: String?): List<MediaItem> {
            if (json.isNullOrBlank()) return emptyList()
            val list = mutableListOf<MediaItem>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MediaItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            uri = obj.optString("uri", ""),
                            isVideo = obj.optBoolean("isVideo", false),
                            durationSec = obj.optDouble("durationSec", 4.0).toFloat(),
                            originalDurationSec = obj.optDouble("originalDurationSec", 4.0).toFloat(),
                            orderIndex = obj.optInt("orderIndex", i + 1),
                            cameraMovementId = obj.optInt("cameraMovementId", 0),
                            transitionId = obj.optInt("transitionId", 0)
                        )
                    )
                }
            } catch (_: Exception) {}
            return list
        }

        fun serializeIntList(list: List<Int>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            return arr.toString()
        }

        fun deserializeIntList(json: String?): List<Int> {
            if (json.isNullOrBlank()) return emptyList()
            val list = mutableListOf<Int>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    list.add(arr.getInt(i))
                }
            } catch (_: Exception) {}
            return list
        }
    }
}
