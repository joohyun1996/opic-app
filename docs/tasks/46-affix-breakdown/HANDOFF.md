# HANDOFF: 영어 단어 접사 분해 2단계 (작업 중단)

> 작성: GPT · 경로: docs/tasks/46-affix-breakdown/HANDOFF.md

## 커밋 범위
- base: `d96fc82`
- head: `HEAD` (아래 중단 지점의 `wip` 커밋)
- 리뷰 명령: `git diff d96fc82..HEAD`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `exports/affixes.json` | 생성 | 검수 결과 true 426개만 dataVersion 1로 고정 |
| `app/build.gradle.kts`, `OpicApplication.kt` | 수정 | 씨드 내장 및 단어 적재 완료 후 접사 적재 |
| `core/database`의 엔티티·DAO·적재기·Migration·스키마·테스트 | 수정·생성 | v5→v6, 언어별 키 조회, 트랜잭션 적재와 학습 기록 보존 검증 |
| `core/common/Affix.kt`와 테스트 | 생성 | 순서대로 한 줄 분해 문자열 생성 |
| `feature/words`의 학습 화면·ViewModel·관련 단어 화면 | 수정·생성 | 정답 확인 뒤 분해 표시 및 접사·어간 관련 단어 목록 |
| `app/OpicRoot.kt`, 앱 상태 테스트 | 수정·생성 | 관련 단어 경로와 확인 전·후 상태 검증 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트 |
|----|------|--------------|
| AC1·AC2 | ✅ | 1단계 추출 스크립트와 REPORT-1.md 결과 유지 |
| AC3 | ✅ | 씨드와 review.tsv 키 426개 일치, `AffixImporterTest` |
| AC4 | 진행 중 | `AffixStudyStateTest` 통과; 실기기에서 미검수 단어 확인 전·후 비노출 확인, 검수 단어 확인 후는 미확인 |
| AC5 | 자동 테스트 ✅, 실기기 미확인 | `AffixImporterTest`의 언어별 접사·어간 조회 |
| AC6 | ✅ | `MigrationTest`의 v5→v6 기록 보존, `AffixImporterTest`의 재적재 후 기록 보존 |
| AC7 | ✅ | 새 DAO의 언어 조건 및 코드 확인, REPLACE 미사용 |
| AC8 | 재실행 필요 | 전체 명령은 앱 상태 테스트를 추가하기 전에 통과함 |

## 검증 결과
```
./gradlew :core:database:testDebugUnitTest :core:common:test :app:compileDebugKotlin  → 통과
./gradlew test lint :app:assembleRelease                            → 통과 (앱 상태 테스트 추가 전)
./gradlew :app:testDebugUnitTest                                    → 통과 (앱 상태 테스트 추가 후)
git diff --check                                                    → 통과
```

실기기 S23+에 release APK를 `adb install -r`로 덮어 설치하여 성공했다. Day 1 첫 카드 `prostate`에서는 확인 전과 오답 확인 후 모두 분해가 보이지 않았다. Day 1에서 완료한 카드는 1장이고, 두 번째 카드 `dislodge`는 답변을 입력하지 않았다. 기존 앱 데이터 삭제·초기화는 하지 않았다.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `AffixImporter.kt` — 씨드의 `(language, word)`와 어간을 기존 단어 ID로 바꾸고, 접사는 키로 갱신·삽입하며 단어와 UserWord는 읽기만 한다.
- `StudyViewModel.kt` — 확인 뒤에만 분해를 조회하고, 다음 카드로 이동하면 상태에서 즉시 제거한다.
- `AffixWordsScreen.kt` — `WordsApp.kt`는 수정 범위에 없어 CompositionLocal로 앱 경로를 전달한다.

## 범위 밖 변경
- 없음

## 질문
- 없음

## 중단 지점
- 끝난 것: 검수 통과 426개 씨드, DB v6와 Migration, 적재기, 확인 후 화면과 관련 단어 경로, 자동 테스트, release 덮어 설치 및 미검수 단어 비노출 확인.
- 남은 것: 검수된 단어의 실기기 확인 전·후 표시와 관련 단어 화면 확인, 앱 상태 테스트 추가 이후 `./gradlew test lint :app:assembleRelease` 재실행, 최종 커밋과 HANDOFF 완료 표 갱신.
- 다음에 이어서 할 첫 단계: 5시간 사용량 초기화 후 전체 검증을 재실행한다. 그다음 Day별 5장 제한 안에서 검수된 단어의 실기기 표시를 확인한다.
- 확인 안 된 것: 검수된 단어를 누른 뒤 관련 단어 목록이 실기기에 정상 표시되는지.
- 중단 이유: AGENTS.md에 따라 큰 단계 뒤 사용량을 확인했으며 5시간 한도 잔량이 18%였다(초기화 예정: 2026-10-13 오전 1:35).
- 자동 승인 검토: 검수 단어가 나올 때까지 Day 1 학습 화면을 최대 16회 다시 여는 조작은 Day당 5장 제한을 초과할 위험이 있어 거절되었다. 해당 조작은 수행하지 않았다.

## 중단 지점 이어서 (Claude, 2026-10-12 — GPT 사용량 부족으로 대신 수행)
- `./gradlew test lint :app:assembleRelease` 재실행 → 종료 코드 0 (`f43a6f0` 기준)
- release APK `adb install -r` 재설치 (데이터 유지)
- Day 4 영→한: 카드가 섞여 나와 5장(radiate·shameless·artifact·pendulum·prod) 안에 분해 대상이 없음. 비대상 단어는 확인 후에도 분해 미표시 확인
- Day 6 영→한 1장째 `regenerate`(대상):
  - 확인 전: 분해 줄 없음 ✅
  - 확인 후: `re(다시) + generate(생산하다) → 재생하다`와 관련 단어 버튼 2개 표시 ✅
  - `re- 관련 단어`: reconstruct·regenerate·rekindle·readjust·reconstitute·realign 등 목록 ✅ / `generate 관련 단어`: regenerate ✅ / 뒤로 가기 시 같은 카드로 복귀 ✅
- Day별 5장 제한 준수 (Day 4 5장, Day 6 1장). 확인한 카드는 입력 `x`로 오답 처리됨
