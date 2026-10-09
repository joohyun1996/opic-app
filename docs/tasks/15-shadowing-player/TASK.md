# TASK: 섀도잉 1 — YouTube 재생 + 구간 반복 + 따라 말하기 비교

> 작성: Claude · 승인: [x] 사용자 (2026-10-09) · 구현: GPT
> 경로: docs/tasks/15-shadowing-player/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 섀도잉 결정 C, § 음성 인식 결정 / TASK 14 측정 결과 (small.en 채택)

## 목표
YouTube 영상을 앱 안에서 재생하면서 구간(A-B)을 반복하고, 사용자가 문장을 따라 말하면 Whisper(small.en)로 받아 적어 원문과 단어 단위로 비교해 보여 준다. 비공식 자막 가져오기는 TASK 16에서 한다 (이번에는 문장을 직접 붙여 넣는다).

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `feature/shadowing/**` | 생성 | 새 모듈: 화면, ViewModel, WebView 플레이어 |
| `core/common/.../Shadowing.kt` + 테스트 | 생성 | YouTube 링크 → 영상 ID, 단어 비교(diff) 순수 함수 |
| `core/stt/**` | 수정 | 사용자 기능용 small.en 하나만 노출하는 API 추가 (검증용 3개 목록은 유지), abort 콜백(취소) |
| `app/build.gradle.kts` | 수정 | `:core:stt`를 `debugImplementation` → `implementation`, `:feature:shadowing` 추가 |
| `app/src/main/java/com/jooh/opic/OpicRoot.kt` | 수정 | `shadowing` 경로 등록 |
| `app/src/main/java/com/jooh/opic/OpicApplication.kt` | 수정 | Whisper 엔진·모델 저장소 1개 보관 (지연 생성) |
| `feature/words/.../WordsApp.kt` | 수정 | 홈에 "섀도잉" 카드 (영문법 카드 아래) |
| `settings.gradle.kts` | 수정 | `:feature:shadowing` |

## 관련 파일 (읽기만)
- `core/stt/src/main/java/com/jooh/opic/core/stt/WhisperEngine.kt` — load/transcribe/close, `SttModels`
- `app/src/debug/java/com/jooh/opic/debug/SttBenchScreen.kt` — AudioRecord 녹음 방식 (16kHz mono PCM16, VOICE_RECOGNITION)
- `feature/grammar/.../GrammarFlow.kt` — 모델 준비 화면(상태 표시·버튼 눌러 받기) 패턴
- `docs/tasks/14-whisper-spike/HANDOFF.md` — 측정 결과와 주의점(Release 빌드 강제)

## 요구사항
### 화면 / 동작
1. **영상 열기**: 홈 "섀도잉" → YouTube 링크 입력칸 + "열기". `youtu.be/ID`, `youtube.com/watch?v=ID`, `/shorts/ID`, `/embed/ID`, `m.youtube.com`, 뒤에 붙은 `&t=`·`?si=` 를 처리한다. 잘못된 링크면 "YouTube 링크를 확인하세요"
2. **플레이어**: WebView + YouTube **IFrame Player API** (영상을 내려받지 않는다). Kotlin ↔ JS 브리지로 재생/일시정지, 현재 시각, 이동(seek), 속도(0.5 / 0.75 / 1.0)
   - HTML은 `loadDataWithBaseURL`로 넣고 base URL을 `https://` 주소로 둔다 (origin 없으면 임베드 오류 152/153이 날 수 있음). 재생 실패 시 오류 코드를 화면에 표시
3. **구간 반복**: "A 지정" / "B 지정" 버튼(현재 시각), ±0.5초 미세 조정, 반복 켬/끔. 켜면 B에 닿을 때 A로 돌아간다 (확인 주기 200ms 이하). A ≥ B면 지정 불가
4. **문장 입력**: 지금 구간의 원문 문장을 붙여 넣는 칸 (여러 줄 가능, 앱 실행 중 영상별로 기억)
5. **따라 말하기**: "● 녹음" → "■ 정지" → Whisper 받아 적기 → 결과 표시
   - 원문이 있으면 단어 비교: 맞음(기본색) / 틀림(빨강, `원문 → 들린 말`) / 빠뜨림(회색 취소선) / 더 말함(주황). 일치율 = 1 − WER (`core:common/Wer.kt` 재사용)
   - 원문이 없으면 받아 적은 문장만 표시
   - 녹음 중 영상은 자동 일시정지. 녹음 최대 30초, 넘으면 자동 정지
   - 녹음은 앱 내부 저장소에 마지막 1개만 둔다 (재생 버튼으로 내 목소리 듣기)
6. **Whisper 준비**: small.en(190MB)이 없으면 "음성 인식 모델 받기 (190MB)" 버튼. 버튼을 눌렀을 때만 받는다. 상태: 없음 / 받는 중 N% / 불러오는 중 / 준비됨 / 실패(다시 시도)
   - Gemma가 메모리에 있으면 Whisper를 올리기 전에 내린다 (ADR: 동시 적재 금지)
   - 받아 적는 중 "취소" 가능 (whisper abort 콜백)

### 순수 함수 (`core/common/Shadowing.kt`)
- `youtubeVideoId(url: String): String?` — ID는 11자 `[A-Za-z0-9_-]`
- `compareWords(reference: String, hypothesis: String): List<WordDiff>` — `werWords` 정규화 후 편집 거리 역추적. `WordDiff`는 `Match(word)` / `Substitute(ref, hyp)` / `Delete(ref)` / `Insert(hyp)`

## 수용 기준
- [ ] AC1: `youtubeVideoId`가 위 6가지 형식에서 ID를 돌려주고, YouTube가 아니거나 ID가 11자가 아니면 null (테스트)
- [ ] AC2: `compareWords("I walk my dog", "I work my dog")` → Match, Substitute(walk, work), Match, Match. 빠뜨림·더 말함·완전 일치·빈 문자열 테스트. 오류 개수 합 / 원문 단어 수 = `wordErrorRate` 와 같다 (테스트)
- [ ] AC3: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과. release APK에 `libopic_whisper.so`가 들어가고 SttBench 화면은 없다
- [ ] AC4: S23+에서 영상 1개로 확인 — 재생, 속도 0.75, A-B 반복 3회, 녹음 → 받아 적기 → 비교 표시 (스크린샷 2장: 플레이어, 비교 결과)
- [ ] AC5: AndroidManifest 권한이 `INTERNET`, `RECORD_AUDIO` 그대로다 (새 권한 없음)

## 제약 / 주의
- Room 스키마·Migration 변경 없음 (영상 기록·문장 저장은 이번 범위 밖)
- 네트워크: YouTube 임베드 재생 + 모델 다운로드만. 녹음·받아 적기 결과를 밖으로 보내지 않는다
- WebView: JavaScript 허용, 파일 접근 끔(`allowFileAccess = false`), 브리지는 이 화면 HTML에서만 쓴다. 영상 밖 링크를 누르면 WebView 안에서 열지 않는다
- `core/stt` CMake의 Release 강제·`GGML_CPU_ARM_ARCH` 설정을 지우지 않는다 (지우면 수십 배 느려짐)
- `gradle/gradle-daemon-jvm.properties`가 생기면 커밋하지 않는다 (JDK 25 → Robolectric 테스트 실패)
- 녹음·받아 적기는 IO/Default 스레드, 4스레드

## 범위 밖 (하지 말 것)
- 비공식 자막 가져오기, 자막 시각 맞춤 (TASK 16)
- 영상·문장·점수 저장, 기록 화면
- 발음 점수(음소 단위), LLM 피드백
- 스피킹 탭
