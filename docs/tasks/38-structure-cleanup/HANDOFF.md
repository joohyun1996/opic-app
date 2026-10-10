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
