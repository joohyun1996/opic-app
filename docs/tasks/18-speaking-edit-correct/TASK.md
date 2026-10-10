# TASK: 스피킹 2 — 받아 적은 답변 직접 고치기 + Gemma 문법 교정

> 작성: Claude · 승인: [x] 사용자 (2026-10-10) · 구현: Claude
> 경로: docs/tasks/18-speaking-edit-correct/TASK.md
> 근거: 사용자 제안 (2026-10-10) — "AI 분석 전에 내가 말한 걸 들으면서 틀린 단어를 꾹 눌러 고치고(Whisper가 옳게 고쳐 적은 실수는 다시 틀리게), 그걸로 문법 체크"

## 목표
Whisper는 작은 문법 실수를 저절로 고쳐 받아 적는다("he go" → "he goes"). 사용자가 내 녹음을 단어 단위로 들으며 받아 적은 글을 **실제로 말한 대로** 고친 뒤, 그 글로 Gemma 문법 교정을 받는다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/stt/src/main/cpp/whisper_jni.c`, `core/stt/.../WhisperEngine.kt`, `UserWhisper.kt` | 수정 | 단어 시각·확신도를 함께 돌려주는 받아 적기 (`token_timestamps`) |
| `core/common/.../SpokenWords.kt` + 테스트 | 생성 | 토큰 → 단어 합치기, 단어 편집(바꾸기·지우기·뒤에 넣기·되돌리기) 순수 함수 |
| `core/common/.../Writing.kt` + 테스트 | 수정 | `sentencesToCorrect(input, max = 5)` — 기본값 5 유지(문법 탭 그대로), 스피킹은 15 |
| `core/correction/**` (새 모듈) | 생성 | `feature/grammar`의 `CorrectionCoordinator`·`CorrectionResultCard`·`GrammarPreparationScreen`(→ `LlmPreparationScreen`)을 옮김. 문법·스피킹이 같이 씀 |
| `feature/grammar/**` | 수정 | 옮긴 코드 import만 바꿈 (동작 변경 없음) |
| `feature/speaking/**` | 수정 | 결과 화면: 단어 칩(누르면 그 부분 재생, 꾹 누르면 편집), 교정 받기 흐름 |
| `core/stt/.../PcmPlayer.kt` | 수정 | `play(file, fromMs, toMs)` 구간 재생 |
| `app/src/main/java/com/jooh/opic/OpicRoot.kt`, `settings.gradle.kts`, 각 모듈 `build.gradle.kts` | 수정 | 모듈 연결, 스피킹에 `llmEngine`·`hfTokenStore` 전달 |

## 관련 파일 (읽기만)
- `feature/grammar/.../GrammarWritingViewModel.kt`, `GrammarFlow.kt` — 교정 흐름·준비 화면 사용 방식
- `core/common/.../Correction.kt` — `buildCorrectionPrompt`, `parseCorrection`
- `third_party/whisper.cpp/include/whisper.h` — `whisper_full_get_token_data`(t0, t1, p), `whisper_full_get_token_text`

## 요구사항
### 1. 단어 시각·확신도 (core/stt)
- `params.token_timestamps = true`. 세그먼트·토큰을 돌며 `t0·t1`(10ms 단위)·`p`·텍스트를 줄 단위(`t0\tt1\tp\ttext\n`)로 넘긴다. 특수 토큰(`[_`, `<|`로 시작)은 빼고, `token_timestamps`가 꺼진 기존 호출 결과 문자열은 그대로
- Kotlin: `Transcription.words: List<SpokenWord>` (`text, startMs, endMs, confidence`). 섀도잉은 쓰지 않아도 된다
- `mergeTokens(tokens): List<SpokenWord>` (core/common, 순수): 앞 공백으로 새 단어 시작, 공백 없는 토큰은 앞 단어에 붙임, 문장부호 토큰은 앞 단어에 붙임, 시각은 첫 토큰 t0 ~ 마지막 t1, confidence는 토큰 p의 최솟값

### 2. 결과 화면 — 직접 고치기 (feature/speaking)
- 답변 전문을 단어 칩(FlowRow)으로: **짧게 누르기** → 그 단어 구간(앞뒤 0.2초 여유) 재생. **꾹 누르기** → 편집 창: 바꾸기(입력칸) / 지우기 / 뒤에 단어 넣기 / 원래대로
- 고친 단어는 파란 밑줄, 지운 단어는 취소선으로 남겨 보이게(되돌리기 가능), 넣은 단어는 파란색. 위에 "Whisper 받아 적기 ↔ 내가 고친 글" 전환
- "전체 되돌리기" 버튼
- 지표(단어 수·머뭇거림·반복 단어)는 고친 글로 다시 계산, 말한 시간은 그대로
- 안내 한 줄(고정): "Whisper는 작은 실수를 고쳐서 적을 때가 있어요. 내 목소리를 들으며 실제로 말한 대로 고친 뒤 교정을 받으세요"
- 순수 함수 (`SpokenWords.kt`): `EditableWord(original: SpokenWord?, text, state = KEPT|REPLACED|DELETED|INSERTED)`, `replace/delete/insertAfter/restore(list, index)`, `editedText(list)` (DELETED 제외, 공백 연결)

### 3. 문법 교정 (feature/speaking + core/correction)
- "문법 교정 받기" → Whisper를 내리고(`app.whisper.close()`) Gemma 준비(문법 탭과 같은 준비 화면: 모델 없음·받기·HF 토큰·불러오기) → 고친 글을 `sentencesToCorrect(text, 15)`로 나눠 **문장마다 끝나는 대로 표시**, 취소 가능, 실패한 문장만 다시 시도
- 결과 카드는 문법 탭과 같은 `CorrectionResultCard` (원문 → 고친 문장, 유형별 고정 설명 + 접힌 AI 설명)
- 진행 표시 "3 / 9 문장 · 문장당 15초 안팎"
- 교정 중 다시 녹음하려면 Gemma를 내리고 Whisper를 다시 올린다 (동시 적재 금지, 기존 `releaseGemmaBeforeWhisper` 사용)

## 수용 기준
- [x] AC1: `mergeTokens` — `" I"," li","ke"," it","."` → `I`, `like`, `it.` (시각·최소 p 확인), 특수 토큰 제외, 빈 목록 (테스트)
- [x] AC2: 편집 함수 — 바꾸기·지우기·뒤에 넣기·되돌리기 후 `editedText`, 넣은 단어 되돌리기 = 제거, 범위 밖 index는 그대로 (테스트)
- [x] AC3: `sentencesToCorrect(text)`는 기존처럼 5문장, `sentencesToCorrect(text, 15)`는 15문장 (테스트). 문법 탭 동작 변경 없음
- [x] AC4: `core/correction`으로 옮긴 뒤 문법 탭 기존 테스트 전부 통과
- [x] AC5: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, Manifest·Room 변경 없음
- [ ] AC6: 실기기 — 1분 답변으로: 단어 눌러 구간 재생, 단어 하나 고치기·지우기·넣기·되돌리기, 고친 글로 교정 2문장 이상 표시, 교정 뒤 다시 녹음 (스크린샷: 편집 화면, 교정 결과)

## 제약 / 주의
- Gemma와 Whisper를 동시에 올리지 않는다
- 녹음·고친 글·교정 결과를 밖으로 보내지 않는다. 새 권한·의존성 없음
- `core/stt` CMake 설정 유지, `gradle/gradle-daemon-jvm.properties` 커밋 금지
- 실기기 확인은 `adb install -r`만, 앱·데이터 삭제 금지

## 범위 밖 (하지 말 것)
- 발음 힌트·불명확 단어 표시 (TASK 19 — 이번에 만든 confidence를 거기서 씀)
- 답변·교정 기록 저장 (TASK 21), 섀도잉 화면 변경
