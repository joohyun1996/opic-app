# TASK: 릴리스 R8 최적화 + Baseline Profile

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "태스크로 잡고 지피티에 넘기자") · 구현: GPT — 의존성 추가(profileinstaller, baselineprofile 플러그인) 포함 승인

## 목표
릴리스 빌드에 R8(코드 축소·최적화)과 Baseline Profile을 적용해 첫 실행과 스크롤을 빠르게 한다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `app/build.gradle.kts` | 수정 | release `isMinifyEnabled = true`, `isShrinkResources = true`, proguard 파일 |
| `app/proguard-rules.pro` | 생성/수정 | kotlinx.serialization(@Serializable), Room, MediaPipe GenAI, whisper JNI(native 메서드), WorkManager Worker 클래스 keep 규칙 |
| `gradle/libs.versions.toml` | 수정 | androidx.profileinstaller, (가능하면) baselineprofile 플러그인 |
| `baselineprofile/` 모듈 | 생성 | 홈 → 단어 Day 목록 스크롤 → 문법 → 스피킹 경로 프로파일 생성 (기기 필요, 어려우면 profileinstaller만 넣고 HANDOFF에 적기) |

## 수용 기준
- [ ] AC1: 릴리스 APK에서 다섯 탭, 단어 학습 5장(5장 제한 규칙), 문법 문제·설명, 스피킹 녹음·Whisper 받아쓰기, Gemma 교정 1문장, 섀도잉 영상 재생, 백업·복원, 학습 알림 켜기가 모두 동작한다 (실기기, 하나라도 깨지면 keep 규칙 추가)
- [x] AC2: APK 크기 전/후를 HANDOFF에 적는다
- [x] AC3: 앱 첫 실행(콜드 스타트) 시간 전/후를 `adb shell am start -W`로 3회씩 재서 HANDOFF에 적는다
- [x] AC4: `./gradlew test lint :app:assembleRelease` 통과

## 제약
- 앱 데이터 삭제 금지, `adb install -r`만
- 이 TASK는 36~39 이후에 한다 (구조가 바뀐 뒤 keep 규칙 확정)
