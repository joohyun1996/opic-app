import { NextRequest, NextResponse } from 'next/server'
import { getIronSession } from 'iron-session'
import bcrypt from 'bcryptjs'
import { prisma } from '@/lib/prisma'
import { sessionOptions, SessionData } from '@/lib/session'

export async function POST(request: NextRequest) {
  const { username, password } = await request.json()

  const user = await prisma.user.findUnique({ where: { username } })

  if (!user || !(await bcrypt.compare(password, user.password))) {
    return NextResponse.json(
      { error: '아이디 또는 비밀번호가 틀렸습니다' },
      { status: 401 }
    )
  }

  const response = NextResponse.json({ ok: true })
  const session = await getIronSession<SessionData>(request, response, sessionOptions)

  session.userId = user.id
  session.username = user.username
  session.role = user.role as 'admin' | 'member'
  await session.save()

  return response
}
