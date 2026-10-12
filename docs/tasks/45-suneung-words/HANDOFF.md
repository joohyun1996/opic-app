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
