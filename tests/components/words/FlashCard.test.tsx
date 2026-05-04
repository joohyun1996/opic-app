import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashCard, { makeHint, checkAnswer } from '@/components/words/FlashCard'

// Web Speech API와 next/navigation은 테스트 환경에 없으므로 모킹
vi.mock('@/lib/tts', () => ({ speak: vi.fn() }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: vi.fn() }) }))

const mockWord = {
  id: 1,
  word: 'ambitious',
  phonetic: '/æmˈbɪʃəs/',
  meaningKo: '야심찬',
  partOfSpeech: 'adjective',
}

const defaultProps = {
  word: mockWord,
  lang: 'en' as const,
  onResult: vi.fn(),
  current: 1,
  total: 10,
}

describe('makeHint', () => {
  it('긴 단어는 첫글자_빈칸_마지막글자 형태로 반환한다', () => {
    expect(makeHint('correct')).toBe('c _ _ _ _ _ t')
  })

  it('2글자 이하는 그대로 반환한다', () => {
    expect(makeHint('go')).toBe('go')
    expect(makeHint('a')).toBe('a')
  })
})

describe('checkAnswer', () => {
  it('소문자 변환 후 완전 일치하면 true', () => {
    expect(checkAnswer('Ambitious', '야심찬')).toBe(false)
    expect(checkAnswer('야심찬', '야심찬')).toBe(true)
    expect(checkAnswer('AMBITIOUS', 'ambitious')).toBe(true)
  })

  it('앞뒤 공백은 무시한다', () => {
    expect(checkAnswer('  ambitious  ', 'ambitious')).toBe(true)
  })

  it('부분 일치는 false', () => {
    expect(checkAnswer('ambiti', 'ambitious')).toBe(false)
  })
})

describe('FlashCard — en-ko 모드', () => {
  beforeEach(() => defaultProps.onResult.mockClear())

  it('영어 단어와 발음기호를 렌더링한다', () => {
    render(<FlashCard {...defaultProps} mode="en-ko" />)
    expect(screen.getByText('ambitious')).toBeInTheDocument()
    expect(screen.getByText('/æmˈbɪʃəs/')).toBeInTheDocument()
  })

  it('정답 입력 후 확인 시 ✓ 표시', async () => {
    const user = userEvent.setup()
    render(<FlashCard {...defaultProps} mode="en-ko" />)
    await user.type(screen.getByRole('textbox'), '야심찬')
    await user.click(screen.getByRole('button', { name: '확인' }))
    expect(screen.getByText('✓')).toBeInTheDocument()
  })

  it('오답 입력 후 확인 시 ✗ + 정답 표시', async () => {
    const user = userEvent.setup()
    render(<FlashCard {...defaultProps} mode="en-ko" />)
    await user.type(screen.getByRole('textbox'), '틀린답')
    await user.click(screen.getByRole('button', { name: '확인' }))
    expect(screen.getByText('✗')).toBeInTheDocument()
    expect(screen.getByText(/정답: 야심찬/)).toBeInTheDocument()
  })

  it('완료 버튼 클릭 시 onResult 호출', async () => {
    const user = userEvent.setup()
    render(<FlashCard {...defaultProps} mode="en-ko" />)
    await user.type(screen.getByRole('textbox'), '야심찬')
    await user.click(screen.getByRole('button', { name: '확인' }))
    await user.click(screen.getByRole('button', { name: /다음|완료/ }))
    expect(defaultProps.onResult).toHaveBeenCalledWith(1, 'correct')
  })
})

describe('FlashCard — ko-en 모드', () => {
  beforeEach(() => defaultProps.onResult.mockClear())

  it('한국어 뜻과 힌트를 렌더링한다', () => {
    render(<FlashCard {...defaultProps} mode="ko-en" />)
    expect(screen.getByText('야심찬')).toBeInTheDocument()
    // makeHint('ambitious') = 9글자 → 중간 7자 마스킹 → 'a _ _ _ _ _ _ _ s'
    expect(screen.getByText('a _ _ _ _ _ _ _ s')).toBeInTheDocument()
  })

  it('오답 시 onResult(wordId, wrong) 호출', async () => {
    const user = userEvent.setup()
    render(<FlashCard {...defaultProps} mode="ko-en" />)
    await user.type(screen.getByRole('textbox'), '틀린단어')
    await user.click(screen.getByRole('button', { name: '확인' }))
    await user.click(screen.getByRole('button', { name: /다음|완료/ }))
    expect(defaultProps.onResult).toHaveBeenCalledWith(1, 'wrong')
  })
})
