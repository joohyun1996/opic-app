'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Header from '@/components/navigation/Header'
import WordListItem from '@/components/words/WordListItem'
import { isValidLang, type Lang } from '@/lib/lang'

interface WordItem {
  id: number
  word: string
  phonetic?: string | null
  meaningKo: string
  partOfSpeech?: string | null
  status: 'new' | 'learning' | 'mastered'
  wrongCount: number
}

// 학습 모드 선택 모달 — 영→한 / 한→영 선택 후 study 페이지로 이동
function ModeModal({ onClose, onSelect }: { onClose: () => void; onSelect: (mode: string) => void }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center"
      style={{ background: 'rgba(0,0,0,0.4)' }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-[430px] bg-white rounded-t-2xl p-6 pb-10"
        onClick={e => e.stopPropagation()}
      >
        <p className="text-base font-bold mb-4" style={{ color: '#1a1a18' }}>학습 방향 선택</p>
        <div className="flex flex-col gap-3">
          <button
            onClick={() => onSelect('en-ko')}
            className="w-full py-3.5 rounded-xl text-sm font-semibold border"
            style={{ borderColor: '#e8e6e0', color: '#1a1a18' }}
          >
            영어 → 한국어
          </button>
          <button
            onClick={() => onSelect('ko-en')}
            className="w-full py-3.5 rounded-xl text-sm font-semibold"
            style={{ background: '#1a1a18', color: '#fff' }}
          >
            한국어 → 영어
          </button>
        </div>
      </div>
    </div>
  )
}

export default function DayWordListPage() {
  const router = useRouter()
  const params = useParams()
  const lang = params.lang as string
  const day = params.day as string

  const [words, setWords] = useState<WordItem[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)

  if (!isValidLang(lang)) {
    router.replace('/home')
    return null
  }

  const mastered = words.filter(w => w.status === 'mastered').length

  useEffect(() => {
    async function fetchWords() {
      const res = await fetch(`/api/words?lang=${lang}&day=${day}`)
      if (res.ok) {
        const data = await res.json()
        setWords(data.words)
      }
      setLoading(false)
    }
    fetchWords()
  }, [lang, day])

  function handleModeSelect(mode: string) {
    router.push(`/${lang}/words/${day}/study?mode=${mode}`)
  }

  return (
    <div className="flex flex-col min-h-screen" style={{ background: '#f8f7f4' }}>
      <Header title={`Day ${day}`} back={`/${lang}/words`} />

      {/* 진행률 바 */}
      {!loading && (
        <div className="px-4 py-3 border-b" style={{ background: '#fff', borderColor: '#e8e6e0' }}>
          <div className="flex items-center gap-3">
            <div className="flex-1 rounded-full overflow-hidden" style={{ height: 6, background: '#e8e6e0' }}>
              <div
                className="h-full rounded-full transition-all"
                style={{
                  width: words.length > 0 ? `${(mastered / words.length) * 100}%` : '0%',
                  background: mastered === words.length && words.length > 0 ? '#16a34a' : '#1a1a18',
                }}
              />
            </div>
            <span className="text-xs font-semibold flex-shrink-0" style={{ color: '#1a1a18' }}>
              {mastered} / {words.length}
            </span>
          </div>
        </div>
      )}

      {/* 단어 리스트 */}
      <div className="flex-1 bg-white pb-24">
        {loading ? (
          <div className="flex flex-col">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="px-4 py-4 border-b animate-pulse" style={{ borderColor: '#e8e6e0' }}>
                <div className="h-4 rounded mb-2" style={{ background: '#e8e6e0', width: '40%' }} />
                <div className="h-3 rounded" style={{ background: '#e8e6e0', width: '60%' }} />
              </div>
            ))}
          </div>
        ) : words.length === 0 ? (
          <div className="flex items-center justify-center h-40">
            <p className="text-sm" style={{ color: '#9ca3af' }}>단어 데이터가 없습니다</p>
          </div>
        ) : (
          words.map(w => (
            <WordListItem
              key={w.id}
              word={w.word}
              phonetic={w.phonetic}
              meaningKo={w.meaningKo}
              partOfSpeech={w.partOfSpeech}
              status={w.status}
              wrongCount={w.wrongCount}
              lang={lang as Lang}
            />
          ))
        )}
      </div>

      {/* 하단 고정 학습 시작 버튼 */}
      {!loading && words.length > 0 && (
        <div
          className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] px-4 pb-8 pt-4"
          style={{ background: 'linear-gradient(to top, #fff 70%, transparent)' }}
        >
          <button
            onClick={() => setShowModal(true)}
            className="w-full py-3.5 rounded-xl font-semibold text-sm"
            style={{ background: '#1a1a18', color: '#fff' }}
          >
            학습 시작
          </button>
        </div>
      )}

      {showModal && (
        <ModeModal onClose={() => setShowModal(false)} onSelect={handleModeSelect} />
      )}
    </div>
  )
}
