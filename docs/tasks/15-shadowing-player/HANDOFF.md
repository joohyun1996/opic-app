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

---

## 리뷰 반영 (1차)

- 리뷰 반영 시작 커밋: `c1652cd`
- 반영 커밋 범위: `e9e9d96..8de4c31` (리뷰 문서 커밋 포함, 코드 수정은 `8de4c31`)
- HANDOFF 문서 커밋까지 포함한 재리뷰 명령: `git diff e9e9d96..HEAD`

| 리뷰 ID | 조치 | 커밋 | 비고 / 반론 |
|---------|------|------|------------|
| M1 | 반영 | `8de4c31` | 섀도잉 경로에 상태·내비게이션 바 여백과 최대 너비 430dp를 적용 |
| S1 | 반영 | `8de4c31` | 화면 이탈은 중단 상태와 작업 취소만 전달. AudioRecord는 IO 작업 지역 변수로 소유하고 `finally`에서만 stop/release. 취소된 녹음은 받아 적기를 시작하지 않음 |
| S2 | 반영 | `8de4c31` | 활성 녹음 작업이 있으면 즉시 반환하고, IO 작업 실행 전에 녹음 상태를 설정해 연속 입력 방지 |
| S3 | 반영 | `8de4c31` | `closeWhenIdle()` 사용. 추가로 UserWhisper의 prepare/transcribe/close를 같은 Mutex로 직렬화해 엔진 참조 교체도 보호 |
| N1 | 미반영 | - | 선택 항목. 단어별 결과 배치는 유지 |
| N2 | 미반영 | - | 선택 항목. Gemma 비동기 해제와 Whisper 로딩이 잠시 겹칠 가능성은 기존과 같으며 실기기 메모리 확인은 보류 |
| N3 | 문서 반영 | 이 HANDOFF 커밋 | Whisper는 섀도잉 재진입 때 로딩 대기를 줄이려고 메모리에 유지하며, 문법 화면 진입 시 닫음 |
| N4 | 미반영 | - | 선택 항목. HTTPS가 포함된 공유 링크 기준을 유지 |

검증: `./gradlew test lint :app:assembleDebug :app:assembleRelease --quiet` 종료 코드 0, `git diff --check` 출력 없음.

이번 수정은 코드 검토와 빌드 검증으로 확인했다. 마이크·네이티브 동시 실행을 실기기에서 재현한 결과는 아니며, 폰 설치와 AC4는 **보류 — 사용자가 나중에 한꺼번에 확인**.

## 실기기 확인 (2026-10-10)

대상: S23+ (`SM-S916N`), 기존 앱 데이터 유지. `./gradlew :app:assembleDebug`는 중복 생성 DEX 오류로 처음 실패했고, `./gradlew :app:clean :app:assembleDebug --quiet` 통과 후 `adb install -r --no-streaming` 성공. 앱 삭제·데이터 초기화 없음.

| 항목 | 결과 | 확인 내용 |
|------|------|-----------|
| 홈 카드 | ❌ | 영단어·영문법·섀도잉은 보임. 오답노트 카드는 보이지 않음. |
| 영문법 제목 | ✅ | 단원 목록 제목이 `영문법`으로 표시됨. |
| 단원 1 오답 | ⚠️ | `u1-03`, `u1-04`를 의도적으로 첫 시도에 틀리고 두 번째에 맞힘. `u1-05`도 입력 끝에 숫자가 붙어 의도치 않게 첫 시도 오답. 3/10까지 진행 후 추가 오답을 피하려고 중단. |
| 영상 재생·속도·A-B | ❌ | 아래 두 링크에서 플레이어 영역이 흰색으로 비어 있고 재생을 눌러도 `현재 0.0초`. 따라서 0.75×와 3회 반복은 검증 불가. 오류 152/153은 화면에 표시되지 않음. |
| Whisper 다운로드 | ✅ | `음성 인식 모델 받기 (190MB)` 후 `받는 중 0%` → `33%` → `89%` → `준비됨`. |
| 녹음·받아 적기·비교 | ⚠️ | 원문 `I like to walk in the park.` 입력 후 녹음·정지·받아 적기·단어 비교 화면은 동작. 결과는 `[silence]`, 일치율 0.0%라 발화 인식 성공은 확인하지 못함. |
| 녹음 중 화면 이탈 | ✅ | 사용자가 준비됐다고 답한 뒤 두 번째 녹음을 시작하고 뒤로 가기로 홈으로 이동. 홈이 표시되고 `pidof com.jooh.opic`은 기존 프로세스 `27404`를 반환함. |
| 메모리 | ⚠️ | Whisper 준비 직후 TOTAL PSS `471,889 KB`. 섀도잉 → 홈 → 영문법 목록 진입 후 `293,504 KB`. 영문법의 `AI 교정: 준비 필요` 상태 때문에 AI 교정 화면 진입 후 수치는 측정하지 못함. |

영상 링크: `https://www.youtube.com/watch?v=aircAruvnKk`, `https://www.youtube.com/watch?v=jNQXAC9IVRw`.

재현: 홈 → 섀도잉 → 첫 링크 입력·열기 → 자막 없음 및 빈 플레이어 확인 → 재생 → 5초 뒤에도 현재 0.0초. 두 번째 링크에서도 동일. [플레이어 화면](device-player-2026-10-10.png), [비교 결과](device-compare-2026-10-10.png), [관련 logcat](device-logcat-2026-10-10.txt). 로그에는 WebView 시작 무렵 `Failed to read DnsConfig`가 있으나 재생 실패의 원인으로 확정할 수 없음. 코드 변경 없음.
