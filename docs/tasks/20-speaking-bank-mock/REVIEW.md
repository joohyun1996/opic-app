# REVIEW: 스피킹 3 — 문항 확대 + 모의고사

> 작성: Claude · 경로: docs/tasks/20-speaking-bank-mock/REVIEW.md

## 1차 리뷰 — 2026-10-10
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)
- 데이터 안전: Room 변경 없음. speaking.json은 assets(읽기 전용), 기존 id 유지
- 녹음 파일: `mock-NN.pcm` 최대 15개(2분이면 각 3.8MB, 합계 약 58MB) — 새 모의고사 때 지움
### Must-fix / Should-fix
- 없음
### Nit
- **N1** 실기기 AC5 사용자 확인 대기 (무선 디버깅 끊김)
- **N2** 콘텐츠 GPT 교차 검토 권장 (`CONTENT-REVIEW.md`)
- **N3** 모의고사 결과는 앱을 나가면 사라짐 → TASK 21 기록 저장에서 함께 저장
- **N4** `.kotlin/`, `.vscode/` 미추적 폴더 → `.gitignore` 추가 권장
