import 'dotenv/config'
import { PrismaClient } from '../app/generated/prisma'
import { PrismaPg } from '@prisma/adapter-pg'
import Anthropic from '@anthropic-ai/sdk'

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL! })
const prisma = new PrismaClient({ adapter } as any)
const claude = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY! })

// gigamorph/hsk-vocabulary 레포 — HSK 1~6 JSON
const HSK_URLS: [number, string][] = [1, 2, 3, 4, 5, 6].map(n => [
  n,
  `https://raw.githubusercontent.com/gigamorph/hsk-vocabulary/master/data/hsk${n}.json`,
])

const BATCH = 20

// 레포 구조에 따라 필드명이 다를 수 있어 다중 키 대응
function parseWord(item: any): { word: string; pinyin: string; en: string } | null {
  const word = item.hanzi ?? item.simplified ?? item.word ?? ''
  const pinyin = item.pinyin ?? ''
  const en = item.en ?? item.english ?? item.definition ?? ''
  return word ? { word, pinyin, en } : null
}

// Claude Haiku — 중국어 단어 배치의 한국어 뜻 생성
async function fetchKoreanMeanings(words: string[], enMeanings: string[]): Promise<string[]> {
  const lines = words.map((w, i) => `${i + 1}. ${w}: ${enMeanings[i] ?? w}`).join('\n')
  try {
    const msg = await claude.messages.create({
      model: 'claude-haiku-4-5-20251001',
      max_tokens: 512,
      messages: [{
        role: 'user',
        content: `중국어 단어들의 한국어 기본 뜻을 JSON 배열로만 반환해. 설명 없이 배열만.\n\n${lines}\n\n형식: ["뜻1","뜻2",...]`,
      }],
    })
    const text = msg.content[0].type === 'text' ? msg.content[0].text.trim() : '[]'
    const parsed = JSON.parse(text)
    return Array.isArray(parsed) ? parsed : words.map(() => '')
  } catch { return words.map(() => '') }
}

async function main() {
  console.log('▶ 중국어 HSK 단어 씨드 시작\n')
  let totalCreated = 0, totalFailed = 0

  for (const [level, url] of HSK_URLS) {
    console.log(`HSK ${level} 다운로드 중... (${url})`)
    let items: any[] = []

    try {
      const res = await fetch(url)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      // 배열 직접 or 중첩 구조(레포 버전에 따라 다름) 모두 대응
      items = Array.isArray(data)
        ? data
        : data.words ?? data.vocabulary ?? Object.values(data)
    } catch (e) {
      console.error(`HSK ${level} 다운로드 실패:`, e)
      continue
    }

    const parsed = items.map(parseWord).filter(Boolean) as { word: string; pinyin: string; en: string }[]
    console.log(`HSK ${level}: ${parsed.length}개 단어`)

    let created = 0, failed = 0

    for (let i = 0; i < parsed.length; i += BATCH) {
      const batch = parsed.slice(i, i + BATCH)

      // Claude로 한국어 뜻 일괄 생성
      const korMeanings = await fetchKoreanMeanings(
        batch.map(b => b.word),
        batch.map(b => b.en),
      )

      for (let j = 0; j < batch.length; j++) {
        const { word, pinyin, en } = batch[j]
        const meaningKo = korMeanings[j] || en || word
        try {
          await prisma.word.upsert({
            where: { language_word: { language: 'zh', word } },
            create: {
              language: 'zh',
              word,
              phonetic: pinyin || null,
              meaningKo,
              meaningEn: en || null,
              example: null,
              exampleKo: null,
              level,
              category: `HSK${level}`,
              partOfSpeech: null,
              collocations: [],
            },
            update: {},
          })
          created++
        } catch { failed++ }
      }

      const done = Math.min(i + BATCH, parsed.length)
      process.stdout.write(`\r  HSK ${level} 진행: ${done}/${parsed.length} | 생성 ${created} 실패 ${failed}   `)
      await new Promise(r => setTimeout(r, 250))
    }

    console.log(`\n  HSK ${level} 완료 — 생성: ${created}, 실패: ${failed}\n`)
    totalCreated += created
    totalFailed += failed
  }

  console.log(`✓ 전체 완료 — 생성: ${totalCreated}, 실패: ${totalFailed}`)
  await prisma.$disconnect()
}

main().catch(e => { console.error(e); process.exit(1) })
