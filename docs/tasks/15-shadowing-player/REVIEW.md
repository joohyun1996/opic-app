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

## 3차 리뷰 — 실기기 확인 결과 (2026-10-10, `8ab9739`)
- 판정: **Request changes** (Must-fix 3) — TASK 15·16 함께
- 근거: GPT 실기기 기록 + Claude 추가 확인 (아래)

### Claude 추가 확인
- 자막: 데스크톱에서 같은 요청을 재현함. `aircAruvnKk` watch 페이지에 `captionTracks` 31개(사람 `en` 포함)가 있으나 `baseUrl&fmt=json3` 응답이 **0바이트**. baseUrl에 `exp=xpe`가 붙어 있어, 플레이어가 만드는 PO 토큰(`pot`) 없이는 빈 응답을 준다 → `CaptionClient` 코드 문제가 아니라 경로 자체가 막힘 (TASK 16에서 예상한 위험)
- 녹음: 기기의 `shadowing-last.pcm` = **1.6초**, 최대 진폭 4854/32767(약 15%), 1초 뒤로는 거의 무음. 짧고 작은 소리에서 Whisper가 `[silence]`만 냈다. 사용자는 "street"을 말했다고 함
- 홈 오답노트 카드: `WordsApp.kt`에서 `state.wrong > 0`일 때만 보임. 10-07 재설치로 기록이 0이라 안 보인 것 → 정상 동작

### Must-fix
- **M1 플레이어가 흰 화면, 현재 시각 0.0** (`feature/shadowing/.../YouTubePlayer.kt`)
  - `domStorageEnabled = true` (YouTube 플레이어는 localStorage를 씀 — 끈 것이 가장 유력한 원인), `mediaPlaybackRequiresUserGesture = false`
  - `WebChromeClient.onConsoleMessage`를 logcat 태그 `ShadowingWeb`로 남기고, IFrame `onReady`·`onStateChange`를 브리지로 받아 화면에 "플레이어 준비 중 / 준비됨 / 오류 N" 표시
  - debug 빌드만 `WebView.setWebContentsDebuggingEnabled(true)`
  - 위로도 안 되면 원인(콘솔 오류)을 HANDOFF에 적고 멈춘다 — 추측으로 다른 우회(영상 다운로드 등) 금지
- **M2 자막 경로 교체** (`CaptionClient.kt`, `YouTubePlayer.kt`)
  - 직접 요청은 막혔으므로, **플레이어가 스스로 보내는 자막 요청을 가로챈다**: playerVars에 `cc_load_policy: 1, cc_lang_pref: 'en'` → `WebViewClient.shouldInterceptRequest`에서 경로가 `/api/timedtext`이고 `lang=en`인 요청을 앱이 대신 받아(같은 URL, `fmt=json3`로) WebView에 그대로 돌려주고, 같은 본문을 `parseJson3`로 넘긴다
  - 가로채기 실패 시 기존 직접 요청 → 둘 다 실패면 지금처럼 "자막 없음" + 붙여 넣기
  - 허용 호스트·경로 검사, 5MiB 상한, 시간 제한은 그대로 적용. 자막 외 요청은 건드리지 않는다(`null` 반환)
  - 순수 함수는 바꾸지 않는다. 가로챈 URL에서 `lang`·`kind` 고르는 부분만 테스트 추가
- **M3 짧은·작은 녹음 처리** (`ShadowingViewModel.kt`)
  - 녹음이 1.5초 미만이면 받아 적지 않고 "너무 짧아요 — 문장 전체를 말해 보세요"
  - 결과가 `[silence]`, `[BLANK_AUDIO]`, `(silence)` 같은 대괄호·괄호 태그뿐이면 "말소리가 잘 들리지 않았어요 — 폰을 입에 가까이" (일치율 표시 안 함). 태그는 결과 문장에서 제거
  - 녹음 중 입력 크기 막대(최근 100ms RMS) 표시 — 말하는 게 들어가는지 보이게

### 실기기 재확인 (GPT, M1~M3 반영 후)
- 같은 두 영상으로 재생·0.75×·A-B 3회 → 자막 목록·문장 선택 → 녹음(사용자가 문장 전체를 말함) → 비교. 스크린샷을 HANDOFF에 추가
