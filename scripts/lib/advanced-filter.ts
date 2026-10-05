export type EcdictRow = {
  word: string
  tag: string
  oxford: string
  collins: string
  frq: string
  definition: string
  exchange: string
  phonetic: string
}

export const TARGET_TAGS = ['gre', 'toefl', 'ielts'] as const
export const MAX_FRQ = 20000
export const MAX_COLLINS = 3

const POS_MAP: Record<string, string> = {
  n: 'noun', v: 'verb', a: 'adjective', s: 'adjective', adj: 'adjective',
  r: 'adverb', adv: 'adverb', prep: 'preposition', conj: 'conjunction',
  pron: 'pronoun', int: 'interjection', interj: 'interjection',
}

// RFC 4180 CSV. 따옴표 안의 쉼표·줄바꿈·"" 이스케이프를 처리한다.
export function parseCsv(text: string): string[][] {
  const rows: string[][] = []
  let row: string[] = []
  let field = ''
  let quoted = false
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (quoted) {
      if (ch === '"' && text[i + 1] === '"') { field += '"'; i++ }
      else if (ch === '"') quoted = false
      else field += ch
    } else if (ch === '"') quoted = true
    else if (ch === ',') { row.push(field); field = '' }
    else if (ch === '\n' || ch === '\r') {
      if (ch === '\r' && text[i + 1] === '\n') i++
      row.push(field); rows.push(row); row = []; field = ''
    } else field += ch
  }
  if (field || row.length) { row.push(field); rows.push(row) }
  return rows
}

export function toRows(table: string[][]): EcdictRow[] {
  const [header, ...body] = table
  return body.map(cells => Object.fromEntries(header.map((key, i) => [key, cells[i] ?? ''])) as EcdictRow)
}

export function isCandidate(row: EcdictRow, known: ReadonlySet<string>): boolean {
  const tags = row.tag.split(/\s+/)
  const frq = Number(row.frq) || 0
  return /^[a-z]+$/.test(row.word) &&
    TARGET_TAGS.some(tag => tags.includes(tag)) &&
    row.oxford !== '1' &&
    (Number(row.collins) || 0) <= MAX_COLLINS &&
    frq > 0 && frq <= MAX_FRQ &&
    !known.has(row.word) &&
    !row.exchange.split('/').some(part => part.startsWith('0:')) &&
    row.definition.trim() !== ''
}

// ECDICT definition은 줄 구분이 리터럴 "\n"이다. 첫 줄의 "v. ..." 또는 "v ..." 접두어를 품사로 바꾼다.
export function parseDefinition(definition: string): { partOfSpeech: string | null; meaningEn: string } {
  const first = definition.split(/\\n|\n/)[0].trim()
  const match = /^([a-z]+)\.?\s+(.+)$/.exec(first)
  if (match && POS_MAP[match[1]]) return { partOfSpeech: POS_MAP[match[1]], meaningEn: match[2].trim() }
  return { partOfSpeech: null, meaningEn: first }
}

export type JudgeLine = { word: string; keep: true } | { word: string; keep: false; reason: string }
export type FillLine = { word: string; meaningKo: string; example: string; exampleKo: string; meaningEn?: string }

function tsvLines(text: string): string[][] {
  return text.split('\n').map(line => line.replace(/\r$/, '')).filter(line => line.trim()).map(line => line.split('\t'))
}

export function parseJudge(text: string): JudgeLine[] {
  return tsvLines(text).map(([word, verdict, reason]) => {
    if (verdict === 'keep') return { word, keep: true }
    if (verdict === 'exclude' && reason?.trim()) return { word, keep: false, reason: reason.trim() }
    throw new Error(`판정 형식 오류: ${word}`)
  })
}

export function parseFill(text: string): FillLine[] {
  return tsvLines(text).map(([word, meaningKo, example, exampleKo, meaningEn]) => {
    if (!word || !meaningKo?.trim() || !example?.trim() || !exampleKo?.trim()) throw new Error(`작성 형식 오류: ${word}`)
    return { word, meaningKo: meaningKo.trim(), example: example.trim(), exampleKo: exampleKo.trim(), ...(meaningEn?.trim() ? { meaningEn: meaningEn.trim() } : {}) }
  })
}

// frq 오름차순(자주 쓰이는 순)으로 5등분한 구간 번호를 돌려준다. 단어 순서는 바꾸지 않는다.
export function assignLevels<T extends { word: string; frq: number }>(words: readonly T[]): Map<string, number> {
  const sorted = [...words].sort((a, b) => a.frq - b.frq || (a.word < b.word ? -1 : a.word > b.word ? 1 : 0))
  return new Map(sorted.map((word, i) => [word.word, Math.floor(i * 5 / sorted.length) + 1]))
}
