# TASK: 안드로이드 골격 (Gradle 멀티모듈 + Room 단어 스키마)

> 작성: Claude · 승인: [x] 사용자 (2026-10-05)
> 경로: docs/tasks/04-android-skeleton/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 결정, § Day 구성, § 단어 데이터, § 언어 범위, § 후속 작업 순서 4
> 선행: TASK 03 Approve (`17d8450`)

## 목표
빈 저장소 위에 Kotlin + Compose + Room 안드로이드 프로젝트를 만든다. 빌드 도구 버전은 머니로그(`~/Desktop/develop/investment`)와 맞추고, 단어 데이터의 핵심 규칙(Day 계산, `(language, word)` 기준 upsert, seq 불변, UserWord 보존)을 Room 스키마와 단위 테스트로 고정한다. 화면은 빈 홈 하나만 둔다.

## 확정 사항
| 항목 | 값 | 근거 |
|------|----|------|
| applicationId / namespace | `com.jooh.opic` | |
| 모듈 | `:app`, `:core:common`, `:core:model`, `:core:database` | AGENTS.md § 폴더 구조. `core/llm`은 TASK 05, `feature/*`는 각 기능 TASK에서 만든다 |
| Gradle / AGP / Kotlin / KSP | 9.5.0 / 9.3.3 / 2.2.10 / 2.2.10-2.0.2 | 머니로그와 동일 |
| Room / Compose BOM / coroutines | 2.8.5 / 2024.12.01 / 1.10.2 | 머니로그와 동일 |
| compileSdk / minSdk / targetSdk | 37 / 34 / 35 | 머니로그와 동일. 테스트 기기 갤럭시 S23+ (Android 16, API 36) |
| JVM target | 17 | 머니로그와 동일 |

버전은 `~/Desktop/develop/investment/gradle/libs.versions.toml`에서 **필요한 항목만** 옮긴다. 머니로그 전용 의존성(biometric, markdown, okhttp, work, dokka, security-crypto, mediapipe 등)은 가져오지 않는다. 머니로그 저장소는 읽기만 한다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` | 생성 | 4개 모듈 include, rootProject.name = "OpicApp" |
| `gradle/libs.versions.toml` | 생성 | 위 확정 버전 + Robolectric, androidx.test core |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/*` | 생성 | Gradle 9.5.0 wrapper |
| `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` | 생성 | 권한 선언 없음 |
| `app/src/main/java/com/jooh/opic/{OpicApplication,MainActivity}.kt` | 생성 | Compose 빈 홈 화면 ("OPIc 학습" 텍스트) |
| `app/src/main/res/values/{strings,themes}.xml` | 생성 | 앱 이름, 테마만 (레이아웃 XML 금지) |
| `core/common/**` | 생성 | 순수 Kotlin(JVM) 모듈: Day 계산 |
| `core/model/**` | 생성 | 순수 Kotlin(JVM) 모듈: `Language`, `WordStatus` |
| `core/database/**` | 생성 | Android 라이브러리: Entity, DAO, Database, 스키마 JSON 내보내기 |
| `core/*/src/test/**` | 생성 | 단위 테스트 |
| `README.md` | 수정 | 빌드 방법 한 줄 추가 |

## 요구사항

### core:common — Day 계산 (순수 Kotlin)
- `dayOf(seq: Int): Int` = `(seq - 1) / 40 + 1`. seq < 1이면 `IllegalArgumentException`
- `seqRange(day: Int): IntRange` = `(day-1)*40+1 .. day*40`. day < 1이면 `IllegalArgumentException`
- `totalDays(maxSeq: Int): Int` = 40 올림 나눗셈. maxSeq = 0이면 0
- 상수 `WORDS_PER_DAY = 40`

### core:model
- `enum class Language(val code: String) { EN("en"), ZH("zh") }`. `en`/`zh` 두 언어를 처음부터 전제한다
- `enum class WordStatus { NEW, LEARNING, MASTERED }`. 판정 함수: UserWord가 없으면 NEW, `correctCount >= 3`이면 MASTERED, 그 외 LEARNING

### core:database — Room
**WordEntity** (`words`)
- 필드: `id`(자동 PK), `language`, `word`, `seq`, `phonetic`, `meaningKo`, `meaningEn`, `example`, `exampleKo`, `level`, `category`, `partOfSpeech`, `collocations`(List<String> → TypeConverter, JSON 문자열), `deleted`
- 필드 이름과 타입은 `exports/words.json`의 항목과 1:1로 맞춘다. nullable 여부는 실제 데이터를 확인해 정하고, HANDOFF에 근거를 적는다
- 인덱스: `(language, word)` UNIQUE, `(language, seq)` UNIQUE

**UserWordEntity** (`user_words`)
- 필드: `wordId`(PK, `words.id` 참조), `correctCount`, `wrongCount`, `lastStudiedAt`(epoch millis, nullable)
- 외래키는 `onDelete = NO_ACTION`. **CASCADE 금지** (words 행이 지워져도 학습 기록이 같이 사라지면 안 된다)

**DataMetaEntity** (`data_meta`): `key`(PK), `value`. 저장된 dataVersion을 둔다

**WordDao**
- `getDayWords(language, day)`: seq 범위 안, `deleted = 0`, `ORDER BY seq`
- `maxSeq(language)`, `countByLanguage(language)`
- `upsertWords(words)`: 한 트랜잭션. `(language, word)`로 기존 행을 찾아 있으면 UPDATE, 없으면 INSERT한다
  - UPDATE 시 **id와 seq는 바꾸지 않는다** (입력 seq가 달라도 기존 값 유지)
  - `OnConflictStrategy.REPLACE` 금지. `@Upsert`는 UNIQUE 인덱스 충돌 시 id가 유지되는지 테스트로 증명할 때만 쓴다. 자신 없으면 조회 후 `@Update`/`@Insert`로 나눈다

**UserWordDao**
- `recordResult(wordId, correct: Boolean, now)`: 없으면 생성, 있으면 해당 카운트 +1
- `get(wordId)`

**OpicDatabase**: version 1, `exportSchema = true`, 스키마 경로 `core/database/schemas` (커밋 대상). `fallbackToDestructiveMigration` 사용 금지

### app
- `OpicApplication`에서 Database를 lazy 싱글톤으로 만든다 (수동 DI, Hilt 미사용. 머니로그 `AppContainer` 방식)
- 단어 JSON 적재(`words.json` → Room)는 이 TASK에서 하지 않는다

## 수용 기준
- [ ] AC1: `./gradlew assembleDebug`가 성공한다
- [ ] AC2: `./gradlew test`, `./gradlew lint`가 통과한다
- [ ] AC3: `dayOf(1)=1`, `dayOf(40)=1`, `dayOf(41)=2`, `dayOf(5517)=138`, `totalDays(5517)=138`, `totalDays(0)=0`, `seqRange(2)=41..80`이고, `dayOf(0)`은 예외를 던진다
- [ ] AC4: `getDayWords(en, 1)`은 seq 1~40 중 `deleted = false`인 단어만 seq 순으로 반환하고, 같은 seq 범위의 `zh` 단어는 섞이지 않는다
- [ ] AC5: 이미 있는 `(en, "abc")`를 뜻과 seq를 바꿔 `upsertWords`하면, 뜻은 바뀌고 **id와 seq는 그대로**다
- [ ] AC6: AC5 이후 그 단어의 UserWord 기록(correctCount 등)이 그대로 남아 있다
- [ ] AC7: 새 단어를 `upsertWords`하면 INSERT된다. 같은 입력을 두 번 실행해도 행 수와 id가 같다
- [ ] AC8: `recordResult`를 정답 3번 호출하면 상태 판정이 MASTERED, 오답 1번이면 `wrongCount = 1`이다
- [ ] AC9: `core/database/schemas/.../1.json`이 생성되어 커밋된다
- [ ] AC10: `grep -rn "REPLACE\|fallbackToDestructive\|CASCADE" --include=*.kt .` 결과가 0줄이다
- [ ] AC11: `AndroidManifest.xml`에 `uses-permission`이 0개다
- [ ] AC12: `git diff <base> HEAD -- exports/ docs/decisions/ SPEC.md` 결과가 없다

DAO 테스트(AC4~AC8)는 Robolectric + in-memory Room으로 `./gradlew test`에서 돌린다. 기기가 필요한 instrumented 테스트로 두지 않는다.

## 제약 / 주의
- AGENTS.md "파일 수정 권한"에도 불구하고, 이 TASK는 Room 엔티티 최초 생성과 위 의존성 설치가 승인되어 있다. 위 표에 없는 의존성을 추가하려면 먼저 묻는다
- `local.properties`(SDK 경로)는 커밋하지 않는다 (.gitignore에 있음)
- 커밋은 기능 단위로 나눠도 된다. 예: `chore(android): Gradle 멀티모듈 골격`, `feat(database): 단어 Room 스키마와 DAO`
- 생성되는 diff가 wrapper·스키마 JSON을 빼고 600줄을 크게 넘으면 멈추고 보고한다
- push 금지

## 범위 밖 (하지 말 것)
- `core/llm` 복사, MediaPipe 의존성 (TASK 05)
- `words.json` 적재, dataVersion 비교 로직, 단어 화면 (단어 탭 TASK)
- `feature/*` 모듈 생성, Navigation
- 문법·섀도잉·스피킹 관련 엔티티
- `exports/`, `docs/decisions/`, `SPEC.md`, AGENTS.md, CLAUDE.md 수정
