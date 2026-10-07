# HANDOFF: 문법 단원 1~3과 연습 화면

> 작성: GPT · 경로: docs/tasks/10-grammar-units/HANDOFF.md

## 커밋 범위
- base: `6cc1431` (작업 시작 시 기록)
- head: `645f63f`
- 리뷰 명령: `git diff e59c125..645f63f`
- 작업 중 다른 에이전트가 TASK 11 문서 커밋 `e59c125`를 추가했다. `6cc1431..645f63f`에는 해당 문서도 포함되므로 위 범위가 TASK 10 구현만 표시한다.

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `exports/grammar.json`, `CONTENT-REVIEW.md` | 생성 | 30문제 전수 검토 후 7문제 수정 기록 및 확정 데이터 |
| `core/common/.../GrammarGrading.kt` + 테스트 | 생성 | 문장 정규화·채점과 두 번의 시도·결과 집계 |
| `feature/grammar/**` | 생성 | 데이터 검증, ViewModel, 단원 목록·설명·문제·결과 화면 및 테스트 |
| `settings.gradle.kts`, `app/build.gradle.kts` | 수정 | 모듈 등록과 grammar.json assets 포함 |
| `feature/words/.../WordsApp.kt`, `feature/words/build.gradle.kts` | 수정 | 홈의 문법 카드·단원 수·문법 경로 연결 |
| `docs/tasks/10-grammar-units/*.png` | 생성 | S23+ 설명·1차 오답·2차 오답 화면 증빙 3장 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트·명령 |
|----|------|------------------|
| AC1 | ✅ | `rg -c '^\\| u[1-3]-[0-9]{2} \\|' docs/tasks/10-grammar-units/CONTENT-REVIEW.md` → `30` |
| AC2 | ✅ | `GrammarCatalogTest.catalogHasThirtyValidExercises`: 단원 3·문제 30·고유 ID 33·choice 범위 검사 |
| AC3 | ✅ | `GrammarGradingTest.normalizationAndTextGrading`: 곡선 따옴표·공백·대소문자·문장부호·오답 |
| AC4 | ✅ | `GrammarGradingTest.choiceAndTwoAttempts`, `GrammarViewModelTest`: 힌트 → 재시도 → 정답 공개·결과 집계 |
| AC5 | ✅ | `GrammarCatalogTest.invalidDataReturnsFailure`: 중복 ID·범위 밖 answer·깨진 JSON → Failed; `GrammarViewModelTest.failedCatalogRemainsSafeState` |
| AC6 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 성공; `unzip -l app/build/outputs/apk/release/app-release-unsigned.apk | rg 'assets/grammar.json'` → 20,878바이트 |
| AC7 | ✅ | S23+ 설치 성공. 단원 1에서 2문제만 채점. 설명·1차 오답 힌트·2차 오답 정답 공개 스크린샷 저장. 결과 화면은 사용자 승인에 따라 10문제 상태 테스트로 검증 |

## 검증 결과
```
./gradlew test lint                         → BUILD SUCCESSFUL, 30 tests, 0 failures, 0 errors
./gradlew :app:assembleDebug :app:assembleRelease → BUILD SUCCESSFUL
릴리스 APK assets/grammar.json             → 포함, 20,878바이트
adb install -r app/build/outputs/apk/debug/app-debug.apk → Success
S23+                                       → 단원 1 설명·힌트·정답 공개 확인, 키보드 위 확인 버튼 표시 확인
git diff --cached --check                  → 출력 없음
```

실기기 증빙: [단원 1 설명](unit1-explanation.png), [1차 오답 힌트](first-wrong-hint.png), [2차 오답 정답 공개](second-wrong-answer.png). 단원별 최대 5문제 제한을 지키기 위해 결과 화면을 실기기에서 열지 않았다. 결과 화면 진입과 오답 목록은 `GrammarViewModelTest`의 10문제 완료 테스트로 검증했다.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `feature/grammar/.../GrammarCatalog.kt` — 엄격한 필드 파싱과 문제 종류별 필수 값 검사 후 실패를 `Failed`로 바꿔 앱이 종료되지 않게 했다.
- `feature/grammar/.../GrammarViewModel.kt` — 문제를 시작·재시작할 때 섞고, 첫 오답은 힌트만 표시하며 둘째 오답에서 점수를 확정한다. 진행 기록은 저장하지 않는다.
- `feature/words/.../WordsApp.kt` — 기존 내비게이션 소유 위치에 문법 경로를 추가했다. 앱 내비게이션 구조를 옮길 필요가 없어 기존 경로를 유지했다.
- `feature/grammar/.../GrammarExerciseScreen.kt` — `imePadding()`으로 키보드가 확인 버튼을 가리지 않게 했다. S23+ 화면에서 확인했다.

## 범위 밖 변경
- `feature/words/build.gradle.kts` — 문법 모듈을 `WordsApp.kt`에서 사용하기 위한 의존성 한 줄. 사용자에게 사전 허락을 받았다.

## 질문
- 없음
