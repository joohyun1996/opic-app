# HANDOFF: 기기 내 LLM 이식과 문장 교정 실측

> 작성: GPT · 경로: docs/tasks/05-llm-port/HANDOFF.md

## 커밋 범위
- base: `de2a3b0`
- 구현·벤치 head: `ea9ee72` (이 HANDOFF는 별도 문서 커밋)
- 리뷰 명령: `git diff de2a3b0..ea9ee72`
- 주요 커밋: `7d34f5d` TASK 승인, `4d5adfa` 원본 7개 복사, `d76ebff` TASK 계약 정정(Claude), `c86b0b2` 앱 연결·파서·화면, `ea9ee72` 실측 기록

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/llm/` | 생성 | 머니로그 LLM 7개 파일 이식, 앱 전용 토큰 저장 파일 이름 적용 |
| `core/common/` | 수정 | 교정 프롬프트·결과 파서·테스트 추가 |
| `app/` | 수정 | 모델 엔진 연결, debug 전용 검증 화면, 20문장 고정 입력 |
| Gradle 설정 | 수정 | LLM 모듈 및 TASK 지정 의존성 추가 |
| `BENCHMARK.md` | 생성 | S23+ 첫 버전 20문장 측정과 설명 품질 사례 기록 |

## 수용 기준 체크
| AC | 결과 | 검증한 명령·테스트 |
|----|------|------------------|
| AC1 | ✅ | `./gradlew assembleDebug test lint` → 성공. 아래 결과 참고 |
| AC2 | ✅ | `4d5adfa`의 7개 파일을 원본에서 패키지명만 치환한 내용과 비교 → 파일별 `diff 0줄` 7개. 비교 대상: `LlmContract`, `DefaultLlmEngine`, `HttpModelStore`, `HfTokenStore`, `MediaPipeRuntime`, `ModelCatalog`, `LlmStateLabels` |
| AC3 | ✅ | `ModelCatalog.kt:26`은 `context.noBackupFilesDir/llm`; 기기에서 `/data/user/0/com.jooh.opic/no_backup/llm/gemma-3n-e4b-it.task` 크기 4,405,655,031바이트 확인 |
| AC4 | ✅ | `rg -n 'uses-permission' app/src/main/AndroidManifest.xml` → `INTERNET` 1줄. `rg -n 'OkHttpClient\|Request.Builder\|fetch\(\|HttpURLConnection\|Socket\(' app/src/main core --glob '*.kt'` → `HttpModelStore.kt`만 해당 |
| AC5 | ✅ | `CorrectionTest` 5개 중 파서 테스트 4개: 정상·포장된 JSON·잘림·모순 3종·미지 type 확인 |
| AC6 | ✅ | `CorrectionTest.promptContainsContract` → 문장·JSON 지시·7개 type 확인 |
| AC7 | ✅ | `./gradlew assembleRelease` 성공, release `BuildConfig.DEBUG = false`; `MainActivity`는 debug 조건일 때만 버튼과 화면 진입을 노출 |
| AC8 | ✅ | `git grep -nE 'hf_[A-Za-z0-9]{30,}'` → 0줄. 수집한 `LlmBench` Logcat 동일 패턴 0건. 실제 토큰은 입력하지 않았고 모델은 기기 내부 복사 |
| AC9 | ✅ | `BENCHMARK.md`에 S23+ 20문장 표·요약: InvalidJson 0, Contradiction 0, 판정 일치 20/20, 중앙값 21,983.5ms, 최댓값 25,624ms, 첫 로딩 11,006ms |
| AC10 | ✅ | `git diff de2a3b0 ea9ee72 -- exports/ docs/decisions/ SPEC.md core/database/` → 0줄 |

## 검증 결과
```text
./gradlew assembleDebug → 통과
./gradlew test → 통과 (10개 테스트, 실패 0)
./gradlew lint → 통과 (앱 오류 0·경고 21, LLM 오류 0·경고 2)
./gradlew assembleRelease → 통과 (AC7 추가 확인)
```

## 설계 판단 / 리뷰어가 봐야 할 곳
- `core/llm/src/main/java/com/jooh/opic/core/llm/ModelCatalog.kt:26` — 원본도 `context.noBackupFilesDir/llm`을 사용했다. 이 앱의 Context를 전달하면 다른 앱 데이터와 분리되므로 폴더 로직은 고치지 않았다.
- `core/llm/src/main/java/com/jooh/opic/core/llm/HfTokenStore.kt:19` — 저장 파일 이름을 `opic_hf_token_store`로 변경했다. 실제 HF 토큰 값은 사용하지 않았다.
- `core/common/src/main/kotlin/com/jooh/opic/core/common/Correction.kt` — 정정된 TASK대로 `parseCorrection(raw, original)`을 사용한다.
- `app/src/main/java/com/jooh/opic/debug/LlmBenchScreen.kt` — Logcat에 모델 원문을 남기며 토큰은 남기지 않는다. 다운로드 경로는 모델을 복사했으므로 실제 다운로드를 시작하지 않았다.
- `BENCHMARK.md` — `correct` 판정은 20/20이지만 한국어 설명에서 문법 용어 오류 2건을 발견했다. 채택 여부는 이 수치와 함께 판단해야 한다.

## 범위 밖 변경
- 없음

## 질문
- 없음
