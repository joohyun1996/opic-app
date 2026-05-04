import 'dotenv/config'
import { PrismaClient } from '../app/generated/prisma'
import { PrismaPg } from '@prisma/adapter-pg'
import Anthropic from '@anthropic-ai/sdk'

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL! })
const prisma = new PrismaClient({ adapter } as any)
const claude = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY! })

// COCA 동일 기반 공개 Google 빈도 단어 목록 (BYU COCA는 유료)
const WORD_SOURCE = 'https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english-usa-no-swears.txt'
const DICT_API = 'https://api.dictionaryapi.dev/api/v2/entries/en'
const BATCH = 20
const TOTAL = 5000

// Free Dictionary API — phonetic/품사/예문 조회
async function fetchDict(word: string) {
  try {
    const res = await fetch(`${DICT_API}/${word}`, { signal: AbortSignal.timeout(4000) })
    if (!res.ok) return null
    const [entry] = await res.json()
    const phonetic = entry?.phonetics?.find((p: any) => p.text)?.text ?? null
    const meaning = entry?.meanings?.[0]
    const def = meaning?.definitions?.[0]
    return {
      phonetic,
      partOfSpeech: meaning?.partOfSpeech ?? null,
      meaningEn: def?.definition ?? null,
      example: def?.example ?? null,
    }
  } catch { return null }
}

// Claude Haiku — 배치 단위 영→한 번역 (비용 절감)
async function fetchKoreanMeanings(words: string[], enMeanings: (string | null)[]): Promise<string[]> {
  const lines = words.map((w, i) => `${i + 1}. ${w}: ${enMeanings[i] ?? w}`).join('\n')
  try {
    const msg = await claude.messages.create({
      model: 'claude-haiku-4-5-20251001',
      max_tokens: 512,
      messages: [{
        role: 'user',
        content: `영어 단어들의 한국어 기본 뜻을 JSON 배열로만 반환해. 설명 없이 배열만.\n\n${lines}\n\n형식: ["뜻1","뜻2",...]`,
      }],
    })
    const text = msg.content[0].type === 'text' ? msg.content[0].text.trim() : '[]'
    const parsed = JSON.parse(text)
    return Array.isArray(parsed) ? parsed : words.map(() => '')
  } catch { return words.map(() => '') }
}

async function main() {
  console.log('▶ 영어 단어 씨드 시작\n')

  // 1. 단어 목록 다운로드 및 상위 5000개 추출
  console.log('단어 목록 다운로드 중...')
  const raw = await fetch(WORD_SOURCE).then(r => r.text())
  const words = raw
    .split('\n')
    .map(w => w.trim().toLowerCase())
    .filter(w => w && /^[a-z]+$/.test(w))
    .slice(0, TOTAL)
  console.log(`${words.length}개 로드 완료\n`)

  let created = 0, failed = 0

  for (let i = 0; i < words.length; i += BATCH) {
    const batch = words.slice(i, i + BATCH)
    // 빈도 순서 기준 레벨: 1~1000 → 1, 1001~2000 → 2 ... 4001~5000 → 5
    const level = Math.min(5, Math.floor(i / 1000) + 1)

    // 2. Free Dictionary API 병렬 조회
    const dicts = await Promise.all(batch.map(fetchDict))

    // 3. Claude Haiku로 한국어 뜻 일괄 생성
    const korMeanings = await fetchKoreanMeanings(batch, dicts.map(d => d?.meaningEn ?? null))

    // 4. DB upsert — update:{} 로 재실행 시 기존 데이터 유지
    for (let j = 0; j < batch.length; j++) {
      const word = batch[j]
      const d = dicts[j]
      const meaningKo = korMeanings[j] || word
      try {
        await prisma.word.upsert({
          where: { language_word: { language: 'en', word } },
          create: {
            language: 'en',
            word,
            phonetic: d?.phonetic ?? null,
            meaningKo,
            meaningEn: d?.meaningEn ?? null,
            example: d?.example ?? null,
            exampleKo: null,
            level,
            category: d?.partOfSpeech ?? 'general',
            partOfSpeech: d?.partOfSpeech ?? null,
            collocations: [],
          },
          update: {},
        })
        created++
      } catch { failed++ }
    }

    const done = Math.min(i + BATCH, words.length)
    process.stdout.write(`\r진행: ${done}/${words.length} (level ${level}) | 생성 ${created} 실패 ${failed}   `)
    await new Promise(r => setTimeout(r, 250))
  }

  console.log(`\n\n✓ 완료 — 생성: ${created}, 실패: ${failed}`)
  await prisma.$disconnect()
}

main().catch(e => { console.error(e); process.exit(1) })
