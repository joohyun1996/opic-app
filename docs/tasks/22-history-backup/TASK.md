# TASK: 기록 저장 (스피킹·모의고사·섀도잉) + 학습 기록 백업·복원

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "22번까지 하자") · 구현: Claude
> 경로: docs/tasks/22-history-backup/TASK.md

## 목표
1. 스피킹 답변·모의고사·섀도잉 연습을 저장해 지난 기록과 비교한다 (**Room v2 → v3**)
2. 학습 기록 전체를 파일로 백업하고 복원한다 — 10-07처럼 앱이 재설치돼 기록이 사라지는 일 대비

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/database/.../Entities.kt`, `OpicDatabase.kt`, `Migrations.kt`, `schemas/3.json` | 수정·생성 | `speaking_answers`, `shadowing_attempts` 추가, `MIGRATION_2_3` (테이블 추가만) |
| `core/database/.../SpeakingDao.kt`, `ShadowingAttemptDao.kt` | 생성 | 저장·조회 (모든 쿼리 `language` 필터) |
| `core/database/.../Backup.kt` + 테스트 | 생성 | 백업 JSON 만들기·복원 (한 트랜잭션, REPLACE 금지) |
| `core/database/src/test/.../MigrationTest.kt` | 수정 | v2 → v3 기록 유지 |
| `feature/speaking/**` | 수정 | 답변 저장·고친 글 갱신, 질문 화면 "지난 답변", 결과 화면 지난번과 비교, 기록 화면 |
| `feature/shadowing/**` | 수정 | 연습 저장, "최근 연습한 영상", 영상별 연습 횟수·최고 일치율 |
| `feature/words/.../WordsApp.kt`, `app/.../OpicRoot.kt`, `OpicApplication.kt` | 수정 | 홈 "학습 기록 백업·복원", DAO 연결 |

## 데이터 (v3)
- `speaking_answers(id PK auto, language, questionId, topicId, createdAt, durationMs, transcript, editedText, wordCount, wordsPerMinute, fillerCount, sentenceCount, mockId?)`
- `shadowing_attempts(id PK auto, language, videoId, sentence, heard, matchRate, createdAt)`
- 녹음 파일은 저장하지 않는다 (텍스트·지표만)

## 백업 파일
- `opic-backup-YYYYMMDD.json`: `{backupVersion: 1, exportedAt, userWords:[{language, word, correctCount, wrongCount, lastStudiedAt}], grammarReviews:[…], speakingAnswers:[…], shadowingAttempts:[…]}` — 단어는 id가 아니라 `(language, word)`로 (재설치 후 id가 달라져도 맞게)
- 저장 위치는 사용자가 고른다 (시스템 파일 선택 창, 새 권한 없음). 밖으로 보내지 않는다
- 복원 규칙 (한 트랜잭션): 단어·문법 복습은 `lastStudiedAt`이 더 최근인 쪽을 남김(있으면 UPDATE, 없으면 INSERT), 없는 단어는 건너뜀. 스피킹·섀도잉은 같은 `(createdAt, questionId/videoId)`가 없을 때만 INSERT. 형식이 틀리면 아무것도 바꾸지 않는다

## 수용 기준
- [x] AC1: v2 DB(단어 기록·문법 복습 있음) → v3 열기 → 기록 유지, 새 테이블 사용 가능 (MigrationTest)
- [x] AC2: 백업 → 새 DB(단어만 있음)에 복원 → 단어 기록·복습·답변·섀도잉 같음, 두 번 복원해도 중복 없음, 더 최근 기록은 덮어쓰지 않음, 잘못된 JSON은 변경 없음 (테스트)
- [x] AC3: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, Manifest 변경 없음, REPLACE 없음
- [ ] AC4: 실기기 — v2 앱 위에 덮어 설치해 기존 기록 유지, 백업 파일 만들기, 스피킹 답변이 기록 화면에 남음
