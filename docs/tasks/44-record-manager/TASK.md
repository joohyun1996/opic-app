# TASK: 학습 기록 관리 — 목록에서 골라 지우기

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10, "리스트로 뜨고 거기서 선택해서 지우는게 나을 것 같은데? 항목마다")
> AGENTS.md "UserWord 손실 금지"의 예외: **사용자가 직접 골라 확인하고 지우는 기능**은 이번에 허락받음

## 한 일
- 전체 메뉴 → 데이터 → "학습 기록 관리" → 탭 단어 / 문법 / 스피킹 / 섀도잉
- 탭마다 기록 목록 (최근 순, LazyColumn), 체크로 선택, 전체 선택·해제, 삭제 → 확인 창 ("되돌릴 수 없어요, 먼저 백업")
  - 단어: 단어·뜻·맞음/틀림·마지막 학습. 지우면 맞음·틀림 기록만 사라져 새 단어로 돌아감 (**단어장 words는 그대로**)
  - 문법: 장 이름·문제 id·틀림·다음 복습일
  - 스피킹: 질문·날짜·길이·답변 앞부분·모의고사 표시
  - 섀도잉: 영상 제목·날짜·일치율·문장
- `core/database/RecordDeletion.kt` `deleteRecords(db, language, kind, keys)`: 한 트랜잭션, 그 언어 행만, 500개씩 나눠 삭제
- DAO: `UserWordDao.records/deleteRecords`, `GrammarReviewDao/SpeakingDao/ShadowingAttemptDao.deleteRecords` (모두 language 조건)

## 수용 기준
- [x] AC1: 고른 행만, 그 언어만 지워지고 단어 데이터는 남는다. 1,201개도 한 번에 지워진다 (`RecordDeletionTest`)
- [x] AC2: 실기기에서 스피킹 탭 목록·전체 선택·확인 창 (`confirm.png`, 취소해서 실제 삭제는 하지 않음)
- [x] AC3: `./gradlew test lint :app:assembleRelease` 통과
