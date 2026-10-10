# TASK: 단어 간격 복습

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10, "1,2,3,4 하고 스페인어 가자")

## 목표
틀린 단어가 오답노트에 쌓이기만 하지 않고, 1·3·7일 뒤 다시 나오게 한다. 홈 "오늘 할 일"에 단어 복습을 보여 준다.

## 한 일
- 규칙(`core/common/Review.kt` `isWordReviewDue`): 틀린 적 있고 습득(정답 3회) 전인 단어가, **맞은 횟수 0·1·2 → 마지막 학습 후 1·3·7일**이 지나면 복습 대상이다
  - 맞히면 맞은 횟수가 늘어 간격이 길어지고, 3회면 습득으로 빠진다. 틀리면 같은 간격으로 다시 나온다
  - 문법 복습과 같은 간격 표(`REVIEW_INTERVAL_DAYS`)를 쓴다
- **스키마 변경 없음**: 기존 `correctCount`·`lastStudiedAt`만 쓴다. 조회용 POJO `ReviewCandidate`와 쿼리 `reviewCandidates` (language 필터)
- 복습 세션: `StudySource(review = true)`, 경로 `study/review/{mode}`, 오래된 순으로 최대 20개, 영→한
- 홈: "단어 N개 · 문법 M문제 복습" + 단어/문법 복습 버튼 각각 (기록이 바뀌면 다시 셈)

## 수용 기준
- [x] AC1: 맞은 횟수별 간격 1·3·7일, 습득 단어는 대상 아님 (`ReviewTest.wordReviewFollowsCorrectCountIntervals`)
- [x] AC2: 후보는 그 언어의 미습득 오답 단어만, 오래된 순 (`StatsQueriesTest.reviewCandidatesAreUnmasteredWrongWordsOfLanguage`)
- [x] AC3: `./gradlew test lint :app:assembleRelease` 통과
- [ ] AC4: 실기기에서 단어를 틀린 다음 날 홈에 "단어 N개 복습"이 나오고 복습 세션이 열린다 — 하루가 지나야 확인 가능 (지금은 기기 기록 초기화로 오답 없음, 홈은 기존 문구 그대로인 것 확인)
