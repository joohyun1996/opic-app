# HANDOFF: 문법 직접 쓰기와 기기 내 문장 교정

> 작성: GPT · 경로: docs/tasks/11-grammar-writing/HANDOFF.md

## 커밋 범위
- base: `93880bd` (시작 전 `git rev-parse --short HEAD`)
- head: `8efdf8e` (구현 커밋, HANDOFF는 별도 커밋)
- 리뷰 명령: `git diff 93880bd..8efdf8e`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/common/.../Writing.kt` + 테스트 | 생성 | 약어를 보존하는 문장 분리, 첫 5문장 제한, 오류 유형 7개 고정 설명 |
| `feature/grammar/**` | 수정/생성 | 모델 준비·토큰 입력·쓰기·문장별 교정 결과·재시도·취소 화면과 가짜 엔진 테스트 |
| `app/.../OpicApplication.kt`, `OpicRoot.kt`, `MainActivity.kt` | 수정/생성 | 문법 JSON 백그라운드 1회 파싱, 앱 소유 내비게이션, LLM 엔진·토큰 저장소 전달 |
| `feature/words/.../WordsApp.kt`, `build.gradle.kts` | 수정 | 단어 화면을 경로별로 분리하고 문법 모듈 의존 제거 |
| `app/build.gradle.kts`, `feature/grammar/build.gradle.kts` | 수정 | 기존 내비게이션·코루틴 라이브러리와 `core:llm`을 필요한 모듈에 연결 |
| `docs/tasks/11-grammar-writing/*.png` | 생성 | S23+ 모델 없음·준비됨·단원 목록 상태·교정 결과 증빙 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트·명령 |
|----|------|------------------|
| AC1 | ✅ | `WritingTest.keepsAbbreviationsAndDropsEmptySentences`, `takesOnlyFirstFive`: 약어 보존, 빈 문장 제거, 6개 중 첫 5개 |
| AC2 | ✅ | `WritingTest.guidesCoverKnownAndUnknownTypes`: 7개 문구와 미지 유형의 기타 대체 |
| AC3 | ✅ | `CorrectionCoordinatorTest.emitsThreeResultsInSentenceOrder`: 가짜 엔진으로 맞음 → 틀림 → 잘못된 JSON, 입력 순서대로 콜백 3회 |
| AC4 | ✅ | `CorrectionCoordinatorTest.cancellationPreventsLaterGenerateCalls`: 첫 `generate` 대기 중 취소, 호출 수 1회 |
| AC5 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet` 성공; 릴리스 APK DEX에 `LlmBenchScreen` 문자열 없음; `feature/words/build.gradle.kts`의 `:feature:grammar` 참조 0개 |
| AC6 | ✅ | S23+에 앱 데이터를 유지하며 덮어 설치. 모델 없음·준비됨 상태와 단원 2의 3문장 교정 결과 확인. 아래 문장별 시간·스크린샷 참고 |

## 검증 결과
```
./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet → 통과
JUnit XML 합계                                              → 35 tests, 0 failures, 0 errors
릴리스 APK의 LlmBenchScreen 문자열 검사                     → False
릴리스 APK의 assets/grammar.json 검사                      → True
rg -n 'feature:grammar' feature/words/build.gradle.kts      → 0줄
rg -n 'NavHost' app/src/main feature/words/src/main           → app/OpicRoot.kt만 표시
git diff --cached --check                                    → 출력 없음
adb install -r app/build/outputs/apk/debug/app-debug.apk     → Success (앱 데이터 유지)
adb run-as com.jooh.opic 모델 크기 확인                       → 4,405,655,031바이트
```

S23+에는 OPIC 앱 모델이 없었고 머니로그 앱의 모델 원본이 있었다. 머니로그 데이터는 읽기만 하면서 ADB 스트림으로 `com.jooh.opic/no_backup/llm/`에 복사했다. **다운로드 버튼을 누르지 않았고 앱 데이터를 지우지 않았다.** 복사 전후 원본·대상 크기는 모두 4,405,655,031바이트였다.

단원 2는 `writingTask.minSentences = 3`이므로 TASK의 2문장 예시 대신 사용자 요청의 2~3문장 범위 안에서 3문장을 한 번 교정했다. 맞는 문장 2개와 과거 시제를 틀린 문장 1개였고, 결과는 맞음·맞음·틀림(`visit → visited`, 유형 `tense`) 순이었다.

| 문장 | 결과 | 걸린 시간 |
|------|------|----------:|
| `Last summer, I went to Jeju with my family.` | 맞음 | 11,599ms |
| `We stayed at a small hotel near the beach.` | 맞음 | 12,451ms |
| `I visit a museum yesterday.` | 틀림 → `I visited a museum yesterday.` | 17,922ms |

스크린샷: [모델 없음](preparation-model-missing.png), [준비됨](preparation-ready.png), [단원 목록 AI 상태](unit-list-ai-ready.png), [단원 2 교정 결과](unit2-correction-results.png). 스크린샷은 마지막 상태 표시줄 여백 수정 직전에 찍었다. 최종 APK에서는 문법 목록의 뒤로 가기 버튼이 `y=157~200`에 표시되고 단어 Day 목록도 열리는 것을 다시 확인했다. 추가 AI 교정은 하지 않았다.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `feature/grammar/.../GrammarWritingViewModel.kt:44` — 모델 파일 크기가 정확히 맞을 때만 기존 파일 로딩을 자동 시도한다. 파일이 없으면 다운로드 버튼을 누를 때만 `ensureModelReady()`를 호출한다.
- `feature/grammar/.../GrammarWritingViewModel.kt:75` — 문장을 최대 5개로 제한하고 `Dispatchers.IO`에서 순서대로 교정한다. 이탈·취소·화면 회전 시 작업을 취소한다.
- `feature/grammar/.../CorrectionResultCard.kt:30` — 모델 설명은 기본 접힌 보조 정보로 두고, 오류 유형별 고정 설명을 먼저 보여 준다.
- `app/src/main/java/com/jooh/opic/OpicApplication.kt:41` — 문법 JSON은 앱 시작 시 백그라운드에서 한 번 읽고 파싱한다. Compose 안에서 다시 파싱하지 않는다.
- `app/src/main/java/com/jooh/opic/OpicRoot.kt:45` — `NavHost`를 앱으로 옮겨 단어 모듈이 문법 모듈에 의존하지 않는다.
- `feature/grammar/.../GrammarExerciseScreen.kt:33` — 선택한 보기의 보라색을 단어 화면의 검정 글자·연한 회색 배경으로 바꿨다.
- `core/common/.../Writing.kt:26` — 시제 설명의 끝을 `be + -ing를 써요`로 자연스럽게 고쳤다. `word_order`에서는 괄호 속 조동사 예시를 덜어 문장을 짧게 했다. 나머지 고정 문구는 TASK 초안과 같다.
- TASK 10에서 단원별 최대 5문제를 지시했으나 TASK 11 쓰기 화면은 결과 화면 뒤에 있다. 자동 승인 검토가 10문제 진행을 처음 거절했고, 사용자에게 사유를 설명한 뒤 **이번 TASK의 단원 2만 10문제 진행 허락**을 받았다. APK 화면 수정을 확인하려고 단원 2 연습은 두 차례 완료했으며, 실제 LLM 교정은 3문장 한 차례만 실행했다.

## 범위 밖 변경
- 없음. 앱 경로 파일과 `feature/words/**` 수정은 TASK §6에서 승인됐다. 외부 라이브러리의 새 버전이나 권한은 추가하지 않았다.

## 질문
- 없음
