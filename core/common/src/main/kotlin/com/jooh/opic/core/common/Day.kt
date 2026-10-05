package com.jooh.opic.core.common

const val WORDS_PER_DAY = 40

fun dayOf(seq: Int): Int {
    require(seq >= 1)
    return (seq - 1) / WORDS_PER_DAY + 1
}

fun seqRange(day: Int): IntRange {
    require(day >= 1 && day <= Int.MAX_VALUE / WORDS_PER_DAY)
    return (day - 1) * WORDS_PER_DAY + 1..day * WORDS_PER_DAY
}

fun totalDays(maxSeq: Int): Int {
    require(maxSeq >= 0)
    return if (maxSeq == 0) 0 else (maxSeq - 1) / WORDS_PER_DAY + 1
}
