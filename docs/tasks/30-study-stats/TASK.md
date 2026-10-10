# TASK: 학습 통계 화면

> 작성: Claude · 승인: [ ] 사용자
> 경로: docs/tasks/30-study-stats/TASK.md

## 목표
전체 메뉴에 "학습 통계"를 추가해 단어·문법·스피킹·섀도잉 기록을 한 화면에서 본다. SPEC의 빈 "분석" 섹션을 채운다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/common/.../Stats.kt` | 생성 | 집계 순수 함수 (최근 7일 날짜별 학습 수, 연속 학습일) |
| `core/common/src/test/.../StatsTest.kt` | 생성 | |
| `core/database/...Dao.kt` (UserWord, GrammarReview, Speaking, ShadowingAttempt) | 수정 | 읽기 전용 집계 쿼리 추가 (모두 `language` 필터) |
| `feature/analysis/` | 생성 | 모듈 + `StatsScreen.kt`, `StatsViewModel.kt` |
| `settings.gradle.kts`, `app/build.gradle.kts` | 수정 | 모듈 추가 |
| `app/.../MenuScreen.kt`, `OpicRoot.kt` | 수정 | 메뉴 항목·경로 |

## 요구사항
- 맨 위 요약: 연속 학습일, 최근 7일 막대(그날 단어+문법+스피킹+섀도잉 횟수)
- 단어: 습득 수 / 전체, 오답 많은 단어 5개
- 문법: 장별 오답 수 상위 5개(unitId → 장 제목), 복습 대기 수
- 스피킹: 답변 수, 최근 10개 답변의 평균 WPM·필러 수 추이
- 섀도잉: 연습 횟수, 평균 일치율
- 차트는 Compose Canvas로 직접 그린다 (라이브러리 추가 금지)

## 수용 기준
- [ ] AC1: 날짜별 기록 목록을 주면 최근 7일 일별 합계를 오늘 기준으로 반환한다 (빈 날은 0)
- [ ] AC2: 오늘·어제·그제 기록이 있고 그 전날이 비면 연속 학습일 3을 반환한다. 오늘 기록이 없으면 어제까지로 센다
- [ ] AC3: 모든 새 쿼리는 `language`로 필터링한다. 단, `grammar_reviews`·`user_words`는 language 컬럼이 없으므로 join이나 id 규칙으로 처리하고 HANDOFF에 적는다
- [ ] AC4: 기록이 하나도 없으면 각 칸에 "아직 기록이 없어요"를 보여 주고 죽지 않는다
- [ ] AC5: `./gradlew test lint :app:assembleRelease` 통과, 실기기 스크린샷

## 제약 / 주의
- 스키마 변경·Migration 금지 (읽기 쿼리만). 필요하면 멈추고 묻는다
- 로컬 날짜(기기 시간대) 기준

## 범위 밖
- 기록 내보내기, 목표 설정
