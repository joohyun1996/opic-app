# REVIEW: 단어 탭 다듬기

> 작성: Claude · 경로: docs/tasks/09-words-polish/REVIEW.md

## 1차 리뷰 — 2026-10-07
- 대상: `git diff 6b006e3..30cfa16`
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)

### Must-fix / Should-fix
- 없음

### Nit
- **N1** `DayScreen.kt` — "한국어 숨기기/보기" 선택은 앱을 다시 시작하면 "보기"로 돌아간다. 계속 유지가 필요하면 DataStore 등에 저장한다.
- **N2** 셔플은 `Random` 기본값을 써서 테스트로 순서를 고정할 수 없다. 지금은 실기기 비교로 확인했다.
