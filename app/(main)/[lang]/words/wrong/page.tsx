'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Header from '@/components/navigation/Header'
import WrongWordCard from '@/components/words/WrongWordCard'
import { isValidLang, type Lang } from '@/lib/lang'

interface WrongWord {
  id: number
  word: string
  phonetic?: string | null
  meaningKo: string
  partOfSpeech?: string | null
  wrongCount: number
  day: number
}

interface DayStat {
  day: number
  total: number
  mastered: number
  learning: number
  wrong: number
}

export default function WrongWordsPage() {
  const router = useRouter()
  const params = useParams()
  const lang = params.lang as string

  const [allWords, setAllWords] = useState<WrongWord[]>([])
  const [stats, setStats] = useState<DayStat[]>([])
  const [selectedDay, setSelectedDay] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)

  if (!isValidLang(lang)) {
    router.replace('/home')
    return null
  }

  useEffect(() => {
    async function fetchData() {
      // stats로 Day 목록 파악 후 오답 있는 Day의 단어를 수집
      const statsRes = await fetch(`/api/words/stats?lang=${lang}`)
      if (!statsRes.ok) { setLoading(false); return }
      const statsData = await statsRes.json()
      setStats(statsData.days)

      const wrongDays: DayStat[] = statsData.days.filter((d: DayStat) => d.wrong > 0)

      // 오답 있는 Day들의 단어를 병렬로 가져옴
      const results = await Promise.all(
        wrongDays.map((d: DayStat) =>
          fetch(`/api/words?lang=${lang}&day=${d.day}`)
            .then(r => r.json())
            .then(data => data.words
              .filter((w: any) => w.wrongCount > 0)
              .map((w: any) => ({ ...w, day: d.day }))
            )
        )
      )

      setAllWords(results.flat())
      setLoading(false)
    }
    fetchData()
  }, [lang])

  // 선택된 Day 필터 적용
  const filtered = selectedDay === null
    ? allWords
    : allWords.filter(w => w.day === selectedDay)

  // 오답 있는 Day만 필터 탭에 표시
  const wrongDayNums = [...new Set(allWords.map(w => w.day))].sort((a, b) => a - b)

  return (
    <div className="flex flex-col min-h-screen" style={{ background: '#f8f7f4' }}>
      <Header title="오답 모음" back={`/${lang}/words`} />

      {/* Day 필터 탭 */}
      <div
        className="flex gap-2 px-4 py-3 overflow-x-auto border-b"
        style={{ background: '#fff', borderColor: '#e8e6e0' }}
      >
        <button
          onClick={() => setSelectedDay(null)}
          className="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-semibold transition-colors"
          style={{
            background: selectedDay === null ? '#1a1a18' : '#f1efe8',
            color: selectedDay === null ? '#fff' : '#6b7280',
          }}
        >
          전체
        </button>
        {wrongDayNums.map(d => (
          <button
            key={d}
            onClick={() => setSelectedDay(d === selectedDay ? null : d)}
            className="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-semibold transition-colors"
            style={{
              background: selectedDay === d ? '#1a1a18' : '#f1efe8',
              color: selectedDay === d ? '#fff' : '#6b7280',
            }}
          >
            Day {d}
          </button>
        ))}
      </div>

      {/* 오답 단어 리스트 */}
      <div className="flex-1 px-4 pt-4 pb-24 flex flex-col gap-3">
        {loading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-16 rounded-xl animate-pulse" style={{ background: '#e8e6e0' }} />
          ))
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center flex-1 py-20 gap-2">
            <p className="text-base font-semibold" style={{ color: '#1a1a18' }}>오답이 없습니다</p>
            <p className="text-sm" style={{ color: '#9ca3af' }}>학습을 시작해 보세요</p>
          </div>
        ) : (
          filtered.map(w => (
            <WrongWordCard
              key={w.id}
              word={w.word}
              phonetic={w.phonetic}
              meaningKo={w.meaningKo}
              partOfSpeech={w.partOfSpeech}
              wrongCount={w.wrongCount}
              lang={lang as Lang}
            />
          ))
        )}
      </div>

      {/* 오답 학습 시작 버튼 — 단어가 있을 때만 표시 */}
      {!loading && filtered.length > 0 && (
        <div
          className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] px-4 pb-8 pt-4"
          style={{ background: 'linear-gradient(to top, #f8f7f4 70%, transparent)' }}
        >
          <button
            onClick={() => {
              // Day 선택 시 해당 Day study, 전체 선택 시 첫 오답 Day로 이동
              const targetDay = selectedDay ?? wrongDayNums[0]
              if (targetDay) router.push(`/${lang}/words/${targetDay}/study?mode=ko-en`)
            }}
            className="w-full py-3.5 rounded-xl font-semibold text-sm"
            style={{ background: '#dc2626', color: '#fff' }}
          >
            오답 학습 시작
          </button>
        </div>
      )}
    </div>
  )
}
