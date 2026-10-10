# HANDOFF: 속도 개선 — 불필요한 재구성·목록·시작 읽기

> 작성: GPT · 경로: docs/tasks/36-speed-quick/HANDOFF.md

## 커밋 범위
- base: `274eaeb`
- head: `f9f85a4`
- 리뷰 명령: `git diff 274eaeb..f9f85a4`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `app/src/main/java/com/jooh/opic/OpicRoot.kt` | 수정 | 문법 연결·통계 제목을 문법 데이터가 바뀔 때만 구성 |
| `app/src/main/java/com/jooh/opic/OpicApplication.kt` | 수정 | 단어 자산을 스트림으로 전달 |
| `core/database/src/main/java/com/jooh/opic/core/database/WordImporter.kt` | 수정 | 앞부분 4KB에서 버전을 확인하고 동일 버전이면 전체 읽기 생략 |
| `core/database/src/test/java/com/jooh/opic/core/database/WordImporterTest.kt` | 수정 | 동일 버전의 부분 읽기와 학습 기록 보존 검증 |
| `feature/shadowing/src/main/java/com/jooh/opic/feature/shadowing/ShadowingScreen.kt` | 수정 | 추천 영상 첫 화면 전체를 `LazyColumn`으로 구성 |
| `feature/speaking/src/main/java/com/jooh/opic/feature/speaking/SpeakingScreen.kt` | 수정 | 주제 첫 화면 전체를 `LazyColumn`으로 구성 |
| `shadowing-recommendations.png`, `speaking-topics.png` | 생성 | S23+ 실기기 화면 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트 |
|----|------|--------------|
| AC1 | ✅ | `sameVersionStreamReadsOnlyPrefixAndKeepsProgress`: 동일 버전 `UpToDate`, Word·UserWord 유지 |
| AC2 | ✅ | `upgradePreservesIdentitySequenceAndProgress` 등 기존 적재 테스트 |
| AC3 | ✅ | S23+에서 추천 영상·주제 첫 화면 문구와 카드 순서 확인, 스크린샷 2장 |
| AC4 | ✅ | `./gradlew test lint :app:assembleRelease --quiet` 통과 |

## 검증 결과
- 전체 단위 테스트, 린트, release 빌드 통과.
- S23+ (`SM_S916N`)에 release APK를 `adb install -r`로 덮어 설치. 기존 앱 데이터와 일반 사용자·DUAL_APP 데이터를 삭제하거나 초기화하지 않음.
- 섀도잉은 추천 영상 100개, 분류 순서와 첫 영상 카드들이 표시됨. 스피킹은 50주제 164문항, 자기소개 다음 설문 주제와 첫 카드들이 표시됨.
- 확인하지 못한 것: 화면 밖의 모든 카드 150개를 개별로 대조하지는 않음. 목록은 기존 데이터의 필터·순회 순서를 유지함.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `WordImporter.kt` — 처음 4KB에서 버전을 못 찾으면 스트림을 되돌린 뒤 기존 전체 파싱 경로로 들어감. 버전이 같을 때만 전체 읽기를 생략함.
- `ShadowingScreen.kt`, `SpeakingScreen.kt` — 스크롤 컨테이너를 첫 화면에서 하나의 `LazyColumn`으로 바꾸고 머리글을 `item`으로 넣어 중첩 스크롤을 피함.

## 범위 밖 변경
- 없음.

## 질문
- 없음.
