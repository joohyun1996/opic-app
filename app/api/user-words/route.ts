import { NextRequest, NextResponse } from 'next/server'
import { getIronSession } from 'iron-session'
import { prisma } from '@/lib/prisma'
import { sessionOptions, SessionData } from '@/lib/session'

const MASTERED_THRESHOLD = 3

export async function POST(request: NextRequest) {
  const res = NextResponse.next()
  const session = await getIronSession<SessionData>(request, res, sessionOptions)
  if (!session.userId) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const { wordId, result } = await request.json()

  if (!wordId || (result !== 'correct' && result !== 'wrong')) {
    return NextResponse.json({ error: 'Invalid params' }, { status: 400 })
  }

  const existing = await prisma.userWord.findUnique({
    where: { userId_wordId: { userId: session.userId, wordId } },
  })

  const correctCount = (existing?.correctCount ?? 0) + (result === 'correct' ? 1 : 0)
  const wrongCount = (existing?.wrongCount ?? 0) + (result === 'wrong' ? 1 : 0)
  const status = correctCount >= MASTERED_THRESHOLD ? 'mastered' : 'learning'

  const userWord = await prisma.userWord.upsert({
    where: { userId_wordId: { userId: session.userId, wordId } },
    create: {
      userId: session.userId,
      wordId,
      status,
      correctCount,
      wrongCount,
      lastStudied: new Date(),
    },
    update: {
      status,
      correctCount,
      wrongCount,
      lastStudied: new Date(),
    },
  })

  return NextResponse.json({ userWord })
}
