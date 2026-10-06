# TASK: 단어 데이터 적재 + 홈·Day 목록 화면

> 작성: Claude · 승인: [ ] 사용자
> 경로: docs/tasks/06-words-import-day-index/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 단어 데이터, § Day 구성 / `AGENTS.md` § 데이터 규칙 / `docs/design/words-ui.md` ①② / `SPEC.md` § 안드로이드 골격 + Room 스키마

## 목표
앱 첫 실행 때 `exports/words.json`을 Room에 넣고, 이후에는 dataVersion이 오를 때만 단어를 갱신한다. 그 데이터로 **① 홈**과 **② Day 목록** 화면을 만든다. 단어 탭 이식 3개 TASK 중 첫 번째다 (07: Day 단어 목록 + 플래시카드, 08: 오답 모음).

## 수정 범위
> GPT는 아래 파일만 허락 없이 생성/수정할 수 있다. 이 TASK에 한해 아래 라이브러리 추가(버전 카탈로그에 이미 있는 것 + navigation-compose, lifecycle-viewmodel-compose)를 승인한다.

| 파일 | 작업 | 내용 |
|------|------|------|
| `core/database/src/main/java/.../WordImporter.kt` | 생성 | JSON 파싱 + 검증 + dataVersion 비교 + 적재 |
| `core/database/src/main/java/.../WordDao.kt` | 수정 | Day 쿼리를 seq 범위 인자로 변경, Day별 통계 쿼리 추가 |
| `core/database/src/main/java/.../DataMetaDao.kt` | 생성 | dataVersion 읽기/쓰기 |
| `core/database/src/main/java/.../OpicDatabase.kt` | 수정 | `dataMetaDao()` 추가 (**스키마 변경 없음**, DB version 1 유지) |
| `core/database/build.gradle.kts` | 수정 | `:core:common` 의존, kotlinx-serialization 추가 |
| `core/database/src/test/...` | 생성/수정 | 적재·통계·Day 쿼리 테스트 |
| `feature/words/**` | 생성 | 새 모듈: 홈·Day 목록 화면 + ViewModel |
| `settings.gradle.kts` | 수정 | `:feature:words` 추가 |
| `gradle/libs.versions.toml` | 수정 | navigation-compose, lifecycle-viewmodel-compose 추가 |
| `app/build.gradle.kts` | 수정 | `:feature:words` 의존, `words.json`을 assets로 포함하는 설정 |
| `app/src/main/java/com/jooh/opic/MainActivity.kt` | 수정 | edge-to-edge, 내비게이션, 적재 상태 표시 |
| `app/src/main/java/com/jooh/opic/OpicApplication.kt` | 수정 | 앱 시작 시 적재 실행 |
| `app/src/main/java/com/jooh/opic/debug/LlmBenchScreen.kt` | 이동 | → `app/src/debug/java/com/jooh/opic/debug/` (TASK 05 N4) |
| `.gitignore` | 수정 | `bin/` 추가 (VS Code Java 확장 산출물) |

## 관련 파일 (읽기만)
- `core/database/src/main/java/.../Entities.kt` — WordEntity 필드 (words.json과 1:1)
- `core/common/src/main/kotlin/.../Day.kt` — `WORDS_PER_DAY`, `seqRange`, `totalDays`
- `core/model/src/main/kotlin/.../WordStatus.kt` — 상태 판정 (MASTERED = correctCount ≥ 3)
- `docs/tasks/04-android-skeleton/REVIEW.md` § Nit N1~N3

## 요구사항

### 1. words.json을 APK에 포함
- 저장소의 `exports/words.json` **하나만** APK assets에 들어가게 한다. 복사본을 저장소에 커밋하지 않는다 (Gradle 빌드 단계에서 복사하거나 assets 경로를 지정). `exports/source/` 등 다른 파일은 APK에 들어가면 안 된다.

### 2. WordImporter (`core:database`)
- 입력: JSON 문자열(또는 InputStream). Android Context에 의존하지 않게 만들어 단위 테스트가 쉽게 한다.
- 파싱: kotlinx-serialization. 형식은 `SPEC.md` § 단어 데이터.
- 검증 (하나라도 어기면 **전체 적재 실패**):
  - `language`는 `en` 또는 `zh`, `word`·`meaningKo`는 공백이 아님, `seq` ≥ 1, `level` ≥ 1
  - 같은 파일 안에 `(language, word)` 중복 없음, `(language, seq)` 중복 없음
  - 영어(`en`)는 `phonetic`, `meaningEn`, `example`, `exampleKo`가 공백이 아님 (TASK 02 교차 리뷰 Should-fix를 여기서 처리)
- 적재 규칙:
  - 저장된 dataVersion(`data_meta`의 `words_data_version`)이 없거나 JSON보다 작을 때만 적재한다. 같거나 크면 아무것도 쓰지 않는다.
  - **하나의 트랜잭션**에서 `upsertWords` + dataVersion 저장. 중간에 실패하면 전부 롤백된다.
  - `user_words`는 읽지도 쓰지도 않는다.
- 결과: `Imported(version, count)` / `UpToDate(version)` / `Failed(reason)`를 돌려준다. **예외를 밖으로 던지지 않는다.** DB 제약 위반(TASK 04 N2: 기존 seq와 충돌) 같은 오류도 `Failed`로 바꾼다.

### 3. DAO
- `getDayWords`: SQL에 `40`을 쓰지 않는다. 호출하는 쪽에서 `seqRange(day)`의 처음과 끝을 넘긴다 (TASK 04 N1).
- Day별 통계 쿼리를 추가한다. 언어 하나에 대해 Day마다 `total`(deleted 제외), `mastered`(correctCount ≥ 3), `wrong`(wrongCount > 0) 개수를 돌려준다. Day 번호 계산에 쓰는 40도 인자로 받는다 (`WORDS_PER_DAY`).
- 전체 Day 수는 `totalDays(maxSeq)`로 계산한다.

### 4. 앱 시작
- `OpicApplication`에서 백그라운드(IO)로 적재를 한 번 실행하고 결과를 상태(StateFlow 등)로 노출한다.
- `Failed`이면 앱이 죽지 않고, 화면 상단에 "단어 데이터를 불러오지 못했습니다" 안내를 띄운다. 로그에 이유를 남긴다. 이미 적재된 데이터가 있으면 그 데이터로 화면을 계속 보여 준다.
- 적재 중에는 로딩 표시를 한다.

### 5. 화면 (`:feature:words`, Compose + Material3)
- 공통: `enableEdgeToEdge()` + Scaffold 패딩 (TASK 04 N3). 라이트 모드만 지원. 색은 `words-ui.md`의 흑백 베이스와 상태 뱃지 색을 쓴다.
- **① 홈**
  - 언어 카드 2개. 영어: "영어 (OPIc)", 습득 단어 수 / 전체 단어 수와 진행률 바. 누르면 ②로 간다.
  - 중국어: "중국어 (HSK) — 준비 중" 비활성 카드 (ADR 001 § 언어 범위)
- **② Day 목록**
  - 4열 그리드. 칸마다 "Day N"과 "습득수/전체수" (예: `12/40`, 마지막 Day는 `12/37`)
  - 상단에 오답 단어 총개수 뱃지 (빨강, 0이면 숨김). 이번 TASK에서는 누르면 아무 동작 없음 (TASK 08)
  - Day 칸을 누르면 "Day N — 준비 중" 빈 화면으로 이동 (TASK 07에서 채움)
- 화면 이동은 navigation-compose. 상태는 ViewModel이 DAO를 읽어 만든다.
- `BuildConfig.DEBUG`일 때만 홈에 "LLM 검증" 진입 버튼을 유지한다. 화면 파일은 debug 소스셋으로 옮긴다 (TASK 05 N4). release 빌드에 포함되면 안 된다.

## 수용 기준
- [ ] AC1: 빈 DB에 실제 `exports/words.json`을 적재하면 `Imported(1, 5517)`을 돌려주고, `words` 행 5,517개, 저장된 dataVersion은 1이다 (저장소 파일을 읽는 테스트)
- [ ] AC2: 같은 파일을 다시 적재하면 `UpToDate(1)`을 돌려주고 어떤 행도 바뀌지 않는다
- [ ] AC3: dataVersion 2로 뜻을 바꾼 JSON을 적재하면 해당 단어의 `meaningKo`가 바뀌고, `id`·`seq`는 그대로이며, 미리 넣어 둔 `user_words` 행이 그대로 남는다
- [ ] AC4: 기존 단어와 seq가 겹치는 새 단어가 든 JSON(TASK 04 N2)을 적재하면 `Failed`를 돌려주고 예외를 던지지 않으며, 행 수와 저장된 dataVersion이 적재 전과 같다
- [ ] AC5: 깨진 JSON, `(language, word)` 중복, 영어 `example` 공백 중 하나라도 있으면 `Failed`를 돌려주고 DB가 바뀌지 않는다
- [ ] AC6: Day별 통계는 deleted 단어를 total에서 빼고, correctCount 3 이상을 mastered로, wrongCount 1 이상을 wrong으로 센다 (deleted·습득·오답이 섞인 픽스처로 확인)
- [ ] AC7: `getDayWords`에 `seqRange(2)`를 넘기면 seq 41~80 중 deleted가 아닌 단어만 seq 순으로 돌려준다. `WordDao.kt`에 숫자 `40`이 없다
- [ ] AC8: 실제 데이터 기준 전체 Day 수는 138이고, 마지막 Day의 total은 37이다
- [ ] AC9: release APK의 assets에 `words.json`이 있고 `exports/source/`의 파일은 없다. release APK에 `LlmBenchScreen` 클래스가 없다 (HANDOFF에 확인 명령과 출력 첨부)
- [ ] AC10: `./gradlew test`, `./gradlew lint` 통과
- [ ] AC11: S23+ 실기기에서 확인한 내용을 HANDOFF에 적는다. 첫 실행 적재 시간(ms), 두 번째 실행 시 `UpToDate` 로그, 홈·Day 목록 스크린샷 각 1장 (Day 138까지 스크롤, 상태바와 겹치지 않음)

## 제약 / 주의
- **Room 스키마를 바꾸지 않는다** (엔티티·DB version 그대로). 바꿔야 할 것 같으면 멈추고 묻는다.
- `OnConflictStrategy.REPLACE` 금지. 모든 쿼리는 `language` 조건을 포함한다.
- 적재는 메인 스레드에서 하지 않는다.
- 단어 5,517개를 하나씩 `find` 후 UPDATE/INSERT하는 기존 `upsertWords`를 그대로 쓴다. 실기기 적재가 3초를 넘으면 HANDOFF에 수치를 적고, 최적화는 하지 말고 보고만 한다.
- 새 권한을 추가하지 않는다.

## 범위 밖 (하지 말 것)
- Day 단어 목록, 플래시카드, 발음(TTS), 오답 모음 화면 (TASK 07·08)
- `exports/words.json` 내용 수정
- 중국어 데이터 적재
- 다크 모드
