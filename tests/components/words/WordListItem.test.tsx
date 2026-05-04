import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import WordListItem from '@/components/words/WordListItem'

vi.mock('@/lib/tts', () => ({ speak: vi.fn() }))

const defaultProps = {
  word: 'ambitious',
  phonetic: '/æmˈbɪʃəs/',
  meaningKo: '야심찬',
  partOfSpeech: 'adjective',
  status: 'new' as const,
  wrongCount: 0,
  lang: 'en' as const,
}

describe('WordListItem', () => {
  it('단어와 한국어 뜻을 렌더링한다', () => {
    render(<WordListItem {...defaultProps} />)
    expect(screen.getByText('ambitious')).toBeInTheDocument()
    expect(screen.getByText('야심찬')).toBeInTheDocument()
  })

  it('발음기호를 표시한다', () => {
    render(<WordListItem {...defaultProps} />)
    expect(screen.getByText('/æmˈbɪʃəs/')).toBeInTheDocument()
  })

  it('status=new 이면 신규 뱃지를 표시한다', () => {
    render(<WordListItem {...defaultProps} status="new" />)
    expect(screen.getByText('신규')).toBeInTheDocument()
  })

  it('status=learning 이면 학습중 뱃지를 표시한다', () => {
    render(<WordListItem {...defaultProps} status="learning" />)
    expect(screen.getByText('학습중')).toBeInTheDocument()
  })

  it('status=mastered 이면 습득 뱃지를 표시한다', () => {
    render(<WordListItem {...defaultProps} status="mastered" />)
    expect(screen.getByText('습득')).toBeInTheDocument()
  })

  it('wrongCount > 0 이면 빨간 왼쪽 선이 렌더링된다', () => {
    render(<WordListItem {...defaultProps} wrongCount={2} />)
    expect(screen.getByTestId('wrong-bar')).toBeInTheDocument()
  })

  it('wrongCount === 0 이면 빨간 왼쪽 선이 없다', () => {
    render(<WordListItem {...defaultProps} wrongCount={0} />)
    expect(screen.queryByTestId('wrong-bar')).not.toBeInTheDocument()
  })
})
