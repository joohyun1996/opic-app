'use client'

import { speak } from '@/lib/tts'
import type { Lang } from '@/lib/lang'

interface WordListItemProps {
  word: string
  phonetic?: string | null
  meaningKo: string
  partOfSpeech?: string | null
  status: 'new' | 'learning' | 'mastered'
  wrongCount: number
  lang: Lang
}

// CLAUDE.md 지정 상태 배지 색상
const STATUS_STYLE: Record<string, { label: string; bg: string; color: string }> = {
  mastered: { label: '습득', bg: '#eaf3de', color: '#166534' },
  learning: { label: '학습중', bg: '#faeeda', color: '#92400e' },
  new: { label: '신규', bg: '#f1efe8', color: '#6b7280' },
}

export default function WordListItem({ word, phonetic, meaningKo, partOfSpeech, status, wrongCount, lang }: WordListItemProps) {
  const s = STATUS_STYLE[status] ?? STATUS_STYLE.new

  return (
    <div
      className="flex items-center gap-3 px-4 py-3 border-b"
      style={{ borderColor: '#e8e6e0' }}
    >
      {wrongCount > 0 && (
        <div data-testid="wrong-bar" className="w-1 self-stretch rounded-full flex-shrink-0" style={{ background: '#dc2626' }} />
      )}

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-sm" style={{ color: '#1a1a18' }}>
            {word}
          </span>
          <button
            onClick={() => speak(word, lang)}
            className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full active:bg-gray-100"
            aria-label="발음 듣기"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M11 5L6 9H2v6h4l5 4V5z" fill="#6b7280" />
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14" stroke="#6b7280" strokeWidth="2" strokeLinecap="round" />
              <path d="M15.54 8.46a5 5 0 0 1 0 7.07" stroke="#6b7280" strokeWidth="2" strokeLinecap="round" />
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
        className="flex-shrink-0 text-[11px] font-medium px-2 py-0.5 rounded-full"
        style={{ background: s.bg, color: s.color }}
      >
        {s.label}
      </span>
    </div>
  )
}
