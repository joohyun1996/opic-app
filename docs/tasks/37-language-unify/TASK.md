# TASK: 학습 언어 하나로 통일 (스페인어 준비)

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "태스크로 잡고 지피티에 넘기자") · 구현: GPT

## 목표
코드에 직접 들어간 `"en"`과 언어 정의 중복을 없앤다. 앱이 "현재 학습 언어"를 하나 들고 각 화면에 넘기게 한다. 스페인어를 추가할 때 `StudyLanguages.all`에 한 줄만 더하면 되게 한다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/model/.../WordStatus.kt` | 수정 | `enum Language` 제거, 사용처는 `StudyLanguages`로 |
| `core/common/.../Language.kt` | 수정 | 필요하면 ZH 등 정의 추가 (all에는 EN만) |
| `core/database/.../WordImporter.kt` | 수정 | `setOf("en","zh")` → `StudyLanguages.codes` 기반. 영어 필수 필드 검사는 언어 설정값으로 |
| `core/database/.../Entities.kt` | 수정 | SpeakingAnswer·ShadowingAttempt의 `language = "en"` 기본값 제거 (스키마는 그대로, 기본값은 Kotlin에만 있음 — Migration 불필요인지 확인하고 HANDOFF에 적기) |
| `feature/words/*` (StudyViewModel, WordsViewModel, WrongScreen, DayScreen) | 수정 | language를 생성자·인자로 받기 |
| `feature/speaking/.../SpeakingViewModel.kt` | 수정 | `LANGUAGE` 상수 → 생성자 인자 |
| `feature/shadowing/.../ShadowingViewModel.kt`, `CaptionClient.kt`, `core/common/.../Captions.kt` | 수정 | 자막 언어를 인자로 |
| `feature/analysis/.../StatsData.kt`, `app/.../Reminder.kt` | 수정 | `40` → `WORDS_PER_DAY` 상수 (core/common Day.kt로 옮겨 공용) |
| `app/.../OpicApplication.kt`, `OpicRoot.kt` | 수정 | `currentLanguage`(지금은 EN 고정) 하나를 만들어 모든 ViewModel·DAO 호출에 전달 |
| 관련 테스트 | 수정 | |

## 수용 기준
- [ ] AC1: `app/`, `feature/`의 main 코드에 `"en"` 문자열이 없다 (grep 결과를 HANDOFF에 적기; 테스트 코드와 Language.kt는 예외)
- [ ] AC2: 하루 단어 수 40은 한 곳(core/common)에만 정의되어 있다
- [ ] AC3: `StudyLanguages.all`에 언어를 더해도 WordImporter가 그 언어 단어를 받는다 (테스트, 가짜 언어 코드)
- [ ] AC4: 기존 DB에서 앱을 업데이트하면 학습 기록이 그대로 남는다. 스키마 해시가 바뀌면 멈추고 사용자에게 묻는다
- [ ] AC5: `./gradlew test lint :app:assembleRelease` 통과, 실기기에서 다섯 탭 진입 확인

## 범위 밖
- 언어 선택 UI, 스페인어 데이터
