# HANDOFF: 단어 탭 다듬기 — 셔플, 힌트 버튼, Day 목록에 뜻 표시

> 작성: Claude (구현 대행) · 경로: docs/tasks/09-words-polish/HANDOFF.md

## 커밋 범위
- base: `6b006e3`
- 구현: `30cfa16` (사용자 커밋, push 완료) + 이 커밋(문서·스크린샷)

## 변경 요약
```diff
~ StudyViewModel.kt — 시작·"다시 하기" 때 cards.shuffled(), hintShown 상태, showHint(), 다음 카드에서 hintShown 초기화
~ StudyScreen.kt    — 한→영: 힌트는 "힌트 보기" 버튼을 눌러야 보임, 확인 후에는 버튼 숨김
~ DayScreen.kt      — 상태 뱃지 제거, 오른쪽에 한국어 뜻(습득 단어는 초록, 2줄 말줄임), ♪는 단어 바로 오른쪽,
                      "습득 N/40" 줄 오른쪽 끝에 "한국어 숨기기/보기" (앱 실행 중 유지되는 전역 상태)
```

## 수용 기준 체크
| AC | 결과 | 근거 (S23+) |
|----|------|------|
| AC1 셔플 | 통과 | 영→한 두 번 시작, 첫 5장: `undermine, spire, saber, fanaticism, cavity` / `cultivate, fleck, fanaticism, annihilate, boulevard` |
| AC2 힌트 | 통과 | 한→영 5장 모두 처음엔 힌트 숨김 + "힌트 보기" 버튼, 누르면 `힌트: a _ _ _ _ _ _ _ r` 등 표시, 다음 카드에서 다시 숨김 (`hint-shown.png`) |
| AC3 뜻·토글 | 통과 | 뱃지 없음·뜻 표시 (`day1-korean.png`), "한국어 숨기기" → 뜻 사라짐·버튼 "한국어 보기" (`day1-hidden.png`) → 다시 표시 |
| AC4 | 통과 | `./gradlew test lint` exit 0 |

플래시카드는 사용자 요청대로 Day마다 5장만 풀었다 (40장 전체 아님).

## 리뷰어가 봐야 할 곳
- 한→영 카드 3번 뜻이 `군도, 기병검`(saber)이었다. "군도"는 archipelago의 뜻이기도 해서 한→영에서 헷갈릴 수 있다 (데이터의 동음이의어, TASK 08 REVIEW N2와 같은 문제).

## 범위 밖 변경
- 없음
