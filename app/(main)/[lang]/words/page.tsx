'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Header from '@/components/navigation/Header'
import DayCard from '@/components/words/DayCard'
import { isValidLang, LANG_CONFIG, type Lang } from '@/lib/lang'

interface DayStat {
  day: number
  total: number
  mastered: number
  learning: number
  wrong: number
}

interface StatsResponse {
  totalDays: number
  days: DayStat[]
}

// Day 인덱스 스켈레톤 — 데이터 로딩 중 레이아웃 유지
function SkeletonGrid() {
  return (
    <div className="grid grid-cols-4 gap-2 px-4 pt-4">
      {Array.from({ length: 8 }).map((_, i) => (
        <div
          key={i}
          className="rounded-xl animate-pulse"
          style={{ height: 88, background: '#e8e6e0' }}
        />
      ))}
    </div>
  )
}

export default function WordsIndexPage() {
  const router = useRouter()
  const params = useParams()
  const lang = params.lang as string

  const [stats, setStats] = useState<StatsResponse | null>(null)
  const [loading, setLoading] = useState(true)

  // lang 파라미터 유효성 검사 — 잘못된 언어 접근 시 홈으로 복귀
  if (!isValidLang(lang)) {
    router.replace('/home')
    return null
  }

  const config = LANG_CONFIG[lang as Lang]

  // 전체 오답 합산 — 헤더 뱃지 표시용
  const totalWrong = stats?.days.reduce((s, d) => s + d.wrong, 0) ?? 0

  useEffect(() => {
    async function fetchStats() {
      const res = await fetch(`/api/words/stats?lang=${lang}`)
      if (res.ok) setStats(await res.json())
      setLoading(false)
    }
    fetchStats()
  }, [lang])

  return (
    <div className="flex flex-col min-h-screen" style={{ background: '#f8f7f4' }}>
      <Header
        title={`${config.label} 단어`}
        back="/home"
        action={
          totalWrong > 0 ? (
            // 오답이 있을 때만 헤더 우측에 뱃지 표시
            <button
              onClick={() => router.push(`/${lang}/words/wrong`)}
              className="text-xs font-bold px-2.5 py-1 rounded-full"
              style={{ background: '#fef2f2', color: '#dc2626' }}
            >
              오답 {totalWrong}
            </button>
          ) : undefined
        }
      />

      {/* 전체 진행률 바 */}
      {stats && (
        <div className="px-4 py-3" style={{ background: '#fff', borderBottom: '1px solid #e8e6e0' }}>
          {(() => {
            const totalWords = stats.days.reduce((s, d) => s + d.total, 0)
            const mastered = stats.days.reduce((s, d) => s + d.mastered, 0)
            const percent = totalWords > 0 ? Math.round((mastered / totalWords) * 100) : 0
            return (
              <div className="flex items-center gap-3">
                <div className="flex-1 rounded-full overflow-hidden" style={{ height: 6, background: '#e8e6e0' }}>
                  <div
                    className="h-full rounded-full transition-all"
                    style={{ width: `${percent}%`, background: '#1a1a18' }}
                  />
                </div>
                <span className="text-xs font-semibold flex-shrink-0" style={{ color: '#1a1a18' }}>
                  {mastered} / {totalWords}
                </span>
              </div>
            )
          })()}
        </div>
      )}

      {/* Day 그리드 */}
      {loading ? (
        <SkeletonGrid />
      ) : !stats || stats.days.length === 0 ? (
        // 단어 데이터가 없는 경우 안내 메시지
        <div className="flex flex-col items-center justify-center flex-1 gap-2 px-8 text-center">
          <p className="text-base font-semibold" style={{ color: '#1a1a18' }}>단어 데이터가 없습니다</p>
          <p className="text-sm" style={{ color: '#9ca3af' }}>관리자가 씨드 데이터를 추가해야 합니다</p>
        </div>
      ) : (
        <div className="grid grid-cols-4 gap-2 px-4 pt-4 pb-8">
          {stats.days.map((d) => (
            <DayCard
              key={d.day}
              {...d}
              onClick={() => router.push(`/${lang}/words/${d.day}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
