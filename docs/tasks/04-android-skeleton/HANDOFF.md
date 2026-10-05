# HANDOFF: 안드로이드 골격과 Room 단어 스키마

> 작성: GPT · 경로: docs/tasks/04-android-skeleton/HANDOFF.md

## 커밋 범위
- base: `17d8450` (작업 시작 시점)
- TASK 승인 문서: `640648c`
- 구현 head: `0b7e421` (이 HANDOFF는 별도 문서 커밋)
- 리뷰 명령: `git diff 640648c..0b7e421`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| Gradle 설정·wrapper | 생성 | 네 모듈과 머니로그 기준 버전 구성 |
| `app/` | 생성 | 권한 없는 Manifest와 Compose 빈 홈 |
| `core/common/` | 생성 | Day 계산과 단위 테스트 |
| `core/model/` | 생성 | 언어·학습 상태와 단위 테스트 |
| `core/database/` | 생성 | Room 엔티티·DAO·스키마와 Robolectric 테스트 |
| `README.md` | 수정 | 빌드 명령 추가 |

## 수용 기준 체크
| AC | 결과 | 검증한 명령·테스트 |
|----|------|------------------|
| AC1 | ✅ | `./gradlew assembleDebug` → 성공 |
| AC2 | ✅ | `./gradlew test`, `./gradlew lint` → 성공 |
| AC3 | ✅ | `DayTest.dayAndRange` → 지정 경계·예외 통과 |
| AC4 | ✅ | `DaoTest.dayFiltersLanguageDeletionAndOrder` → 언어·삭제·순서 확인 |
| AC5 | ✅ | `DaoTest.upsertPreservesIdentitySequenceAndLearning` → 뜻 변경, id·seq 유지 |
| AC6 | ✅ | 같은 테스트 → UserWord 정답 기록 유지 |
| AC7 | ✅ | 같은 테스트 → 삽입·재실행 시 행 수와 id 유지 |
| AC8 | ✅ | `DaoTest.recordResultTracksStatusAndErrors` → 정답 3회·오답 1회 |
| AC9 | ✅ | `git ls-files 'core/database/schemas/**'` → `OpicDatabase/1.json` 출력 |
| AC10 | ✅ | `rg -n 'REPLACE|fallbackToDestructive|CASCADE' --glob '*.kt' .` → 0줄 |
| AC11 | ✅ | `rg -n 'uses-permission' app/src/main/AndroidManifest.xml` → 0줄 |
| AC12 | ✅ | `git diff 17d8450 0b7e421 -- exports/ docs/decisions/ SPEC.md` → 0줄 |

## 검증 결과
```text
./gradlew assembleDebug → 통과
./gradlew test → 통과 (Day 1, 상태 1, Robolectric DAO 3; 실패 0)
./gradlew lint → 통과 (오류 0, 앱 경고 17, DB 문제 0)
```

## 설계 판단 / 리뷰어가 봐야 할 곳
- `core/database/src/main/java/com/jooh/opic/core/database/Entities.kt` — `exports/words.json`의 영어 5,517개에서 `phonetic`, `meaningKo`, `meaningEn`, `example`, `exampleKo`, `category`, `partOfSpeech`, `collocations` 모두 null 0개·빈 문자열 0개라 필수 타입으로 정의했다. `lastStudiedAt`만 nullable이다.
- `core/database/src/main/java/com/jooh/opic/core/database/WordDao.kt` — `@Upsert` 대신 `(language, word)` 조회 후 UPDATE/INSERT를 선택했다. 기존 id·seq를 명시적으로 유지하며 Robolectric으로 검증했다.
- `gradle.properties` — AGP 9.3.3의 내장 Kotlin과 현재 KSP 설정이 충돌해 머니로그와 같은 `android.builtInKotlin=false`, `android.newDsl=false`를 사용했다. AGP 10 이전에 호환 설정 갱신이 필요하다.
- `local.properties` — SDK 경로를 로컬에만 설정했고 커밋하지 않았다.

## 범위 밖 변경
- 없음

## 질문
- 없음
