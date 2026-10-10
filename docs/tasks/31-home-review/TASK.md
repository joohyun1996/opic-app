# TASK: 홈 "오늘 할 일"에 실전 영문법 복습 포함

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10)
> 경로: docs/tasks/31-home-review/TASK.md

## 목표
틀린 실전 영문법 문제도 간격 복습에 들어가 홈의 "오늘 할 일"에 나오게 한다. 복습 화면은 OPIc 문법·실전 영문법 문제를 함께 낸다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `feature/grammar/...` (복습 ViewModel·화면) | 수정 | 복습 대상에 core 문제 포함, 문제마다 출처 장 표시 |
| `app/.../HomeScreen.kt` | 수정 | 오늘 할 일 문구에 문법 복습 개수(합계) |
| `feature/grammar/src/test/...` | 생성/수정 | |

## 관련 파일 (읽기만)
- `core/database/.../GrammarReviewDao.kt`, `Entities.kt` (GrammarReviewEntity: exerciseId, unitId)

## 요구사항
- 먼저 현재 동작을 확인해 HANDOFF에 적는다: 실전 문제를 틀리면 grammar_reviews에 저장되는지, 복습 목록에 나오는지
- 복습 문제는 `mergeBooks` 결과에서 exerciseId로 찾는다. 찾지 못한 id(콘텐츠에서 지워진 문제)는 건너뛴다
- 복습 화면의 문제 위에 "실전 4장 시제", "OPIc 3단원"처럼 출처를 표시한다
- 홈: "영문법 복습 N문제". 0이면 기존 문구를 유지한다

## 수용 기준
- [ ] AC1: 실전 문제를 틀리면 복습 기록이 생기고, 마감일이 되면 복습 목록에 포함된다
- [x] AC2: 콘텐츠에 없는 exerciseId는 복습 목록에서 빠지고 오류가 나지 않는다
- [x] AC3: 홈 개수는 OPIc와 실전 문제의 합이다
- [ ] AC4: `./gradlew test lint :app:assembleRelease` 통과, 실기기에서 실전 문제 1개를 틀린 뒤 복습에 나오는지 확인

## 제약 / 주의
- 스키마 변경 금지. exerciseId가 c로 시작하면 실전 문제다 (id 규칙)

## 범위 밖
- 복습 알고리즘(간격) 변경

## 구현 메모
- 현재 동작 확인: 앱은 OPIc + 실전을 `mergeBooks`로 합친 책 하나로 복습을 계산 → 실전 문제를 틀리면 이미 grammar_reviews에 저장되고 홈 개수에도 합산되고 있었다 (AC1·AC3 코드상 충족)
- 콘텐츠에 없는 id는 `dueExercises`가 건너뜀 (기존, AC2)
- 추가: 복습 문제 위 출처 표시 `GrammarBook.sourceLabels()` ("실전 4장 시제 12개 한눈에" / "OPIc 1단원 …"), 홈 "복습 시작" → 문법 탭에서 바로 오늘의 복습 시작
- AC1·AC4 실기기: 복습은 틀린 다음 날부터 나오므로 내일 확인 필요
