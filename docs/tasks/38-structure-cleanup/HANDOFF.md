# HANDOFF: 구조 정리 — 중단 지점

> 작성: GPT · 경로: docs/tasks/38-structure-cleanup/HANDOFF.md

## 커밋 범위
- base: `72d89ee`
- head: `e9d00f3` (`wip`)
- 리뷰 명령: `git diff 72d89ee..e9d00f3`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `feature/grammar/GrammarCatalog.kt` | 수정 | `GrammarTracks`와 `GrammarUnit.displayTitle()` 추가, 출처 제목에 사용 |
| `feature/grammar/GrammarFlow.kt`, `GrammarUnitListScreen.kt` | 수정 | 트랙 문자열을 상수로 치환 |
| `app/OpicRoot.kt` | 수정 | 문법 연결과 통계 제목에 `displayTitle()` 사용 |
| `feature/grammar/GrammarCatalogTest.kt` | 수정 | 출처 제목과 `displayTitle()` 일치 검증 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트 |
|----|------|--------------|
| AC1 | 진행 중 | 문법 제목 계산은 통합했으나 전체 중복 점검 전 |
| AC2 | 진행 중 | 읽은 문법·앱 범위의 트랙 문자열은 치환, 전체 main 검사 전 |
| AC3 | 미완료 | `OpicRoot.kt` 분리 전 |
| AC4 | 미확인 | 실기기 경로 확인 전 |
| AC5 | 미완료 | 문법 단위 테스트와 앱 release Kotlin 컴파일만 통과 |

## 검증 결과
- `./gradlew :feature:grammar:testDebugUnitTest :app:compileReleaseKotlin --quiet` 통과.
- 이번 TASK에서는 APK 설치나 실기기 확인을 하지 않음.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `OpicRoot.kt`의 교정 카드 장 링크는 기존 표시를 유지하려고 `displayTitle().removePrefix("실전 ")`을 사용함. 통계와 복습 출처에는 전체 제목을 사용함.

## 범위 밖 변경
- 없음.

## 질문
- 없음.

## 중단 지점
- 끝난 것: 문법 장 제목 함수·트랙 상수 추가, 일부 사용처 치환, 관련 테스트와 앱 컴파일, `wip` 커밋.
- 남은 것: 나머지 트랙 문자열 점검, 스피킹·섀도잉 ViewModel 팩토리 이동, `pending*` 요청 타입 통합, 토큰 저장 로직 공유, `OpicRoot.kt` 250줄 이하 분리, 전체 검증과 실기기 경로 확인, 완료 커밋.
- 다음에 이어서 할 첫 단계: `docs/tasks/38-structure-cleanup/TASK.md`와 이 HANDOFF를 확인한 뒤 `OpicRoot.kt` 및 관련 ViewModel 팩토리 구조를 정리.
- 확인 안 된 것: AC1~AC5 완료 여부, 실기기 동작, 전체 테스트·린트·release 빌드.
- 중단 이유: 5시간 사용량이 18%로 떨어져 AGENTS.md의 20% 중단 규칙 적용. 오후 9:31 초기화 예정.

## 이어서 완료 (2026-10-10, Claude — GPT 사용량 부족으로 대신 구현)
- base: `5b3c286` → 이 커밋
- OpicRoot.kt 372 → 240줄 (AC3)
  - `AppDialogs.kt`(모델·백업 다이얼로그, gemmaStatus), `Navigation.kt`(Tab·TabRoot·safeBack), `MenuScreen.kt`의 `MenuRoute`(메뉴 연결·알림 권한)로 분리
- `pending*` 전역 3개 → `TabRequest`(sealed) + `tabRequest` 하나
- `ShadowingViewModel.Factory`, `SpeakingViewModel.Factory` (스피킹은 인자가 13개라 생성 람다를 받는 형태)
- 토큰 저장 공용 함수 `saveHfToken` (core/correction/LlmPreparationScreen.kt), 문법 영작·스피킹 ViewModel이 사용
- AC2: main 코드의 트랙 문자열은 `GrammarTracks` 정의에만 있음 (grep 확인)
- 검증: `./gradlew test lint :app:assembleRelease` 통과. 실기기에서 메뉴 → 스피킹 기록(`menu-to-speaking-history.png`), 메뉴 → 학습 알림 선택 창, 메뉴 → 학습 통계 확인
- 미확인: 교정 카드 → 장 이동(Gemma 필요), 홈 "복습 시작"(복습 대기 0)

## ⚠️ 발견: 기기의 앱 데이터가 초기화되어 있음 (이 TASK와 무관)
- `dumpsys package`: 사용자 0의 `firstInstallTime=2026-10-10 16:52:03` → 16:29 이후 앱이 **삭제 후 새로 설치**되었다 (`install -r`이면 firstInstallTime이 바뀌지 않음)
- 16:24 통계 화면에는 스피킹 4개·문법 오답 3회가 있었으나, 지금은 모두 0이다. 이 시각은 TASK 34 커밋(16:41~16:42) 직후다
- 04192d4의 "토큰 복호화 오류로 앱 종료"도 같은 원인으로 보인다: 앱을 다시 설치하면 Android 자동 백업이 shared_prefs(암호화 토큰 파일)를 복원하지만, Keystore 키는 복원되지 않아 복호화에 실패한다
- 후속 제안: 매니페스트에 `android:allowBackup="false"` 또는 암호화 prefs를 백업에서 제외하는 규칙을 넣는다 (별도 TASK, 사용자 결정)
