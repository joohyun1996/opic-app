import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import DayCard from '@/components/words/DayCard'

const defaultProps = {
  day: 1,
  total: 40,
  mastered: 10,
  learning: 5,
  wrong: 0,
  onClick: vi.fn(),
}

describe('DayCard', () => {
  it('Day 번호를 렌더링한다', () => {
    render(<DayCard {...defaultProps} />)
    expect(screen.getByText('Day 1')).toBeInTheDocument()
  })

  it('mastered/total 수치를 표시한다', () => {
    render(<DayCard {...defaultProps} />)
    expect(screen.getByText('10')).toBeInTheDocument()
    expect(screen.getByText('/40')).toBeInTheDocument()
  })

  it('wrong > 0 이면 오답 뱃지를 표시한다', () => {
    render(<DayCard {...defaultProps} wrong={3} />)
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('wrong === 0 이면 오답 뱃지를 표시하지 않는다', () => {
    render(<DayCard {...defaultProps} wrong={0} />)
    // wrong 뱃지는 숫자만 표시하므로 "3" 같은 텍스트 없어야 함
    expect(screen.queryByText('3')).not.toBeInTheDocument()
  })

  it('클릭 시 onClick 호출', () => {
    render(<DayCard {...defaultProps} />)
    fireEvent.click(screen.getByRole('button'))
    expect(defaultProps.onClick).toHaveBeenCalledOnce()
  })
})
