# HANDOFF: 섀도잉 2 — 비공식 영어 자막 가져오기

> 작성: GPT · 경로: docs/tasks/16-shadowing-captions/HANDOFF.md

## 커밋 범위
- base: `97f24c7` (시작 전 `git rev-parse --short HEAD`)
- head: `ad63bd8` (구현 커밋, HANDOFF는 별도 커밋)
- 리뷰 명령: `git diff 97f24c7..ad63bd8`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/common/.../Captions.kt` | 생성 | 영어 트랙 선택, HTML·json3 파싱, 문장 합치기, 현재 문장 이진 탐색 |
| `core/common/.../CaptionsTest.kt`, `src/test/resources/captions/**` | 생성 | 개인정보·쿠키 없는 최소 응답 형태 고정 데이터와 순수 함수 테스트 |
| `feature/shadowing/.../CaptionClient.kt` | 생성 | 공개 YouTube 페이지·자막 요청, 크기·시간 제한, 취소 시 연결 해제 |
| `feature/shadowing/.../CaptionList.kt` | 생성 | 높이 제한 내부 스크롤 목록, 현재 문장 강조, 이전·다음 문장 |
| `feature/shadowing/.../ShadowingViewModel.kt` | 수정 | 영상별 자막 메모리 캐시, 요청 상태·재시도·취소와 오래된 응답 무시 |
| `feature/shadowing/.../ShadowingScreen.kt` | 수정 | 자막 선택으로 A-B·원문·이동·반복 연결, 화면 이탈 시 요청 취소 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트 |
|----|------|--------------|
| AC1 | ✅ | `CaptionsTest.trackPriority`: 사람이 만든 en → en-GB → asr en, 다른 언어·빈 목록은 없음 |
| AC2 | ✅ | `tracksFromHtml`: 고정 HTML의 트랙 2개, JSON 이스케이프·HTML 엔티티로 표현된 & 복원, 자막 없는 HTML과 잘린 JSON |
| AC3 | ✅ | `jsonTimingTextAndEmptyEvents`: 시각·세그먼트 결합·공백·엔티티 복원, 빈 이벤트와 잘못된 응답 제거 |
| AC4 | ✅ | `sentenceMergingAndLimit`: 조각 3개를 한 문장으로 합침, 12초 초과 전 분리, 기존 문장 단위 유지 |
| AC5 | ✅ | `currentCueBoundaries`: 첫 문장 전·시작·연속 경계·빈 구간·마지막 끝·빈 목록 |
| AC6 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet` 종료 코드 0. 테스트는 자원 파일·메모리 값만 사용하며 네트워크 호출 없음 |
| AC7 | 보류 — 사용자가 나중에 한꺼번에 확인 | 아래 확인 순서 참고. 폰 설치·실기기 확인은 하지 않음 |

## 검증 결과
```
./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet → 통과
CaptionsTest JUnit XML                              → 5 tests, 0 failures, 0 errors
git diff --cached --check                           → 출력 없음
git diff 97f24c7 -- core/stt app/build.gradle.kts settings.gradle.kts → 출력 없음
gradle/gradle-daemon-jvm.properties                  → 없음
```

## 설계 판단 / 리뷰어가 봐야 할 곳
- `feature/shadowing/.../CaptionClient.kt` — `HttpURLConnection` 사용. 고정 데스크톱 User-Agent와 `Accept-Language: en-US,en;q=0.9`, HTML/JSON별 Accept 헤더를 보낸다. 앱이 수집한 쿠키·로그인 정보·API 키·녹음·원문·교정 결과는 요청에 포함하지 않는다. 트랙의 baseUrl 매개변수는 공개 페이지에서 받은 값만 사용한다.
- 같은 파일 — 연결·읽기 시간 제한은 각각 10초, 각 응답은 5MiB(5×1024×1024바이트) 상한이다. Content-Length 사전 검사와 스트림 누적 크기 검사를 모두 한다. 이동은 최대 4회 요청 안에서만 처리하며 HTTPS YouTube 호스트와 `/watch` 또는 `/api/timedtext` 경로만 허용한다.
- 같은 파일 — 빈 응답·트랙 없음·파싱 실패는 자막 없음으로, HTTP·통신·크기 제한 오류는 실패로 표시한다. 양쪽 모두 붙여 넣기 안내와 다시 시도 버튼을 제공한다. 요청 취소 시 연결을 끊으며 예외·응답 내용은 로그에 남기지 않는다.
- `core/common/.../Captions.kt` — 순수 Kotlin 모듈은 이미 설치된 kotlinx-serialization으로 JSON을 읽는다. Android 요청 경계에서는 `org.json`으로 JSON 형태를 확인한다. 새 의존성은 추가하지 않았다. 문자열 속 괄호와 이스케이프를 고려해 captionTracks 배열을 추출한다.
- 같은 파일 — cue는 시작 포함·끝 제외로 찾는다. 다음 조각을 합치면 12초를 넘는 경우 이전 조각까지 먼저 확정한다. 원래부터 12초를 넘는 단일 cue는 텍스트와 시각을 임의 분할하지 않는다.
- `feature/shadowing/.../ShadowingViewModel.kt` — 성공·빈 목록은 프로세스 메모리에 영상별로 기억한다. 실패는 사용자 재시도 전 자동 반복하지 않는다. 영상 변경·화면 이탈 때 요청 번호를 올리고 작업을 취소해 이전 응답이 현재 화면을 덮지 못하게 했다.
- `feature/shadowing/.../ShadowingScreen.kt`, `CaptionList.kt` — 현재 재생 문장과 사용자가 선택한 반복 문장은 별개다. 이전·다음 버튼은 선택한 문장 기준으로 이동한다. 선택 시 시작−0.3초(0 이상), 끝+0.3초로 설정하며 기존 A-B·속도·녹음·단어 비교 코드 흐름은 유지했다.
- 고정 데이터는 응답 형식에서 필요한 필드만 남기고 예시 문구로 구성했다. 실시간 YouTube 응답의 접근 성공률은 이번 검증에 포함하지 않는다.

### AC7 확인 순서 (사용자 수행 예정)
1. 자막 있는 영상 하나를 열어 받는 중 → 문장 수·목록을 확인한다. 현재 재생 문장 강조와 목록 내부 스크롤을 확인한다.
2. 문장을 눌러 원문, 시작·끝 앞뒤 0.3초, 이동·반복 켬을 확인하고 이전·다음 문장 버튼으로 이동한다.
3. 같은 영상을 다시 열어 메모리 캐시를 확인하고, 받는 중 다른 영상으로 바꾸거나 홈으로 나가 이전 응답이 표시되지 않는지 확인한다.
4. 자막 없는 영상 하나를 열어 안내와 다시 시도를 확인한다. 원문 붙여 넣기, 수동 A-B 반복·속도·녹음·단어 비교가 계속 동작하는지 확인한다.
5. 자막이 있는 영상에서도 YouTube가 빈 응답을 반환할 수 있으므로 자막 없음 안내가 나온 경우 이를 기록한다.

## 범위 밖 변경
- 없음. Room·Migration·권한·의존성·Whisper CMake 설정은 변경하지 않았다.

## 질문
- 없음. 실기기 및 실제 YouTube 응답 확인은 요청에 따라 보류했다.
