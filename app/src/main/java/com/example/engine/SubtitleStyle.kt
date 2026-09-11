package com.example.engine

import androidx.compose.ui.graphics.Color

data class SubtitleStyle(
    val id: Int,
    val name: String,
    val description: String,
    val sampleText: String,
    val textColor: Color,
    val backgroundColor: Color = Color.Transparent,
    val highlightColor: Color = Color.Transparent,
    val hasBorder: Boolean = false,
    val isBold: Boolean = true
) {
    companion object {
        val ALL = listOf(
            SubtitleStyle(
                id = 0,
                name = "Padrão Clássico",
                description = "Texto limpo com contorno escuro e alta legibilidade",
                sampleText = "ESTILO CLÁSSICO",
                textColor = Color.White,
                backgroundColor = Color.Transparent,
                hasBorder = true
            ),
            SubtitleStyle(
                id = 1,
                name = "Neon Glow",
                description = "Brilho ciano luminoso moderno para vídeos dinâmicos",
                sampleText = "NEON GLOW",
                textColor = Color(0xFF00F0FF),
                highlightColor = Color(0x6600F0FF),
                hasBorder = false
            ),
            SubtitleStyle(
                id = 2,
                name = "Pop Yellow (CapCut)",
                description = "Amarelo ultra destacado com sombra profunda e impacto visual",
                sampleText = "POP AMARELO",
                textColor = Color(0xFFFFEB3B),
                highlightColor = Color(0xFF000000),
                isBold = true
            ),
            SubtitleStyle(
                id = 3,
                name = "Caixa Minimalista",
                description = "Fundo preto translúcido arredondado com tipografia nítida",
                sampleText = "BOX RETRÔ",
                textColor = Color.White,
                backgroundColor = Color(0xCC111118),
                isBold = true
            ),
            SubtitleStyle(
                id = 4,
                name = "Destaque Karaokê",
                description = "Destaque verde elétrico sincronizado na palavra falada",
                sampleText = "KARAOKÊ AO VIVO",
                textColor = Color.White,
                highlightColor = Color(0xFF00E676),
                isBold = true
            ),
            SubtitleStyle(
                id = 5,
                name = "Sombra Moderna 3D",
                description = "Tipografia sólida com relevo escuro e contraste reforçado",
                sampleText = "SOMBRA 3D BOLD",
                textColor = Color(0xFFF3F4F6),
                highlightColor = Color(0xFFE11D48),
                isBold = true
            )
        )

        fun getById(id: Int): SubtitleStyle {
            return ALL.find { it.id == id } ?: ALL[0]
        }
    }
}
