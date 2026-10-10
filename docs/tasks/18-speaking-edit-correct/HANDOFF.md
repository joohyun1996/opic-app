# HANDOFF: 스피킹 2 — 받아 적은 답변 직접 고치기 + Gemma 문법 교정

> 작성: Claude (사용자 요청으로 Claude 구현, 2026-10-10) · 경로: docs/tasks/18-speaking-edit-correct/HANDOFF.md

## 커밋 범위
- base: `01cc19e` · head: `17a36b3` · 리뷰 명령: `git diff 01cc19e..17a36b3`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/stt/.../whisper_jni.c` | 수정 | `with_tokens`면 `token_timestamps` 켜고 텍스트 뒤 `\n#TOKENS\n` + `t0\tt1\tp\ttext` 줄 |
| `core/stt/.../WhisperEngine.kt`, `UserWhisper.kt` | 수정 | `transcribe(withWords = false)` → `Transcription.words` |
| `core/stt/.../PcmPlayer.kt` | 수정 | `play(file, fromMs, toMs)` 구간 재생 |
| `core/common/.../SpokenWords.kt` + 테스트 | 생성 | `parseTokenLines`, `mergeTokens`, `EditableWord` 편집 함수, `editedText`, `playRange` |
| `core/common/.../Writing.kt` + 테스트 | 수정 | `sentencesToCorrect(input, max = 5)` |
| `core/correction/**` | 생성(이동) | `CorrectionCoordinator`(`correct(max)`)·`CorrectionResultCard`·`LlmPreparationScreen`(구 `GrammarPreparationScreen`, 버튼 문구 인자) + 테스트 |
| `feature/grammar/**` | 수정 | import만 변경 |
| `feature/speaking/**` | 수정 | 결과 화면 편집·교정, ViewModel |
| `app/.../OpicRoot.kt`, `settings.gradle.kts`, `build.gradle.kts`들 | 수정 | 모듈 연결, 스피킹에 Gemma 엔진·토큰 저장소 전달 |

## 수용 기준 체크
| AC | 결과 | 검증 |
|----|------|------|
| AC1 | ✅ | `SpokenWordsTest.mergesTokensIntoWords`(` I`·` li`·`ke`·` it`·`.` → I / like / it., 시각·최소 p, 특수 토큰 제외, 빈 목록), `parsesTokenLines` |
| AC2 | ✅ | `editsWords`(바꾸기·원문으로 바꾸면 KEPT·지우기·넣기·되돌리기·넣은 단어 지우면 제거·전체 되돌리기·범위 밖 그대로), `playRangeUsesNeighbourForInsertedWord` |
| AC3 | ✅ | `WritingTest.speakingAllowsFifteenSentences` (기본 5, 15) |
| AC4 | ✅ | `CorrectionCoordinatorTest`(core:correction으로 이동) 포함 문법 탭 테스트 통과. 실기기 문법 탭 진입 확인 |
| AC5 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, Manifest·Room 변경 없음 |
| AC6 | 보류 | 녹음이 필요해 사용자 확인 (아래) |

### AC6 사용자 확인 순서
1. 스피킹 → 1분 답변 → 결과에서 단어 **짧게 누르기** → 그 단어 부분만 들리는지
2. 단어 **꾹 누르기** → 바꾸기(파란 밑줄) / 지우기(취소선) / 뒤에 넣기(파란 글자) / 원래대로, "Whisper 원문 보기" 전환, 전체 되돌리기
3. "문법 교정 받기 (AI)" → (모델 있으면 자동 불러오기) → "교정 시작" → 문장 카드가 하나씩 나오는지, 고친 단어가 반영된 문장으로 교정되는지
4. 교정 뒤 "다시 답하기" → "음성 인식 모델 불러오기" → 다시 녹음되는지

## 설계 판단 / 리뷰어가 봐야 할 곳
- **토큰 → 단어**: whisper 토큰은 단어 조각이라 앞 공백 기준으로 합친다. 문장부호는 앞 단어에 붙여 칩 하나로(`it.`). confidence는 조각 중 최솟값 — TASK 19 "불명확 단어"용
- **넣은 단어의 재생 구간**: 시각이 없으므로 앞(없으면 뒤) Whisper 단어의 구간을 들려준다
- **교정 흐름의 모델 교체**: 교정을 열면 `whisper.close()` → `app.llmEngine`(없으면 새로 만듦). 다시 녹음하려면 기존 "음성 인식 모델 불러오기"가 `releaseGemmaBeforeWhisper`로 Gemma를 내린다 → 동시 적재 없음
- **지표 재계산**: 편집할 때마다 `speakingMetrics(editedText, durationMs)`
- **단어 시각 실패 시**: words가 비면 텍스트를 공백으로 나눠 시각 0~전체로 채운다 (편집은 되고, 재생은 전체)

## 범위 밖 변경
- 없음 (feature/grammar는 TASK 수정 범위에 포함)
