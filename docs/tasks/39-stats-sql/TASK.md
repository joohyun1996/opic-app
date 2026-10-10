# TASK: 통계·Day 집계를 SQL로 (기록이 쌓여도 빠르게)

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "태스크로 잡고 지피티에 넘기자") · 구현: GPT

## 목표
통계와 학습 알림이 모든 기록을 메모리로 읽지 않게 하고, Day 통계 반복 계산을 줄인다. 문법 데이터는 처음 필요할 때 읽는다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/database/...Dao.kt` (UserWord, GrammarReview, Speaking, ShadowingAttempt) | 수정 | 읽기 전용 집계 쿼리 추가: 날짜(ms) 목록 또는 일별 COUNT, 오답 상위 N, 최근 N개 WPM·필러, 평균 일치율. 모두 language 필터 |
| `feature/analysis/.../StatsData.kt` | 수정 | 위 쿼리 사용 (transcript 등 큰 열은 읽지 않음) |
| `feature/words/.../WordsViewModel.kt` 등 | 수정 | dayStats 결과를 학습 기록이 바뀔 때만 다시 계산 (공유 캐시 또는 Flow) |
| `app/.../OpicApplication.kt` | 수정 | grammar.json·grammar-core.json 파싱을 처음 쓸 때로 미루기 (선택 — 효과 측정 후 결정) |
| `core/database/src/test/...`, `feature/analysis/src/test/...` | 생성 | 쿼리 결과가 기존 loadStats 결과와 같은지 |

## 수용 기준
- [x] AC1: 같은 테스트 데이터에서 새 loadStats와 기존 계산 결과(연속일·7일 막대·상위 5개·평균)가 같다
- [x] AC2: 통계 계산이 speaking_answers의 transcript·editedText 열을 읽지 않는다
- [x] AC3: 모든 새 쿼리에 language 조건이 있다
- [x] AC4: 스키마 변경·Migration 없음 (인덱스가 필요하면 멈추고 묻기)
- [x] AC5: `./gradlew test lint :app:assembleRelease` 통과, 통계 화면 실기기 스크린샷

## 범위 밖
- 통계 화면 디자인
