# TASK: 언어 확장 가능한 DB (v4)

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10, "언어 디비는 확장 가능하게, 수정에 닫히고 확장에 열려야")
> 경로: docs/tasks/35-db-language/TASK.md

## 목표
영어 → 스페인어 → 중국어를 추가할 때 기존 코드·데이터를 고치지 않고 언어 목록에 한 줄만 더하면 되게 한다.

## 한 일
- `core/common/Language.kt`: `StudyLanguage(code, label, exam)`, `StudyLanguages.all` — 언어 추가는 여기 한 줄
- DB v4: `grammar_reviews`에 `language` 추가, 키 `(language, exerciseId)` (언어마다 같은 문제 id가 있어도 안 겹침)
  - `MIGRATION_3_4`: 새 테이블 복사 → 기존 삭제 → 이름 변경, 기존 행은 모두 `'en'`. REPLACE 사용 없음
- `GrammarReviewDao` 모든 쿼리에 `language` 필수 인자. `RoomGrammarReviewStore(dao, language = "en")`
- 백업: `BackupGrammarReview.language`(옛 백업 파일은 기본값 "en"), `BackupManager` 기본 언어 목록 = `StudyLanguages.codes`
- 이제 모든 학습 기록 테이블이 언어를 구분: words(language), user_words(Word 경유), grammar_reviews, speaking_answers, shadowing_attempts

## 수용 기준
- [x] AC1: v3 DB의 문법 복습 행이 v4에서 `language = "en"`으로 그대로 남는다 (`MigrationTest.v3ToV4…`)
- [x] AC2: 같은 exerciseId라도 언어가 다르면 별도 행이다 (같은 테스트)
- [x] AC3: 단어 기록이 마이그레이션 후에도 유지된다
- [x] AC4: `:core:database`, `:core:common`, `:feature:grammar` 테스트 통과
