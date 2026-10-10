package com.jooh.opic.core.common

/** Whisper 토큰 하나. 시각은 ms. */
data class RawToken(val text: String, val startMs: Long, val endMs: Long, val probability: Float)

/** 받아 적은 단어 하나. confidence = 단어를 이룬 토큰 확률의 최솟값 (TASK 19 발음 힌트에서 사용). */
data class SpokenWord(val text: String, val startMs: Long, val endMs: Long, val confidence: Float)

/** JNI가 넘긴 "t0\tt1\tp\ttext" 줄들 (t0·t1은 10ms 단위). 형식이 틀린 줄은 건너뛴다. */
fun parseTokenLines(lines: String): List<RawToken> = lines.lineSequence().mapNotNull { line ->
    val parts = line.split('\t', limit = 4)
    if (parts.size < 4) return@mapNotNull null
    val t0 = parts[0].toLongOrNull() ?: return@mapNotNull null
    val t1 = parts[1].toLongOrNull() ?: return@mapNotNull null
    val p = parts[2].toFloatOrNull() ?: return@mapNotNull null
    RawToken(parts[3], t0 * 10, t1 * 10, p)
}.toList()

private fun isSpecial(text: String) = text.startsWith("[_") || text.startsWith("<|") || text.startsWith("[")

/** 앞 공백이 있으면 새 단어, 없으면 앞 단어에 붙인다 (문장부호 포함). */
fun mergeTokens(tokens: List<RawToken>): List<SpokenWord> {
    val words = mutableListOf<SpokenWord>()
    for (token in tokens) {
        if (token.text.isBlank() || isSpecial(token.text.trim())) continue
        val startsWord = token.text.first().isWhitespace() || words.isEmpty()
        val piece = token.text.trim()
        if (startsWord) words += SpokenWord(piece, token.startMs, token.endMs, token.probability)
        else {
            val last = words.removeAt(words.lastIndex)
            words += SpokenWord(last.text + piece, last.startMs, maxOf(last.endMs, token.endMs), minOf(last.confidence, token.probability))
        }
    }
    return words
}

enum class EditState { KEPT, REPLACED, DELETED, INSERTED }

/** 사용자가 고칠 수 있는 단어. original은 Whisper 단어(넣은 단어는 null). */
data class EditableWord(val original: SpokenWord?, val text: String, val state: EditState = EditState.KEPT)

fun editable(words: List<SpokenWord>): List<EditableWord> = words.map { EditableWord(it, it.text) }

fun List<EditableWord>.replaceAt(index: Int, text: String): List<EditableWord> {
    val word = getOrNull(index) ?: return this
    val clean = text.trim()
    if (clean.isEmpty()) return deleteAt(index)
    val state = when {
        word.state == EditState.INSERTED -> EditState.INSERTED
        clean == word.original?.text -> EditState.KEPT
        else -> EditState.REPLACED
    }
    return toMutableList().also { it[index] = word.copy(text = clean, state = state) }
}

fun List<EditableWord>.deleteAt(index: Int): List<EditableWord> {
    val word = getOrNull(index) ?: return this
    if (word.state == EditState.INSERTED) return filterIndexed { i, _ -> i != index }
    return toMutableList().also { it[index] = word.copy(state = EditState.DELETED) }
}

fun List<EditableWord>.insertAfter(index: Int, text: String): List<EditableWord> {
    if (index !in -1..lastIndex || text.isBlank()) return this
    return toMutableList().also { it.add(index + 1, EditableWord(null, text.trim(), EditState.INSERTED)) }
}

/** 원래대로: 넣은 단어는 빼고, 나머지는 Whisper 단어로. */
fun List<EditableWord>.restoreAt(index: Int): List<EditableWord> {
    val word = getOrNull(index) ?: return this
    val original = word.original ?: return filterIndexed { i, _ -> i != index }
    return toMutableList().also { it[index] = EditableWord(original, original.text) }
}

fun List<EditableWord>.restoreAll(): List<EditableWord> = mapNotNull { w -> w.original?.let { EditableWord(it, it.text) } }

fun editedText(words: List<EditableWord>): String = words.filter { it.state != EditState.DELETED }.joinToString(" ") { it.text }

/** 이 단어를 재생할 구간 (앞뒤 여유 포함). 넣은 단어는 앞뒤 단어 시각으로 대신한다. */
fun playRange(words: List<EditableWord>, index: Int, marginMs: Long = 200): LongRange? {
    val word = words.getOrNull(index) ?: return null
    val source = word.original
        ?: words.take(index).lastOrNull { it.original != null }?.original
        ?: words.drop(index + 1).firstOrNull { it.original != null }?.original
        ?: return null
    return (source.startMs - marginMs).coerceAtLeast(0)..(source.endMs + marginMs)
}
