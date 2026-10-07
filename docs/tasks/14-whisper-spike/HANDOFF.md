# HANDOFF: 스피킹 0 — Whisper 기기 내 음성 인식 검증

> 작성: Claude (GPT 토큰 없음, 사용자 요청으로 Claude 구현) · 경로: docs/tasks/14-whisper-spike/HANDOFF.md

## 커밋 범위
- base: `61105b4`
- head: 이 HANDOFF를 담은 커밋
- 리뷰 명령: `git diff 61105b4..HEAD -- ':!third_party'`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `third_party/whisper.cpp/` | 생성 | whisper.cpp v1.9.5(`d1be6fde`) CPU 빌드에 필요한 소스만 (GPU 백엔드 제외, 6.6MB), `VERSION.md` |
| `core/stt/build.gradle.kts` | 생성 | android library, arm64-v8a만, CMake 3.22.1, `c++_static` |
| `core/stt/src/main/cpp/CMakeLists.txt` | 생성 | whisper + ggml 정적 링크 → `libopic_whisper.so`. **Release 강제**, `-march=armv8.2-a+fp16+dotprod` |
| `core/stt/src/main/cpp/whisper_jni.c` | 생성 | initContext / freeContext / transcribe(greedy, language "en", UTF-8 bytes 반환) / systemInfo |
| `core/stt/.../WhisperEngine.kt` | 생성 | `SttModels`(3개, `no_backup/stt/`, 크기·SHA-256), `WhisperEngine.load/transcribe/close`, `WavDecoder` |
| `core/common/.../Wer.kt` + `WerTest.kt` | 생성 | `werWords`, `wordErrorRate` |
| `app/src/debug/.../SttBenchScreen.kt` | 생성 | 모델 받기·불러오기, 대본 3개 녹음(16kHz mono, VOICE_RECOGNITION), 측정·logcat `SttBench` |
| `app/src/debug/.../DebugContent.kt` | 수정 | 디버그 메뉴에 "STT 검증 (Whisper)" |
| `app/build.gradle.kts` | 수정 | `debugImplementation(project(":core:stt"))` — release에는 없음 |
| `app/src/main/AndroidManifest.xml` | 수정 | `RECORD_AUDIO` |
| `settings.gradle.kts` | 수정 | `:core:stt` |

## 수용 기준 체크
| AC | 결과 | 검증 |
|----|------|------|
| AC1 | ✅ | `WerTest` › 같음 0, 치환·삽입·삭제 각 1/8, 대소문자·문장부호·`’` 무시, 빈 문자열 |
| AC2 | ✅ | `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과. release APK에 `libopic_whisper.so`·`SttBench` 없음, debug APK에만 있음 (3.98MB) |
| AC3 | ✅ | 아래 측정표 (S23+, 2026-10-07) |
| AC4 | ✅ | 아래 추천 |

## 측정표 (S23+ / Snapdragon 8 Gen 2, CPU 4스레드, greedy)
녹음: 사용자(한국인) 대본 3개, 각 약 19~20초. jfk = whisper.cpp 샘플(원어민 11초).

| 모델 | 로딩 | PSS | jfk | 대본 1 | 대본 2 | 대본 3 | 평균 RTF | 평균 WER(대본) |
|------|------|-----|-----|-------|-------|-------|---------|---------------|
| large-v3 (1.08GB) | 1.8초 | 1.72GB | 49.1초 · 0% | 64.3초 · 8.6% | 56.7초 · 1.9% | 56.9초 · 1.9% | 3.0 | 4.1% |
| large-v3-turbo (0.57GB) | 1.0초 | 0.87GB | 41.9초 · 0% | 43.8초 · 8.6% | 45.1초 · 1.9% | 47.3초 · 1.9% | 2.3 | 4.1% |
| **small.en (0.19GB)** | **0.3초** | **0.43GB**¹ | **6.4초 · 0%** | **7.3초 · 8.6%** | **8.0초 · 3.7%** | **8.8초 · 0%** | **0.41** | **4.1%** |

(셀 = 처리 시간 · WER. RTF = 처리 시간 / 음성 길이. 평균 RTF는 대본 3개 기준)
¹ 앱을 새로 켜고 small.en만 불러왔을 때 427MB. 측정 중 표시(0.80~0.84GB)는 앞서 쓴 turbo 메모리가 덜 반환된 값

- WER 오류 대부분은 실제 오류가 아니다: `four → 4`(숫자 표기), `bikes → bike`
- 대본 1의 8.6%는 세 모델 모두 같은 "walk my dog before **work**" → "**work** my dog before **walk**" — 실제 발음(walk/work)을 그대로 받아 적은 것으로 보인다. **발음 피드백에 그대로 쓸 수 있는 신호**
- whisper는 30초 창 단위로 인코딩해 짧은 음성일수록 RTF가 나쁘다 (jfk 11초 RTF가 가장 높음)

## 추천: small.en
- 성공 기준(RTF ≤ 0.5, WER ≤ 15%)을 **넘은 유일한 모델**. 20초 답변을 약 8초에 받아 적는다
- 정확도는 large 계열과 같다 (대본 WER 평균 4.1% 동일). 이 녹음 조건(조용한 실내, 대본 낭독)에서 large의 이점이 없다
- large-v3·turbo는 20초 답변에 45~65초 → 사용자 대기로는 무리. large-v3는 PSS 1.7GB로 Gemma(≈3GB)와 함께 올리기도 어렵다
- 남은 위험: 즉흥 답변(머뭇거림, 문법 오류가 섞인 말)은 대본 낭독보다 WER이 높을 수 있다. 스피킹 TASK에서 실제 답변으로 한 번 더 본다. 필요하면 medium.en(약 0.5GB)을 후보로 다시 잰다

## 설계 판단 / 리뷰어가 봐야 할 곳
- `core/stt/src/main/cpp/CMakeLists.txt` — **`CMAKE_BUILD_TYPE Release` 강제.** 처음 측정 때 debug APK의 ggml-cpu가 `-O0`으로 빌드돼 large-v3가 11초 음성을 10분 넘게 처리했다. `-O3`가 `ggml` 타깃에만 걸리고 실제 연산 타깃 `ggml-cpu`에는 빠져 있었음. `GGML_CPU_ARM_ARCH`로 ggml-cpu에도 fp16·dotprod 지정
- `whisper_jni.c` — 결과 텍스트를 `NewStringUTF` 대신 바이트 배열로 넘김 (modified UTF-8 문제 회피)
- 측정 중 취소 불가 (JNI abort 콜백 없음). 검증 화면 전용이라 그대로 둠. 사용자 기능에서는 `abort_callback` 필요
- 4스레드 고정. 6~8스레드나 i8mm(armv8.6)는 재지 않았다

## 범위 밖 변경
- 개발 환경: NDK 28.2.13676358 (Android Studio 설치가 압축 해제 중 실패 → 받은 zip을 직접 풀어 설치), CMake 3.22.1 (Gradle 자동 설치)
- `gradle/gradle-daemon-jvm.properties`(Android Studio가 생성, JDK 25) 커밋 안 함 — Robolectric 4.14가 Java 25 클래스를 못 읽어 DB 테스트 14개 실패. 파일은 저장소 밖으로 옮겨 둠

## 질문
- 없음
