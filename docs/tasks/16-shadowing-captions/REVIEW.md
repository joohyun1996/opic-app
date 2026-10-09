# REVIEW: 섀도잉 2 — 비공식 자막 가져오기

> 작성: Claude · 경로: docs/tasks/16-shadowing-captions/REVIEW.md

## 1차 리뷰 — 2026-10-09
- 범위: `97f24c7..42f7ab7` (구현 `ad63bd8`, HANDOFF `42f7ab7`)
- 판정: **Approve** (Must-fix 0)
- AC7(실기기)는 사용자 요청으로 보류 — 코드·빌드 기준

### 확인한 것
- 데이터·권한: Room·Manifest·의존성 변경 없음 (`core/common`은 기존 kotlinx-serialization 사용)
- 네트워크 (`CaptionClient.kt`): https + `www.youtube.com`/`youtube.com` + 경로 `/watch`·`/api/timedtext`만 허용, 이동 직접 처리(최대 4회), 시간 제한 10초, 5MiB 상한(Content-Length + 누적), 쿠키·계정 없음. 영상 ID 정규식 재검사
- 취소·경쟁: 요청 번호 + `videoId` 비교로 늦은 응답 버림, 영상 변경·화면 이탈·`onCleared` 취소, 취소 시 연결 끊음
- 실패: 모든 예외 → NONE/FAILED + "다시 시도", 붙여 넣기 유지. TASK 15 흐름(A-B·속도·녹음·비교) 변경 없음
- 순수 함수: `pickTrack` 우선순위, `extractCaptionTracks` 문자열 안 괄호·이스케이프 처리, `cueAt` 이진 탐색(시작 포함·끝 제외), `mergeSentences` 12초 상한 — 테스트로 확인

### Must-fix / Should-fix
- 없음

### Nit
- **N1** `CaptionClient.kt` — 유럽 등에서 `consent.youtube.com`으로 이동하면 호스트 검사에서 FAILED. 한국에서는 해당 없음, 실기기에서 실패가 잦으면 그때 본다
- **N2** 문장 선택 시 B = 끝 + 0.3초라 다음 문장 첫소리가 조금 들릴 수 있다. 실기기에서 거슬리면 0.1~0.2초로
- **N3** `ShadowingScreen.kt`의 `LaunchedEffect(state.videoId)`와 `open()`이 둘 다 `loadCaptions()`를 부른다 (두 번째는 진행 중이라 무시되어 무해). 한쪽만 남기면 읽기 쉽다
