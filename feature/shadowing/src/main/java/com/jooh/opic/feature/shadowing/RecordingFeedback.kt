package com.jooh.opic.feature.shadowing

internal const val MIN_RECORDING_BYTES = 16_000 * 2 * 3 / 2

internal fun recordingTooShort(bytes: Long): Boolean = bytes < MIN_RECORDING_BYTES

private val silenceTag = Regex("\\[(?:silence|blank_audio)\\]|\\((?:silence|blank_audio)\\)", RegexOption.IGNORE_CASE)

internal fun cleanWhisperText(text: String): String = text.replace(silenceTag, " ").trim().replace(Regex("\\s+"), " ")
