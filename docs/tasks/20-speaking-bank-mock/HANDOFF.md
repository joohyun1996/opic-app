# HANDOFF: 스피킹 3 — 문항 확대 + 모의고사

> 작성: Claude (사용자 요청으로 Claude 구현, 2026-10-10) · 경로: docs/tasks/20-speaking-bank-mock/HANDOFF.md

## 커밋 범위
- base: `5c1fb38` · head: `f9d4e94` (콘텐츠 `bd3f166`, 구현 `f9d4e94`) · 리뷰 명령: `git diff 5c1fb38..f9d4e94`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `exports/speaking.json` | 수정 | dataVersion 2, 50주제 164문항, category·level |
| `core/common/.../Speaking.kt` + 테스트 | 수정 | category·level 검증, type `issue`, `MockItem`·`buildMockExam` |
| `feature/speaking/...ViewModel.kt` | 수정 | 녹음 대상 파일 분기, `transcribeFile`, 모의고사 시작·다음·끝내기·한꺼번에 받아 적기·요약·문항 열기 |
| `feature/speaking/...Screen.kt` | 수정 | 주제 목록 구역·등급 범위, 모의고사 헤더·건너뛰기·끝내기, 받아 적기 진행·요약 화면 |

## 수용 기준 체크
| AC | 결과 | 검증 |
|----|------|------|
| AC1 | ✅ | `SpeakingTest.parsesBundledFile` (140문항 이상, 기존 id 표본 9개, category 4종·level 3종) |
| AC2 | ✅ | `rejectsInvalidCatalogs` (잘못된 category·level, 기존 type·id 검사) |
| AC3 | ✅ | `mockExamFollowsOpicOrder` (15문항 순서·주제 규칙·중복 없음·seed 결정적·롤플레이 세트 없으면 null) |
| AC4 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과 |
| AC5 | 보류 | 무선 디버깅이 끊겨 실기기 미확인 (아래 순서) |

### AC5 사용자 확인 순서
1. 스피킹 → 주제 목록이 자기소개·설문·돌발·롤플레이로 나뉘고 "4문항 · IM~AL" 표시
2. "모의고사" → 헤더 "모의고사 1 / 15", 질문 TTS, 1번 답변 → 자동으로 2번
3. 2번 답변 → "건너뛰기" 한 번 → "끝내기" → 받아 적기 진행 → 요약(답한 2문항 지표, 나머지 "답하지 않음")
4. 요약에서 1번 누르기 → 결과 화면(고치기·교정·발음 힌트) → "← 모의고사 결과"로 돌아와 고친 내용이 남는지

## 설계 판단
- **받아 적기를 끝에 한꺼번에**: 실제 시험처럼 흐름이 끊기지 않게. 녹음은 `mock-01.pcm`~`mock-15.pcm` (새 모의고사 시작 때 지움)
- **롤플레이 세트 판정**: 주제 앞 3문항 type이 `[roleplay_ask, roleplay_solve, experience]`인 것만 — TASK 17의 연습용 롤플레이 주제는 자동 제외
- **14·15번**: 앞에서 쓰지 않은 설문·돌발 주제에서 compare·issue를 고른다 (같은 주제 반복 방지)
- **결과 화면 재사용**: 모의고사 답변을 열면 `audioFile`을 그 답변 파일로 바꿔 단어 재생·발음 힌트가 그대로 동작. 돌아갈 때 고친 글을 목록에 저장
- **끝내기 때 Whisper가 내려가 있으면**(교정을 열었다 온 경우) 저장된 모델을 다시 불러온다

## 범위 밖 변경
- 없음
