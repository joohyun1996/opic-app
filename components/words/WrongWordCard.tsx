'use client'

import { speak } from '@/lib/tts'
import type { Lang } from '@/lib/lang'

interface WrongWordCardProps {
  word: string
  phonetic?: string | null
  meaningKo: string
  partOfSpeech?: string | null
  wrongCount: number
  lang: Lang
}

export default function WrongWordCard({ word, phonetic, meaningKo, partOfSpeech, wrongCount, lang }: WrongWordCardProps) {
  return (
    <div
      className="flex items-center gap-3 px-4 py-3 rounded-xl border-l-4 bg-white"
      style={{ borderColor: '#dc2626', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' }}
    >
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-sm" style={{ color: '#1a1a18' }}>{word}</span>
          <button
            onClick={() => speak(word, lang)}
            className="w-6 h-6 flex items-center justify-center rounded-full active:bg-gray-100"
            aria-label="발음 듣기"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M11 5L6 9H2v6h4l5 4V5z" fill="#9ca3af" />
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14" stroke="#9ca3af" strokeWidth="2" strokeLinecap="round" />
              <path d="M15.54 8.46a5 5 0 0 1 0 7.07" stroke="#9ca3af" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>
        {phonetic && (
          <span className="text-xs" style={{ color: '#9ca3af' }}>{phonetic}</span>
        )}
        <p className="text-sm mt-0.5" style={{ color: '#4b5563' }}>
          {partOfSpeech && <span className="text-xs mr-1" style={{ color: '#9ca3af' }}>{partOfSpeech}</span>}
          {meaningKo}
        </p>
      </div>

      <span
        className="flex-shrink-0 text-xs font-bold px-2 py-1 rounded-full"
        style={{ background: '#fcebeb', color: '#dc2626' }}
      >
        {wrongCount}회
      </span>
    </div>
  )
}
