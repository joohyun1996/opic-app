package com.jooh.opic.core.common

/** 발음 힌트 (TASK 19). 음소 채점이 아니라 Whisper 신호와 고정 규칙으로 고칠 곳을 알려 준다. */

const val UNCLEAR_THRESHOLD = 0.5f
private val FILLERS = setOf("um", "uh", "er", "erm", "hmm", "mm")

private fun bare(word: String) = werWords(word).firstOrNull().orEmpty()

/** 확신도가 낮은 단어의 index (낮은 순, 최대 [max]개). 머뭇거림·숫자·1글자·중복 단어 제외. */
fun unclearWords(words: List<SpokenWord>, threshold: Float = UNCLEAR_THRESHOLD, max: Int = 5): List<Int> {
    val seen = mutableSetOf<String>()
    return words.indices.filter { i ->
        val w = bare(words[i].text)
        words[i].confidence < threshold && w.length > 1 && w !in FILLERS && w.none(Char::isDigit)
    }.sortedBy { words[it].confidence }.filter { seen.add(bare(words[it].text)) }.take(max)
}

data class PronunciationRule(val id: String, val title: String, val explanation: String, val matches: (String) -> Boolean)
data class PronunciationTip(val rule: PronunciationRule, val examples: List<String>, val hits: Int)

private val VOWELS = "aeiou"

val PRONUNCIATION_RULES = listOf(
    PronunciationRule("r_l", "R과 L", "R은 혀끝을 어디에도 대지 않고 입술을 살짝 둥글게, L은 혀끝을 윗니 뒤에 확실히 붙여요. 한국어 'ㄹ' 하나로 둘 다 내면 구분이 안 돼요.") { 'r' in it || 'l' in it },
    PronunciationRule("f_p", "F는 P가 아니다", "F는 윗니를 아랫입술에 살짝 대고 바람을 내보내요. 'ㅍ'처럼 입술을 붙였다 떼면 P로 들려요 (coffee ≠ 커피).") { 'f' in it || "ph" in it },
    PronunciationRule("v_b", "V는 B가 아니다", "V는 F와 같은 입 모양에 목을 울려요. 'ㅂ'처럼 입술을 붙이면 B로 들려요 (very ≠ berry).") { 'v' in it },
    PronunciationRule("th", "TH 소리", "혀끝을 윗니와 아랫니 사이에 살짝 내밀고 바람을 내요. 'ㅆ'(think → 씽크)이나 'ㄷ'(the → 더)으로 바꾸지 않게.") { "th" in it },
    PronunciationRule("z_j", "Z는 J가 아니다", "Z는 S 입 모양 그대로 목을 울리는 '즈~' 소리예요. 'ㅈ'으로 내면 J로 들려요 (zoo ≠ 주).") { 'z' in it },
    PronunciationRule("final", "끝소리에 '으' 붙이지 않기", "영어 끝자음은 모음 없이 닫아요. milk를 '밀크'처럼 '으'를 붙이면 음절이 늘어나 어색하게 들려요.") { w ->
        w.length >= 3 && (w.endsWith("ch") || w.endsWith("sh") || w.last() in "bdgkpt")
    },
    PronunciationRule("w", "W는 입술을 먼저", "W는 입술을 동그랗게 모았다가 풀면서 소리 내요. work, world, would는 '워'를 확실히 — 안 그러면 walk/wood처럼 들려요.") { w ->
        w.length >= 3 && w[0] == 'w' && w[1] in "ou"
    },
    PronunciationRule("ee_i", "긴 '이'와 짧은 '이'", "ee·ea는 입을 옆으로 당긴 긴 '이~'(sheep), i는 힘을 뺀 짧은 '이'(ship)예요. 길이를 구분해야 뜻이 갈려요.") { "ee" in it || "ea" in it },
)

/** 단어들에서 걸린 규칙 (많이 걸린 순, 최대 [max]개). 예시는 중복 없이 최대 4개. */
fun pronunciationTips(words: List<String>, max: Int = 4): List<PronunciationTip> {
    val clean = words.map(::bare).filter { it.length > 1 && it !in FILLERS }
    return PRONUNCIATION_RULES.mapNotNull { rule ->
        val hit = clean.filter(rule.matches)
        if (hit.isEmpty()) null else PronunciationTip(rule, hit.distinct().take(4), hit.size)
    }.sortedByDescending { it.hits }.take(max)
}

/** 연음: 자음으로 끝나는 단어 + 모음으로 시작하는 단어 (최대 [max]쌍). */
fun linkingPairs(text: String, max: Int = 4): List<Pair<String, String>> {
    val words = werWords(text).filter { it !in FILLERS }
    return words.zipWithNext().filter { (a, b) ->
        a.last().isLetter() && a.last() !in VOWELS && a.last() != 'y' && b.first() in VOWELS
    }.distinct().take(max)
}

private val REDUCTIONS = listOf(
    listOf("want", "to") to "wanna", listOf("going", "to") to "gonna", listOf("got", "to") to "gotta",
    listOf("kind", "of") to "kinda", listOf("a", "lot", "of") to "a lotta", listOf("have", "to") to "hafta",
)

/** 약화: 원래 말 → 들리는 소리. */
fun reductions(text: String): List<Pair<String, String>> {
    val words = werWords(text)
    return REDUCTIONS.filter { (phrase, _) -> words.windowed(phrase.size).any { it == phrase } }
        .map { (phrase, sound) -> phrase.joinToString(" ") to sound }
}

private val FLAP = Regex("[aeiou]tt?[aeiouy]")

/** t 약화: 단어 가운데 모음 + t(t) + 모음/y (water, city). 첫 글자 t는 제외. */
fun flapWords(text: String, max: Int = 4): List<String> =
    werWords(text).filter { w -> FLAP.containsMatchIn(w.drop(1)) }.distinct().take(max)

const val RHYTHM_TIP = "내용어(명사·동사·형용사)는 세고 길게, 기능어(a, the, to, of)는 약하고 짧게 말해 보세요. 영어는 강세 박자로 리듬이 생겨요."
