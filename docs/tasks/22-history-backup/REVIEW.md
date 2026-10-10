# REVIEW: 기록 저장 + 학습 기록 백업·복원

> 작성: Claude · 경로: docs/tasks/22-history-backup/REVIEW.md

## 1차 리뷰 — 2026-10-10
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)
- 데이터 안전 (리뷰 관점 1순위): Migration 있음·테이블 추가만·실기기 덮어 설치로 기존 기록 유지 확인. `REPLACE` 없음. UserWord는 복원 때 (language, word)로 찾아 UPDATE/INSERT, seq·words 테이블 무변경. 백업 전 기기 DB 사본을 scratchpad에 보관
- 권한: 파일 저장·열기는 Storage Access Framework라 새 권한 없음
### Must-fix / Should-fix
- 없음
### Nit
- **N1** 백업 파일 저장·복원 실기기 확인 대기
- **N2** 백업 알림(예: 주 1회 "백업할까요?")은 디자인 개편 때 전체 메뉴에 넣기
