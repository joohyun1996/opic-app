import { describe, expect, it } from 'vitest'
import { assignLevels, isCandidate, parseCsv, parseDefinition, parseFill, parseJudge, toRows, type EcdictRow } from '../../scripts/lib/advanced-filter'

const row = (over: Partial<EcdictRow> = {}): EcdictRow => ({
  word: 'mitigate', tag: 'toefl ielts gre', oxford: '', collins: '1', frq: '8991',
  definition: 'v. make less severe or harsh', exchange: 'd:mitigated/i:mitigating', phonetic: "'mitigeit", ...over,
})

describe('advanced-filter', () => {
  it('CSV: 따옴표 안의 쉼표, 줄바꿈, "" 이스케이프를 처리한다', () => {
    const rows = toRows(parseCsv('word,definition\nabc,"a, b\nc ""q"""\nxyz,plain\n'))
    expect(rows).toEqual([{ word: 'abc', definition: 'a, b\nc "q"' }, { word: 'xyz', definition: 'plain' }])
  })

  it('AC1: 모든 조건을 만족하면 후보다', () => {
    expect(isCandidate(row(), new Set())).toBe(true)
  })

  it.each([
    ['대문자', { word: 'Mitigate' }], ['하이픈', { word: 'well-known' }], ['시험 태그 없음', { tag: 'cet4 cet6' }],
    ['Oxford 3000', { oxford: '1' }], ['Collins 4', { collins: '4' }], ['빈도 정보 없음', { frq: '0' }],
    ['빈도 초과', { frq: '20001' }], ['굴절형', { exchange: '0:mitigate/1:d' }], ['뜻 없음', { definition: '' }],
  ])('AC1: %s이면 제외한다', (_, over) => {
    expect(isCandidate(row(over), new Set())).toBe(false)
  })

  it('AC3: 기존 단어나 제외 목록에 있으면 제외한다', () => {
    expect(isCandidate(row(), new Set(['mitigate']))).toBe(false)
  })

  it('AC2: 품사 접두어를 변환하고 첫 줄만 쓴다', () => {
    expect(parseDefinition('v. make less severe\\nn. other')).toEqual({ partOfSpeech: 'verb', meaningEn: 'make less severe' })
    expect(parseDefinition('s being present everywhere')).toEqual({ partOfSpeech: 'adjective', meaningEn: 'being present everywhere' })
    expect(parseDefinition('r. in a calm way')).toEqual({ partOfSpeech: 'adverb', meaningEn: 'in a calm way' })
    expect(parseDefinition('something odd')).toEqual({ partOfSpeech: null, meaningEn: 'something odd' })
  })

  it('판정·작성 TSV를 파싱하고 형식 오류는 거부한다', () => {
    expect(parseJudge('abate\tkeep\nnod\texclude\t쉬운 단어(토익 800+)\n')).toEqual([
      { word: 'abate', keep: true }, { word: 'nod', keep: false, reason: '쉬운 단어(토익 800+)' },
    ])
    expect(() => parseJudge('nod\texclude\n')).toThrow()
    expect(parseFill('abate\t줄어들다\tThe storm abated.\t폭풍이 잦아들었다.\n')[0]).toEqual({
      word: 'abate', meaningKo: '줄어들다', example: 'The storm abated.', exampleKo: '폭풍이 잦아들었다.',
    })
    expect(() => parseFill('abate\t줄어들다\n')).toThrow()
  })

  it('AC8: frq 순으로 5등분해 level 차이가 1 이하다', () => {
    const words = Array.from({ length: 23 }, (_, i) => ({ word: `w${i}`, frq: 100 - i }))
    const levels = assignLevels(words)
    const counts = [1, 2, 3, 4, 5].map(l => [...levels.values()].filter(v => v === l).length)
    expect(Math.max(...counts) - Math.min(...counts)).toBeLessThanOrEqual(1)
    expect(levels.get('w22')).toBe(1)
    expect(levels.get('w0')).toBe(5)
  })
})
