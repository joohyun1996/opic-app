# HANDOFF: 통계·Day 집계를 SQL로

> 작성: Claude (GPT 사용량 부족으로 대신 구현) · base: `8a41627`

## 변경
| 파일 | 내용 |
|------|------|
| `core/database/UserWordDao.kt` | `studiedAt`, `masteredCount`, `mostWrong` (words JOIN으로 language 필터) |
| `core/database/GrammarReviewDao.kt` | `studiedAt`, `mostWrongUnits`(GROUP BY unitId), `dueCount` |
| `core/database/SpeakingDao.kt` | `createdAt`, `recentPace`(WPM·필러만) |
| `core/database/ShadowingAttemptDao.kt` | `createdAt`, `averageMatch` |
| `core/database/WordDao.kt` | `KeyCount`, `PaceRow`, `observeDayStats`(Flow), `activeCount` |
| `feature/analysis/StatsData.kt` | 위 쿼리로 다시 작성. 날짜 변환(시간대)만 Kotlin |
| `feature/words/WordsViewModel.kt` | `WordStatsCache`: 언어별 Day 통계 Flow 하나를 홈·메뉴·단어 탭이 공유, Room이 words·user_words 변경 때만 다시 계산. `refresh()` 제거 |
| `app/OpicRoot.kt`, `MenuScreen.kt`, `feature/words/WordsApp.kt` | `refresh()` 호출 제거 |
| `core/database/src/test/.../StatsQueriesTest.kt` | 신규 |

## 수용 기준
- [x] AC1: 각 집계 쿼리 = 전체 행을 읽어 Kotlin으로 센 값 (`StatsQueriesTest`). 연속일·7일 막대는 같은 순수 함수(`studyStreak`, `recentDailyCounts`)에 같은 시각 목록을 넣으므로 결과가 같다
- [x] AC2: 통계는 speaking_answers에서 createdAt·wordsPerMinute·fillerCount만 읽는다
- [x] AC3: 모든 새 쿼리에 language 조건 (테스트가 es 행이 섞이지 않는지 확인)
- [x] AC4: 스키마 변경 없음 (인덱스 추가 없음, 엔티티 그대로)
- [x] AC5: `./gradlew test lint :app:assembleRelease` 통과, 실기기 홈·단어·통계 확인 (`stats.png`)

## 판단
- 문법 데이터 지연 로딩(선택 항목)은 하지 않음: 홈 "오늘 할 일"이 시작하자마자 복습 수를 쓰므로 효과가 없다
- Day 통계 Flow는 앱 프로세스 동안 유지 (`SharingStarted.Eagerly`). 언어당 하나라 메모리 부담이 작다
- 미확인: 실기기에서 단어 학습 직후 Day 숫자 갱신 (Flow 갱신은 테스트로 확인)
