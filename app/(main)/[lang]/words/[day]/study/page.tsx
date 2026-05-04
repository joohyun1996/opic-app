'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams, useSearchParams } from 'next/navigation'
import Header from '@/components/navigation/Header'
import FlashCard from '@/components/words/FlashCard'
import { isValidLang } from '@/lib/lang'

interface WordItem {
  id: number
  word: string
  phonetic?: string | null
  meaningKo: string
  partOfSpeech?: string | null
}

// 학습 완료 화면 — 정답/오답 수 요약 + 다음 액션
function ResultScreen({
  correct,
  wrong,
  onRetry,
  onBack,
}: {
  correct: number
  wrong: number
  onRetry: () => void
  onBack: () => void
}) {
  const total = correct + wrong
  const percent = total > 0 ? Math.round((correct / total) * 100) : 0

  return (
    <div className="flex flex-col items-center justify-center flex-1 px-6 gap-6">
      <div className="text-center">
        <p className="text-4xl font-bold mb-2" style={{ color: '#1a1a18' }}>{percent}%</p>
        <p className="text-sm" style={{ color: '#9ca3af' }}>정답률</p>
      </div>
      <div className="flex gap-8">
        <div className="text-center">
          <p className="text-2xl font-bold" style={{ color: '#16a34a' }}>{correct}</p>
          <p className="text-xs" style={{ color: '#9ca3af' }}>정답</p>
        </div>
        <div className="text-center">
          <p className="text-2xl font-bold" style={{ color: '#dc2626' }}>{wrong}</p>
          <p className="text-xs" style={{ color: '#9ca3af' }}>오답</p>
        </div>
      </div>
      <div className="flex flex-col gap-3 w-full mt-4">
        {wrong > 0 && (
          <button
            onClick={onRetry}
            className="w-full py-3.5 rounded-xl font-semibold text-sm border"
            style={{ borderColor: '#e8e6e0', color: '#1a1a18' }}
          >
            오답만 다시 풀기
          </button>
        )}
        <button
          onClick={onBack}
          className="w-full py-3.5 rounded-xl font-semibold text-sm"
          style={{ background: '#1a1a18', color: '#fff' }}
        >
          단어 목록으로
        </button>
      </div>
    </div>
  )
}

export default function StudyPage() {
  const router = useRouter()
  const params = useParams()
  const searchParams = useSearchParams()
  const lang = params.lang as string
  const day = params.day as string
  const mode = (searchParams.get('mode') ?? 'en-ko') as 'en-ko' | 'ko-en'

  const [words, setWords] = useState<WordItem[]>([])
  const [queue, setQueue] = useState<WordItem[]>([])
  const [index, setIndex] = useState(0)
  const [correctCount, setCorrectCount] = useState(0)
  const [wrongCount, setWrongCount] = useState(0)
  const [wrongWords, setWrongWords] = useState<WordItem[]>([])
  const [done, setDone] = useState(false)
  const [loading, setLoading] = useState(true)

  if (!isValidLang(lang)) {
    router.replace('/home')
    return null
  }

  useEffect(() => {
    async function fetchWords() {
      const res = await fetch(`/api/words?lang=${lang}&day=${day}`)
      if (res.ok) {
        const data = await res.json()
        setWords(data.words)
        setQueue(data.words)
      }
      setLoading(false)
    }
    fetchWords()
  }, [lang, day])

  // 정오답 처리 및 결과 API 호출
  async function handleResult(wordId: number, result: 'correct' | 'wrong') {
    // 결과를 서버에 기록 (fire-and-forget — 학습 흐름을 블로킹하지 않음)
    fetch('/api/user-words', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ wordId, result }),
    })

    if (result === 'correct') {
      setCorrectCount(c => c + 1)
    } else {
      setWrongCount(w => w + 1)
      const word = queue.find(w => w.id === wordId)
      if (word) setWrongWords(prev => [...prev, word])
    }

    if (index + 1 >= queue.length) {
      setDone(true)
    } else {
      setIndex(i => i + 1)
    }
  }

  // 오답만 다시 풀기 — 오답 단어들로 새 큐 구성
  function handleRetry() {
    setQueue(wrongWords)
    setIndex(0)
    setCorrectCount(0)
    setWrongCount(0)
    setWrongWords([])
    setDone(false)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-sm" style={{ color: '#9ca3af' }}>불러오는 중...</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen" style={{ background: '#fff' }}>
      <Header
        title={`Day ${day} · ${mode === 'en-ko' ? '영→한' : '한→영'}`}
        back={`/${lang}/words/${day}`}
      />

      {done ? (
        <ResultScreen
          correct={correctCount}
          wrong={wrongCount}
          onRetry={handleRetry}
          onBack={() => router.push(`/${lang}/words/${day}`)}
        />
      ) : queue.length > 0 ? (
        <FlashCard
          word={queue[index]}
          mode={mode}
          lang={lang as any}
          onResult={handleResult}
          current={index + 1}
          total={queue.length}
        />
      ) : (
        <div className="flex items-center justify-center flex-1">
          <p className="text-sm" style={{ color: '#9ca3af' }}>단어가 없습니다</p>
        </div>
      )}
    </div>
  )
}
