import { describe, expect, it } from 'vitest'
import { buildWordExport, seededShuffle, WORD_EXPORT_SEED, type WordInput } from '../../scripts/lib/word-export'

const firstTime = '2026-10-04T00:00:00.000Z'
const nextTime = '2026-10-05T00:00:00.000Z'

function word(word: string, language = 'en', level = 1): WordInput {
  return {
    language, word, phonetic: null, meaningKo: '뜻', meaningEn: null,
    example: null, exampleKo: null, level, category: 'general',
    partOfSpeech: null, collocations: [],
  }
}

function fixture(): WordInput[] {
  return ['en', 'zh'].flatMap(language => [1, 2, 3].flatMap(level =>
    Array.from({ length: 40 }, (_, i) => word(`word-${level}-${i}`, language, level))))
}

describe('단어 내보내기', () => {
  it('AC1: 입력 순서가 달라도 같은 시드로 같은 결과를 만든다', () => {
    const input = fixture()
    const snapshot = structuredClone(input)
    const output = buildWordExport(input, firstTime)
    expect(buildWordExport([...input].reverse(), firstTime)).toEqual(output)
    expect(buildWordExport(seededShuffle(input, 17), firstTime)).toEqual(output)
    expect(input).toEqual(snapshot)
    expect(output.seed).toBe(WORD_EXPORT_SEED)
    expect(seededShuffle([1, 2, 3, 4, 5], 42)).toEqual(seededShuffle([1, 2, 3, 4, 5], 42))
  })

  it('AC2: 최초 seq는 언어별로 1부터 연속이며 언어와 seq 순으로 정렬된다', () => {
    const output = buildWordExport(fixture(), firstTime)
    for (const language of ['en', 'zh']) {
      expect(output.words.filter(w => w.language === language).map(w => w.seq))
        .toEqual(Array.from({ length: 120 }, (_, i) => i + 1))
    }
    expect(output.words.map(w => w.language)).toEqual([
      ...Array(120).fill('en'), ...Array(120).fill('zh'),
    ])
  })

  it('AC3: level 1~3 각 40개 중 Day 1에 2개 이상 난이도가 섞인다', () => {
    const output = buildWordExport(fixture(), firstTime)
    for (const language of ['en', 'zh']) {
      const dayOne = output.words.filter(w => w.language === language && w.seq <= 40)
      expect(dayOne).toHaveLength(40)
      expect(new Set(dayOne.map(w => w.level)).size).toBeGreaterThanOrEqual(2)
    }
  })

  it('AC4: 기존 seq를 보존하고 새 단어를 언어별 최대 seq 뒤에 붙인다', () => {
    const input = [word('a'), word('b'), word('甲', 'zh')]
    const previous = buildWordExport(input, firstTime)
    const additions = [word('c'), word('d'), word('乙', 'zh')]
    const output = buildWordExport([...additions, ...input], nextTime, previous)
    for (const old of previous.words) {
      expect(output.words.find(w => w.language === old.language && w.word === old.word)?.seq).toBe(old.seq)
    }
    expect(output.words.filter(w => ['c', 'd'].includes(w.word)).map(w => w.seq)).toEqual([3, 4])
    expect(output.words.find(w => w.word === '乙')?.seq).toBe(2)
    expect(buildWordExport([...input, ...additions].reverse(), nextTime, previous)).toEqual(output)
    expect(buildWordExport([word('甲', 'zh')], nextTime, buildWordExport([word('a')], firstTime))
      .words.find(w => w.language === 'zh')?.seq).toBe(1)
  })

  it('AC5: 사라진 단어는 seq와 내용을 유지하며 삭제 표시하고 재등장하면 복구한다', () => {
    const input = [word('a'), word('b')]
    const previous = buildWordExport(input, firstTime)
    const old = previous.words.find(w => w.word === 'b')!
    const removed = buildWordExport([input[0]], nextTime, previous)
    expect(removed.words.find(w => w.word === 'b')).toEqual({ ...old, deleted: true })
    expect(buildWordExport(input, nextTime, removed).words.find(w => w.word === 'b')).toEqual(old)
    const added = buildWordExport([input[0], word('c')], nextTime, removed)
    expect(added.words.find(w => w.word === 'c')?.seq).toBe(3)
  })

  it('AC6: 추가·수정·삭제·복구 시 버전이 1 증가하고 무변경이면 그대로다', () => {
    const input = [word('a')]
    const previous = buildWordExport(input, firstTime)
    expect(previous.dataVersion).toBe(1)
    expect(buildWordExport(input, nextTime, previous)).toEqual(previous)
    const changes = [
      [...input, word('b')], [], [{ ...input[0], meaningKo: '*수정된 뜻' }],
      [{ ...input[0], collocations: ['a phrase'] }],
    ]
    for (const changedInput of changes) {
      const changed = buildWordExport(changedInput, nextTime, previous)
      expect(changed.dataVersion).toBe(2)
      expect(changed.exportedAt).toBe(nextTime)
      expect(buildWordExport(changedInput, nextTime, changed)).toEqual(changed)
    }
    const removed = buildWordExport([], nextTime, previous)
    expect(buildWordExport(input, nextTime, removed).dataVersion).toBe(3)
  })

  it('AC7: 단어만 소문자·trim 처리하고 별표로 시작하는 뜻은 보존한다', () => {
    const input = { ...word('  ApPlE  '), meaningKo: '*사과', meaningEn: '*fruit' }
    const output = buildWordExport([input], firstTime)
    expect(output.words[0]).toMatchObject({ word: 'apple', meaningKo: '*사과', meaningEn: '*fruit' })
    expect(buildWordExport([{ ...input, word: 'APPLE' }], nextTime, output)).toEqual(output)
  })

  it('AC8: 정규화 후 모든 중복 목록을 에러에 포함하며 언어가 다르면 허용한다', () => {
    expect(() => buildWordExport([word(' A '), word('a'), word('B'), word('b')], firstTime))
      .toThrow('정규화 후 중복 단어: ["en","a"], ["en","b"]')
    expect(buildWordExport([word('a'), word('a', 'zh')], firstTime).words).toHaveLength(2)
  })

  it('AC9: DB 전용 필드를 제외하고 모든 nullable 키와 배열을 출력한다', () => {
    const input = { ...word('a'), id: 42, createdAt: new Date(firstTime) }
    const output = JSON.parse(JSON.stringify(buildWordExport([input], firstTime))).words[0]
    expect(Object.keys(output)).toEqual([
      'language', 'word', 'seq', 'phonetic', 'meaningKo', 'meaningEn', 'example',
      'exampleKo', 'level', 'category', 'partOfSpeech', 'collocations', 'deleted',
    ])
    for (const key of ['phonetic', 'meaningEn', 'example', 'exampleKo', 'partOfSpeech']) {
      expect(output[key]).toBeNull()
    }
    expect(output.collocations).toEqual([])
    expect(output.deleted).toBe(false)
  })

  it('빈 입력도 내보낼 수 있고 마지막 단어 삭제 후 반복 실행은 버전을 유지한다', () => {
    expect(buildWordExport([], firstTime).words).toEqual([])
    const previous = buildWordExport([word('a')], firstTime)
    const removed = buildWordExport([], nextTime, previous)
    expect(removed.words[0].deleted).toBe(true)
    expect(buildWordExport([], firstTime, removed)).toEqual(removed)
  })

  it('잘못된 기존 seq와 버전 및 중복 단어는 거부한다', () => {
    const previous = buildWordExport([word('a'), word('b')], firstTime)
    for (const seq of [0, 1.5, previous.words[1].seq]) {
      expect(() => buildWordExport([], nextTime, {
        ...previous, words: [{ ...previous.words[0], seq }, previous.words[1]],
      })).toThrow('기존 내보내기의 seq')
    }
    expect(() => buildWordExport([], nextTime, { ...previous, dataVersion: 0 })).toThrow('dataVersion')
    expect(() => buildWordExport([], nextTime, {
      ...previous, words: [previous.words[0], { ...previous.words[0], seq: 3 }],
    })).toThrow('정규화 후 중복 단어')
  })
})
