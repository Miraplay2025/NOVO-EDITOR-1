package com.example.engine

data class VideoTransition(
    val id: Int,
    val name: String,
    val description: String,
    val styleCategory: String
) {
    companion object {
        val ALL = listOf(
            VideoTransition(0, "Sem transição", "Corte seco direto entre as mídias", "Corte"),
            VideoTransition(1, "Dissolvência Suave (Crossfade)", "Dissolução cruzada clássica sem filtros", "Dissolve"),
            VideoTransition(2, "Fade Sutil", "Escurecimento sutil e elegante na junção", "Fade"),
            VideoTransition(3, "Zoom Dinâmico", "Aproximação suave conectando os quadros", "Zoom"),
            VideoTransition(4, "Desfoque de Movimento", "Motion blur horizontal neutro sem cor", "Blur"),
            VideoTransition(5, "Iris Fechando", "Fechamento circular suave no ponto focal", "Iris"),
            VideoTransition(6, "Iris Abrindo", "Abertura circular suave revelando o próximo quadro", "Iris"),
            VideoTransition(7, "Mergulho no Preto (Dip to Black)", "Transição suave com fade out total em preto", "Dip"),
            VideoTransition(8, "Mergulho no Branco (Dip to White)", "Fade sutil neutro em branco suave", "Dip"),
            VideoTransition(9, "Desfoque Suave (Soft Blur)", "Suavização e dissolução com blur progressivo", "Blur"),
            VideoTransition(10, "Distorção Suave (Smooth Glitch)", "Sutil deslocamento geométrico sem aberração cromática", "Glitch"),
            VideoTransition(11, "Ondulação Fading (Ripple Fade)", "Efeito de onda suave dissipando para o próximo clipe", "Wave"),
            VideoTransition(12, "Queima de Filme Suave", "Dissolvência vintage sutil e orgânica sem flashes", "Film"),
            VideoTransition(13, "Escala Suave (Scale Wipe)", "Interpolação de escala entre mídias consecutivas", "Scale"),
            VideoTransition(14, "Bokeh Dissolve", "Desfocagem em pontos de luz neutros na junção", "Bokeh"),
            VideoTransition(15, "Reflexo Suave (Lens Flare Fade)", "Brilho ótico sutil e neutro integrando a transição", "Flare")
        )

        fun getById(id: Int): VideoTransition {
            return ALL.find { it.id == id } ?: ALL[0]
        }
    }
}
