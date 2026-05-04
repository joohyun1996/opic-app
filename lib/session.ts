import { SessionOptions } from 'iron-session'

export interface SessionData {
  userId: number
  username: string
  role: 'admin' | 'member'
}

export const sessionOptions: SessionOptions = {
  cookieName: 'opic_session',
  password: process.env.SESSION_SECRET!,
  cookieOptions: {
    secure: process.env.NODE_ENV === 'production',
    maxAge: 60 * 60 * 24 * 7,
  },
}