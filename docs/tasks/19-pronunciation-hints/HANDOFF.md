# HANDOFF: 발음 힌트

> 작성: Claude (사용자 요청으로 Claude 구현, 2026-10-10) · 경로: docs/tasks/19-pronunciation-hints/HANDOFF.md

## 커밋 범위
- base: `81478fd` · head: 아래 feat 커밋 · 리뷰 명령: `git diff 81478fd..HEAD -- . ':!docs'`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/common/.../Pronunciation.kt` + `PronunciationTest` | 생성 | `unclearWords`, `PRONUNCIATION_RULES`(8개), `pronunciationTips`, `linkingPairs`, `reductions`, `flapWords`, `RHYTHM_TIP` |
| `core/ui/**` | 생성 | 새 모듈, `PronunciationHintsCard`·`UnclearHint` |
| `feature/speaking/...Screen.kt`, `ViewModel.kt` | 수정 | 결과 화면 지표 아래 발음 힌트, `say()` |
| `feature/shadowing/...Screen.kt` | 수정 | 비교 결과 아래 발음 힌트, `speak` 인자 |
| `app/.../OpicRoot.kt`, `settings.gradle.kts`, `build.gradle.kts`들 | 수정 | 모듈 연결, 섀도잉에 `speaker::speak` |

## 수용 기준 체크
| AC | 결과 | 검증 |
|----|------|------|
| AC1 | ✅ | `unclearWordsLowestFirstAndFiltered` (낮은 순, 머뭇거림·숫자·1글자·같은 단어 중복 제외, max·threshold) |
| AC2 | ✅ | `matchesKoreanSpeakerRules`(8규칙), `tipsSortedByHitsWithDistinctExamples` |
| AC3 | ✅ | `linkingReductionsAndFlaps` |
| AC4 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, 권한·Room 변경 없음 |
| AC5 | 보류 | 녹음 필요 — 사용자 확인: 스피킹 결과·섀도잉 비교 아래 "발음 힌트" 카드, 칩 누르면 TTS, 스피킹 "내 발음" |

## 설계 판단
- **불명확 단어 (스피킹)**: 사용자가 고치지 않은(KEPT) Whisper 단어만 본다. 고친 단어는 사용자가 이미 판단한 것이라 제외
- **다르게 들린 단어 (섀도잉)**: 원문이 있으므로 확신도보다 원문 대비 치환·빠뜨림이 더 정확한 신호 → diff 사용
- **규칙은 넓게 걸린다** (예: very는 r_l과 v_b 둘 다). 많이 걸린 순 최대 4개라 답변에 자주 나온 소리부터 보인다. 오탐은 "팁"이라 해가 적다고 판단
- **임계값 0.5**: TASK 14 대본 녹음 기준 감. 실기기에서 너무 많거나 적으면 조정 (Nit)
- **core:ui 모듈**: 디자인 개편 때 공용 테마·부품을 둘 자리로 먼저 만듦

## 범위 밖 변경
- 없음
