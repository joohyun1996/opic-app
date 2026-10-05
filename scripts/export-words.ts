import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'
import { buildWordExport, type WordExport, type WordInput } from './lib/word-export'

const outputUrl = new URL('../exports/words.json', import.meta.url)

async function main() {
  let previous: WordExport | undefined
  try {
    previous = JSON.parse(await readFile(outputUrl, 'utf8')) as WordExport
    if (!previous || !Array.isArray(previous.words)) {
      throw new Error('기존 내보내기 파일의 words 배열이 올바르지 않습니다.')
    }
    for (const word of previous.words) {
      if (!['en', 'zh'].includes(word.language)) {
        throw new Error(`기존 내보내기 파일의 language가 올바르지 않습니다: ${word.language}`)
      }
    }
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error
  }

  const paths = process.argv.slice(2)
  const sources = paths.length ? paths.map(path => resolve(path)) : [
    new URL('../exports/source/words-en.json', import.meta.url),
    new URL('../exports/source/words-zh.json', import.meta.url),
  ]
  const input: WordInput[] = []
  for (const path of sources) {
    const source = JSON.parse(await readFile(path, 'utf8')) as {
      words: WordInput[]; metadata?: { excluded?: { word: string; reason: string }[] }
    }
    if (!Array.isArray(source.words)) throw new Error(`원본의 words 배열이 없습니다: ${path}`)
    const excluded = new Set(source.metadata?.excluded?.map(item => item.word) ?? [])
    for (const word of source.words) {
      if (!['en', 'zh'].includes(word.language) || typeof word.word !== 'string' || !word.word.trim() ||
        typeof word.meaningKo !== 'string' || !word.meaningKo.trim() ||
        typeof word.category !== 'string' || !Number.isInteger(word.level) || word.level < 1 || word.level > (word.language === 'en' ? 5 : 6) ||
        !Array.isArray(word.collocations) || word.collocations.some(item => typeof item !== 'string')) {
        throw new Error(`원본 단어 필드가 올바르지 않습니다: ${word.word}`)
      }
      for (const field of ['phonetic', 'meaningEn', 'example', 'exampleKo', 'partOfSpeech'] as const) {
        if (word[field] !== null && typeof word[field] !== 'string') throw new Error(`원본 필드 누락 또는 형식 오류: ${word.word}.${field}`)
      }
      if (word.example && !word.exampleKo?.trim()) throw new Error(`예문 번역 누락: ${word.word}`)
      if (word.language === 'en') {
        if ((word.word.length === 1 && !['a', 'i'].includes(word.word)) || excluded.has(word.word)) {
          throw new Error(`제외 대상 영어 단어가 포함되어 있습니다: ${word.word}`)
        }
        for (const field of ['phonetic', 'meaningEn', 'partOfSpeech', 'example', 'exampleKo'] as const) {
          if (!word[field]?.trim()) throw new Error(`영어 직접 작성 필드 누락: ${word.word}.${field}`)
        }
        if (word.category !== word.partOfSpeech) throw new Error(`영어 category와 품사 불일치: ${word.word}`)
      }
      input.push(word)
    }
  }
  if (!input.length) throw new Error('내보낼 단어가 없습니다.')
  const result = buildWordExport(input, new Date().toISOString(), previous)
  await mkdir(new URL('../exports/', import.meta.url), { recursive: true })
  await writeFile(outputUrl, `${JSON.stringify(result, null, 2)}\n`, 'utf8')

  for (const language of ['en', 'zh']) {
    const words = result.words.filter(word => word.language === language)
    const maxSeq = words.reduce((max, word) => Math.max(max, word.seq), 0)
    const activeCount = words.filter(word => !word.deleted).length
    console.log(`${language}: 단어 ${activeCount}개, 삭제 표시 ${words.length - activeCount}개, 최대 seq ${maxSeq}, Day ${Math.ceil(maxSeq / 40)}개`)
  }
  console.log(`dataVersion: ${result.dataVersion}`)
  console.log(`저장 완료: ${fileURLToPath(outputUrl)}`)
}

main()
  .catch(error => {
    console.error('단어 내보내기 실패:', error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
