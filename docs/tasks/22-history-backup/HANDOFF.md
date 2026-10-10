# HANDOFF: 기록 저장 + 학습 기록 백업·복원

> 작성: Claude (사용자 요청으로 Claude 구현, 2026-10-10) · 경로: docs/tasks/22-history-backup/HANDOFF.md

## 커밋 범위
- base: `9c3a53c` · head: `e7d520c` · 리뷰 명령: `git diff 9c3a53c..e7d520c`

## 수용 기준 체크
| AC | 결과 | 검증 |
|----|------|------|
| AC1 | ✅ | `MigrationTest.v2ToV3KeepsProgressAndReviewsAndAddsHistoryTables` (2.json으로 만든 실제 v2 DB). **실기기**: v2 앱 위 덮어 설치 → `user_version` 3, 문법 복습 3개(u1-03·04·05) 그대로, 새 테이블 2개 |
| AC2 | ✅ | `BackupTest.exportThenRestoreIntoFreshDatabase`(내보내기 → 새 DB 복원, 두 번 복원해도 0개, 더 최근 기록 유지), `unknownWordsAreSkippedAndBadJsonChangesNothing` |
| AC3 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, Manifest 변경 없음, `REPLACE` 없음 |
| AC4 | 일부 | 덮어 설치·기록 유지·백업 창 표시 확인. 백업 파일 저장(시스템 창에서 위치 선택)·스피킹 기록 화면은 사용자 확인 |

## 설계 판단
- **Migration**: 3.json의 createSql을 그대로 옮김 (테이블 2 + 인덱스 3). 기존 테이블 무변경
- **`grammar_reviews`에는 language 열이 없다** (v2, 문법은 영어만) → 백업용 `all()`은 language 필터 없음. 새 테이블은 language 열·필터 있음
- **백업의 단어 키**: id 대신 `(language, word)` — 재설치하면 words.json 적재 순서에 따라 id가 달라질 수 있어서
- **복원 병합**: 단어·문법 복습은 `lastStudiedAt`이 더 최근일 때만 덮어씀(UPDATE), 없으면 INSERT. 답변·섀도잉은 `(language, createdAt, questionId/videoId)`가 같으면 건너뜀 → 두 번 복원해도 그대로
- **스피킹 저장 시점**: 받아 적기 성공 때 INSERT, 단어를 고칠 때마다 UPDATE(고친 글·지표). 모의고사는 끝내기 뒤 받아 적힌 답변마다 같은 `mockId`로 INSERT
- **녹음 파일은 저장·백업하지 않음** (용량, 개인 음성)

## 사용자 확인 순서
1. 홈 맨 아래 "학습 기록 백업·복원" → "백업 파일 만들기" → 저장 위치(예: 다운로드) 고르기 → "백업 파일을 만들었어요"
2. 스피킹 답변 하나 → 주제 목록 "내 기록"에 나오는지, 같은 질문을 다시 답하면 "지난번과 비교" 카드
3. 섀도잉 따라 말하기 → 추천 목록 맨 위 "최근 연습한 영상"·"연습 1회 · 최고 N%"
4. 내일(10-11) 영문법 "오늘의 복습 3개" (u1-03·04·05)
