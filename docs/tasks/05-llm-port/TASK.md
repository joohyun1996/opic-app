# TASK: `core/llm` 이식 + 문장 교정 채점 품질·속도 검증

> 작성: Claude · 승인: [x] 사용자 (2026-10-05)
> 경로: docs/tasks/05-llm-port/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 결정, § 위험(JSON 출력 불안정), § 후속 작업 순서 5
> 선행: TASK 04 Approve (`de2a3b0`)

## 목표
머니로그의 기기 내 LLM 모듈을 `:core:llm`으로 복사해 이 앱에서 Gemma 3n E4B를 돌린다. 그 위에 "문장 교정 채점" 프롬프트 하나를 만들고, 갤럭시 S23+에서 **JSON 출력 안정성·자기모순·속도**를 측정한다. 이 결과로 문법·스피킹 탭에서 E4B 채점을 쓸지 판단한다. 사용자용 기능은 만들지 않고, 개발용 검증 화면만 둔다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `settings.gradle.kts` | 수정 | `:core:llm` include |
| `gradle/libs.versions.toml` | 수정 | mediapipe tasks-genai 0.10.27, okhttp 4.12.0, security-crypto 1.1.0, kotlinx-serialization-json 1.7.3 + serialization 플러그인 (머니로그와 동일 버전) |
| `core/llm/**` | 생성 | 머니로그 `core/llm` 복사 (아래 "복사 규칙") |
| `core/common/**` | 수정 | 교정 프롬프트 빌더, 결과 파서 + 테스트 |
| `core/common/build.gradle.kts` | 수정 | serialization 플러그인·의존성 |
| `app/build.gradle.kts` | 수정 | `:core:llm` 의존 |
| `app/src/main/AndroidManifest.xml` | 수정 | `INTERNET` 권한 1개 (모델 다운로드 전용) |
| `app/src/main/java/com/jooh/opic/OpicApplication.kt` | 수정 | `hfTokenStore`, `llmEngine` lazy 등록 |
| `app/src/main/java/com/jooh/opic/MainActivity.kt` | 수정 | 홈에 "LLM 검증" 진입 버튼 (debug 빌드에서만 노출) |
| `app/src/main/java/com/jooh/opic/debug/LlmBenchScreen.kt` | 생성 | 개발용 검증 화면 |
| `app/src/main/assets/llm-bench/sentences.json` | 생성 | 고정 테스트 문장 20개 |
| `docs/tasks/05-llm-port/BENCHMARK.md` | 생성 | 실기기 측정 결과 |

## 관련 파일 (읽기만)
- `~/Desktop/develop/investment/app/src/main/java/com/jooh/finance/core/llm/*` — 복사 원본
- `~/Desktop/develop/investment/app/src/main/java/com/jooh/finance/AppContainer.kt:130-146` — 엔진·토큰 저장소 연결 방식

## 요구사항

### 복사 규칙 (`:core:llm`, Android 라이브러리)
- 복사: `LlmContract.kt`, `DefaultLlmEngine.kt`, `HttpModelStore.kt`, `HfTokenStore.kt`, `MediaPipeRuntime.kt`, `ModelCatalog.kt`, `LlmStateLabels.kt`
- 복사하지 않음: `SectionGeneration.kt` (머니로그 종목분석 전용)
- 바꿀 것은 **패키지명(`com.jooh.opic.core.llm`)과 아래 두 가지뿐**이다. 로직은 고치지 않는다 (실기기 검증된 코드)
  - `ModelCatalog`: 모델 폴더는 이 앱의 `noBackupFilesDir/llm` (머니로그와 공유하지 않음, ADR 001). URL·바이트 수·파일명은 그대로
  - `HfTokenStore`: SharedPreferences 파일 이름을 이 앱용으로 바꾼다
- 복사 직후 원본과 같다는 것을 증명할 수 있게, 복사만 한 커밋을 먼저 만들고 수정은 다음 커밋으로 나눈다

### 교정 채점 (`core:common`, 순수 Kotlin)
- `buildCorrectionPrompt(sentence: String): String` — 영어 문장 하나를 받아 **JSON만** 반환하라고 지시하는 프롬프트
  - 출력 형식: `{"correct": Boolean, "corrected": String, "errors": [{"type": String, "original": String, "fix": String, "explanationKo": String}]}`
  - `type`은 `tense | article | preposition | agreement | word_choice | word_order | other` 중 하나
  - 설명은 한국어, 한 문장
- `parseCorrection(raw: String): CorrectionParse` — 결과는 `Ok(result)` / `InvalidJson` / `Contradiction` 셋 중 하나
  - 앞뒤 공백, ```` ```json ```` 코드 펜스, JSON 앞뒤의 잡문은 걷어내고 첫 `{`부터 짝이 맞는 `}`까지 파싱한다
  - 모순 판정: `correct = true`인데 errors가 있음 / `correct = false`인데 errors가 비었음 / `corrected`가 원문과 같은데 errors가 있음
  - 모르는 `type`은 `other`로 바꾸고 Ok로 둔다

### 개발용 검증 화면 (`LlmBenchScreen`)
- debug 빌드에서만 진입할 수 있다 (`BuildConfig.DEBUG`)
- 구성: HF 토큰 입력칸(저장 후 마스킹 표시) / 엔진 상태(`llmProgressLabel`) / "모델 준비" 버튼 / "벤치 실행" 버튼 / 결과 표
- 벤치: `sentences.json`의 20개 문장을 순서대로 `generate`(타임아웃 120초)하고, 문장마다 `소요 ms`, `parse 결과(Ok/InvalidJson/Contradiction/Timeout/Failed)`, `correct 판정이 기대값과 일치하는지`를 기록한다
- 결과는 화면 표 + Logcat(태그 `LlmBench`, 한 줄에 문장 하나)으로 내보낸다. 원문 출력도 Logcat에 남긴다
- 모델 다운로드 전에는 "약 4.4GB를 받습니다" 확인 다이얼로그를 띄운다
- 화면 코드는 이 TASK가 끝나면 지워도 되는 수준으로 단순하게 짠다. ViewModel·Navigation 라이브러리를 쓰지 않는다

### 테스트 문장 (`sentences.json`)
- 20개: 맞는 문장 6개, 틀린 문장 14개 (시제·관사·전치사·수일치·어순·단어 선택 골고루). OPIc IM~IH 수준의 일상 회화 문장
- 각 항목: `{ "sentence": String, "expectedCorrect": Boolean, "note": String }`

### BENCHMARK.md
- 기기, 모델, 측정 날짜 / 문장별 결과 표 / 요약: InvalidJson 수, Contradiction 수, correct 판정 일치율, 소요시간 중앙값·최댓값, 첫 로딩 시간
- 맨 아래 "판단 근거"에 숫자만 적는다. 채택 여부 판단은 Claude 리뷰와 사용자가 한다

### 실기기 측정
- 기기: 갤럭시 S23+ (SM-S916N, Android 16). `adb devices`로 연결을 확인하고, 연결되어 있지 않으면 **멈추고 사용자에게 요청**한다
- 4.4GB 재다운로드를 피하는 방법(선택): 머니로그 debug 앱의 모델 파일을 `adb exec-out run-as com.jooh.finance cat no_backup/llm/gemma-3n-e4b-it.task > <임시 경로>`로 맥에 옮긴 뒤, 이 앱 폴더로 `run-as com.jooh.opic` + `cat`으로 넣는다. 크기가 4,405,655,031바이트인지 확인한다. 이 방법을 쓰면 HANDOFF에 적고, 다운로드 경로는 토큰 입력 → 다운로드 시작까지만 확인해도 된다. 임시 파일은 끝나면 지운다 (커밋 금지)
- Logcat은 `adb logcat -s LlmBench`로 수집해 BENCHMARK.md 표를 채운다

## 수용 기준
- [ ] AC1: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint`가 통과한다
- [ ] AC2: 복사 커밋의 `core/llm` 파일 7개는 패키지 선언·import 줄을 빼면 머니로그 원본과 같다 (`diff` 출력 HANDOFF에 첨부)
- [ ] AC3: `ModelCatalog`의 모델 경로가 `com.jooh.opic` 앱의 `noBackupFilesDir/llm` 아래다
- [ ] AC4: Manifest의 `uses-permission`은 `INTERNET` 1개뿐이고, 앱 코드에서 네트워크를 쓰는 곳은 `HttpModelStore`뿐이다
- [ ] AC5: `parseCorrection`이 ① 정상 JSON → Ok ② 코드 펜스·앞뒤 잡문이 붙은 JSON → Ok ③ 잘린 JSON → InvalidJson ④ 모순 3종 각각 → Contradiction ⑤ 모르는 type → Ok + `other` 를 반환한다 (단위 테스트)
- [ ] AC6: `buildCorrectionPrompt` 결과에 입력 문장과 "JSON만 반환" 지시, 7개 type 목록이 들어 있다 (단위 테스트)
- [ ] AC7: release 빌드에서는 LLM 검증 화면에 들어갈 수 없다
- [ ] AC8: HF 토큰이 소스·Logcat·커밋 파일 어디에도 없다 (`git grep -n "hf_"` 결과 0줄, Logcat 출력에 토큰 없음)
- [ ] AC9: BENCHMARK.md에 S23+ 실측 20문장 결과와 요약 수치가 있다
- [ ] AC10: `git diff <base> HEAD -- exports/ docs/decisions/ SPEC.md core/database/` 결과가 없다

## 제약 / 주의
- 이 TASK는 `INTERNET` 권한 추가와 위 4개 의존성 설치가 승인되어 있다. 다른 의존성이 필요하면 먼저 묻는다
- 머니로그 저장소와 머니로그 앱 데이터는 **읽기만** 한다 (`run-as com.jooh.finance`로 쓰기·삭제 금지)
- 프롬프트를 여러 번 고쳐 가며 숫자를 맞추지 않는다. 첫 버전으로 측정하고, 고쳤다면 버전별 결과를 모두 BENCHMARK.md에 남긴다 (최대 2회)
- 커밋 예: `chore(llm): 머니로그 core/llm 복사`, `feat(llm): 이 앱용 모델 경로·토큰 저장소`, `feat(llm): 교정 프롬프트·파서`, `feat(debug): LLM 검증 화면`, `docs(llm): TASK 05 벤치 결과`
- 복사한 `core/llm`과 wrapper를 빼고 diff가 600줄을 크게 넘으면 멈추고 보고한다
- push 금지

## 범위 밖 (하지 말 것)
- 문법·스피킹 화면, 실제 채점 기능 연결
- 사용자용 설정 화면, 다운로드 동의 화면의 디자인 작업
- 머니로그 쪽 코드 수정, `core/llm` 공용 라이브러리 분리
- E2B/1B 폴백 모델 추가
- Gradle 호환 옵션(`builtInKotlin`, `newDsl`) 정리 (TASK 04 N5)
- 음성 인식
