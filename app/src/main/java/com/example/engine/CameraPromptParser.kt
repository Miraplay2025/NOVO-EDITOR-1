package com.example.engine

import com.example.data.model.MediaItem

sealed class PromptValidationResult {
    data class Success(val assignments: Map<Int, Int>) : PromptValidationResult()
    data class Error(val message: String) : PromptValidationResult()
    object Empty : PromptValidationResult()
}

object CameraPromptParser {

    /**
     * Parses and validates text prompt format:
     * Imagem1=MOVIMENTO 2,
     * Imagem2=MOVIMENTO 5,
     * Imagem3=MOVIMENTO 1,
     */
    fun validateAndParse(prompt: String, mediaItems: List<MediaItem>): PromptValidationResult {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return PromptValidationResult.Empty
        }

        val lines = trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return PromptValidationResult.Empty
        }

        // Map media by 1-based order index
        val imageItems = mediaItems.filter { !it.isVideo }
        val allImagesIndices = imageItems.map { it.orderIndex }.toSet()

        val parsedMap = mutableMapOf<Int, Int>() // media orderIndex -> movementId

        val regex = Regex("""^Imagem(\d+)\s*=\s*MOVIMENTO\s*(\d+)\s*,?$""", RegexOption.IGNORE_CASE)

        for ((lineIndex, line) in lines.withIndex()) {
            val match = regex.find(line)
            if (match == null) {
                return PromptValidationResult.Error(
                    "Linha ${lineIndex + 1} inválida: '$line'. Use o formato estrito: 'Imagem1=MOVIMENTO 2,'"
                )
            }

            val mediaIndex = match.groupValues[1].toIntOrNull() ?: 0
            val movementId = match.groupValues[2].toIntOrNull() ?: 0

            // Check if this mediaIndex exists in project
            val targetMedia = mediaItems.find { it.orderIndex == mediaIndex }
            if (targetMedia == null) {
                return PromptValidationResult.Error(
                    "Mídia $mediaIndex não encontrada na linha do tempo. Total de mídias: ${mediaItems.size}."
                )
            }

            // Rigorous rule: Videos cannot have camera movements
            if (targetMedia.isVideo) {
                return PromptValidationResult.Error(
                    "Vídeos não têm suporte a movimentos de câmera. Altere para um número de imagem válido."
                )
            }

            if (movementId !in 0..6) {
                return PromptValidationResult.Error(
                    "Movimento $movementId inválido na linha ${lineIndex + 1}. Escolha entre 0 e 6."
                )
            }

            parsedMap[mediaIndex] = movementId
        }

        // Check rule: Must declare ALL images in the project by order
        val declaredImages = parsedMap.keys
        for (requiredImgOrder in allImagesIndices) {
            if (!declaredImages.contains(requiredImgOrder)) {
                return PromptValidationResult.Error(
                    "Atenção: Você deve declarar obrigatoriamente TODAS as imagens do projeto. Falta a Imagem$requiredImgOrder."
                )
            }
        }

        return PromptValidationResult.Success(parsedMap)
    }

    /**
     * Generates a template prompt for the user based on currently loaded images
     */
    fun generateTemplate(mediaItems: List<MediaItem>): String {
        val images = mediaItems.filter { !it.isVideo }
        if (images.isEmpty()) return ""
        return buildString {
            images.forEachIndexed { idx, item ->
                val sampleMove = if (idx == 0) 2 else if (idx % 2 == 1) 5 else 1
                append("Imagem${item.orderIndex}=MOVIMENTO $sampleMove,\n")
            }
        }
    }
}
