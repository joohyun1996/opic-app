import { describe, it, expect } from 'vitest'
import { isValidLang, LANG_CONFIG } from '@/lib/lang'

describe('isValidLang', () => {
  it('en은 유효한 언어다', () => {
    expect(isValidLang('en')).toBe(true)
  })

  it('zh는 유효한 언어다', () => {
    expect(isValidLang('zh')).toBe(true)
  })

  it('ko는 유효하지 않다', () => {
    expect(isValidLang('ko')).toBe(false)
  })

  it('빈 문자열은 유효하지 않다', () => {
    expect(isValidLang('')).toBe(false)
  })
})

describe('LANG_CONFIG', () => {
  it('en 설정에 OPIc 정보가 있다', () => {
    expect(LANG_CONFIG.en.examName).toBe('OPIc')
    expect(LANG_CONFIG.en.wordLevels).toBe(5)
    expect(LANG_CONFIG.en.whisperLang).toBe('en')
  })

  it('zh 설정에 HSK 정보가 있다', () => {
    expect(LANG_CONFIG.zh.examName).toBe('HSK')
    expect(LANG_CONFIG.zh.wordLevels).toBe(6)
    expect(LANG_CONFIG.zh.whisperLang).toBe('zh')
  })
})
