package com.example.engine

data class CameraMovement(
    val id: Int,
    val name: String,
    val description: String,
    val iconName: String
) {
    companion object {
        val ALL = listOf(
            CameraMovement(0, "Sem movimento (Estático)", "Imagem fixa sem deslocamento", "pause"),
            CameraMovement(1, "Mover para a Esquerda (Pan Left)", "Deslocamento suave para a esquerda", "arrow_left"),
            CameraMovement(2, "Mover para a Direita (Pan Right)", "Deslocamento suave para a direita", "arrow_right"),
            CameraMovement(3, "Mover para Cima (Tilt Up)", "Deslocamento vertical para cima", "arrow_up"),
            CameraMovement(4, "Mover para Baixo (Tilt Down)", "Deslocamento vertical para baixo", "arrow_down"),
            CameraMovement(5, "Mover para o Centro (Zoom In)", "Aproximação suave estilo Ken Burns", "zoom_in"),
            CameraMovement(6, "Sair do Centro (Zoom Out)", "Recuo gradual a partir do centro", "zoom_out")
        )

        fun getById(id: Int): CameraMovement {
            return ALL.find { it.id == id } ?: ALL[0]
        }
    }
}
