'use client'

import { useState, useRef, useEffect } from 'react'
import { speak } from '@/lib/tts'
import type { Lang } from '@/lib/lang'

interface Word {
  id: number
  word: string
  phonetic?: string | null
  meaningKo: string
  partOfSpeech?: string | null
}

interface FlashCardProps {
  word: Word
  mode: 'en-ko' | 'ko-en'
  lang: Lang
  onResult: (wordId: number, result: 'correct' | 'wrong') => void
  current: number
  total: number
}

// 첫글자 + 빈칸(글자수만큼) + 마지막글자 힌트 생성
// 예) "correct" → "c _ _ _ _ _ t"
export function makeHint(word: string): string {
  if (word.length <= 2) return word
  const hidden = Array(word.length - 2).fill('_').join(' ')
  return `${word[0]} ${hidden} ${word[word.length - 1]}`
}

// 소문자 변환 후 완전 일치 — CLAUDE.md 정답 판정 규칙
export function checkAnswer(input: string, answer: string): boolean {
  return input.trim().toLowerCase() === answer.toLowerCase()
}

// 스피커 아이콘 SVG — 두 곳에서 재사용
function SpeakerIcon({ color = '#6b7280' }: { color?: string }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
      <path d="M11 5L6 9H2v6h4l5 4V5z" fill={color} />
      <path d="M19.07 4.93a10 10 0 0 1 0 14.14" stroke={color} strokeWidth="2" strokeLinecap="round" />
      <path d="M15.54 8.46a5 5 0 0 1 0 7.07" stroke={color} strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

export default function FlashCard({ word, mode, lang, onResult, current, total }: FlashCardProps) {
  const [input, setInput] = useState('')
  const [checked, setChecked] = useState(false)
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // 카드 전환(word.id 변경) 시 상태 초기화 및 입력 포커스
  useEffect(() => {
    setInput('')
    setChecked(false)
    setIsCorrect(null)
    setTimeout(() => inputRef.current?.focus(), 80)
  }, [word.id, mode])

  function handleCheck() {
    if (!input.trim()) return
    const answer = mode === 'en-ko' ? word.meaningKo : word.word
    const correct = checkAnswer(input, answer)
    setIsCorrect(correct)
    setChecked(true)
    // 한→영 정답 시 자동 발음 재생
    if (correct && mode === 'ko-en') speak(word.word, lang)
  }

  function handleNext() {
    onResult(word.id, isCorrect ? 'correct' : 'wrong')
  }

  // Enter 키: 미확인 → 확인, 확인 후 → 다음
  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter') {
      if (!checked) handleCheck()
      else handleNext()
    }
  }

  const cardBorder = !checked ? '#e8e6e0' : isCorrect ? '#16a34a' : '#dc2626'
  const cardBg = !checked ? '#fff' : isCorrect ? '#f6fef9' : '#fff8f8'

  return (
    <div className="flex flex-col h-full px-4 pt-4 pb-6">

      {/* 진행률 바 */}
      <div className="flex items-center gap-2 mb-6">
        <div className="flex-1 rounded-full overflow-hidden" style={{ height: 4, background: '#e8e6e0' }}>
          <div
            className="h-full rounded-full transition-all"
            style={{ width: `${(current / total) * 100}%`, background: '#1a1a18' }}
          />
        </div>
        <span className="text-xs flex-shrink-0" style={{ color: '#9ca3af' }}>{current}/{total}</span>
      </div>

      {/* 문제 카드 */}
      <div
        className="rounded-2xl p-6 mb-4 border-2 transition-colors"
        style={{ borderColor: cardBorder, background: cardBg }}
      >
        {mode === 'en-ko' ? (
          // ④ 영→한: 영어 단어 + ♪ + 발음기호 표시
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <span className="text-3xl font-bold" style={{ color: '#1a1a18' }}>{word.word}</span>
              <button
                onClick={() => speak(word.word, lang)}
                className="w-8 h-8 flex items-center justify-center rounded-full"
                style={{ background: '#f3f4f6' }}
                aria-label="발음 듣기"
              >
                <SpeakerIcon />
              </button>
            </div>
            {word.phonetic && (
              <span className="text-sm" style={{ color: '#9ca3af' }}>{word.phonetic}</span>
            )}
          </div>
        ) : (
          // ⑤ 한→영: 한국어 뜻 + 품사 + 힌트(첫글자 _ _ _ 마지막글자)
          <div className="flex flex-col gap-2">
            <p className="text-2xl font-semibold" style={{ color: '#1a1a18' }}>{word.meaningKo}</p>
            {word.partOfSpeech && (
              <span className="text-xs" style={{ color: '#9ca3af' }}>{word.partOfSpeech}</span>
            )}
            <p className="text-base font-mono tracking-widest mt-1" style={{ color: '#d1d5db' }}>
              {makeHint(word.word)}
            </p>
          </div>
        )}

        {/* 정답 확인 결과 표시 */}
        {checked && (
          <div className="flex items-start gap-2 mt-4 pt-4 border-t" style={{ borderColor: '#e8e6e0' }}>
            <span className="text-lg leading-none mt-0.5">{isCorrect ? '✓' : '✗'}</span>
            {isCorrect ? (
              // 정답: 한→영 모드에서 정답 단어 + ♪ + 발음기호
              mode === 'ko-en' && (
                <div className="flex items-center gap-2">
                  <span className="font-semibold" style={{ color: '#16a34a' }}>{word.word}</span>
                  <button onClick={() => speak(word.word, lang)} aria-label="발음 듣기">
                    <SpeakerIcon color="#16a34a" />
                  </button>
                  {word.phonetic && (
                    <span className="text-xs" style={{ color: '#9ca3af' }}>{word.phonetic}</span>
                  )}
                </div>
              )
            ) : (
              // 오답: 정답 표시
              <div>
                <p className="text-sm font-medium" style={{ color: '#dc2626' }}>
                  정답: {mode === 'en-ko' ? word.meaningKo : word.word}
                </p>
                {mode === 'ko-en' && word.phonetic && (
                  <span className="text-xs" style={{ color: '#9ca3af' }}>{word.phonetic}</span>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 답안 입력 필드 */}
      <input
        ref={inputRef}
        value={input}
        onChange={e => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={checked}
        placeholder={mode === 'en-ko' ? '한국어 뜻을 입력하세요' : '영어 단어를 입력하세요'}
        className="w-full px-4 py-3 rounded-xl border text-sm mb-4 outline-none transition-colors"
        style={{
          borderColor: !checked ? '#e8e6e0' : isCorrect ? '#16a34a' : '#dc2626',
          background: '#fff',
          color: '#1a1a18',
        }}
      />

      {/* 확인 버튼 → 다음/완료 버튼으로 전환 */}
      {!checked ? (
        <button
          onClick={handleCheck}
          disabled={!input.trim()}
          className="w-full py-3 rounded-xl font-semibold text-sm transition-opacity"
          style={{ background: '#1a1a18', color: '#fff', opacity: input.trim() ? 1 : 0.4 }}
        >
          확인
        </button>
      ) : (
        <button
          onClick={handleNext}
          className="w-full py-3 rounded-xl font-semibold text-sm"
          style={{ background: '#1a1a18', color: '#fff' }}
        >
          {current < total ? '다음' : '완료'}
        </button>
      )}
    </div>
  )
}
