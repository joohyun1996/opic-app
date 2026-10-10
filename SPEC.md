# OPIc · HSK 학습 앱 기능 명세서

> 마지막 업데이트: 2026-10-10
>
> 2026-10-04 Kotlin 네이티브 앱 전환 (`docs/decisions/001-native-pivot.md`). 이 저장소에서 계속 작성한다. 웹앱 시절 명세(인증, `/api/*`, 웹 단어 탭)는 `web-final` 태그 기준이며 아래 "전환 기록" 이전 섹션에 남아 있다.

## 변경 이력
| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2026-05-04 | v0.1.0 | 초기 세팅 (인증, 스키마) |
| 2026-05-04 | v0.2.0 | Phase 1 단어 탭 구현 |
| 2026-10-04 | v0.3.0 | 네이티브 전환 결정 (ADR 001). 아래 "전환 기록" 참고 |
| 2026-10-05 | v0.4.0 | 단어 데이터 `exports/words.json` 확정 (영어 5,517개, dataVersion 1), 웹 코드 제거 (TASK 01~03) |
| 2026-10-05 | v0.5.0 | 안드로이드 골격 + Room 단어 스키마 (TASK 04) |
| 2026-10-05 | v0.6.0 | 기기 내 LLM 이식 + 문장 교정 채점 실측 (TASK 05) |
| 2026-10-06 | v0.7.0 | 단어 데이터 적재 + 홈·Day 목록 화면 (TASK 06) |
| 2026-10-06 | v0.8.0 | Day 단어 목록 + 영→한·한→영 플래시카드 + 발음 (TASK 07). 영→한 채점 규칙 변경 |
| 2026-10-06 | v0.9.0 | 오답 모음 + 오답만 학습 (TASK 08). 오답 정의 변경. 단어 탭 이식 완료 |
| 2026-10-07 | v0.10.0 | 단어 탭 다듬기: 학습 카드 셔플, 힌트 버튼, Day 목록에 뜻 표시 (TASK 09) |
| 2026-10-07 | v0.11.0 | 문법 탭 1: 단원 1~3 콘텐츠, 단원 목록·설명·연습(두 번 시도)·결과 (TASK 10) |
| 2026-10-07 | v0.12.0 | 문법 탭 2: 직접 써 보기 + Gemma 교정, AI 교정 준비 화면, 내비게이션 `:app`으로 이동 (TASK 11) |
| 2026-10-07 | v0.13.0 | 문법 단원 4~10 추가 (grammar.json dataVersion 2, 100문제), 다시 시도 취소 경쟁 상태 수정 (TASK 12) |
| 2026-10-07 | v0.14.0 | 문법 틀린 문제 간격 복습, **Room DB v1 → v2** (`grammar_reviews` 추가) (TASK 13) |
| 2026-10-07 | v0.15.0 | Whisper 기기 내 음성 인식 검증 → **small.en 채택** (debug 전용 `:core:stt`), `RECORD_AUDIO` 권한 (TASK 14). 홈 이름 변경(영단어·영문법·오답노트), LLM 검증 화면 삭제 |
| 2026-10-09 | v0.16.0 | 섀도잉: YouTube IFrame 재생·A-B 반복·속도·따라 말하기(Whisper small.en) 단어 비교, `:core:stt` release 포함 (TASK 15, 실기기 확인 보류) |
| 2026-10-09 | v0.17.0 | 섀도잉 비공식 자막: 영어 자막 목록·현재 문장 강조·문장 누르면 A-B 반복+원문 채우기, 실패 시 붙여 넣기 (TASK 16, 실기기 확인 보류) |
| 2026-10-10 | v0.18.0 | 섀도잉 실기기 수정: WebView 즉시 파괴·iframe 높이 0 수정, 자막은 플레이어 요청 가로채기, 일치율 = 맞은 단어/원문, 짧은 녹음·무음 안내, 비교 결과 가로 표시. 개발자 검증 화면 삭제, Whisper 모델 small.en만 |
| 2026-10-10 | v0.19.0 | 스피킹 탭: 질문 은행(15주제 43문항), TTS 질문·다시 듣기 1회, 2분 답변 녹음, Whisper 받아 적기(머뭇거림 유지 prompt), 즉시 지표 (TASK 17) |
| 2026-10-10 | v0.20.0 | 스피킹 결과: 단어 눌러 구간 듣기·꾹 눌러 고치기, 고친 글로 Gemma 문법 교정(최대 15문장). 교정 코드 `core:correction`으로 이동 (TASK 18) |
| 2026-10-10 | v0.21.0 | 발음 힌트 (스피킹·섀도잉): 불명확·다르게 들린 단어, 한국인 발음 팁 8종, 연음·약화·t 약화, 리듬 팁 (TASK 19) |
| 2026-10-10 | v0.22.0 | 스피킹 문항 50주제 164문항(설문·돌발·롤플레이·IM/IH/AL), 모의고사 15문항 (TASK 20) |
| 2026-10-10 | v0.23.0 | 섀도잉 추천 영상 100개 (분류 6종, 누르면 바로 열기) (TASK 21) |
| 2026-10-10 | v0.24.0 | **Room DB v2 → v3** (`speaking_answers`, `shadowing_attempts`), 스피킹·모의고사·섀도잉 기록과 지난번 비교, 학습 기록 백업·복원 (TASK 22) |
| 2026-10-10 | v0.25.0 | Gemma 모델 공용 폴더(`Develop/Core/llm`) 사용, **권한 `MANAGE_EXTERNAL_STORAGE` 추가**(모델 읽기 전용, 사용자 승인) (ADR 002) |
| 2026-10-10 | v0.26.0 | 섀도잉 자막: 열면 소리 없이 잠깐 재생해 자막 요청을 일으키고 0초로 멈춤, en-GB·en-US 자막도 받음 — 표본 15/15 (TASK 23) |
| 2026-10-10 | v0.27.0 | 디자인 개편 1: 공용 테마(밝게 = 흰·주황 / 어둡게 = 남색·파랑, IBM Plex Sans KR), 하단 바 5개, 전체 메뉴, 새 홈 (TASK 24) |
| 2026-10-10 | v0.28.0 | 앱 로고, release 빌드 사용(스크롤), 탭 첫 화면 머리글 통일·메뉴 구분선·어두운 모드 버튼 색 수정 (TASK 24 추가·TASK 25) |
| 2026-10-10 | v0.29.0 | 실전 영문법 탭 + 1부 문장의 뼈대 3장(30문제), 새 문제 유형 틀린 곳 찾기·구조 찾기 (TASK 26) |
| 2026-10-10 | v0.30.0 | 실전 영문법 0장 용어 풀이·2부 동사(4~7장)·자세히 보기, 뒤로 가기 시 탭 유지 (TASK 27) |
| 2026-10-10 | v0.31.0 | 실전 영문법 3~6부(8~18장) 완성 — 19장 188문제, 빈 흔한 실수 제목 숨김 (TASK 28) |

## 인증
### POST /api/auth/login
- 기능: 로그인
- 요청: { username, password }
- 응답: { ok: true }
- 세션: userId, username, role 저장

### POST /api/auth/logout
- 기능: 로그아웃
- 세션 삭제

## 화면 구조·디자인 (2026-10-10, TASK 24)
- 테마 `core:ui/Theme.kt`: 밝게 = 시안 A(배경 #FFFFFF, 카드 #F7F5F1, 강조 #C8460F), 어둡게 = 시안 B(배경 #0E1430, 카드 #151D40, 버튼 #4C5BF0, 강조 글자 #8EA0FF). 상태 색 `Opic.colors`(정답·오답·경고·학습 중). 글꼴 IBM Plex Sans KR 400·600·700 (앱 내장, OFL). 시안: https://claude.ai/artifact/4UkaAdg75DhYj7u4CJTyZN
- 하단 바 5개: 홈 / 단어(Day 목록) / 문법 / 섀도잉 / 스피킹 — 탭마다 화면 기록 유지. 탭 첫 화면 오른쪽 위 ≡ → 전체 메뉴
- 홈: 날짜·"오늘의 학습", 오늘 할 일(문법 복습 N문제 또는 "오늘 복습 끝!" + 영단어 이어서), 이어서 하기(스피킹 모의고사 / 영단어 Day N·습득 / 섀도잉 최근 영상)
- 전체 메뉴: 학습(오답노트·스피킹 기록·중국어 준비 중) / 설정(AI 모델 상태, TTS 속도 느리게 0.8·보통 1.0·빠르게 1.2, 화면 테마 기기 설정·밝게·어둡게) / 데이터(학습 기록 백업·복원) / 앱 정보. 설정은 SharedPreferences `opic_ui`
- 탭 첫 화면(영단어·영문법·섀도잉·스피킹)은 제목 + 오른쪽 위 ≡만, 안쪽 화면은 "← 위 화면" (TASK 25). 앱 로고: 주황 배경 + 흰 말풍선 + 소리 막대 3개(적응형·테마 아이콘)
- 폰 설치는 release 빌드(이 Mac의 debug 키로 서명, debug 위에 덮어 설치 가능) — debug는 Compose가 느려 스크롤 버벅임

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
- ~~미구현: `words.json` 적재와 dataVersion 비교, 단어 화면~~ → TASK 06에서 구현 (아래)

### 기기 내 LLM + 문장 교정 (2026-10-05, TASK 05)
- (2026-10-10, ADR 002) 모델 위치: 공용 파일 `Develop/Core/llm/gemma-3n-e4b-it.task`를 "모든 파일 접근"(MANAGE_EXTERNAL_STORAGE, 설정에서 허용) 권한으로 직접 연다. AI 교정 준비 화면에 허용 버튼. 안 골랐으면 앱 전용 `no_backup/llm/` 파일·다운로드
- `:core:llm` — 머니로그 `core/llm` 7개 파일 복사 (`4d5adfa`, 원본과 패키지명 외 동일). Gemma 3n E4B, 모델 경로 `com.jooh.opic`의 `no_backup/llm` (머니로그와 별도)
- 권한: `INTERNET` 1개 (모델 다운로드 전용, `HttpModelStore`만 사용). HF 토큰은 `HfTokenStore`(암호화, `opic_hf_token_store`)
- 교정 (`core:common`)
  - `buildCorrectionPrompt(sentence)` → JSON `{correct, corrected, errors:[{type, original, fix, explanationKo}]}`
  - type: tense | article | preposition | agreement | word_choice | word_order | other (모르는 값은 other)
  - `parseCorrection(raw, original)` → Ok / InvalidJson / Contradiction. 모순: correct=true인데 errors 있음, correct=false인데 errors 없음, corrected가 원문과 같은데 errors 있음
- S23+ 실측 (20문장): 유효 JSON 20/20, 모순 0, 판정 일치 20/20, 문장당 중앙값 22.0초·최대 25.6초, 첫 로딩 11.0초. 한국어 설명의 문법 용어 오류 2/14 (`docs/tasks/05-llm-port/BENCHMARK.md`)
- debug 빌드 전용 "LLM 검증" 화면 (사용자 기능 아님)

### 단어 적재 + 홈·Day 목록 (2026-10-06, TASK 06)
- APK assets에 `exports/words.json`만 포함 (Gradle `Sync` → `build/generated/wordAssets`)
- `WordImporter.importWords(raw)` → `Imported(version, count)` / `UpToDate(version)` / `Failed(reason)`, 예외를 던지지 않음
  - 검증: language en|zh, word·meaningKo 공백 아님, seq·level ≥ 1, 파일 내 (language, word)·(language, seq) 중복 없음, 영어는 phonetic·meaningEn·example·exampleKo 필수
  - `data_meta.words_data_version`보다 클 때만 한 트랜잭션에서 `upsertWords` + 버전 저장. 실패 시 전체 롤백. `user_words`는 건드리지 않음
- 앱 시작 시 IO 스레드에서 적재 (`OpicApplication.importResult`, 로그 태그 `WordImport`). S23+: 첫 적재 3,283ms, 재실행 637ms
- DAO 변경
  | 이전 | 이후 |
  |------|------|
  | `WordDao.getDayWords(language, day)` | `getDayWords(language, firstSeq, lastSeq)` — 호출부가 `seqRange(day)` 전달 |
  | (GET /api/words/stats 대체 미구현) | `WordDao.dayStats(language, wordsPerDay)` → `DayStats(day, total, mastered, wrong)` (deleted 제외) |
  | — | `DataMetaDao.get / insert(IGNORE) / update` |
- `:feature:words` — ① 홈 (영어 카드: 습득/전체 + 진행률, 중국어 "준비 중" 비활성), ② Day 목록 (4열, `습득/전체`, 오답 총수 뱃지), Day 칸 → "준비 중" 화면. navigation-compose
- debug 빌드 전용: LLM 검증 화면과 `llm-bench/sentences.json` (`app/src/debug`)

### Day 단어 목록 + 플래시카드 + 발음 (2026-10-06, TASK 07)
- ③ Day 단어 목록 (`day/{day}`): 단어·발음기호·♪·상태 뱃지·오답 N, 하단 "영→한 학습" / "한→영 학습"
- ④⑤ 플래시카드 (`study/{day}/{en-ko|ko-en}`): Day 단어를 seq 순서로 한 장씩, 확인 후 정답/오답 + 예문·번역, 결과 화면(정답 수, 틀린 단어, 다시 하기)
- **채점 규칙 (웹 규칙에서 변경)**
  | | 이전 (웹) | 이후 |
  |---|---|---|
  | 영→한 | 뜻 전체와 소문자 완전 일치 | `*` 제거 → `;`·`,`로 나눔 → 괄호 내용 제거 → 후보 중 하나와 공백·문장부호 무시하고 일치 (`gradeMeaning`) |
  | 영→한 오답 | — | "맞았어요"로 정답 정정 가능 (한 문제 한 번): `UserWordDao.correctLastWrong` = wrong −1, correct +1 |
  | 한→영 | 소문자 완전 일치 | 동일 (`gradeWord`) |
  | 힌트 | `c _ _ _ _ _ t` | 동일 (`maskHint`, 2글자 이하는 첫 글자 + `_`, 하이픈 유지) |
- 답을 확인할 때마다 `recordResult` 1회. 쓰기는 순서대로 실행
- DAO 추가: `WordDao.getDayWordsWithProgress(language, firstSeq, lastSeq)` → `WordWithProgress(word, correctCount?, wrongCount?)`
- 발음: Android `TextToSpeech`(Locale.US) 하나를 공유. 엔진·영어 음성이 없으면 ♪ 비활성 + 1회 안내
- 적재: 파일 앞 256자에서 dataVersion을 먼저 읽어 같으면 전체 파싱 생략 (S23+ 재실행 260ms)

### 오답 모음 (2026-10-06, TASK 08)
- **오답 정의 변경**
  | 이전 (v0.7.0~) | 이후 |
  |---|---|
  | `wrongCount > 0` | `wrongCount > 0 AND correctCount < 3` — 습득하면 오답에서 빠진다 |
  - `DayStats.wrong`, 뱃지, ③ "오답 N", ⑥ 목록이 모두 이 정의
- ⑥ 오답 모음 (`wrong`): Day 필터 칩(전체 + 오답 있는 Day), 빨간 세로선 카드(단어·발음·♪·뜻·틀린 횟수·Day), 하단 영→한/한→영 학습
  - 진입: Day 목록 "오답 N개 →" 뱃지, 홈 "오답 N개 다시 보기 →"
- 오답 학습 (`study/wrong/{day}/{mode}`, day 0 = 전체): 시작 시 카드 목록 고정
- DAO 추가: `WordDao.getWrongWords(language, firstSeq, lastSeq)`
- 학습 기록 쓰기 실패 시 `Log.e("WordStudy")` + 화면에 "기록 저장 실패"

### 단어 탭 다듬기 (2026-10-07, TASK 09)
- 학습 카드 순서: seq 순 → **매번 섞음** (Day 학습·오답 학습, 시작과 "다시 하기" 때마다). ③·⑥ 목록은 seq 순 유지
- 한→영 힌트: 항상 표시 → **"힌트 보기" 버튼을 눌러야 표시**, 다음 카드에서 다시 숨김
- ③ Day 단어 목록: 상태 뱃지(신규/학습중/습득) 제거 → **오른쪽에 한국어 뜻** (습득 단어는 초록). ♪는 단어 바로 오른쪽. "습득 N/40" 줄 오른쪽에 "한국어 숨기기/보기" (앱 실행 중 유지)
- 홈 이름 변경 (2026-10-07, 사용자 요청): "영어 (OPIc)" → **영단어**, "영어 문법" → **영문법**, "오답 N개 다시 보기" → **오답노트 N개**, 카드의 "· 학습 시작 →" 삭제. debug 홈 버튼 "LLM 검증" → "개발자 검증" (LLM 검증 화면 삭제, 문법 교정이 실제 Gemma 사용)

## 문법

### 설계 근거 (2026-10-07 사용자 결정)
- 명시적 설명(Norris & Ortega 2000), 인출 연습(Roediger & Karpicke 2006), 스스로 고치게 하는 피드백(Lyster & Saito 2010), 쓰기 교정 피드백(Kang & Han 2015), 간격 복습(Cepeda et al. 2006)
- 단원 10개 계획: 현재/현재진행, 과거, used to/would, 현재완료, 비교급, 의문문, 관사, 전치사, 수 일치, 연결어 (TASK 10: 1~3, TASK 12: 4~10)

### 단원 1~3 + 연습 화면 (2026-10-07, TASK 10)
- 데이터: `exports/grammar.json` (dataVersion 1, 단원 3, 문제 30) — Claude 초안 → GPT 검토(7문제 수정, `docs/tasks/10-grammar-units/CONTENT-REVIEW.md`). APK assets에 포함
  - 단원: `id, order, title, opicUse, errorType, explanation{summary, points, examples, commonMistakes}, exercises, writingTask`
  - 문제 kind: `fix`(문장 전체, answers[]) / `blank`(빈칸, answers[]) / `choice`(choices[], answer 인덱스)
  - 검증 실패 시 "문법 데이터를 불러오지 못했습니다", 앱은 계속 동작
- 채점 (`core:common/GrammarGrading.kt`): `normalizeAnswer` = 앞뒤 공백·연속 공백·끝 `.!?` 제거, 소문자, `’`→`'`. `gradeText`는 answers 중 하나와 일치
- 두 번 시도 (`GrammarAttempt`): 1차 오답 → "다시 생각해 보세요" + 힌트 / 2차 오답 → 정답 + 설명 / 정답 → 1차·2차 구분
- 화면: 홈 "영어 문법" 카드 → 단원 목록 → 설명(요약·규칙·예문·흔한 실수) → 연습 10문제(섞음) → 결과(1차·2차 정답, 오답 목록, 다시 풀기). 진행 기록 저장 없음 (TASK 13)

### 직접 써 보기 + Gemma 교정 (2026-10-07, TASK 11)
- 진입: 단원 결과 화면 "직접 써 보기" → `writingTask`(promptKo/promptEn, 최소 N문장) → 여러 줄 타이핑 → "교정 받기"
- 문장 나누기 (`core:common/Writing.kt` `sentencesToCorrect`): `.!?` 기준, 약어(Mr., a.m., U.S. 등) 보존, 빈 문장 제거, **최대 5문장**
- 교정: 문장마다 `buildCorrectionPrompt` → `generate(timeout 120s)` → `parseCorrection` (TASK 05). 결과는 문장 순서대로 하나씩 표시, 취소 가능
  - 틀림: 원문 → 고친 문장, 오류마다 **유형별 고정 설명**(`errorTypeGuide`, 7종) 먼저 + `original → fix` + 접힌 "AI 설명"(모델 explanationKo)
  - 실패(InvalidJson / Contradiction / 시간 초과 / 엔진 오류): "교정하지 못했습니다" + 그 문장만 "다시 시도"
  - S23+: 문장당 11.6~17.9초
- AI 교정 준비 화면: 엔진 상태(모델 없음/다운로드 중/불러오는 중/준비됨/실패), HF 토큰 입력(가림, `HfTokenStore`), 다운로드는 버튼을 눌렀을 때만. 모델 파일이 이미 있으면 자동으로 불러오기만 함
- 앱 구조: NavHost를 `:app`의 `OpicRoot`로 이동, `:feature:words`와 `:feature:grammar`는 서로 의존하지 않음. `grammar.json`은 `OpicApplication`에서 백그라운드 1회 파싱

### 단원 4~10 (2026-10-07, TASK 12)
- `exports/grammar.json` dataVersion 1 → 2: 단원 10개, 문제 100개 — 4 현재완료 vs 과거, 5 비교급·최상급, 6 의문문, 7 관사, 8 전치사, 9 수 일치, 10 연결어
- 단원 4~10 콘텐츠는 Claude 작성·자체 검토 (GPT 교차 검토 대기)
- 교정 경쟁 상태 수정: 다시 시도 결과는 그 칸이 같은 실패일 때만 교체, 취소 뒤 늦게 끝난 결과는 버림

### 틀린 문제 간격 복습 (2026-10-07, TASK 13)
- 복습 대상: 단원 연습에서 2차 정답 또는 끝내 오답 (1차 정답 제외)
- 일정 (`core:common/Review.kt`): 1단계 다음 날 → 2단계 3일 뒤 → 3단계 7일 뒤 → 3단계 1차 정답이면 졸업(삭제). 2차 정답·오답이면 1단계·다음 날
- 복습 세션: 오늘까지 밀린 문제 중 오래된 순 최대 10개, 섞어서. 홈 문법 카드·단원 목록에 "오늘의 복습 N개" / "오늘 복습 끝!"
- **DB v2**: `grammar_reviews(exerciseId PK, unitId, stage, dueEpochDay, wrongCount, lastStudiedAt)` 추가. `MIGRATION_1_2`는 CREATE TABLE 한 문장, 기존 테이블 변경 없음
  - DAO: `GrammarReviewDao.due(today)`, `recordPractice(exerciseId, unitId, today, now)`, `recordReview(exerciseId, firstTryCorrect, today, now)`
- grammar.json에서 빠진 문제의 기록은 남기고 목록에서만 건너뜀

### 실전 영문법 (2026-10-10, TASK 26~)
- 영문법 탭 위 "OPIc 문법 | 실전 영문법" 전환. 데이터 `exports/grammar-core.json`(grammar.json과 같은 형식 + 선택 필드), 앱이 두 파일을 합쳐 읽음(`mergeBooks`, id 중복 시 실패 → core 없이 OPIc만)
- 장 필드: `track:"core"`, `part`(부), 설명 `concept`(왜 이렇게 쓸까) · `table`(한눈에 보기) · `koreanNote`(한국어와 다른 점) · `breakdowns`(문장 구조 분해: S 주어·V 동사·O 목적어·IO/DO·C 보어·OC·M 수식어, 역할별 색). `writingTask` 없으면 "직접 써 보기" 숨김
- 새 문제 유형: `spot`(틀린 곳 찾기), `structure`(구조 찾기) — 문장의 단어 칩을 눌러 고름, 채점은 선택형과 같음(`TAP_KINDS`), 간격 복습 공유
- 1부 문장의 뼈대: 1장 품사와 문장 성분, 2장 문장의 5형식, 3장 주어 찾기(긴 주어·It·There) — 장마다 10문제
- (TASK 27) 0장 문법 용어 풀이(용어 40여 개, 확인 8문제), 2부 동사: 4장 시제 12개, 5장 현재완료, 6장 조동사, 7장 수동태. 장 설명에 `details`(자세히 보기: 소주제 + 예문). OPIc/실전 선택은 앱 실행 중 유지
- (TASK 28) 3부 준동사: 8 to부정사, 9 동명사, 10 분사. 4부 문장 잇기: 11 접속사, 12 관계대명사, 13 관계부사·what, 14 명사절. 5부: 15 형용사·부사, 16 비교, 17 가정법. 6부: 18 관사·수 일치·전치사. 합계 19장 188문제. 빈 섹션(요점·예문·흔한 실수)은 제목도 숨김

## 섀도잉

### 재생 + 구간 반복 + 따라 말하기 (2026-10-09, TASK 15)
- 진입: 홈 "섀도잉" 카드 → YouTube 링크 붙여 넣기 → "열기". `youtubeVideoId`(`core:common/Shadowing.kt`): youtu.be, watch(일반·m.), shorts, embed, 뒤 매개변수 무시, ID 11자
- 플레이어: WebView + YouTube IFrame Player API (영상 내려받지 않음). base URL·origin `https://appassets.androidplatform.net`. JS 브리지는 시각·오류 수신 2개 + nonce 확인, 파일·콘텐츠 접근 끔, 메인 프레임 이동 차단
- 조작: 재생/일시정지, 속도 0.5·0.75·1.0, A/B 지정(현재 시각) ±0.5초, 반복 켬/끔 (150ms마다 B 넘으면 A로)
- 원문 문장: 직접 붙여 넣기, 앱 실행 중 영상별 기억 (저장 안 함)
- 따라 말하기: 녹음(최대 30초, 영상 자동 일시정지) → Whisper small.en 받아 적기(취소 가능) → `compareWords` 단어 비교: 맞음 / 틀림 빨강 `원문 → 들린 말` / 빠뜨림 회색 취소선 / 더 말함 주황, 일치율 = 1 − WER. 마지막 녹음 1개만 `filesDir/shadowing-last.pcm`, "내 목소리 듣기"
- Whisper: `UserWhisper`(앱 1개, Mutex), 모델은 버튼을 눌렀을 때만 받음(190MB). 불러오기 전 Gemma 해제, 문법 진입 때 Whisper 해제
- 실기기 확인(AC4) 보류 — 사용자가 다음에 한꺼번에 확인

### 비공식 자막 (2026-10-09, TASK 16)
- 영상을 열면 자동 1회: watch 페이지 HTML의 `captionTracks` → `pickTrack`(사람 en > en-* > 자동 생성 en) → `baseUrl&fmt=json3`. 쿠키·계정·API 키 없음, https YouTube 호스트·`/watch`·`/api/timedtext`만, 시간 제한 10초, 응답 5MiB 상한
- `core:common/Captions.kt`: `extractCaptionTracks`, `parseJson3`(빈 이벤트 제거, 엔티티 풀기), `mergeSentences`(`.?!`까지 합침, 12초 상한), `cueAt`(시작 포함·끝 제외)
- 화면: 자막 목록(`m:ss` + 문장, 최대 높이 240dp 스크롤, 현재 문장 강조), 문장 누르기 → A = 시작 − 0.3초, B = 끝 + 0.3초, 원문 채우기, 이동, 반복 켬. 이전/다음 문장
- 실패: "자막 없음 / 자막 요청 실패 — 문장을 붙여 넣으세요" + 다시 시도. 자막은 앱 실행 중 영상별 메모리에만
- ~~실기기 확인(AC7) 보류~~ → 2026-10-10 확인

### 실기기 수정 (2026-10-10, TASK 15 리뷰 3~6차)
- 플레이어: WebView 정리는 `AndroidView(onRelease)`만 (이전 `DisposableEffect`가 새 WebView를 파괴했음). iframe 크기는 JS가 `innerWidth`·`innerHeight` px로 넣음 (CSS 100%·100vh가 0으로 계산됐음). DOM storage 켬
- 자막: watch 페이지 직접 요청은 빈 응답(`exp=xpe`, PO 토큰 필요) → **플레이어가 보내는 `/api/timedtext` 요청을 `shouldInterceptRequest`로 가로채** 같은 본문을 파싱. 실패하면 직접 요청 → 붙여 넣기
- 녹음: 1.5초 미만은 받아 적지 않고 안내, `[silence]`·`[BLANK_AUDIO]` 태그 제거 후 비면 "잘 들리지 않았어요", 입력 크기 막대
- 일치율 = `matchRate` (맞은 단어 / 원문 단어, 0~100%) + 더 말한·빠뜨린·틀린 단어 수. 비교 결과는 가로로 이어 표시
- 개발자 검증 화면(STT·LLM 측정)과 측정 자료 삭제. `SttModels`는 small.en만

### 자막 수정 (2026-10-10, TASK 23)
- 원인: 플레이어는 재생을 시작해야 `/api/timedtext`를 요청하고, 가로채기는 `lang=en`만 받았다 (BBC는 en-GB)
- 영상을 열면 플레이어 준비 뒤 소리 끄고 잠깐 재생 → 첫 재생 신호(또는 5초)에 일시정지·0초·소리 켜기 (한 번만). 직접 요청은 10초 기다린 뒤 (그 사이 가로채기로 받으면 생략)
- 가로채기 언어: `en`, `en-*`
- 결과: 추천 영상 표본 15개 모두 열기만 해도 자막 목록 (수정 전 0/5)

### 추천 영상 (2026-10-10, TASK 21)
- `exports/shadowing.json` (dataVersion 1, checkedAt 2026-10-10): 100개 `{id, title, channel, category, level, minutes}` — 학습자용 32, 일상 대화 19, 길거리 인터뷰 9, 발음 5, TED-Ed 12, TED 강연 23. 모두 YouTube 자막 필터 검색 + oEmbed 임베드 허용 확인
- 섀도잉 첫 화면: 링크 칸 아래 "추천 영상 100개", 분류 칩(전체·분류별 개수), 카드(제목, 채널 · N분 · 분류 · 등급). 누르면 링크 칸 채우고 바로 열기. 영상 화면에 "← 추천 영상 목록"
- `parseShadowingLibrary` 실패 시 목록만 숨김 (붙여 넣기 그대로)

## 스피킹

### 질문·답변·즉시 지표 (2026-10-10, TASK 17)
- 데이터: `exports/speaking.json` (dataVersion 1, 15주제 43문항) — `{dataVersion, topics:[{id, titleKo, questions:[{id, type, en, ko, tip}]}]}`, type = describe / routine / experience / compare / roleplay_ask / roleplay_solve. `parseSpeakingCatalog` 실패 시 "스피킹 데이터를 불러오지 못했습니다"
- 흐름: 홈 "스피킹" → 주제 목록 + 무작위 질문 → 질문 화면(TTS 자동 재생, 글 숨김, 다시 듣기 1회, "질문 글 보기" = 영어·한국어·팁) → "● 답변 시작" (경과 `m:ss / 2:00`, 입력 크기 막대, 2:00 자동 정지) → 받아 적기(취소 가능) → 결과
- Whisper: `UserWhisper` 공유, `prompt = "Um, uh, so, like, you know, I mean."` (머뭇거림 유지). 1.5초 미만·무음은 안내만
- 결과: 답변 전문(머뭇거림 회색), 말한 시간(< 60초 "1분 이상 말해 보세요"), 분당 단어(< 90 "조금 더 빠르게" / 90~150 "적당한 속도" / > 150 "조금 천천히"), 단어·문장 수, 머뭇거림 횟수(um, uh, er, erm, hmm, mm, you know, i mean — like 제외), 자주 쓴 단어(기능어 제외 3회 이상 상위 3개). 다시 답하기 / 다음 질문 / 내 답변 듣기
- 녹음·재생 공용: `core:stt/PcmRecorder`, `PcmPlayer` (섀도잉·스피킹), `core:common/Recording.kt`
- 저장: TASK 22 참고

### 답변 직접 고치기 + 문법 교정 (2026-10-10, TASK 18)
- Whisper 단어별 시각·확신도: `transcribe(withWords = true)` → `SpokenWord(text, startMs, endMs, confidence)` (`token_timestamps`, 토큰을 앞 공백 기준으로 합침, confidence = 조각 확률 최솟값)
- 결과 화면 단어 칩: 짧게 누르기 → 그 단어 구간(앞뒤 0.2초) 재생, 꾹 누르기 → 바꾸기 / 지우기 / 뒤에 넣기 / 원래대로. 표시: 바꿈 파란 밑줄, 지움 회색 취소선, 넣음 파란 글자, 머뭇거림 회색. "Whisper 원문 보기" 전환, 전체 되돌리기. 지표는 고친 글로 다시 계산
- "문법 교정 받기 (AI)": Whisper 해제 → Gemma 준비(문법 탭과 같은 준비 화면) → 고친 글을 최대 15문장으로 나눠 문장마다 결과 카드(문법 탭과 같음), 취소·실패 문장 다시 시도
- 공용 모듈 `core:correction`: `CorrectionCoordinator.correct(sentences, max)`, `CorrectionResultCard`, `LlmPreparationScreen`

### 발음 힌트 (2026-10-10, TASK 19 — 섀도잉 비교 결과에도 표시)
- 채점이 아닌 힌트. 카드 "발음 힌트"(접기 가능), 단어 칩을 누르면 TTS 원어민 발음
- A. 스피킹: 고치지 않은 Whisper 단어 중 확신도 < 0.5 (머뭇거림·숫자·1글자 제외, 낮은 순 5개) + "내 발음"(그 구간 재생). 섀도잉: 원문 대비 다르게 들린·빠뜨린 단어 5개
- B. 한국인 발음 팁 8종 (`core:common/Pronunciation.kt`): R/L, F≠P, V≠B, TH, Z≠J, 끝소리 '으' 금지, W 입술, 긴·짧은 '이' — 답변(스피킹은 고친 글, 섀도잉은 원문)에 나온 단어만, 많이 걸린 순 4개, 예시 4개
- C. 연음(자음 끝 + 모음 시작, 4쌍), 약화(want to→wanna, going to→gonna, got to→gotta, kind of→kinda, a lot of→a lotta, have to→hafta), t 약화(모음 사이 t, 4개), 리듬 팁 고정 문구
- 공용 화면 부품 모듈 `core:ui` (`PronunciationHintsCard`)

### 문항 확대 + 모의고사 (2026-10-10, TASK 20)
- `speaking.json` dataVersion 2: 50주제 164문항 — 자기소개 1, 설문 25, 돌발 16, 롤플레이 8 (세트 6: 질문하기 → 문제 해결 → 관련 경험). 주제 `category`(intro/survey/unexpected/roleplay), 문항 `level`(IM/IH/AL), type `issue` 추가
- 주제 목록: 분류별 구역, "N문항 · IM~AL", 질문 화면에 등급
- 모의고사(`buildMockExam`): 1 자기소개 → 2~4·5~7 서로 다른 설문 주제 앞 3문항 → 8~10 돌발 → 11~13 롤플레이 세트 → 14 비교(compare) → 15 이슈(issue). 문항마다 TTS·다시 듣기 1회·2분 녹음, 건너뛰기·끝내기 → 한꺼번에 받아 적기 → 요약(문항별 시간·분당 단어·머뭇거림, 평균) → 문항을 누르면 결과 화면
- 저장: TASK 22 참고

### 음성 인식 검증 (2026-10-07, TASK 14)
- 엔진: whisper.cpp v1.9.5 (`third_party/whisper.cpp`, CPU, arm64-v8a, fp16·dotprod), 모듈 `:core:stt` — 지금은 `debugImplementation`만 (사용자 기능 TASK에서 `implementation`으로)
- **채택 모델: `ggml-small.en-q5_1.bin`** (190MB, `no_backup/stt/`, 버튼을 눌렀을 때만 받음)
  - S23+ 20초 답변 약 8초 (RTF 0.37~0.47), 대본 낭독 WER 0~8.6%, 메모리 약 0.43GB, 로딩 0.3초
  - large-v3(RTF 약 3.0)·large-v3-turbo(RTF 약 2.3)는 정확도가 같고 속도 기준(RTF ≤ 0.5) 미달
- 오디오: `AudioRecord` 16kHz mono PCM16 (`VOICE_RECOGNITION`), 앱 내부 저장. 받아 적기 greedy, language "en", 4스레드
- 채점용 `core:common/Wer.kt` `wordErrorRate(reference, hypothesis)`: 소문자, 문장부호 제거(단어 안 `'` 유지), 단어 단위 편집 거리 / 기준 단어 수
- 측정표·남은 위험: `docs/tasks/14-whisper-spike/HANDOFF.md`

## 기록·백업 (2026-10-10, TASK 22)
- **DB v3**: `speaking_answers(id, language, questionId, topicId, createdAt, durationMs, transcript, editedText, wordCount, wordsPerMinute, fillerCount, sentenceCount, mockId?)`, `shadowing_attempts(id, language, videoId, sentence, heard, matchRate, createdAt)`. `MIGRATION_2_3` = CREATE TABLE 2 + INDEX 3
- 스피킹: 받아 적기 성공 때 저장, 고칠 때마다 갱신. 질문 화면 "지난 답변 N개 · 마지막 …", 결과 화면 "지난번과 비교"(시간·분당 단어·머뭇거림, 지난 답변 글), 주제 목록 "내 기록"(모의고사 요약 + 최근 답변 50개, 누르면 전문). 모의고사 답변은 같은 mockId
- 섀도잉: 따라 말하기(원문 있을 때)마다 저장. 추천 목록 "최근 연습한 영상" 5개, 카드·영상 화면에 "연습 N회 · 최고 일치율 N%"
- 백업·복원: 홈 "학습 기록 백업·복원" → `opic-backup-YYYY-MM-DD.json` (시스템 파일 선택). 내용: 단어 기록(`language, word`로), 문법 복습, 스피킹 답변, 섀도잉 연습 (녹음 제외). 복원은 한 트랜잭션 병합 — 더 최근 기록 우선, 같은 답변 중복 없음, 없는 단어 건너뜀, 형식이 틀리면 변경 없음

## 분석
(기능 추가 시 여기에 작성)
