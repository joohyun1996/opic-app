# HANDOFF: 문법 탭 4 — 틀린 문제 간격 복습 (Room v1 → v2)

> 작성: Claude (구현 대행) · 경로: docs/tasks/13-grammar-review/HANDOFF.md

## 커밋 범위
- base: `fa7adc2`
- head: 이 커밋 하나

## 변경 요약
```diff
+ core/common/Review.kt (+Test)              — scheduleAfterPractice / scheduleAfterReview, 간격 1·3·7일, 세션 10개
+ core/database/Entities.kt                 — GrammarReviewEntity (grammar_reviews)
+ core/database/GrammarReviewDao.kt         — get, due(today), recordPractice, recordReview (졸업 시 delete). REPLACE 없음
+ core/database/Migrations.kt               — MIGRATION_1_2 (CREATE TABLE 한 문장), ALL_MIGRATIONS
~ core/database/OpicDatabase.kt             — version 2, grammarReviewDao()
+ core/database/schemas/…/2.json            — Room이 생성한 v2 스키마
+ core/database/src/test/MigrationTest.kt   — v1 파일 DB → v2 마이그레이션, DAO 수명 주기
~ core/database/build.gradle.kts            — 테스트에 schema.v1 경로 전달
+ feature/grammar/GrammarReviewStore.kt     — 저장소 인터페이스 + Room 구현(오늘 = LocalDate.now().toEpochDay()), dueExercises
~ feature/grammar/GrammarViewModel.kt       — refreshDue, startReview, 문제 종료 시 기록, reviewMode
~ feature/grammar/GrammarFlow.kt, GrammarUnitListScreen.kt, GrammarResultScreen.kt — 오늘의 복습 버튼, 복습 결과 화면
+ feature/grammar/src/test/GrammarReviewViewModelTest.kt (가짜 저장소)
~ feature/grammar/build.gradle.kts          — :core:database, coroutines-test(테스트)
~ app/OpicApplication.kt                    — addMigrations(*ALL_MIGRATIONS), grammarReviews
~ app/OpicRoot.kt, feature/words/WordsApp.kt — 홈 문법 카드에 "오늘의 복습 N개"
```

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1 | 통과 | `ReviewTest` 3개 |
| AC2 | 통과 | `MigrationTest.v1ToV2KeepsWordProgressAndAddsReviewTable` — v1 스키마 JSON으로 만든 실제 파일 DB, Room 스키마 검증 통과 |
| AC3 | 통과 | `MigrationTest.reviewLifecycle` |
| AC4 | 통과 | `GrammarReviewViewModelTest` 3개 |
| AC5 | 통과 | exit 0, lint 새 경고 없음 |
| AC6 | 통과 | 아래 |

## 실기기 (S23+)
- 기존 v1 DB 위에 덮어 설치: 홈 "오답 13개" 그대로, 크래시 없음. 기기 DB를 꺼내 확인 — `PRAGMA user_version = 2`, identity hash `feef44f0…` (2.json과 일치), `user_words` 13행 유지
- 단원 1에서 문제 3개: 끝내 오답 2개(u1-04, u1-02) → `grammar_reviews`에 stage 1, `dueEpochDay = 20734`(오늘 20733 + 1)
- 단원 목록: "오늘 복습 끝!" (다음 날부터 나옴) (`list-after.png`)
- **복습 세션 화면은 실기기에서 보지 못했다** — 기록이 다음 날부터 열리기 때문. ViewModel 테스트로 확인했고, 내일 앱을 열면 "오늘의 복습 2개"가 보여야 한다

## 리뷰어가 봐야 할 곳
- "오늘"은 기기 시간대의 날짜(`LocalDate.now()`)다. 자정 직후 복습하면 날짜가 바뀐 것으로 본다
- 같은 문제가 연습에서 다시 틀리면 기존 복습 행을 1단계로 되돌린다 (새 행을 만들지 않음)
