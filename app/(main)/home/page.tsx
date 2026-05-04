'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { LANG_CONFIG, type Lang } from '@/lib/lang'

interface DayStat {
  day: number
  total: number
  mastered: number
  learning: number
  wrong: number
}

interface LangStats {
  totalDays: number
  days: DayStat[]
}

function LangCard({
  lang,
  stats,
  onClick,
}: {
  lang: Lang
  stats: LangStats | null
  onClick: () => void
}) {
  const config = LANG_CONFIG[lang]
  const totalWords = stats ? stats.days.reduce((s, d) => s + d.total, 0) : 0
  const mastered = stats ? stats.days.reduce((s, d) => s + d.mastered, 0) : 0
  const totalWrong = stats ? stats.days.reduce((s, d) => s + d.wrong, 0) : 0
  const percent = totalWords > 0 ? Math.round((mastered / totalWords) * 100) : 0

  return (
    <button
      onClick={onClick}
      className="w-full text-left rounded-2xl border p-5 active:scale-[0.98] transition-transform"
      style={{ borderColor: '#e8e6e0', background: '#fff' }}
    >
      <div className="flex items-center justify-between mb-3">
        <div>
          <p className="text-xs font-medium mb-0.5" style={{ color: '#9ca3af' }}>{config.examName}</p>
          <h2 className="text-xl font-bold" style={{ color: '#1a1a18' }}>{config.label}</h2>
        </div>
        {totalWrong > 0 && (
          <span
            className="text-xs font-bold px-2.5 py-1 rounded-full"
            style={{ background: '#fef2f2', color: '#dc2626' }}
          >
            오답 {totalWrong}
          </span>
        )}
      </div>

      <div className="flex items-center gap-2 mb-2">
        <div className="flex-1 rounded-full overflow-hidden" style={{ height: 6, background: '#e8e6e0' }}>
          <div
            className="h-full rounded-full transition-all"
            style={{
              width: `${percent}%`,
              background: percent === 100 ? '#16a34a' : '#1a1a18',
            }}
          />
        </div>
        <span className="text-xs font-semibold flex-shrink-0" style={{ color: '#1a1a18' }}>
          {percent}%
        </span>
      </div>

      <p className="text-xs" style={{ color: '#9ca3af' }}>
        {mastered} / {totalWords} 단어 습득
      </p>
    </button>
  )
}

export default function HomePage() {
  const router = useRouter()
  const [enStats, setEnStats] = useState<LangStats | null>(null)
  const [zhStats, setZhStats] = useState<LangStats | null>(null)

  useEffect(() => {
    async function fetchStats() {
      const [enRes, zhRes] = await Promise.all([
        fetch('/api/words/stats?lang=en'),
        fetch('/api/words/stats?lang=zh'),
      ])
      if (enRes.ok) setEnStats(await enRes.json())
      if (zhRes.ok) setZhStats(await zhRes.json())
    }
    fetchStats()
  }, [])

  return (
    <div className="flex flex-col min-h-screen" style={{ background: '#f8f7f4' }}>
      <header className="px-5 pt-10 pb-6">
        <p className="text-xs font-medium mb-1" style={{ color: '#9ca3af' }}>OPIc · HSK</p>
        <h1 className="text-2xl font-bold" style={{ color: '#1a1a18' }}>학습 언어 선택</h1>
      </header>

      <div className="flex flex-col gap-4 px-5">
        <LangCard lang="en" stats={enStats} onClick={() => router.push('/en/words')} />
        <LangCard lang="zh" stats={zhStats} onClick={() => router.push('/zh/words')} />
      </div>
    </div>
  )
}
