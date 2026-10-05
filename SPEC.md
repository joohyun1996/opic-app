# OPIc · HSK 학습 앱 기능 명세서

> 마지막 업데이트: 2026-10-05
>
> ⚠️ 2026-10-04 Kotlin 네이티브 앱 전환 결정 (`docs/decisions/001-native-pivot.md`). 이 문서는 웹앱 기준 최종본이며 `web-final` 태그로 보관된다. 이후 명세는 새 저장소의 SPEC.md에서 이어진다.

## 변경 이력
| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2026-05-04 | v0.1.0 | 초기 세팅 (인증, 스키마) |
| 2026-05-04 | v0.2.0 | Phase 1 단어 탭 구현 |
| 2026-10-04 | v0.3.0 | 네이티브 전환 결정 (ADR 001). 아래 "전환 기록" 참고 |
| 2026-10-05 | v0.4.0 | 단어 데이터 `exports/words.json` 확정 (영어 5,517개, dataVersion 1), 웹 코드 제거 (TASK 01~03) |
| 2026-10-05 | v0.5.0 | 안드로이드 골격 + Room 단어 스키마 (TASK 04) |
| 2026-10-05 | v0.6.0 | 기기 내 LLM 이식 + 문장 교정 채점 실측 (TASK 05) |

## 인증
### POST /api/auth/login
- 기능: 로그인
- 요청: { username, password }
- 응답: { ok: true }
- 세션: userId, username, role 저장

### POST /api/auth/logout
- 기능: 로그아웃
- 세션 삭제

## 단어 (Phase 1)

### 화면 구조
| 화면 | 경로 | 설명 |
|------|------|------|
| ① 홈 | /home | 언어 선택 카드 + 전체 진행률 |
| ② Day 인덱스 | /[lang]/words | 4열 Day 그리드 + 오답 뱃지 |
| ③ Day 단어 목록 | /[lang]/words/[day] | 단어 리스트 + 학습 시작 버튼 |
| ④ 플래시카드 영→한 | /[lang]/words/[day]/study?mode=en-ko | 영어→한국어 타이핑 |
| ⑤ 플래시카드 한→영 | /[lang]/words/[day]/study?mode=ko-en | 한국어→영어 타이핑 |
| ⑥ 오답 모음 | /[lang]/words/wrong | Day 필터 + 오답 카드 리스트 |

### GET /api/words
- 기능: Day 단위 단어 목록 조회
- 요청: ?lang=en|zh&day=1
- 응답: { words: Word[], totalDays: number }
- 규칙: level ASC, id ASC 정렬 후 40개씩 슬라이싱

### GET /api/words/stats
- 기능: Day별 진행 통계
- 요청: ?lang=en|zh
- 응답: { totalDays, days: [{ day, total, mastered, learning, wrong }] }
- wrong = wrongCount > 0 인 UserWord 수

### POST /api/user-words
- 기능: 단어 학습 결과 기록
- 요청: { wordId: number, result: 'correct' | 'wrong' }
- 응답: { userWord }
- 규칙: correctCount >= 3 → mastered, 그 외 → learning

### POST /api/admin/words/seed (admin 전용)
- 기능: Claude Sonnet 4.6으로 단어 생성 후 DB upsert
- 요청: { lang, level, category, count? }
- 응답: { created, skipped, words }

### 플래시카드 규칙
- 정답 판정: `input.trim().toLowerCase() === answer.toLowerCase()`
- 영→한: 영어 단어 + 발음기호 + ♪ → 한국어 뜻 타이핑
- 한→영: 한국어 뜻 + 품사 + 힌트(c _ _ _ _ _ t) → 영어 단어 타이핑
- 정답: 초록 테두리 + ✓, 오답: 빨간 테두리 + ✗ + 정답 표시

### 단어 상태
| 상태 | 조건 | 배지 배경색 |
|------|------|------------|
| new | 미학습 | #f1efe8 |
| learning | correctCount < 3 | #faeeda |
| mastered | correctCount >= 3 | #eaf3de |

### Day 구조
- Day N = 언어별 단어를 level/id ASC 정렬 후 skip (N-1)*40, take 40

## 전환 기록 (2026-10-04, ADR 001)
### 네이티브 전환
- Next.js 웹앱 → Kotlin + Jetpack Compose 안드로이드 전용 앱 (새 저장소)
- 서버·로그인·PostgreSQL 제거, 데이터는 Room(SQLite) 기기 내 저장
- LLM: Anthropic API → 기기 내 Gemma 3n E4B (MediaPipe, 머니로그 `core/llm` 재사용)
- 음성 인식: OpenAI Whisper API → 미정 (스피킹 직전 검증)
- 배포: APK 직접 전달

### Day 규칙 변경
- 이전: 언어별 `level ASC, id ASC` 정렬 후 skip (N-1)*40, take 40
  - 원래 요구사항("난이도를 섞어서 하루 40개")과 달랐음 (`app/api/words/route.ts:34`)
- 이후: 단어별 고정 `seq` (고정 시드로 섞어 부여, 새 단어는 맨 뒤), `day = (seq-1)/40 + 1`
  - deleted 단어는 해당 Day에서 숨김. Day는 절대 밀리지 않음

### API 제거 (네이티브 앱에서 Repository/DAO로 대체)
| 이전 | 이후 |
|------|------|
| POST /api/auth/login, /api/auth/logout | 제거 (로그인 없음) |
| GET /api/words | Day 단어 조회 DAO (seq 기준) |
| GET /api/words/stats | Day별 통계 DAO |
| POST /api/user-words | UserWord 기록 DAO |
| POST /api/admin/words/seed | 제거 (단어는 내장 `words.json` + dataVersion) |

### 단어 데이터 (2026-10-05, TASK 01·02)
- 파일: `exports/words.json` — `{ dataVersion, seed, exportedAt, words: [{ language, word, seq, phonetic, meaningKo, meaningEn, example, exampleKo, level, category, partOfSpeech, collocations, deleted }] }`
- 영어 5,517개 (Day 138개): Google 10000 중 토익 800+ 쉬운 단어 제외 1,100개 + ECDICT GRE·TOEFL·IELTS 4,417개. 고정 시드(20261004)로 전체 섞음
- 영어 level 1~5: 출처별 빈도 순 5등분
- 중국어 4,991개는 `exports/source/words-zh.json`에 보관 (앱 미포함). 추가 시 HSK 1급 → 6급 순서
- 생성 스크립트는 삭제됨 (`web-final` 이후 커밋 `4d1e460`에 마지막 버전). 이후 수정은 파일 직접 편집 + dataVersion 증가

### 안드로이드 골격 + Room 스키마 (2026-10-05, TASK 04)
- 패키지 `com.jooh.opic`, 모듈 `:app`, `:core:common`, `:core:model`, `:core:database`
- 빌드: Gradle 9.5.0, AGP 9.3.3, Kotlin 2.2.10, Room 2.8.5, compileSdk 37 / minSdk 34 / targetSdk 35 (머니로그와 동일)
- Day 계산 (`core:common`): `dayOf(seq) = (seq-1)/40+1`, `seqRange(day)`, `totalDays(maxSeq)`
- 상태 (`core:model`): UserWord 없음 → NEW, correctCount ≥ 3 → MASTERED, 그 외 LEARNING
- Room v1 테이블
  - `words`: 자동 id, `(language, word)` UNIQUE, `(language, seq)` UNIQUE, `words.json` 필드와 1:1
  - `user_words`: `wordId` PK, correctCount, wrongCount, lastStudiedAt. 외래키 `NO_ACTION` (CASCADE 금지)
  - `data_meta`: key/value (저장된 dataVersion용)
- DAO (웹 API 대체)
  | 이전 API | 이후 DAO |
  |------|------|
  | GET /api/words | `WordDao.getDayWords(language, day)` — deleted 제외, seq 순 |
  | POST /api/user-words | `UserWordDao.recordResult(wordId, correct, now)` |
  | POST /api/admin/words/seed | `WordDao.upsertWords(words)` — `(language, word)` 조회 후 UPDATE/INSERT, 기존 id·seq 유지 |
  | GET /api/words/stats | 미구현 (단어 탭 TASK) |
- 미구현: `words.json` 적재와 dataVersion 비교, 단어 화면

### 기기 내 LLM + 문장 교정 (2026-10-05, TASK 05)
- `:core:llm` — 머니로그 `core/llm` 7개 파일 복사 (`4d5adfa`, 원본과 패키지명 외 동일). Gemma 3n E4B, 모델 경로 `com.jooh.opic`의 `no_backup/llm` (머니로그와 별도)
- 권한: `INTERNET` 1개 (모델 다운로드 전용, `HttpModelStore`만 사용). HF 토큰은 `HfTokenStore`(암호화, `opic_hf_token_store`)
- 교정 (`core:common`)
  - `buildCorrectionPrompt(sentence)` → JSON `{correct, corrected, errors:[{type, original, fix, explanationKo}]}`
  - type: tense | article | preposition | agreement | word_choice | word_order | other (모르는 값은 other)
  - `parseCorrection(raw, original)` → Ok / InvalidJson / Contradiction. 모순: correct=true인데 errors 있음, correct=false인데 errors 없음, corrected가 원문과 같은데 errors 있음
- S23+ 실측 (20문장): 유효 JSON 20/20, 모순 0, 판정 일치 20/20, 문장당 중앙값 22.0초·최대 25.6초, 첫 로딩 11.0초. 한국어 설명의 문법 용어 오류 2/14 (`docs/tasks/05-llm-port/BENCHMARK.md`)
- debug 빌드 전용 "LLM 검증" 화면 (사용자 기능 아님)

## 문법
(기능 추가 시 여기에 작성)

## 섀도잉
(기능 추가 시 여기에 작성)

## 스피킹
(기능 추가 시 여기에 작성)

## 분석
(기능 추가 시 여기에 작성)
