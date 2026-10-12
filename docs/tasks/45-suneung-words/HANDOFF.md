# HANDOFF: 수능 수준 영어 어휘 744개 추가

> 작성: GPT · 경로: `docs/tasks/45-suneung-words/HANDOFF.md`

## 커밋 범위

- base: `2e51df4` (2단계 시작 전)
- 구현 head: `f2ffada` (HANDOFF는 후속 문서 커밋)
- 리뷰 명령: `git diff 2e51df4..HEAD`

## 변경 요약

| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `exports/words.json` | 수정 | 기존 5,517개에 `deck: core`, 신규 744개에 `deck: suneung`을 넣고 `dataVersion` 2로 변경 |
| `core/database/src/main/java/com/jooh/opic/core/database/{Entities,Migrations,OpicDatabase}.kt`, `schemas/.../5.json` | 수정·생성 | v4→v5에서 `words.deck`만 기본값 `core`로 추가 |
| `core/database/src/main/java/com/jooh/opic/core/database/{WordImporter,WordDao}.kt` | 수정 | `deck`을 적재하고 언어·단어장별 Day 통계 쿼리 추가 |
| `feature/words/src/main/java/com/jooh/opic/feature/words/{WordsApp,DayScreen}.kt` | 수정 | 수능 Day 카드와 상세 화면에 `수능` 표시 |
| `core/database/src/test/java/com/jooh/opic/core/database/{MigrationTest,WordImporterTest}.kt` | 수정 | 원본 seq·Day 스냅샷, 신규 범위·중복, v4→v5 기록 보존 확인 |

## 수용 기준 체크

| AC | 결과 | 검증한 테스트·확인 |
|----|------|------------------|
| AC1 | ✅ | 1단계 [REPORT-1.md](REPORT-1.md)와 [candidates.tsv](candidates.tsv): 3,001 → 2,001 제외 → 256 중복 → 744 후보 |
| AC2 | ✅ | `WordImporterTest.originalSequenceAndDaySnapshotAndNewDeckAreStable`: 기존 5,517개의 `(language, word, seq, day)` SHA-256이 1단계 전 값과 일치. 기존 seq 변경 없음 |
| AC3 | ✅ | 동일 테스트: 6,261개 `(language, 소문자 word)`와 `(language, seq)`가 모두 고유. 적재기는 중복 입력 시 실패·롤백 |
| AC4 | ✅ | `MigrationTest.v4ToV5KeepsWordIdentityAndProgressAfterFullSeedImport`: v4의 단어 `id`·`seq` 및 UserWord 정답 2·오답 1·마지막 학습 시각 123을 v5 마이그레이션과 dataVersion 2 적재 후 비교 |
| AC5 | ✅ | 같은 스냅샷 테스트: 신규 seq 5521~6264, `deck = suneung`, Day 138은 37개 기존 단어, Day 157은 24개 신규 단어 |
| AC6 | ✅ | 새 Day 통계 쿼리에 `language`·`deck` 조건이 있고 `OnConflictStrategy.REPLACE`를 사용하지 않음 |
| AC7 | ✅ | `./gradlew test lint :app:assembleRelease` 통과. SM-S916N에 release APK를 `adb install -r`로 설치하고 Day 1·139 화면 확인. 설치 전 학습 횟수 기준값은 없어 실기기에서 기록 수치의 전후 직접 비교는 못함 |

## 검증 결과

```text
./gradlew test lint :app:assembleRelease → BUILD SUCCESSFUL (855개 작업, 1분 18초)
core/database 단위 테스트 → 25개 통과, 실패 0개
MigrationTest → 5개 통과, 실패 0개
WordImporterTest → 12개 통과, 실패 0개
데이터 검증 → 기존 5,517개 해시 일치, 신규 744개 고유, Day 138/139/157 = 37/40/24
adb -t 2 install -r app/build/outputs/apk/release/app-release.apk → Success
```

## 실기기 확인

- 기기: S23+ (`SM-S916N`), 일반 사용자 화면. 앱 삭제·데이터 초기화 없이 release APK만 덮어 설치했다. 플래시카드는 풀지 않았다.
- 단어 목록: `157일 · 6261개 단어`가 표시됐다. Day 138에는 `수능` 라벨이 없고, Day 139~157에는 라벨이 표시됐다.
- Day 1: `습득 0 / 40`, `archipelago`, `rustle`, `containment` 등 기존 첫 단어와 뜻이 표시됐다. 설치 전의 학습 횟수 화면은 확보하지 못해, 이 `0 / 40` 수치가 설치 전과 같은지는 직접 확인하지 못했다. DB 기록 보존은 AC4 마이그레이션 테스트로 검증했다.
- Day 139: `수능`, `습득 0 / 40`과 첫 신규 단어 `abandon`, `abroad`, `absent`가 표시됐다.

## 설계 판단 / 리뷰어가 봐야 할 곳

- `exports/words.json`: 744개를 승인된 후보 순서대로 추가했다. 괄호 안 파생어는 새 행으로 넣지 않았다. `level`은 고등학교 그 외 과목 묶음으로 4를 사용했다. 모든 뜻·영어 정의·예문·번역·발음 표기를 새로 작성했으며 시판 단어장 문구는 사용하지 않았다.
- 뜻을 확신하기 어려운 `aggress`, `destruct`, `environ`, `evitable`, `illude`, `oversea`, `sophisticate`는 한국어 뜻 앞에 `*`를 붙였다. 이 7개와 발음 표기는 콘텐츠 리뷰에서 우선 확인해 달라.
- `WordDao.kt`: 현재 Day에 단어장 하나만 들어가는 seq 배치이므로 Day 통계의 `MIN(deck)`을 화면 라벨에 사용했다. 별도의 `dayStatsByDeck`은 `language`와 `deck`을 모두 필터링한다.
- `Migrations.kt`: `ALTER TABLE ... ADD COLUMN deck TEXT NOT NULL DEFAULT 'core'`만 실행하여 기존 단어 `id`, UserWord 행을 유지한다. Room의 `@ColumnInfo(defaultValue = "'core'")`와 v5 스키마가 일치한다.

## 범위 밖 변경

- `HANDOFF.md`는 사용자 요청에 따라 TASK 수정 범위 표 밖에 작성했다. 다른 범위 밖 변경은 없다.

## 질문

- 없음.

## 리뷰 반영

- 기준: `REVIEW.md` Must-fix 1. 반영 코드 커밋 `ff8bcb0` (`1eb4809..ff8bcb0`). 문서 변경은 후속 커밋.
- `dataVersion` 2→3. 기존 6,261개 행의 단어·seq·Day 배정을 유지하고, 9개는 삭제하지 않고 `deleted: true`로 숨겼다. 새 단어 8개를 seq 6265~6272에 추가했다. 총 6,269행 중 화면 노출은 6,260개이며 Day 157은 32개다.
- 사용자 요청에 따라 Day 카드와 Day 상세 화면의 `수능` 글자 표시를 제거했다. `deck` 데이터 구분은 유지한다.

### 변경 표

| 항목 | 변경 전 | 변경 후 | 이유 |
|------|---------|---------|------|
| 어근 7개 | Day에 노출 | `aggress`, `destruct`, `environ`, `evitable`, `illude`, `oversea`, `sophisticate` 숨김 | 학습 표제어로 부적합 |
| 괄호 표기 추가 검토 | `datum`, `opportune` 노출 | 두 단어 숨김 | 괄호 안 `data`, `opportunity`가 학습에 적합 |
| 신규 단어 | 없음 | `aggressive` 6265, `destruction` 6266, `environment` 6267, `inevitable` 6268, `illusion` 6269, `overseas` 6270, `data` 6271, `opportunity` 6272 | 고시 표기에서 실제 학습 단어 보완. 뜻·정의·예문 새로 작성 |
| `sophisticated` | 기존 core 단어 4363 | 추가하지 않음 | 중복 방지 |
| 단어장 표시 | Day 카드·상세 화면에 `수능` 글자 | 글자 제거 | 사용자 요청. Day 순서와 학습 모드는 유지 |
| 테스트 | dataVersion 2와 최초 744개 검증 | dataVersion 3, 기존 6,261개 seq·Day 해시, 9개 숨김과 새 seq 고유성 검증 | 기존 학습 기록 보존과 표시 대상 확인 |

### 괄호 표기 48개 검토

| 고시 표기 | 기존 표제어 처리 | 괄호 안 단어 처리 | 판단 |
|-----------|------------------|-------------------|------|
| `ache (headache)` | `ache` 유지 | `headache` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `aggress (aggressive)` | `aggress` 숨김 | `aggressive (6265)` 추가 | 독립 학습 표제어로 부적합 |
| `alternate (alternative)` | `alternate` 유지 | `alternative` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `ambition (ambitious)` | `ambition` 유지 | `ambitious` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `architect (architecture)` | `architect` 유지 | `architecture` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `behave (behavior / behaviour)` | `behave` 유지 | `behavior / behaviour` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `commit (commission)` | `commit` 유지 | `commission` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `compete (competent)` | `compete` 유지 | `competent` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `corporate (corporation)` | `corporate` 유지 | `corporation` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `courage (discourage, encourage)` | `courage` 유지 | `discourage, encourage` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `critic (criticize / criticise, criticism)` | `critic` 유지 | `criticize / criticise, criticism` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `datum (data)` | `datum` 숨김 | `data (6271)` 추가 | 독립 학습 표제어로 부적합 |
| `decline (declension)` | `decline` 유지 | `declension` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `defend (defendant)` | `defend` 유지 | `defendant` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `depart (department, departure)` | `depart` 유지 | `department, departure` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `destruct (destruction)` | `destruct` 숨김 | `destruction (6266)` 추가 | 독립 학습 표제어로 부적합 |
| `differ (different)` | `differ` 유지 | `different` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `dominate (dominant)` | `dominate` 유지 | `dominant` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `emerge (emergence)` | `emerge` 유지 | `emergence` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `emit (emission)` | `emit` 유지 | `emission` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `environ (environment)` | `environ` 숨김 | `environment (6267)` 추가 | 독립 학습 표제어로 부적합 |
| `equip (equipment)` | `equip` 유지 | `equipment` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `erase (eraser)` | `erase` 유지 | `eraser` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `evitable (inevitable)` | `evitable` 숨김 | `inevitable (6268)` 추가 | 독립 학습 표제어로 부적합 |
| `exclude (exclusive)` | `exclude` 유지 | `exclusive` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `fame (famous)` | `fame` 유지 | `famous` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `fate (fatal)` | `fate` 유지 | `fatal` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `gene (genetic)` | `gene` 유지 | `genetic` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `generate (generation)` | `generate` 유지 | `generation` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `globe (global)` | `globe` 유지 | `global` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `harmony (harmonious)` | `harmony` 유지 | `harmonious` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `illude (illusion)` | `illude` 숨김 | `illusion (6269)` 추가 | 독립 학습 표제어로 부적합 |
| `import (importance, important)` | `import` 유지 | `importance, important` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `institute (institution)` | `institute` 유지 | `institution` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `medium (media)` | `medium` 유지 | `media` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `mine (miner)` | `mine` 유지 | `miner` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `occupy (occupation)` | `occupy` 유지 | `occupation` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `opportune (opportunity)` | `opportune` 숨김 | `opportunity (6272)` 추가 | 독립 학습 표제어로 부적합 |
| `opt (option)` | `opt` 유지 | `option` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `organ (organic, organize / organise)` | `organ` 유지 | `organic, organize / organise` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `origin (original)` | `origin` 유지 | `original` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `populate (population)` | `populate` 유지 | `population` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `publish (publisher)` | `publish` 유지 | `publisher` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `submit (submission)` | `submit` 유지 | `submission` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `substance (substantial)` | `substance` 유지 | `substantial` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `terror (terrorist)` | `terror` 유지 | `terrorist` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `verb (verbal)` | `verb` 유지 | `verbal` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |
| `virtue (virtual)` | `virtue` 유지 | `virtual` 별도 추가 없음 | 표제어 자체가 독립 학습 단어 |

- 괄호 표기 48개 중 7개 표제어를 숨기고 대응 단어를 추가했다. 나머지 41개 표제어는 독립 학습 단어로 유지했다.
- 괄호 밖의 `oversea / overseas`는 `oversea`를 숨기고 `overseas`를 추가했다. `sophisticate`는 숨겼고 `sophisticated`가 이미 core에 있으므로 추가하지 않았다.

### 검증

- `./gradlew test lint :app:assembleRelease --console=plain` 통과: `BUILD SUCCESSFUL in 42s` (855개 작업).
- `WordImporterTest`: 기존 5,517개 해시와 리뷰 전 6,261개 `(language, word, seq, day)` 해시 유지. 숨긴 9개가 DB에 남고 해당 Day 목록에서는 빠짐. 신규 seq 6265~6272와 전체 `(language, word)`·`(language, seq)` 고유 확인.
- `MigrationTest`: v4→v5 이후 dataVersion 3 전체 적재에서도 기존 단어 id·seq·UserWord 기록 유지.
- 실기기 재설치는 이번 리뷰 반영에서 진행하지 않았다. `수능` 글자 제거는 코드와 릴리스 빌드로 확인했다.
