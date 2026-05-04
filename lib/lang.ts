export type Lang = 'en' | 'zh'

export const LANG_CONFIG = {
  en: {
    label: '영어',
    examName: 'OPIc',
    levels: ['IL', 'IM1', 'IM2', 'IM3', 'IH', 'AL'],
    targetLevel: 'AL',
    wordLevels: 5,
    whisperLang: 'en',
  },
  zh: {
    label: '중국어',
    examName: 'HSK',
    levels: ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'],
    targetLevel: 'HSK6',
    wordLevels: 6,
    whisperLang: 'zh',
  },
} as const

export function isValidLang(lang: string): lang is Lang {
  return lang === 'en' || lang === 'zh'
}