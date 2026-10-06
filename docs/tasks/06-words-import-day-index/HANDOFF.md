# HANDOFF: 단어 데이터 적재 + 홈·Day 목록 화면

> 작성: GPT(구현) + Claude(마무리) · 경로: docs/tasks/06-words-import-day-index/HANDOFF.md
> GPT 토큰 소진으로 마무리(연결 확인, 실기기 검증, 커밋, 이 문서)는 Claude가 했다 (2026-10-06 사용자 지시).

## 커밋 범위
- base: `5bad698`
- head: 이 커밋 하나

## 변경 요약
```diff
+ core/database/.../WordImporter.kt   — kotlinx-serialization 파싱, 검증, dataVersion 비교, 한 트랜잭션 적재, 예외 → Failed
+ core/database/.../DataMetaDao.kt    — get / insert(IGNORE) / update
~ core/database/.../WordDao.kt        — getDayWords(language, firstSeq, lastSeq), dayStats(language, wordsPerDay)
~ core/database/.../OpicDatabase.kt   — dataMetaDao() 추가 (스키마·version 변화 없음)
+ core/database/src/test/.../WordImporterTest.kt (5개), DaoTest 수정
+ feature/words/                      — 새 모듈: WordsApp(홈·Day 목록·준비 중 화면, navigation-compose), WordsViewModel
~ app/build.gradle.kts                — :feature:words 의존, exports/words.json을 build/generated/wordAssets로 Sync해 assets에 포함
~ app/.../OpicApplication.kt          — 앱 시작 시 IO에서 적재, importResult StateFlow, 로그 태그 WordImport
~ app/.../MainActivity.kt             — enableEdgeToEdge, WordsApp 호출
+ app/src/debug/.../DebugContent.kt, app/src/release/.../DebugContent.kt — LLM 검증 화면 연결 (release는 null)
→ LlmBenchScreen.kt, assets/llm-bench/ → app/src/debug/ 로 이동
~ .gitignore                          — bin/
- core/common/bin/, core/model/bin/   — 커밋 5bad698에 실수로 포함된 IDE 산출물 추적 해제
```

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1 | 통과 | `WordImporterTest.actualFileAndRepeatAndLastDay` — 실제 파일 `Imported(1, 5517)` |
| AC2 | 통과 | 같은 테스트 — 재적재 `UpToDate(1)` |
| AC3 | 통과 | `upgradePreservesIdentitySequenceAndProgress` |
| AC4 | 통과 | `collisionRollsBackEarlierUpdatesAndVersion` |
| AC5 | 통과 | `invalidInputsDoNotWrite` (깨진 JSON, 중복, 영어 example 공백) |
| AC6 | 통과 | `dayStatisticsAndExplicitRange` |
| AC7 | 통과 | 같은 테스트 + `WordDao.kt`에 숫자 40 없음 |
| AC8 | 통과 | 실제 파일 Day 138, 마지막 Day total 37 (테스트 + 실기기 `0/37`) |
| AC9 | 통과 | 아래 명령 출력 |
| AC10 | 통과 | `./gradlew test lint assembleDebug assembleRelease` exit 0 |
| AC11 | 통과 | 아래 실기기 결과 |

### AC9 확인
```
release assets: ['assets/words.json']          ← 원본과 cmp 동일
debug   assets: ['assets/llm-bench/sentences.json', 'assets/words.json']
release dex LlmBenchScreen: classes.dex False / classes2.dex False / classes3.dex False
debug   dex LlmBenchScreen: True
```
(처음에는 `llm-bench/sentences.json`이 main assets에 있어 release에도 들어갔다. debug 소스셋으로 옮겼다.)

## 실기기 결과 (S23+, debug 빌드)
| 항목 | 값 |
|------|------|
| 첫 실행 적재 (`pm clear` 후) | `Imported(version=1, count=5517) elapsedMs=3283` |
| 첫 실행 적재 (GPT 측정, 화면 잠금 대기 포함 가능) | 5,901ms |
| 재실행 | `UpToDate(version=1) elapsedMs=637` |
| 앱 시작 `am start -W` TotalTime | 626ms |

- 첫 적재가 3초를 약간 넘는다 (TASK 지시대로 최적화하지 않음). 화면은 적재 중 로딩을 보여 주고 끝나면 바로 갱신된다.
- 재실행도 2.4MB JSON을 매번 파싱해서 637ms가 걸린다. 버전만 먼저 읽으면 줄일 수 있다 (아래 제안).
- 스크린샷: `home.png`, `day-index-top.png`, `day-index-end.png` — 상태바·내비게이션바와 겹치지 않음, Day 138 `0/37`, 중국어 카드 비활성, Day 칸 누르면 "Day N — 준비 중"

## 설계 판단 / 리뷰어가 봐야 할 곳
- `WordImporter`는 `catch (error: Exception)`이라 코루틴 취소(`CancellationException`)도 `Failed`로 바꾼다. 앱 스코프라 실제로 취소될 일은 없지만, 다음에 다듬을 때 다시 던지게 하는 게 맞다.
- dayStats는 `GROUP BY`라 단어가 전부 deleted인 Day는 결과에서 빠진다. 화면은 `totalDays(maxSeq)`로 Day 칸을 만들고 없는 Day는 0/0으로 보여 준다.
- 홈의 진행률 바 끝에 Material3 기본 점(stop indicator)이 보인다. 디자인상 문제는 없지만 지우려면 `drawStopIndicator`를 비우면 된다.

## 범위 밖 변경
- `core/common/bin/`, `core/model/bin/` 추적 해제 (커밋 5bad698에 실수로 포함됨. TASK의 `.gitignore bin/` 항목과 같은 목적)

## 제안 (다음 TASK에서)
1. 재실행 단축: JSON에서 dataVersion만 먼저 읽고, 저장된 값과 같으면 전체 파싱을 건너뛴다
2. 첫 적재 단축이 필요하면 `upsertWords`를 빈 DB일 때만 일괄 insert로 처리

## 질문
- 없음
