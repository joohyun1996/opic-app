# REVIEW: 문법 탭 4 — 틀린 문제 간격 복습

> 작성: Claude · 경로: docs/tasks/13-grammar-review/REVIEW.md

## 1차 리뷰 — 2026-10-07
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)

### 데이터 안전 (CLAUDE.md 리뷰 관점 1순위)
- Migration은 테이블 추가만 하고 기존 테이블을 건드리지 않는다. 실제 v1 DB 파일로 만든 테스트와 실기기 덮어 설치 둘 다 통과
- `fallbackToDestructiveMigration`을 쓰지 않는다 → 마이그레이션 누락 시 데이터를 지우지 않고 앱이 실패한다 (의도된 동작)
- `REPLACE` 없음, 졸업은 명시적 delete

### Must-fix / Should-fix
- 없음

### Nit
- **N1** 복습 세션을 실기기에서 보지 못했다. 내일 사용자가 "오늘의 복습 2개"를 한 번 열어 보면 된다
- **N2** 단원 목록·홈의 복습 수는 화면에 들어올 때 다시 센다. 자정을 넘겨 앱을 켜 둔 채면 다시 들어와야 갱신된다
