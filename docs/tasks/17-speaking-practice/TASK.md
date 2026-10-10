# TASK: 스피킹 1 — 질문 은행 + 답변 녹음 + 받아 적기 + 즉시 지표

> 작성: Claude · 승인: [x] 사용자 (2026-10-10) · 구현: GPT
> 경로: docs/tasks/17-speaking-practice/TASK.md
> 근거: 사용자 결정 (2026-10-10) — 질문은 TTS로 듣기 + 글 숨김, 답변 최대 2분, 17(이번)·18(Gemma 피드백)·19(기록 저장 DB v3)로 분할, 콘텐츠는 Claude 초안 → GPT 검토

## 목표
OPIc 질문을 귀로 듣고 바로 영어로 답하면, Whisper(small.en)로 받아 적고 LLM 없이 계산한 지표(말한 시간, 분당 단어 수, 머뭇거림, 반복 단어)를 보여 준다. 기록 저장·AI 피드백은 하지 않는다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `docs/tasks/17-speaking-practice/CONTENT-REVIEW.md` | 생성 | 초안 검토 기록 (문법 TASK 10과 같은 방식) |
| `exports/speaking.json` | 생성 | `speaking-draft.json` 검토·수정본 (dataVersion 1) |
| `core/common/.../Speaking.kt` + 테스트 | 생성 | 질문 데이터 파싱·검증, 답변 지표 계산 (순수 함수) |
| `core/stt/**` | 수정 | `transcribe`에 선택 인자 `prompt`(whisper `initial_prompt`) 추가 — 기존 호출은 그대로 동작 |
| `feature/speaking/**` | 생성 | 새 모듈: 주제 목록, 질문 화면, 녹음, 결과 화면, ViewModel |
| `app/build.gradle.kts` | 수정 | `exports/speaking.json` → assets 복사 (grammar.json과 같은 방식), `:feature:speaking` 의존 |
| `app/src/main/java/com/jooh/opic/OpicApplication.kt` | 수정 | speaking.json 백그라운드 1회 파싱 |
| `app/src/main/java/com/jooh/opic/OpicRoot.kt` | 수정 | `speaking` 경로, TTS는 기존 `Speaker`를 람다로 넘김 (`feature:speaking`은 `feature:words`에 의존하지 않음) |
| `feature/words/.../WordsApp.kt` | 수정 | 홈 "스피킹" 카드 (섀도잉 카드 아래) |
| `settings.gradle.kts` | 수정 | `:feature:speaking` |

## 관련 파일 (읽기만)
- `docs/tasks/17-speaking-practice/speaking-draft.json` — 질문 초안 (15주제, 43문항)
- `feature/shadowing/.../ShadowingViewModel.kt` — 녹음(AudioRecord)·Whisper 준비·취소 패턴 (TASK 15 리뷰 반영본)
- `core/stt/.../UserWhisper.kt`, `WhisperEngine.kt`, `src/main/cpp/whisper_jni.c`
- `feature/grammar/.../GrammarCatalog.kt` — JSON 검증·실패 처리 패턴
- `feature/words/.../Speaker.kt` — TTS

## 요구사항
### 콘텐츠 검토 (먼저)
- `speaking-draft.json`을 검토해 `exports/speaking.json`으로 확정. 확인할 것: 영어 질문이 실제 OPIc 말투인지, 한국어 번역 정확성, 팁이 질문 유형에 맞는지, id 중복 없음
- 바꾼 문항은 `CONTENT-REVIEW.md`에 `id / 전 / 후 / 이유` 표로. 주제·문항 수는 늘리거나 줄여도 되지만 이유를 적는다
- 형식: `{dataVersion, topics:[{id, titleKo, questions:[{id, type, en, ko, tip}]}]}`, type ∈ `describe | routine | experience | compare | roleplay_ask | roleplay_solve`

### 화면 / 동작
1. **주제 목록**: 홈 "스피킹" → 주제 15개(제목 + 문항 수) + "무작위 질문" 버튼(전체에서 하나)
2. **질문 화면**: 들어가면 TTS로 질문을 자동 재생, 글은 숨김. "다시 듣기"(1회만, 실제 시험처럼), "질문 글 보기"(영어 + 한국어 + 팁)
3. **녹음**: "● 답변 시작" → 경과 시간 `m:ss / 2:00` 표시 → "■ 끝" 또는 2:00 자동 정지. TTS 재생 중이면 멈춘 뒤 녹음. 녹음 파일은 `filesDir/speaking-last.pcm` 1개만
4. **받아 적기**: Whisper small.en, `prompt = "Um, uh, so, like, you know, I mean."` (머뭇거림을 지우지 않게). 진행 표시 + 취소. 모델이 없거나 안 불러왔으면 섀도잉과 같은 준비 UI (받기 190MB / 불러오기 / 실패 다시 시도), Gemma가 올라가 있으면 먼저 내린다
5. **결과 화면**: 받아 적은 답변 전문 + 지표 카드 + "다시 답하기"(같은 질문) / "다음 질문"(같은 주제 다음 문항, 끝이면 첫 문항) + "내 답변 듣기"
   - 머뭇거림 단어는 답변 전문에서 회색으로 표시

### 지표 (`core/common/Speaking.kt`)
`speakingMetrics(transcript: String, durationMs: Long): SpeakingMetrics`
- `wordCount`: `werWords` 기준, 머뭇거림 제외
- `wordsPerMinute`: wordCount / (durationMs / 60000), durationMs < 1000이면 0
- `fillerCount`: `um`, `uh`, `er`, `erm`, `hmm`, `mm` 단어 + `you know`, `i mean` 구 (대소문자·문장부호 무시). `like`는 세지 않는다 (동사·전치사와 구분 불가)
- `sentenceCount`: `.?!` 기준, 빈 문장 제외
- `repeatedWords`: 3회 이상 쓴 단어 상위 3개 (`(word, count)`), 기능어(the, a, an, and, i, to, is, ... 30개 안팎 고정 목록)·머뭇거림 제외, 횟수 내림차순 → 단어 오름차순
- 화면 안내 문구(고정): 분당 단어 수 < 90 "조금 더 빠르게", 90~150 "적당한 속도", > 150 "조금 천천히" / 말한 시간 < 60초 "1분 이상 말해 보세요"

### 데이터 파싱
`parseSpeakingCatalog(json: String): SpeakingCatalog?` — 형식이 틀리거나, id가 중복되거나, type이 목록 밖이면 null → 화면에 "스피킹 데이터를 불러오지 못했습니다" (앱은 계속 동작)

## 수용 기준
- [ ] AC1: `parseSpeakingCatalog` — `exports/speaking.json` 통과, id 중복·잘못된 type·빈 en은 null (테스트, 실제 파일을 읽는 테스트 1개 포함)
- [ ] AC2: `speakingMetrics` — 머뭇거림 포함 문장에서 wordCount·fillerCount, `like` 미집계, 60초에 120단어 → 120 wpm, durationMs 0 → 0, 반복 단어 순서·기능어 제외, 빈 문자열 (테스트)
- [ ] AC3: `transcribe(prompt = null)`이 기존과 같게 동작 (섀도잉·검증 화면 호출 변경 없음), 빌드 통과
- [ ] AC4: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과
- [ ] AC5: AndroidManifest 권한·Room 스키마 변경 없음
- [ ] AC6: 실기기 확인 — **보류 (사용자가 나중에 한꺼번에)**. HANDOFF에 확인 순서만: 질문 TTS·다시 듣기 1회 제한, 2분 자동 정지, 1분 이상 답변의 받아 적기 시간, 머뭇거림이 받아 적히는지

## 제약 / 주의
- Whisper는 `UserWhisper` 하나를 섀도잉과 같이 쓴다 (새로 만들지 않음). 녹음·받아 적기는 IO/Default 스레드
- 2분 오디오는 whisper가 30초 창 여러 개로 처리한다 — 별도로 자르지 않는다
- 녹음·답변을 외부로 보내지 않는다. 새 권한·의존성 없음
- `core/stt` CMake 설정(Release 강제, `GGML_CPU_ARM_ARCH`) 유지, `gradle/gradle-daemon-jvm.properties` 커밋 금지
- 폰에 설치하지 않는다

## 범위 밖 (하지 말 것)
- Gemma 피드백·교정 (TASK 18), 답변 기록 저장·비교 (TASK 19, DB v3)
- 발음 점수, 모의고사(질문 세트·시간 제한 시험 모드), 난이도 선택
