# TASK: 섀도잉 2 — 비공식 자막 가져오기 (보조 기능)

> 작성: Claude · 승인: [x] 사용자 (2026-10-09) · 구현: GPT
> 경로: docs/tasks/16-shadowing-captions/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 섀도잉 — "비공식 자막 가져오기는 보조 기능. 실패하면 붙여 넣기로 돌아간다"

## 목표
영상을 열면 YouTube 자막(영어)을 비공식 경로로 받아 문장 목록으로 보여 준다. 문장을 누르면 그 문장 시각으로 A-B 구간과 원문이 채워진다. **자막을 못 받아도 TASK 15 기능(직접 붙여 넣기)은 그대로 동작해야 한다.**

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/common/.../Captions.kt` + 테스트 | 생성 | 자막 트랙 고르기, json3 파싱, 문장 합치기, 현재 문장 찾기 (순수 함수) |
| `core/common/src/test/resources/captions/**` | 생성 | 테스트용 고정 데이터 (실제 응답을 줄인 것, 개인정보·쿠키 없음) |
| `feature/shadowing/**` | 수정 | 자막 받기(네트워크), 자막 목록 UI, 문장 누르면 A-B·원문 채우기, 현재 문장 강조 |

## 관련 파일 (읽기만)
- `feature/shadowing/.../ShadowingScreen.kt`, `ShadowingViewModel.kt` — TASK 15 화면·상태
- `core/common/.../Shadowing.kt` — `youtubeVideoId`

## 요구사항
### 자막 받기 (`feature/shadowing`, 네트워크)
1. 영상을 열면 자동으로 한 번 시도 (상태: 받는 중 / N문장 / 자막 없음 / 실패 + "다시 시도")
2. 경로: `https://www.youtube.com/watch?v=ID` HTML에서 `ytInitialPlayerResponse`의 `captions.playerCaptionsTracklistRenderer.captionTracks`를 찾는다 → 고른 트랙의 `baseUrl` + `&fmt=json3` 를 받는다
   - 외부 라이브러리 추가 금지 (`HttpURLConnection` + `org.json`). 연결·읽기 시간 제한 10초, 응답 크기 상한 5MB
   - 쿠키·로그인·API 키를 쓰지 않는다. 이 영상 ID 외의 정보를 보내지 않는다
   - 응답이 비었거나(최근 YouTube는 토큰 없으면 빈 응답을 줄 수 있음) 파싱 실패 → "자막 없음 — 문장을 붙여 넣으세요"
3. 받은 자막은 앱 실행 중 영상별로 메모리에 기억 (저장 안 함)

### 순수 함수 (`core/common/Captions.kt`)
- `pickTrack(tracks: List<CaptionTrack>): CaptionTrack?` — 우선순위: 사람이 만든 `en` → `en-*` → 자동 생성(`kind == "asr"`) `en` → null. `CaptionTrack(baseUrl, languageCode, kind)`
- `extractCaptionTracks(html: String): List<CaptionTrack>` — HTML에서 `captionTracks` JSON 배열을 꺼낸다. `&` 처리. 없으면 빈 목록
- `parseJson3(json: String): List<Cue>` — `events[].tStartMs`, `dDurationMs`, `segs[].utf8`를 이어 붙인 `Cue(startMs, endMs, text)`. 빈 텍스트·`\n`만 있는 이벤트 제거, 공백 정리, HTML 엔티티(`&amp;` `&#39;` `&quot;`) 풀기
- `mergeSentences(cues: List<Cue>, maxMs: Long = 12_000): List<Cue>` — 자동 자막처럼 조각난 cue를 `.?!`로 끝날 때까지 합친다. 합친 길이가 `maxMs`를 넘으면 그 자리에서 끊는다
- `cueAt(cues: List<Cue>, positionMs: Long): Int?` — 현재 재생 위치의 문장 인덱스 (이진 탐색)

### 화면
- 원문 입력칸 위에 "자막" 영역: 문장 목록(시각 `m:ss` + 문장), 높이 제한 + 내부 스크롤, 현재 문장 강조
- 문장 누르기 → A = 시작 − 0.3초(0 미만이면 0), B = 끝 + 0.3초, 원문 칸 = 그 문장, 그 시각으로 이동, 반복 켬
- "이전 문장 / 다음 문장" 버튼 (반복 중인 구간 기준)
- 자막이 없으면 영역 대신 안내 한 줄. 붙여 넣기 기능은 그대로

## 수용 기준
- [ ] AC1: `pickTrack` — 사람 en > en-GB > asr en > 없음 순서 (테스트)
- [ ] AC2: `extractCaptionTracks` — 고정 HTML에서 트랙 2개(baseUrl의 `&`이 `&`로 풀림), captions 없는 HTML은 빈 목록 (테스트)
- [ ] AC3: `parseJson3` — 고정 json3에서 cue 시각·텍스트, 빈 이벤트 제거, 엔티티 풀기 (테스트)
- [ ] AC4: `mergeSentences` — 조각 3개 + 마침표 → 1문장, 12초 넘으면 끊김, 이미 문장 단위면 그대로 (테스트)
- [ ] AC5: `cueAt` — 첫 문장 전 null, 경계값, 문장 사이 빈 구간 null, 마지막 문장 뒤 null (테스트)
- [ ] AC6: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과. 단위 테스트는 네트워크를 쓰지 않는다
- [ ] AC7: 실기기 확인 — **보류 (사용자가 나중에 한꺼번에)**. HANDOFF에 확인 순서만 적는다: 자막 있는 영상 1개 / 자막 없는 영상 1개

## 제약 / 주의
- 권한·Room·Migration 변경 없음. 새 의존성 없음
- 네트워크는 YouTube 페이지·자막 요청만 (AGENTS.md 허용 범위). 녹음·받아 적기 결과를 보내지 않는다
- 네트워크·파싱은 IO 스레드. 영상을 바꾸거나 화면을 나가면 진행 중인 요청 취소
- 실패는 앱을 멈추지 않는다 — 모든 예외는 "자막 없음/실패" 상태로
- TASK 15 동작(A-B·속도·녹음·비교)을 바꾸지 않는다
- 이번에도 폰에 설치하지 않는다

## 범위 밖 (하지 말 것)
- 자막 번역·한국어 자막, 자막 저장, 다른 언어
- yt-dlp류 서명 해독, PO 토큰 생성, 로그인 쿠키
- 공식 YouTube Data API
