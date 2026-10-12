package com.jooh.opic.core.common

/** 정답을 확인한 뒤 표시할 검수된 분해 한 줄. */
data class AffixPiece(val label: String, val meaningKo: String, val affixId: Long?, val baseWordId: Long?)

fun formatAffixBreakdown(pieces: List<AffixPiece>, glossKo: String): String =
    pieces.joinToString(" + ") { piece ->
        val label = piece.label.trim('-')
        val meaning = piece.meaningKo.removePrefix("*").substringBefore(';').substringBefore(',').trim()
        "$label($meaning)"
    } + " → $glossKo"
