# HANDOFF: 섀도잉 1 — YouTube 재생·구간 반복·따라 말하기 비교

> 작성: GPT · 경로: docs/tasks/15-shadowing-player/HANDOFF.md

## 커밋 범위
- base: `2eea1f6` (시작 전 `git rev-parse --short HEAD`)
- head: `0ab0bb9` (구현 커밋, HANDOFF는 별도 커밋)
- 리뷰 명령: `git diff 2eea1f6..0ab0bb9`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/common/.../Shadowing.kt` + 테스트 | 생성 | YouTube 영상 ID 검증과 WER 정규화 기반 단어 차이 역추적 |
| `core/stt/.../WhisperEngine.kt`, `whisper_jni.c`, `UserWhisper.kt` | 수정/생성 | 사용자용 small.en 단일 세션과 JNI 취소 콜백 |
| `feature/shadowing/**` | 생성 | WebView 플레이어, A-B 반복, 원문 입력, 녹음·받아 적기·결과 화면 |
| `app/build.gradle.kts`, `settings.gradle.kts`, `OpicRoot.kt`, `OpicApplication.kt` | 수정 | 모듈 연결, 경로, 앱 단위 Whisper 보관 및 Gemma 해제 |
| `feature/words/.../WordsApp.kt` | 수정 | 홈의 섀도잉 카드 |

## 수용 기준 체크
| AC | 결과 | 검증한 테스트·명령 |
|----|------|------------------|
| AC1 | ✅ | `ShadowingTest.youtubeLinks`: 짧은 링크, 일반·모바일 watch, shorts, embed, 뒤에 붙은 매개변수와 잘못된 호스트·ID |
| AC2 | ✅ | `ShadowingTest.diffsAndWer`: walk/work 치환, 삭제·삽입·일치·빈 문자열, 편집 횟수와 `wordErrorRate` 비교 |
| AC3 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet` 성공. 릴리스 APK에 `lib/arm64-v8a/libopic_whisper.so` 있음, DEX에 `SttBenchScreen` 없음 |
| AC4 | 보류 — 사용자가 나중에 한꺼번에 확인 | 요청에 따라 폰 설치·실기기 확인·스크린샷 수행 안 함 |
| AC5 | ✅ | `rg -n 'uses-permission' app/src/main/AndroidManifest.xml` → `INTERNET`, `RECORD_AUDIO` 두 줄 |

## 검증 결과
```
./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet → 통과
core/common ShadowingTest                            → 2 tests, 0 failures
release APK: libopic_whisper.so                      → 있음
release APK DEX: SttBenchScreen                     → 없음
AndroidManifest.xml 권한                            → INTERNET, RECORD_AUDIO
CMakeLists.txt: CMAKE_BUILD_TYPE Release            → 유지
CMakeLists.txt: GGML_CPU_ARM_ARCH                   → 유지
git diff --cached --check                           → 출력 없음
gradle/gradle-daemon-jvm.properties                 → 없음
```

## 설계 판단 / 리뷰어가 봐야 할 곳
- `feature/shadowing/.../YouTubePlayer.kt:35` — `loadDataWithBaseURL`을 `https://appassets.androidplatform.net/`으로 지정하고 IFrame `origin`도 같은 주소로 맞췄다. HTTPS origin을 주면서 YouTube 프레임과 앱 HTML의 출처를 분리하려는 선택이다. 재생 성공 여부는 AC4에서 확인해야 한다.
- `feature/shadowing/.../YouTubePlayer.kt:16` — JavaScript 브리지는 시간·오류 수신 메서드 두 개뿐이다. 파일·콘텐츠 접근을 끄고, 임의의 상위 화면 이동을 막았다. 브리지는 모든 프레임에 보일 수 있으므로 앱 HTML에만 포함된 임의 토큰을 검사한다. YouTube 프레임은 다른 origin이다.
- `core/stt/src/main/cpp/whisper_jni.c` — 취소 플래그는 JNI 전역 참조로 유지하고 콜백 스레드를 JVM에 연결해 읽는다. Java 플래그가 켜지면 whisper.cpp에 중단을 요청한다.
- `core/stt/.../UserWhisper.kt` — 앱이 지연 생성한 모델 저장소·엔진 하나만 보관한다. 모델 파일이 없으면 버튼을 누른 경우에만 다운로드한다.
- `app/src/main/java/com/jooh/opic/OpicApplication.kt` — Whisper 로딩 전 Gemma 엔진에 `close()`를 요청하고 이후 문법 진입 때 새 엔진을 만들 수 있게 참조를 비운다. 문법 진입 시에는 Whisper를 닫는다.
- `feature/shadowing/.../ShadowingViewModel.kt:33` — 영상별 원문은 프로세스 메모리에만 남기며, 녹음 파일은 앱 내부의 `shadowing-last.pcm` 하나를 덮어쓴다. 학습 결과를 외부로 보내지 않는다.
- `feature/shadowing/.../ShadowingScreen.kt` — 녹음 최대 30초, 반복 확인 주기 150ms, 현재 시각 기준 A/B와 ±0.5초 조절. 일치율은 `1 - WER`을 그대로 표시한다.

## 범위 밖 변경
- 없음. 외부 패키지·Room 스키마·Migration·권한은 추가하거나 바꾸지 않았다.

## 질문
- AC4 실기기 확인은 사용자 일정에 맞춰 보류했다. IFrame origin의 실제 재생 동작과 마이크·Whisper 통합은 그때 확인이 필요하다.
