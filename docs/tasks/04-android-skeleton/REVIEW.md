# REVIEW: 안드로이드 골격 (Gradle 멀티모듈 + Room 단어 스키마)

> 작성: Claude · 경로: docs/tasks/04-android-skeleton/REVIEW.md

## 1차 리뷰 — 2026-10-05
- 대상: `git diff 640648c..0b7e421` (HANDOFF `c2f3fa4` 포함 확인)
- 판정: **Approve**

### 수용 기준
| AC | 판정 | 근거 |
|----|------|------|
| AC1 | ✅ | HANDOFF 검증 결과 |
| AC2 | ✅ | HANDOFF 검증 결과. lint 경고 17개는 버전 업데이트 안내·아이콘 누락뿐 (N4) |
| AC3 | ✅ | `DayTest.kt:9-16` — 지정 경계값 7개와 `dayOf(0)` 예외 모두 검사 |
| AC4 | ✅ | `DaoTest.kt:33-37` — deleted 숨김, seq 정렬, zh 분리, Day 2 단어 제외 |
| AC5 | ✅ | `WordDao.kt:35` 기존 id·seq를 덮어쓰지 않음. `DaoTest.kt:43-46` (seq 20 입력 → 1 유지, 대문자 입력 → 소문자 매칭) |
| AC6 | ✅ | `DaoTest.kt:47` — upsert 뒤 correctCount 1 유지 |
| AC7 | ✅ | `DaoTest.kt:48-52` — 재실행 시 id·행 수 동일 |
| AC8 | ✅ | `DaoTest.kt:55-65` |
| AC9 | ✅ | `core/database/schemas/.../1.json` 커밋됨 |
| AC10 | ✅ | `Entities.kt:26` `NO_ACTION`, REPLACE·destructive 없음 |
| AC11 | ✅ | `AndroidManifest.xml` 권한 0개 |
| AC12 | ✅ | `--stat`에 exports/, docs/decisions/, SPEC.md 없음 |

### Must-fix
- 없음

### Should-fix
- 없음

### Nit (선택 — 단어 탭 TASK에서 같이 처리해도 됨)
- **N1** `WordDao.kt:11` — Day 범위의 `40`이 SQL에 하드코딩되어 `core:common`의 `WORDS_PER_DAY`와 따로 논다. 호출부에서 `seqRange(day)`의 first/last를 넘기는 쿼리로 바꾸면 규칙이 한 곳에 모인다 (`core:database` → `core:common` 의존 추가).
- **N2** `WordDao.kt:30-36` — 새 단어의 seq가 같은 언어의 기존 seq와 겹치면 `(language, seq)` UNIQUE 위반으로 트랜잭션 전체가 실패한다. 규칙상 생기면 안 되는 입력이라 실패 자체는 맞다. 다만 `words.json` 적재 TASK에서 이 예외를 잡아 "데이터 오류"로 기록하고 앱이 죽지 않게 처리해야 한다.
- **N3** `MainActivity.kt:11` — targetSdk 35부터 edge-to-edge가 강제되어 텍스트가 상태바 아래로 들어간다. 첫 실제 화면을 만들 때 `enableEdgeToEdge()`와 Scaffold 패딩을 넣는다.
- **N4** lint 경고 17개 — 버전 업데이트 안내(Kotlin 2.4, AGP 9.4 등)와 `MissingApplicationIcon`, `OldTargetApi`뿐이다. 머니로그와 버전을 맞추기로 했으므로 지금은 올리지 않는다. 아이콘은 배포 전에 넣는다.
- **N5** `gradle.properties:4-5` — `android.builtInKotlin=false`, `android.newDsl=false`는 AGP 10에서 제거될 예정이다. 머니로그와 함께 옮겨야 하니 TASK 05(`core/llm` 이식) 이후 별도 작업으로 남긴다.

### 좋았던 점
- `@Upsert` 대신 조회 후 UPDATE/INSERT로 나누고, id·seq 유지를 코드에서 명시했다. UserWord 보존 규칙이 가장 확실하게 지켜지는 방식이다.
- nullable 판단을 실제 데이터 5,517개로 확인하고 HANDOFF에 근거를 남겼다.
