const LANG_MAP = {
  en: 'en-US',
  zh: 'zh-CN',
} as const

export function speak(text: string, lang: 'en' | 'zh'): void {
  if (typeof window === 'undefined' || !window.speechSynthesis) return

  window.speechSynthesis.cancel()

  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = LANG_MAP[lang]
  utterance.rate = 0.9

  window.speechSynthesis.speak(utterance)
}
