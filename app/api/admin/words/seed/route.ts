import { NextRequest, NextResponse } from 'next/server'
import { getIronSession } from 'iron-session'
import { prisma } from '@/lib/prisma'
import { sessionOptions, SessionData } from '@/lib/session'
import { claude } from '@/lib/claude'
import { isValidLang } from '@/lib/lang'

export async function POST(request: NextRequest) {
  const res = NextResponse.next()
  const session = await getIronSession<SessionData>(request, res, sessionOptions)
  if (!session.userId) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }
  if (session.role !== 'admin') {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }

  const { lang, level, category, count = 40 } = await request.json()

  if (!lang || !isValidLang(lang)) {
    return NextResponse.json({ error: 'Invalid lang' }, { status: 400 })
  }
  if (!level || !category) {
    return NextResponse.json({ error: 'level and category required' }, { status: 400 })
  }

  const langLabel = lang === 'en' ? 'English (OPIc)' : 'Chinese (HSK)'
  const prompt = lang === 'en'
    ? `Generate ${count} English vocabulary words for OPIc level ${level}, category: ${category}.
Return a JSON array (no markdown) with this exact shape per item:
{
  "word": "string",
  "phonetic": "IPA string",
  "meaningKo": "Korean meaning",
  "meaningEn": "English definition",
  "example": "English example sentence",
  "exampleKo": "Korean translation of example",
  "partOfSpeech": "noun|verb|adjective|adverb|etc",
  "collocations": ["collocation1", "collocation2"]
}
Return only the JSON array.`
    : `Generate ${count} Chinese vocabulary words for HSK level ${level}, category: ${category}.
Return a JSON array (no markdown) with this exact shape per item:
{
  "word": "Chinese characters",
  "phonetic": "pinyin",
  "meaningKo": "Korean meaning",
  "meaningEn": "English definition",
  "example": "Chinese example sentence",
  "exampleKo": "Korean translation of example",
  "partOfSpeech": "명사|동사|형용사|부사|etc",
  "collocations": ["collocation1", "collocation2"]
}
Return only the JSON array.`

  const message = await claude.messages.create({
    model: 'claude-sonnet-4-6',
    max_tokens: 8192,
    messages: [{ role: 'user', content: prompt }],
  })

  const text = message.content[0].type === 'text' ? message.content[0].text : ''

  let words: any[]
  try {
    words = JSON.parse(text)
    if (!Array.isArray(words)) throw new Error('Not an array')
  } catch {
    return NextResponse.json({ error: 'Claude returned invalid JSON', raw: text }, { status: 500 })
  }

  const created: string[] = []
  const skipped: string[] = []

  for (const w of words) {
    try {
      await prisma.word.upsert({
        where: { language_word: { language: lang, word: w.word } },
        create: {
          language: lang,
          word: w.word,
          phonetic: w.phonetic ?? null,
          meaningKo: w.meaningKo,
          meaningEn: w.meaningEn ?? null,
          example: w.example ?? null,
          exampleKo: w.exampleKo ?? null,
          level: Number(level),
          category,
          partOfSpeech: w.partOfSpeech ?? null,
          collocations: w.collocations ?? [],
        },
        update: {},
      })
      created.push(w.word)
    } catch {
      skipped.push(w.word)
    }
  }

  return NextResponse.json({ created: created.length, skipped: skipped.length, words: created })
}
