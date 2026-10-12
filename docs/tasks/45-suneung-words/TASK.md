# TASK: 수능 수준(고2~고3) 영어 단어 추가

> 작성: Claude · 승인: [x] 사용자 (2026-10-12, B안 · 초·중 어휘 제외 · DB 변경 승인)
> 경로: docs/tasks/45-suneung-words/TASK.md
> 순서: 이 TASK → TASK 46 접사 분해 (단어를 먼저 넣어야 접사 후보 추출을 한 번만 돌린다)

## 목표
기존 5,517개(COCA·GRE·AWL 기반)에 빠진 고2~고3 수준 단어를 채운다. 너무 쉽지도, 전부 맞힐 수 있지도 않은 수준이 목표다.

## 저장소에서 확인한 사실 (설계 전제)
- `exports/words.json`: `dataVersion` 1, 5,517개, seq 1~5517
- **`category` 필드는 지금 품사**(noun 2,688 / adjective 1,397 / verb 1,295 …)로 쓰이고 있다
  - 그래서 "수능" 구분용으로 쓸 수 없다. 새 필드가 필요하다 (아래 2단계)
- **Day 138은 37개뿐이다** (5517 = 137×40 + 37)
  - 새 단어를 seq 5518부터 붙이면 Day 138에 기존 단어 37개와 수능 단어 3개가 섞인다
  - 그래서 수능 단어는 **seq 5521(= Day 139 첫 칸)부터** 시작한다. Day 계산 `(seq-1)/40+1`은 빈 seq(5518~5520)가 있어도 그대로 동작한다
- 단어 적재(`WordImporter`)는 `(language, 소문자 word)`로 찾아 기존 행은 UPDATE, 새 행은 INSERT한다
  - 새 단어 추가만으로는 기존 행과 학습 기록(UserWord)이 바뀌지 않는다

## 출처
- **교육부 고시 영어과 교육과정 기본 어휘** (공개 자료). 사용한 고시 번호·연도·파일명을 리포트에 적는다
- 시판 수능 단어장은 저작권 문제로 사용 금지
- 기본 어휘 중 초·중학 수준(초·중 필수) 단어는 **모두 제외**한다 (2026-10-12 사용자 확인: 초·중 어휘는 필요 없음)
  - 고시 목록이 초등 어휘만 표시하고 중학 수준은 따로 표시하지 않으면, 1단계 리포트에 **제외 기준 후보를 제안하고 사용자 결정을 받는다**
  - 후보 예: 2022 개정 교육과정 학교급 구분 / 기존 `level` / 빈도 순위

## 1단계: 중복 제거 + 리포트 (여기서 멈추고 보고)
- 출처 단어 목록을 `exports/source/curriculum/`에 원본 그대로 저장한다 (출처·고시 정보 포함)
- 비교 스크립트를 `tools/words/compare_curriculum.py`로 만든다
  - 출처 단어와 `exports/words.json`의 영어 단어를 소문자·앞뒤 공백 제거 기준으로 비교한다
  - 구·숙어(띄어쓰기 포함)와 변화형(복수·과거형)은 따로 집계하고 신규 후보에서 뺀다
- 결과 `docs/tasks/45-suneung-words/REPORT-1.md`
  - 출처 총 개수, 초·중학 제외 개수, 기존과 중복 개수, 신규 후보 개수
  - 신규 후보 목록 파일 `docs/tasks/45-suneung-words/candidates.tsv` (단어, 출처 표기, 비고)
- **사용자가 추가할 개수와 진행 여부를 정한 뒤 2단계로 간다**


### 2단계 결정 (2026-10-12 사용자 승인)
- 1단계 결과: 원본 3,001 → 초·중 2,001 제외 → 기존 중복 256 → **신규 후보 744개 전부 반영**
- seq 5521~6264 → Day 139~157 (Day 157은 24개)
- 뜻·예문은 새로 작성한다. 시판 단어장 문구를 옮기지 않는다. 확실하지 않은 뜻은 앞에 `*`
- ~~괄호 표기는 표제어만 넣는다~~ → (REVIEW 1차로 정정) 표제어가 학습용 단독 단어가 아니면(`environ (environment)`) 괄호 안 단어를 넣는다

## 2단계: 씨드 반영 + 화면
- 사용자가 승인한 후보만 넣는다. 각 단어마다 다음을 작성한다
  - 발음기호, 품사(`partOfSpeech`와 지금 쓰임인 `category`), 영어 뜻, 한국어 뜻, 예문·번역
  - 뜻이 불확실하면 `meaningKo` 앞에 `*` (기존 규칙)
- 단어장 구분 필드 `deck` 추가
  - 기존 단어는 `"core"`, 새 단어는 `"suneung"`
  - DB: `words`에 `deck TEXT NOT NULL DEFAULT 'core'` 열 추가, Migration v4→v5 (ALTER TABLE ADD COLUMN, 기존 행 보존)
  - `words.json`의 `dataVersion`을 2로 올린다 (기존 규칙)
- seq: 5521부터 1씩 증가 (Day 139부터 수능 단어만)
- 단어 탭 표시 방식: **B안으로 결정** (2026-10-12 사용자: "어차피 둘 다 봐야 해")
  - ~~A안: 단어장 전환 + 수능 Day 재번호~~ (채택 안 함)
  - **B안**: 지금 Day 목록 그대로, Day 139부터 칸에 "수능" 표시만 붙인다

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/source/curriculum/*` | 생성 | 출처 원본 |
| `tools/words/compare_curriculum.py` | 생성 | 1단계 비교 스크립트 |
| `docs/tasks/45-suneung-words/REPORT-1.md`, `candidates.tsv` | 생성 | 1단계 결과 |
| `exports/words.json` | 수정 | 2단계: 새 단어 추가, dataVersion 2, `deck` 필드 |
| `core/database/.../Entities.kt` | 수정 | `WordEntity.deck` (엔티티 변경 — 2026-10-12 승인) |
| `core/database/.../Migrations.kt`, `OpicDatabase.kt`, `schemas/.../5.json` | 수정·생성 | v4→v5 Migration (2026-10-12 승인) |
| `core/database/.../WordImporter.kt`, `WordDao.kt` | 수정 | `deck` 읽기·쓰기, 단어장별 Day 통계 쿼리 (language 필터 유지) |
| `feature/words/*` (WordsApp, DayScreen 등) | 수정 | Day 칸·Day 화면에 "수능" 표시 (B안) |
| `core/database/src/test/...`, `core/common/src/test/...` | 생성·수정 | 아래 수용 기준 테스트 |

## 수용 기준
- [ ] AC1: 1단계 리포트에 출처·총 개수·제외·중복·신규 후보 개수가 있고 후보 목록 파일이 있다
- [ ] AC2: 추가 전후로 기존 5,517개 단어의 `seq`와 Day 배정이 모두 같다 (테스트: 추가 전 (word, seq, day) 목록 = 추가 후 같은 단어들의 목록)
- [ ] AC3: 같은 단어가 두 번 들어가지 않는다 (language + 소문자 word 기준, 기존 단어와 겹치는 후보는 적재 전 테스트에서 실패)
- [ ] AC4: v4 DB에 학습 기록(정답·오답 수, 마지막 학습)이 있는 상태에서 v5로 Migration하고 dataVersion 2를 적재해도 기록이 그대로다 (`MigrationTest`, `WordImporterTest`)
- [ ] AC5: 새 단어는 모두 seq ≥ 5521, `deck = "suneung"`, Day 138에는 기존 단어만 있다 (테스트)
- [ ] AC6: 모든 새 쿼리에 language 조건이 있고, `OnConflictStrategy.REPLACE`를 쓰지 않는다
- [ ] AC7: `./gradlew test lint :app:assembleRelease` 통과, 실기기에서 기존 Day 1 기록과 수능 단어장 표시 확인 (플래시카드는 Day당 5장만)

## 범위 밖
- 스페인어·중국어 단어, 접사 분해(TASK 46), 어원 설명
