import type { Word } from '../../app/generated/prisma'

export const WORD_EXPORT_SEED = 20261004

export type WordInput = Pick<Word,
  'language' | 'word' | 'phonetic' | 'meaningKo' | 'meaningEn' |
  'example' | 'exampleKo' | 'level' | 'category' | 'partOfSpeech' | 'collocations'
>

export type ExportedWord = WordInput & { seq: number; deleted: boolean }

export type WordExport = {
  dataVersion: number
  seed: number
  exportedAt: string
  words: ExportedWord[]
}

function compareText(a: string, b: string): number {
  return a < b ? -1 : a > b ? 1 : 0
}

function keyOf(word: WordInput): string {
  return JSON.stringify([word.language, word.word])
}

function toRecord(word: WordInput, seq: number, deleted: boolean): ExportedWord {
  return {
    language: word.language,
    word: word.word.trim().toLowerCase(),
    seq,
    phonetic: word.phonetic ?? null,
    meaningKo: word.meaningKo,
    meaningEn: word.meaningEn ?? null,
    example: word.example ?? null,
    exampleKo: word.exampleKo ?? null,
    level: word.level,
    category: word.category,
    partOfSpeech: word.partOfSpeech ?? null,
    collocations: [...(word.collocations ?? [])],
    deleted,
  }
}

function assertUnique(words: WordInput[]): void {
  const seen = new Set<string>()
  const duplicates = new Set<string>()
  for (const word of words) {
    const key = keyOf(word)
    if (seen.has(key)) duplicates.add(key)
    seen.add(key)
  }
  if (duplicates.size) {
    throw new Error(`정규화 후 중복 단어: ${[...duplicates].sort(compareText).join(', ')}`)
  }
}

// mulberry32와 Fisher–Yates를 사용하고 입력 배열은 변경하지 않는다.
export function seededShuffle<T>(input: readonly T[], seed: number): T[] {
  const result = [...input]
  let state = seed >>> 0
  function random(): number {
    state = (state + 0x6D2B79F5) >>> 0
    let value = Math.imul(state ^ (state >>> 15), state | 1)
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61)
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296
  }
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

function sortBySequence(a: ExportedWord, b: ExportedWord): number {
  return compareText(a.language, b.language) || a.seq - b.seq
}

export function buildWordExport(
  input: readonly WordInput[],
  exportedAt: string,
  previous?: WordExport,
): WordExport {
  const normalized = input.map(word => toRecord(word, 0, false))
    .sort((a, b) => compareText(a.language, b.language) || compareText(a.word, b.word))
  assertUnique(normalized)

  const oldWords = (previous?.words ?? []).map(word => toRecord(word, word.seq, word.deleted))
    .sort(sortBySequence)
  assertUnique(oldWords)
  const maxSequences = new Map<string, number>()
  if (previous && (!Number.isSafeInteger(previous.dataVersion) || previous.dataVersion < 1 ||
    !Number.isInteger(previous.seed) || previous.seed < 0 || previous.seed > 0xFFFFFFFF)) {
    throw new Error('기존 내보내기의 dataVersion 또는 seed가 올바르지 않습니다.')
  }
  for (const word of oldWords) {
    const max = maxSequences.get(word.language) ?? 0
    if (!Number.isSafeInteger(word.seq) || word.seq <= max || typeof word.deleted !== 'boolean') {
      throw new Error(`기존 내보내기의 seq 또는 deleted가 올바르지 않습니다: ${keyOf(word)}`)
    }
    maxSequences.set(word.language, word.seq)
  }

  const currentByKey = new Map(normalized.map(word => [keyOf(word), word]))
  const oldKeys = new Set(oldWords.map(keyOf))
  const words = oldWords.map(old => {
    const current = currentByKey.get(keyOf(old))
    return current ? toRecord(current, old.seq, false) : toRecord(old, old.seq, true)
  })
  const newByLanguage = new Map<string, ExportedWord[]>()
  for (const word of normalized) {
    if (oldKeys.has(keyOf(word))) continue
    const group = newByLanguage.get(word.language) ?? []
    group.push(word)
    newByLanguage.set(word.language, group)
  }

  const seed = previous?.seed ?? WORD_EXPORT_SEED
  for (const [language, additions] of newByLanguage) {
    let seq = maxSequences.get(language) ?? 0
    for (const word of seededShuffle(additions, seed)) {
      words.push(toRecord(word, ++seq, false))
    }
  }
  words.sort(sortBySequence)
  const changed = !previous || JSON.stringify(words) !== JSON.stringify(oldWords)
  return {
    dataVersion: previous ? previous.dataVersion + Number(changed) : 1,
    seed,
    exportedAt: changed ? exportedAt : previous!.exportedAt,
    words,
  }
}
