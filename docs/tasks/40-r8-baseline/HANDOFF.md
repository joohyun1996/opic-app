# HANDOFF: 릴리스 R8 + Baseline Profile

> 작성: Claude (GPT 사용량 부족으로 대신 구현) · base: `d4ab26a`

## 변경
- `app/build.gradle.kts`: release에 `isMinifyEnabled`, `isShrinkResources`, `proguard-android-optimize.txt` + `proguard-rules.pro`. `androidx.profileinstaller` 추가
- `app/proguard-rules.pro`: whisper JNI(`WhisperNative`), MediaPipe·protobuf, kotlinx.serialization serializer, `ReminderWorker` keep
- `gradle/libs.versions.toml`: profileinstaller 1.4.1

## 결과
| 항목 | 전 | 후 |
|------|----|----|
| APK 크기 | 87,166,331 B (83.1MB) | 64,165,738 B (61.2MB, −26%) |
| 콜드 스타트 `am start -W` TotalTime (3회) | 268 / 304 / 259 ms (평균 277) | 243 / 230 / 319 ms (평균 264) |

- 콜드 스타트는 설치 직후에 쟀다. 라이브러리 Baseline Profile은 설치 뒤 백그라운드 dex 최적화가 끝나야 효과가 나므로, 하루쯤 지나면 더 줄어들 수 있다

## 실기기 확인 (S23+, `adb install -r`, 앱 데이터 유지)
- [x] 홈·단어(Day 목록·Day 1)·문법(실전 영문법)·섀도잉·스피킹 진입, 크래시 로그 없음
- [x] JSON 콘텐츠 파싱(serialization): 문법·스피킹·템플릿 화면이 정상 표시
- [x] Whisper JNI: 모델 불러오기 → 4초 녹음 → 받아쓰기 결과 표시 (`whisper-r8.png`)
  - 무음 녹음이라 결과는 "unintelligible", 이 답변 1개가 스피킹 기록에 저장됨
- [x] Gemma(MediaPipe) 교정: 위 답변 문법 교정 → 결과 카드 표시
- [x] 섀도잉 YouTube 플레이어 준비됨 (`shadowing-r8.png`)
- [ ] 단어 학습 5장, 백업·복원(파일 선택 창), 학습 알림 켜기(권한 창), 문법 문제 풀이 — 미확인 (사용자 확인 필요)

## 판단
- Baseline Profile 직접 생성(macrobenchmark 모듈)은 하지 않음. 기기 연결 자동화가 필요해 규모가 크다. Compose 등 라이브러리에 들어 있는 프로파일은 profileinstaller로 적용된다
- 크래시가 나면 `app/build/outputs/mapping/release/mapping.txt`로 스택을 복원한다 (커밋하지 않음)
