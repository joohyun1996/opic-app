# REVIEW: 섀도잉 1 — YouTube 재생 + 구간 반복 + 따라 말하기 비교

> 작성: Claude · 경로: docs/tasks/15-shadowing-player/REVIEW.md

## 1차 리뷰 — 2026-10-09
- 범위: `2eea1f6..e9e9d96` (구현 `0ab0bb9`, HANDOFF `e9e9d96`)
- 판정: **Request changes** (Must-fix 1)
- AC4(실기기)는 사용자 요청으로 보류 — 코드·빌드 기준 리뷰

### 확인한 것 (문제 없음)
- 데이터 안전: Room·Migration 변경 없음. 녹음은 `filesDir/shadowing-last.pcm` 1개, 원문은 메모리만
- 권한·네트워크: Manifest 그대로(`INTERNET`, `RECORD_AUDIO`). 네트워크는 IFrame API·모델 다운로드만
- WebView: 파일·콘텐츠 접근 끔, 메인 프레임 이동 차단(`YouTubePlayer.kt` `shouldOverrideUrlLoading`), 브리지는 값 수신 2개 + nonce 확인, 영상 ID는 `JSONObject.quote`로 넣어 스크립트 주입 없음
- `compareWords` 역추적: Match 우선 → 치환 → 삭제 → 삽입, 편집 수 = WER 분자 (테스트로 확인)
- JNI abort: `whisper_full` 호출 스레드에서 콜백 → 이미 붙은 스레드, 전역 참조 해제 있음
- CMake Release 강제·`GGML_CPU_ARM_ARCH` 유지, `gradle-daemon-jvm.properties` 없음

### Must-fix
- **M1** `app/src/main/java/com/jooh/opic/OpicRoot.kt` `composable("shadowing")` — 시스템 바 패딩이 없다. `MainActivity`가 `enableEdgeToEdge`라서 "← 홈" 버튼과 제목이 상태 표시줄 아래로 들어간다 (단어 탭은 `Scaffold`, 문법 경로는 `statusBarsPadding().navigationBarsPadding()`을 씀). 문법 경로처럼 `Box(...statusBarsPadding().navigationBarsPadding())` + `widthIn(max = 430.dp)`로 감싼다

### Should-fix
- **S1** `ShadowingViewModel.kt` `onCleared()` — `recorder?.stop()`을 메인 스레드에서 부르는데, 같은 recorder를 IO 스레드가 `stop()`·`release()`한다. 녹음 중 화면을 나가면 `IllegalStateException`(release 뒤 stop)로 앱이 죽을 수 있다. `onCleared`에서는 `stopRecording()` + `recordingJob?.cancel()`만 하고 정리는 IO 쪽 `finally`에 맡긴다
- **S2** `ShadowingViewModel.startRecording()` — `recording = true`가 IO 코루틴 안에서 켜지므로 "● 녹음"을 빠르게 두 번 누르면 녹음 Job이 둘 뜰 수 있다. 함수 첫머리에서 `recordingJob?.isActive == true`면 돌아간다
- **S3** `UserWhisper.prepare()` — `engine?.close()`를 잠금 없이 부른다. 받아 적는 중 불리면 네이티브 context를 해제하는 중에 쓴다. 이미 있는 `closeWhenIdle()`을 쓴다

### Nit
- **N1** `ShadowingScreen.kt` 비교 결과가 단어마다 한 줄(`Text` 나열)이라 길면 읽기 어렵다. `FlowRow`로 한 문단처럼 보이게
- **N2** Gemma `close()`는 비동기(`DefaultLlmEngine.kt:128` `scope.launch`)라 Whisper 로딩과 잠깐 겹칠 수 있다. small.en은 0.43GB라 당장은 괜찮음 — 실기기 확인 때 메모리만 본다
- **N3** Whisper는 섀도잉을 나가도 메모리에 남고 문법 진입 때만 닫힌다. 의도라면 HANDOFF에 한 줄
- **N4** `youtubeVideoId`는 `https://` 없는 `youtu.be/ID`를 null로 본다. 공유 링크는 항상 https라 괜찮지만, 앞에 `https://`를 붙여 한 번 더 시도하면 손으로 친 링크도 된다

### 재리뷰 요청 시
- M1 + S1~S3 반영 커밋 범위를 HANDOFF "리뷰 반영 (1차)"에 적는다

## 2차 리뷰 — 2026-10-09
- 범위: `e9e9d96..0991c45`
- 판정: **Approve** (Must-fix 0)
- M1 ✅ `OpicRoot.kt` 섀도잉 경로에 `statusBarsPadding().navigationBarsPadding()` + `widthIn(max = 430.dp)`
- S1 ✅ `AudioRecord`를 IO 작업의 지역 변수로 옮기고 `finally`에서만 stop(녹음 중일 때만)·release. `onCleared`는 신호만 보냄
- S2 ✅ `recordingJob?.isActive` 확인 + `recording = true`를 launch 전에 켬
- S3 ✅ `UserWhisper`에 Mutex — prepare·transcribe·close 직렬화, 교체는 `closeWhenIdle()`
- Nit N1~N4 미반영 (선택) — N2·N3은 실기기 확인(AC4) 때 메모리를 같이 본다
- **남은 일: AC4 실기기 확인** (사용자, 와이파이 될 때). IFrame 재생(오류 152/153 여부), 0.75배, A-B 반복, 녹음 → 비교 표시
