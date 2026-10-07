# TASK: 문법 탭 4 — 틀린 문제 간격 복습 (Room v1 → v2)

> 작성: Claude · 승인: [x] 사용자 (2026-10-07) · 구현: Claude (GPT 토큰 소진)
> 경로: docs/tasks/13-grammar-review/TASK.md
> 선행: TASK 12 Approve (`fa7adc2`)
> 근거: 간격 학습 (Cepeda et al., 2006), 섞어 풀기

## 결정 (2026-10-07 사용자)
- 복습 대상: 단원 연습에서 **2차 정답(힌트 보고 맞힘) + 끝내 오답**. 1차 정답은 넣지 않는다
- 간격: 1단계(다음 날) → 2단계(3일 뒤) → 3단계(7일 뒤) → 3단계에서 1차 정답이면 **졸업**(목록에서 삭제). 어느 단계든 2차 정답·오답이면 1단계·다음 날
- 복습 세션: 오늘까지 밀린 문제 중 오래된 것부터 최대 10개, 단원 섞어서. 푸는 방식은 연습과 같음
- 표시: 홈 문법 카드 "오늘의 복습 N개", 단원 목록 맨 위 "오늘의 복습 N개 →" / 0개면 "오늘 복습 끝!"

## 스키마 변경 (DB v1 → v2)
- 새 테이블 `grammar_reviews`(exerciseId PK, unitId, stage, dueEpochDay, wrongCount, lastStudiedAt)만 추가
- 기존 `words`, `user_words`, `data_meta`는 변경 없음
- `MIGRATION_1_2`: CREATE TABLE 한 문장 (schemas/2.json의 createSql과 동일)
- grammar.json에서 빠진 문제는 기록을 지우지 않고 복습 목록에서만 건너뜀

## 수용 기준
- [ ] AC1: 일정 계산 순수 함수 테스트 (1→3→7일, 졸업, 오답 시 1단계·다음 날)
- [ ] AC2: **Migration 테스트** — 내보낸 v1 스키마로 만든 DB에 단어·학습 기록·dataVersion을 넣고 v2로 열면 모두 그대로이고, 복습 테이블을 쓸 수 있다
- [ ] AC3: DAO 테스트 — 추가, 단계 상승, 오답 시 리셋, 졸업 삭제, 없는 id 무시
- [ ] AC4: ViewModel 테스트 — 연습에서는 2차 정답·오답만 기록, 복습 세션은 최대 10개·모르는 id 건너뜀·1차 정답 여부로 기록
- [ ] AC5: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과
- [ ] AC6: S23+ — v1 DB가 있는 기기에 덮어 설치해도 학습 기록이 유지되고, 연습 오답이 복습 테이블에 "다음 날"로 들어간다
