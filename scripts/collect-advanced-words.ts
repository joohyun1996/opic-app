import { mkdir, readFile, readdir, writeFile, access } from 'node:fs/promises'
import {
  assignLevels, isCandidate, MAX_COLLINS, MAX_FRQ, parseCsv, parseDefinition, parseFill, parseJudge, TARGET_TAGS, toRows,
} from './lib/advanced-filter'

// 사용법: vite-node scripts/collect-advanced-words.ts <collect|apply|ipa|finalize|status>
const ECDICT_URL = 'https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv'
const KAIKKI = (w: string) => `https://kaikki.org/dictionary/English/meaning/${w[0]}/${w.slice(0, 2)}/${w}.jsonl`
const cacheFile = new URL('../exports/.cache/ecdict.csv', import.meta.url)
const baseFile = new URL('../exports/source/words-en.json', import.meta.url)
const outFile = new URL('../exports/source/words-en-advanced.json', import.meta.url)
const batchDir = new URL('../exports/source/advanced-batches/', import.meta.url)

type AdvancedWord = {
  language: 'en'; word: string; phonetic: string | null; meaningKo: string; meaningEn: string
  example: string | null; exampleKo: string | null; level: number; category: string
  partOfSpeech: string | null; collocations: string[]; frq: number; ecdictPhonetic: string
}
type Source = {
  metadata: Record<string, unknown> & { excluded: { word: string; reason: string }[]; ipaFailures: string[] }
  words: AdvancedWord[]
}

const exists = (url: URL) => access(url).then(() => true, () => false)
const load = async (): Promise<Source> => JSON.parse(await readFile(outFile, 'utf8'))
const save = (source: Source) => writeFile(outFile, `${JSON.stringify(source, null, 2)}\n`)

async function collect() {
  if (!await exists(cacheFile)) {
    await mkdir(new URL('./', cacheFile), { recursive: true })
    const response = await fetch(ECDICT_URL)
    if (!response.ok) throw new Error(`ECDICT 다운로드 실패: ${response.status}`)
    await writeFile(cacheFile, Buffer.from(await response.arrayBuffer()))
  }
  const base = JSON.parse(await readFile(baseFile, 'utf8'))
  const known = new Set<string>([
    ...base.words.map((w: { word: string }) => w.word),
    ...base.metadata.excluded.map((e: { word: string }) => e.word),
  ])
  const rows = toRows(parseCsv(await readFile(cacheFile, 'utf8'))).filter(row => isCandidate(row, known))
  const words = rows.map((row): AdvancedWord => {
    const { partOfSpeech, meaningEn } = parseDefinition(row.definition)
    return {
      language: 'en', word: row.word, phonetic: null, meaningKo: '', meaningEn, example: null, exampleKo: null,
      level: 0, category: partOfSpeech ?? '', partOfSpeech, collocations: [], frq: Number(row.frq), ecdictPhonetic: row.phonetic,
    }
  }).sort((a, b) => a.frq - b.frq)
  await save({
    metadata: {
      source: ECDICT_URL, license: 'MIT', collectedAt: new Date().toISOString(),
      filter: { tags: TARGET_TAGS, maxFrq: MAX_FRQ, maxCollins: MAX_COLLINS, oxford3000: 'excluded', inflections: 'excluded' },
      candidates: words.length, judged: [], excluded: [], ipaFailures: [],
    },
    words,
  })
  console.log(`후보 ${words.length}개 저장`)
}

async function apply() {
  const source = await load()
  const byWord = new Map(source.words.map(w => [w.word, w]))
  const files = (await readdir(batchDir)).filter(f => f.endsWith('.tsv')).sort()
  const judged = new Set(source.metadata.judged as string[])
  for (const file of files.filter(f => f.startsWith('judge-'))) {
    for (const line of parseJudge(await readFile(new URL(file, batchDir), 'utf8'))) {
      if (judged.has(line.word)) continue
      if (!byWord.has(line.word)) throw new Error(`${file}: 후보에 없는 단어 ${line.word}`)
      judged.add(line.word)
      if (!line.keep) { source.metadata.excluded.push({ word: line.word, reason: line.reason }); byWord.delete(line.word) }
    }
  }
  for (const file of files.filter(f => f.startsWith('fill-'))) {
    for (const line of parseFill(await readFile(new URL(file, batchDir), 'utf8'))) {
      const word = byWord.get(line.word)
      if (!word) { if (!source.metadata.excluded.some(e => e.word === line.word)) throw new Error(`${file}: 없는 단어 ${line.word}`); continue }
      Object.assign(word, { meaningKo: line.meaningKo, example: line.example, exampleKo: line.exampleKo }, line.meaningEn ? { meaningEn: line.meaningEn } : {})
    }
  }
  source.metadata.judged = [...judged]
  source.words = source.words.filter(w => byWord.has(w.word))
  await save(source)
  await status()
}

async function ipa() {
  const source = await load()
  const failures = new Set(source.metadata.ipaFailures)
  const judged = new Set(source.metadata.judged as string[])
  const todo = source.words.filter(w => w.phonetic === null && judged.has(w.word) && !failures.has(w.word))
  let done = 0
  async function lookup(word: AdvancedWord) {
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        const response = await fetch(KAIKKI(word.word), { signal: AbortSignal.timeout(8000) })
        if (response.status === 404) break
        if (!response.ok) throw new Error(String(response.status))
        for (const line of (await response.text()).split('\n').filter(Boolean)) {
          const entry = JSON.parse(line)
          const found = entry.sounds?.find((s: { ipa?: string }) => s.ipa?.startsWith('/'))?.ipa
          if (found) { word.phonetic = found; return }
        }
        break
      } catch { await new Promise(r => setTimeout(r, 500 * 2 ** attempt)) }
    }
    failures.add(word.word)
    word.phonetic = word.ecdictPhonetic ? `/${word.ecdictPhonetic}/` : null
  }
  const queue = [...todo]
  await Promise.all([0, 1].map(async () => {
    for (let item = queue.shift(); item; item = queue.shift()) {
      await lookup(item)
      await new Promise(r => setTimeout(r, 200))
      if (++done % 100 === 0) {
        source.metadata.ipaFailures = [...failures]
        await save(source)
        console.log(`IPA ${done}/${todo.length} (실패 ${failures.size})`)
      }
    }
  }))
  source.metadata.ipaFailures = [...failures]
  await save(source)
  console.log(`IPA 완료 ${done}개, 실패 ${failures.size}개`)
}

async function finalize() {
  const source = await load()
  const failures = new Set(source.metadata.ipaFailures)
  for (const word of source.words) {
    if (!word.meaningKo || !word.example || !word.phonetic || !word.partOfSpeech) throw new Error(`미완성 단어: ${word.word}`)
    if (failures.has(word.word) && !word.meaningKo.startsWith('*')) word.meaningKo = `*${word.meaningKo}`
    word.category = word.partOfSpeech
  }
  const levels = assignLevels(source.words)
  for (const word of source.words) word.level = levels.get(word.word)!
  await save(source)
  await status()
}

async function status() {
  const source = await load()
  const judged = new Set(source.metadata.judged as string[])
  const w = source.words
  console.log(JSON.stringify({
    candidates: source.metadata.candidates, remaining: w.length, excluded: source.metadata.excluded.length,
    unjudged: w.filter(x => !judged.has(x.word)).length, unfilled: w.filter(x => !x.meaningKo).length,
    noIpa: w.filter(x => !x.phonetic).length, ipaFailures: source.metadata.ipaFailures.length,
  }))
}

const commands: Record<string, () => Promise<void>> = { collect, apply, ipa, finalize, status }
const command = commands[process.argv[2] ?? '']
if (!command) { console.error('명령: collect | apply | ipa | finalize | status'); process.exitCode = 1 }
else command().catch(error => { console.error(error instanceof Error ? error.message : error); process.exitCode = 1 })
