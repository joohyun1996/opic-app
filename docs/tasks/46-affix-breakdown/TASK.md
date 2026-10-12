# TASK: 영어 단어 접사 분해

> 작성: Claude · 승인: [x] 사용자 (2026-10-12, DB 변경 승인)
> 경로: docs/tasks/46-affix-breakdown/TASK.md
> 전제: TASK 45(수능 단어) 적재 후 진행. 대상은 기존 단어 + TASK 45 단어

## 목표
단어를 통째로 외우지 않고 구조로 이해하게 한다.
- 예: dissimilar = dis(아닌) + similar(비슷한) → "비슷하지 않은"
- 전부 할 필요는 없고 **확실한 단어만** 한다

## 핵심 규칙: 확실한 것만
- 접사를 뗀 나머지(어간)가 **같은 언어의 단어 목록(`exports/words.json`)에 있을 때만** 후보로 본다
  - uncle = un + cle 같은 잘못된 분해를 막는다
- 형태는 맞아도 뜻이 이어지지 않는 단어(discover, resign, dispatch 등)는 검수에서 뺀다
- 어원(라틴어 뿌리) 추측 금지. 앱이 실행 중에 Gemma로 만들지 않고, 검수한 결과를 씨드 파일로 고정한다

## 1단계: 후보 추출 + 리포트 (여기서 멈추고 보고)
- 접사 사전 `exports/source/affixes/affixes.tsv`: 뜻이 분명한 접두사·접미사 40~60개
  - 각 항목: form, type(prefix/suffix), 한국어 뜻, 예시
  - 예: dis-, un-, in-/im-/ir-/il-, re-, pre-, mis-, over-, under-, non-, anti-, inter-, sub-, -able/-ible, -less, -ness, -ment, -ful, -ly, -er/-or, -ist, -ize, -ion/-tion, -ity, -ous, -ive, -al, -ship, -hood
- 추출 스크립트 `tools/words/extract_affixes.py`
  - 접사를 떼고 어간이 단어 목록에 있는지 확인한다
  - 철자 변형 규칙을 포함한다: y→i (happy→happiness), -e 탈락 (use→usable), 자음 겹침 (begin→beginner), -le→-ly, in-의 동화형 (im-/ir-/il-)
  - 한 단어에 후보가 여러 개면 모두 적고 검수에서 고른다
- 결과 `docs/tasks/46-affix-breakdown/REPORT-1.md`
  - 후보 개수, 접사별 분포
  - 검수용 목록 `review.tsv`: word, 분해, 어간, 접사, 제안 뜻풀이, transparent(빈칸 — 검수자가 true/false)
- **사용자가 검수(또는 검수 방식 결정)한 뒤 2단계로 간다**


### 1단계 검수 결과 (2026-10-12 Claude 표본 확인)
- 전수 판정: true 426 / false 235 / hold 91. 단어당 true 1개 이하 확인
- true 표본 54개 확인, 함정(cavity·cubic·gallant·infraction·undermine)은 false로 정리됨 → 통과
- 2단계에는 **true 426개만** 넣는다. hold·false는 넣지 않는다

## 2단계: 검수 반영 + DB + 화면
- 씨드 `exports/affixes.json` (dataVersion 포함): `transparent = true`인 것만 넣는다
  - 단어는 id가 아니라 `(language, word)`로 적는다. 적재할 때 id로 바꾼다
- DB (제안 — 실제 구조에 맞게 조정 가능)
  - `affixes(id, language, form, type, meaningKo)`, 유일 키 `(language, form, type)`
  - `word_parts(id, language, wordId, position, affixId?, baseWordId?, glossKo)`
    - 한 단어를 순서대로 나눈 조각. 각 조각은 접사이거나 어간 단어다
    - `glossKo`는 한 줄 뜻풀이 ("비슷하지 않은"), 단어 첫 조각에만 둔다
  - Migration (TASK 45의 v5 다음 버전), 기존 테이블은 건드리지 않음
  - 적재: dataVersion이 오르면 `affixes`·`word_parts`만 한 트랜잭션에서 다시 맞춘다. 단어 테이블과 UserWord는 건드리지 않는다
  - 키로 찾아 UPDATE/INSERT, REPLACE 금지
- 화면
  - 단어 학습에서 **정답/오답을 확인한 뒤에만** 한 줄 표시: `dis(아닌) + similar(비슷한) → 비슷하지 않은`
  - **문제를 푸는 동안(확인 전)에는 표시하지 않는다** (힌트가 되므로). 힌트 보기에도 넣지 않는다
  - 어간이나 접사를 누르면 같은 어간 또는 같은 접사를 가진 단어 목록 화면을 보여 준다
  - Day 단어 목록·오답노트처럼 시험이 아닌 화면에는 표시해도 된다 (HANDOFF에 적기)

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/source/affixes/affixes.tsv` | 생성 | 접사 사전 |
| `tools/words/extract_affixes.py` | 생성 | 1단계 추출 스크립트 |
| `docs/tasks/46-affix-breakdown/REPORT-1.md`, `review.tsv` | 생성 | 1단계 결과 |
| `exports/affixes.json` | 생성 | 2단계 씨드 (검수 통과분만) |
| `app/build.gradle.kts`, `app/.../OpicApplication.kt` | 수정 | 씨드 내장·적재 |
| `core/database/.../Entities.kt`, `Migrations.kt`, `OpicDatabase.kt`, `schemas/...` | 수정·생성 | 새 엔티티 2개, Migration (2026-10-12 승인) |
| `core/database/.../AffixDao.kt`, `AffixImporter.kt` | 생성 | 조회(language 필터)·적재 |
| `core/common/.../Affix.kt` | 생성 | 한 줄 표시 문자열 만들기 등 순수 함수 |
| `feature/words/.../StudyScreen.kt`, `StudyViewModel.kt` | 수정 | 확인 후에만 분해 표시 |
| `feature/words/.../AffixWordsScreen.kt` | 생성 | 같은 어간·접사 단어 목록 |
| `app/.../OpicRoot.kt` | 수정 | 목록 화면 경로 |
| 관련 테스트 | 생성 | |

## 수용 기준
- [ ] AC1: 추출 스크립트는 어간이 단어 목록에 없으면 후보에 넣지 않는다 (예: uncle, discover처럼 어간이 목록에 없는 단어 — 스크립트 테스트 또는 리포트 예시로 확인)
- [ ] AC2: 철자 변형 규칙 예시(happiness→happy, usable→use, impossible→possible)가 후보로 잡힌다
- [ ] AC3: 씨드에는 `transparent = true`인 항목만 있고, 검수 미통과 단어는 앱에서 분해 정보가 나오지 않는다 (테스트)
- [ ] AC4: 학습 화면에서 정답/오답 확인 전에는 분해 정보가 보이지 않고, 확인 후에만 보인다 (ViewModel 상태 테스트 + 실기기)
- [ ] AC5: 접사·어간을 누르면 같은 접사·어간 단어 목록이 language 기준으로 나온다 (DAO 테스트)
- [ ] AC6: Migration 후 기존 단어·학습 기록이 그대로다. 분해 데이터를 다시 적재해도 UserWord는 바뀌지 않는다 (`MigrationTest`)
- [ ] AC7: 모든 쿼리에 language 조건, `OnConflictStrategy.REPLACE` 미사용
- [ ] AC8: `./gradlew test lint :app:assembleRelease` 통과

## 범위 밖
- 어원(라틴어·그리스어 뿌리) 설명, 스페인어·중국어 적용, 런타임 LLM 생성
