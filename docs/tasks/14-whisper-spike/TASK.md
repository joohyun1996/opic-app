# TASK: 스피킹 0 — Whisper(whisper.cpp) 기기 내 음성 인식 검증

> 작성: Claude · 승인: [x] 사용자 (2026-10-07) · 구현: Claude
> 경로: docs/tasks/14-whisper-spike/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 음성 인식 결정 (2026-10-07)

## 목표
S23+에서 whisper.cpp로 영어 음성을 받아 적고, 모델별 속도·정확도·메모리를 측정해 스피킹·섀도잉에 쓸 모델을 정한다. TASK 05(LLM)처럼 **debug 빌드 전용 검증 화면**만 만들고, 사용자 기능은 만들지 않는다.

## 측정 대상
| 모델 | 파일 (ggml, Hugging Face `ggerganov/whisper.cpp`) | 역할 |
|------|------|------|
| large-v3 | `ggml-large-v3-q5_0.bin` (약 1.1GB) | 주 대상 — 사용자 요청 "가장 큰 모델" |
| large-v3-turbo | `ggml-large-v3-turbo-q5_0.bin` (약 0.55GB) | 주 대상 — large에 가까운 정확도, 더 빠름 |
| small.en | `ggml-small.en-q5_1.bin` (약 0.18GB) | 비교 기준 |

## 수정 범위
| 파일 | 작업 |
|------|------|
| `third_party/whisper.cpp/` | 추가 — whisper.cpp 소스(태그 고정), 빌드에 필요한 파일만 |
| `core/stt/**` | 새 모듈 — CMake로 whisper.cpp + JNI 빌드, Kotlin `WhisperEngine`(load / transcribe / close), 모델 다운로드 |
| `core/common/.../Wer.kt` + 테스트 | 단어 오류율(WER) 계산 순수 함수 |
| `app/src/debug/**` | "STT 검증" 화면: 모델 선택·다운로드, 녹음(16kHz mono), 문장 대본, 받아 적기, 시간·RTF·WER 표시 |
| `app/src/main/AndroidManifest.xml` | `RECORD_AUDIO` 권한 |
| `settings.gradle.kts`, 버전 카탈로그 | 모듈 등록 (새 외부 라이브러리 없음 — whisper.cpp 소스 빌드) |

## 요구사항
- 모델 파일은 `no_backup/stt/`에 버튼을 눌렀을 때만 받는다 (`.part` 후 이름 바꾸기, 크기 확인)
- 오디오는 `AudioRecord` 16kHz mono PCM → float. 녹음 파일은 앱 내부 저장소에만
- 추론은 IO/기본 스레드에서, 스레드 수는 기기 성능 코어 기준(4)으로 시작
- Gemma가 메모리에 올라가 있으면 Whisper를 올리기 전에 내린다 (검증 화면에서는 Gemma를 쓰지 않음)
- 검증 대본 3개 (각 약 30초, OPIc 답변 형식): 사용자가 읽고 녹음 → 대본과 WER 계산
- WER: 소문자, 문장부호 제거, 단어 단위 편집 거리 / 대본 단어 수

## 수용 기준
- [x] AC1: `wordErrorRate` 테스트 (같음 0, 한 단어 치환·삽입·삭제, 문장부호·대소문자 무시)
- [x] AC2: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, release APK에 STT 검증 화면 없음
- [x] AC3: S23+에서 모델 3개 × 대본 3개 측정표 (처리 시간, RTF = 처리 시간 / 음성 길이, WER, 모델 로딩 시간, 최대 메모리)
- [x] AC4: 추천 모델과 이유를 HANDOFF에 적는다

## 성공 기준 (판단용)
- 30초 음성을 **15초 이내**(RTF ≤ 0.5) + 한국인 억양 영어 **WER ≤ 15%**
- large-v3가 기준을 못 넘으면 turbo, turbo도 못 넘으면 small.en 이하를 검토

## 범위 밖
- 스피킹·섀도잉 사용자 화면, 실시간(스트리밍) 받아 적기, 중국어
