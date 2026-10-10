# HANDOFF: 스피킹 1 — 질문 은행 + 답변 녹음 + 받아 적기 + 즉시 지표

> 작성: Claude (사용자 요청으로 Claude 구현, 2026-10-10) · 경로: docs/tasks/17-speaking-practice/HANDOFF.md

## 커밋 범위
- base: `06c3ab1`
- head: `1556e50` (콘텐츠 `74653e2`, 구현 `1556e50`)
- 리뷰 명령: `git diff 06c3ab1..1556e50`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `exports/speaking.json`, `CONTENT-REVIEW.md` | 생성 | 15주제 43문항, 4문항 문구·1문항 type 수정 |
| `core/common/.../Speaking.kt` + `SpeakingTest` | 생성 | `parseSpeakingCatalog`, `speakingMetrics`, `fillerMask`, `paceAdvice`, `durationAdvice` |
| `core/common/.../Recording.kt` + `RecordingTest` | 이동 | 섀도잉의 `recordingTooShort`·`cleanWhisperText`를 공용으로 (내용 그대로) |
| `core/stt/.../PcmRecorder.kt`, `PcmPlayer.kt` | 생성 | 16kHz 녹음(입력 크기 100ms마다)·재생 공용화 |
| `core/stt/.../whisper_jni.c`, `WhisperEngine.kt`, `UserWhisper.kt` | 수정 | `transcribe(prompt: String? = null)` → whisper `initial_prompt` |
| `feature/shadowing/...ViewModel.kt`, `Screen.kt` | 수정 | 녹음·재생을 공용 코드로 교체 (동작 같음) |
| `feature/speaking/**` | 생성 | 주제 목록·질문·결과 화면, ViewModel |
| `app/build.gradle.kts`, `settings.gradle.kts`, `OpicApplication.kt`, `OpicRoot.kt`, `WordsApp.kt` | 수정 | speaking.json assets·파싱, `speaking` 경로, 홈 "스피킹" 카드 |

## 수용 기준 체크
| AC | 결과 | 검증 |
|----|------|------|
| AC1 | ✅ | `SpeakingTest.parsesBundledFile`(실제 파일 15주제·43문항), `rejectsInvalidCatalogs`(id 중복·잘못된 type·빈 en·빈 JSON) |
| AC2 | ✅ | `countsWordsAndFillers`(like 미집계, you know 1회), `wordsPerMinute`(60초 120단어 → 120, 0ms·999ms → 0), `repeatedWordsSkipFunctionWords`, `emptyTranscript`, `advice` |
| AC3 | ✅ | `prompt` 기본값 null → 섀도잉 호출 변경 없음, 빌드 통과 |
| AC4 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과. release APK에 `assets/speaking.json`, `libopic_whisper.so` |
| AC5 | ✅ | Manifest·Room 변경 없음 |
| AC6 | 일부 확인 | Claude가 덮어 설치로 화면만 확인: 홈 카드, 주제 목록 15개, 질문 화면·질문 글 보기, 모델 불러오기 → "답변 시작". **녹음·TTS 소리·받아 적기는 사용자 확인 필요** (아래 순서) |

### AC6 사용자 확인 순서
1. 홈 → 스피킹 → 주제 하나 → 질문이 소리로 나오는지 (폰 무음 해제)
2. "다시 듣기 (1회)" 한 번 뒤 "다시 듣기 끝"으로 바뀌는지
3. 1분 이상 답변 → "■ 끝" → 받아 적기 시간(1분 답변 약 15~20초 예상) → 결과에 um/uh가 회색으로 남는지
4. 2분까지 말하면 자동 정지되는지
5. "내 답변 듣기", "다시 답하기", "다음 질문"

## 설계 판단 / 리뷰어가 봐야 할 곳
- **Whisper prompt** — `FILLER_PROMPT = "Um, uh, so, like, you know, I mean."`을 `initial_prompt`로 넘긴다. Whisper는 원래 머뭇거림을 지우는 경향이 있어, 앞 문맥에 머뭇거림이 있으면 그대로 받아 적는 경향을 이용. JNI에서 `GetStringUTFChars`로 받고 `whisper_full` 뒤 해제. 섀도잉은 prompt 없음(원문 비교라 머뭇거림이 오히려 오류로 잡힘)
- **2분 녹음·받아 적기 시간** — 2분 = 30초 창 4개. TASK 14 측정(20초 ≈ 8초)으로 보면 약 30~40초 예상, 화면에 "2분 답변은 30~40초 걸려요" 안내 + 취소
- **TTS 연결** — `feature:speaking`은 `feature:words`를 모른다. `OpicRoot`가 기존 `Speaker`의 `speak`·`stop`을 람다로 넘긴다. 녹음 시작·화면 이탈 때 `stop`
- **녹음 공유** — 섀도잉 ViewModel 안에 있던 AudioRecord 루프를 `core:stt/PcmRecorder.record(file, maxMs, keepGoing, onLevel)`로 옮기고 두 화면이 같이 쓴다. AudioRecord 생성·stop·release는 이 함수 안에서만 (TASK 15 S1 원칙 유지). 재생도 `PcmPlayer`로 합침. 순수 판정 함수는 `core:common/Recording.kt` — TASK 17 수정 범위 밖(`feature/shadowing`)을 건드린 부분이라 리뷰 확인 필요
- **지표** — `wordCount`·`wpm`은 머뭇거림 제외. 반복 단어는 기능어 약 40개 제외, 3회 이상 상위 3개. 화면의 머뭇거림 회색 표시는 공백 단위 토큰을 `werWords`로 정규화해 `fillerMask` 적용
- **catalog 로딩** — `OpicApplication.speakingCatalog`는 `SpeakingLoad?` (null = 읽는 중, `catalog = null` = 실패). 읽는 중에 들어오면 ViewModel을 만들지 않는다

## 범위 밖 변경
- `feature/shadowing/**` — 녹음·재생 코드를 공용으로 교체 (동작 변경 없음, 사용자가 Claude에 구현을 맡김)

## 질문
- 없음
