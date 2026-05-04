import { NextRequest, NextResponse } from 'next/server'
import { getIronSession } from 'iron-session'
import { prisma } from '@/lib/prisma'
import { sessionOptions, SessionData } from '@/lib/session'
import { isValidLang } from '@/lib/lang'

const PAGE_SIZE = 40

export async function GET(request: NextRequest) {
  const res = NextResponse.next()
  const session = await getIronSession<SessionData>(request, res, sessionOptions)
  if (!session.userId) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const { searchParams } = request.nextUrl
  const lang = searchParams.get('lang')
  const dayParam = searchParams.get('day')

  if (!lang || !isValidLang(lang)) {
    return NextResponse.json({ error: 'Invalid lang' }, { status: 400 })
  }
  if (!dayParam || isNaN(Number(dayParam))) {
    return NextResponse.json({ error: 'Invalid day' }, { status: 400 })
  }

  const day = Math.max(1, parseInt(dayParam))
  const skip = (day - 1) * PAGE_SIZE

  const [totalCount, words] = await Promise.all([
    prisma.word.count({ where: { language: lang } }),
    prisma.word.findMany({
      where: { language: lang },
      orderBy: [{ level: 'asc' }, { id: 'asc' }],
      skip,
      take: PAGE_SIZE,
      include: {
        userWords: {
          where: { userId: session.userId },
        },
      },
    }),
  ])

  const totalDays = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))

  const result = words.map((word) => {
    const userWord = word.userWords[0] ?? null
    return {
      id: word.id,
      word: word.word,
      phonetic: word.phonetic,
      meaningKo: word.meaningKo,
      meaningEn: word.meaningEn,
      example: word.example,
      exampleKo: word.exampleKo,
      level: word.level,
      category: word.category,
      partOfSpeech: word.partOfSpeech,
      collocations: word.collocations,
      status: userWord?.status ?? 'new',
      correctCount: userWord?.correctCount ?? 0,
      wrongCount: userWord?.wrongCount ?? 0,
      nextReview: userWord?.nextReview ?? null,
    }
  })

  return NextResponse.json({ words: result, totalDays })
}
