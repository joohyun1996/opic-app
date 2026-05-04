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

  if (!lang || !isValidLang(lang)) {
    return NextResponse.json({ error: 'Invalid lang' }, { status: 400 })
  }

  const words = await prisma.word.findMany({
    where: { language: lang },
    orderBy: [{ level: 'asc' }, { id: 'asc' }],
    select: {
      id: true,
      userWords: {
        where: { userId: session.userId },
        select: { status: true, wrongCount: true },
      },
    },
  })

  const totalDays = Math.max(1, Math.ceil(words.length / PAGE_SIZE))

  const days = Array.from({ length: totalDays }, (_, i) => {
    const slice = words.slice(i * PAGE_SIZE, (i + 1) * PAGE_SIZE)
    let mastered = 0
    let learning = 0
    let wrong = 0

    for (const w of slice) {
      const uw = w.userWords[0]
      if (!uw) continue
      if (uw.status === 'mastered') mastered++
      else if (uw.status === 'learning') learning++
      if (uw.wrongCount > 0) wrong++
    }

    return { day: i + 1, total: slice.length, mastered, learning, wrong }
  })

  return NextResponse.json({ totalDays, days })
}
