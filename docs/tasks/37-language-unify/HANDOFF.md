# HANDOFF: 학습 언어 하나로 통일

> 작성: GPT · 경로: docs/tasks/37-language-unify/HANDOFF.md

## 커밋 범위
- base: `4609beb`
- head: `0949138`
- 리뷰 명령: `git diff 4609beb..0949138`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/common/Language.kt`, `Captions.kt` | 수정 | 현재 언어 정의와 자막 선택 인자 통일 |
| `core/model/WordStatus.kt` | 수정 | 중복 언어 enum 제거 |
| `core/database/WordImporter.kt`, `Entities.kt` | 수정 | 언어 정의에 따른 검증과 기록 언어 명시 |
| `feature/words/*`, `feature/speaking/SpeakingViewModel.kt` | 수정 | ViewModel·DAO에 앱의 현재 언어 전달 |
| `feature/shadowing/ShadowingViewModel.kt`, `CaptionClient.kt`, `ShadowingScreen.kt`, `YouTubePlayer.kt` | 수정 | 자막 요청·기록·플레이어에 현재 언어 전달 |
| `feature/analysis/StatsData.kt`, `app/Reminder.kt` | 수정 | 하루 단어 수 공통 상수 사용 |
| `app/OpicApplication.kt`, `OpicRoot.kt` | 수정 | EN으로 고정된 `currentLanguage`를 한 곳에서 보유·전달 |
| 관련 단위 테스트 | 수정 | 새 언어 설정, 자막 선택, 명시적 DB 언어 검증 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트 |
|----|------|--------------|
| AC1 | ✅ | `rg -n '"en"' app/src/main feature/*/src/main` 결과 없음 |
| AC2 | ✅ | 단어 수 40은 `core/common/Day.kt`의 `WORDS_PER_DAY`로만 정의. 화면 크기 `40.dp`는 별개 |
| AC3 | ✅ | `addedLanguageCanBeImportedFromConfiguredList`: 추가 언어 코드의 단어 적재 |
| AC4 | ✅ | Room 스키마 4번 파일 변경 없음, 해시 `aa969164f50d8474c7bf2aea836dbc1e`. S23+ 덮어 설치 뒤 단어 5,517개와 Day 1 0/40 유지 |
| AC5 | ✅ | `./gradlew test lint :app:assembleRelease --quiet` 통과. S23+에서 다섯 탭 진입 확인 |

## 검증 결과
- 단위 테스트·린트·release 빌드 통과.
- `adb install -r` 성공. 앱 삭제·데이터 삭제·사용자 0 또는 DUAL_APP 95 조작 없음. 플래시카드는 풀지 않음.
- 홈, 단어, 문법, 섀도잉, 스피킹에 차례로 진입했고 각 화면의 기존 제목·첫 콘텐츠와 앱 프로세스 생존을 확인함.
- 확인하지 못한 것: 기기에서 기존 단어 학습 완료 기록이 0개여서 비어 있지 않은 UserWord 행의 전후 비교는 불가. Room 스키마 동일성 및 기존 백업·Migration 테스트로 보완함.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `core/common/Language.kt` — `all`에는 EN만 두고, 과거 중국어 단어 파일 적재 호환을 위해 ZH 정의는 별도로 유지. 새 학습 언어는 `all`에 추가하면 기본 `WordImporter` 설정에 포함됨.
- `core/database/Entities.kt` — `language`의 Kotlin 생성자 기본값만 제거함. `@ColumnInfo(defaultValue)`나 DB 열·인덱스는 바꾸지 않았고 Room 스키마 해시는 그대로임.
- `feature/shadowing/ShadowingViewModel.kt` — 자막과 원문 문장 메모리 캐시 키에 언어를 포함해 다른 언어의 내용을 재사용하지 않음.

## 범위 밖 변경
- 사용자 승인으로 `feature/shadowing/ShadowingScreen.kt`, `YouTubePlayer.kt` 수정. 자막 가로채기까지 현재 언어를 전달하려면 필요함.

## 질문
- 없음.
