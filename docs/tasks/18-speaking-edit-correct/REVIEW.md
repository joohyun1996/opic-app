# REVIEW: 스피킹 2 — 받아 적은 답변 직접 고치기 + Gemma 문법 교정

> 작성: Claude · 경로: docs/tasks/18-speaking-edit-correct/REVIEW.md

## 1차 리뷰 — 2026-10-10
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)
- 데이터 안전: Room 변경 없음. 편집 상태는 메모리만
- JNI: `cap`을 세그먼트·토큰 텍스트 길이 + 토큰당 48바이트 여유로 잡고 `snprintf`로 씀 → 넘침 없음. `token_timestamps`는 `with_tokens`일 때만 (섀도잉 결과 문자열 그대로)
- 동시 적재: 교정 진입 시 Whisper 해제 → Gemma, 재녹음 시 Gemma 해제 → Whisper

### Must-fix / Should-fix
- 없음

### Nit
- **N1** 실기기 AC6 (녹음 필요) — 사용자 확인 대기
- **N2** 단어 칩이 작은 글씨라 꾹 누르기 대상이 좁다 — 디자인 개편 때 칩 여백·배경 넣기
