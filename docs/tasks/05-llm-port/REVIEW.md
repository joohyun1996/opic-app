# REVIEW: `core/llm` 이식 + 문장 교정 채점 품질·속도 검증

> 작성: Claude · 경로: docs/tasks/05-llm-port/REVIEW.md

## 1차 리뷰 — 2026-10-05
- 대상: `git diff de2a3b0..ea9ee72` (HANDOFF `dbc3dd4` 포함)
- 판정: **Approve**

### 수용 기준
| AC | 판정 | 근거 |
|----|------|------|
| AC1 | ✅ | HANDOFF 검증 결과 (test 10개, lint 오류 0) |
| AC2 | ✅ | 리뷰어가 직접 재확인: `4d5adfa`의 7개 파일을 패키지명만 되돌리면 머니로그 원본과 동일 |
| AC3 | ✅ | `ModelCatalog.kt:26` `noBackupFilesDir/llm`, 기기 경로 `com.jooh.opic` 확인 (HANDOFF) |
| AC4 | ✅ | `AndroidManifest.xml:2` INTERNET 1개. 네트워크 코드는 `HttpModelStore`뿐 |
| AC5 | ✅ | `CorrectionTest.kt:19-37` — 5가지 경우 모두 검사 |
| AC6 | ✅ | `CorrectionTest.kt:12-17` |
| AC7 | ✅ | `MainActivity.kt` 진입·버튼 모두 `BuildConfig.DEBUG` 조건 |
| AC8 | ✅ | 실제 토큰은 입력하지 않음. 패턴 검사 0건 |
| AC9 | ✅ | BENCHMARK.md 20문장 표와 요약 |
| AC10 | ✅ | `--stat`에 해당 경로 없음 |

### Must-fix
- 없음

### Should-fix
- 없음

### Nit (선택)
- **N1** `LlmBenchScreen.kt:96` — Timeout 판정을 예외 메시지에 "timeout"이 들어 있는지로 하는데, 엔진의 메시지는 `"Inference exceeded ...ms"`(`DefaultLlmEngine.kt:108`)라서 시간 초과가 `Failed`로 찍힌다. `(exception as? LlmException)?.reason == TIMEOUT`으로 봐야 한다. 이번 측정은 실패 0건이라 결과에는 영향이 없다.
- **N2** `core/llm/build.gradle.kts:17` — 공개 API(`StateFlow`)가 coroutines 타입을 노출하므로 `api(libs.kotlinx.coroutines.android)`가 맞다. 지금은 app이 다른 경로로 받아서 빌드된다.
- **N3** `CorrectionTest.kt:28` — `wrong.substring(0, wrong.length)`는 아무 일도 하지 않는 줄이다.
- **N4** 검증 화면과 `sentences.json`은 release APK에도 들어간다 (진입만 막힘). 실제 기능 TASK에서 debug 소스셋으로 옮기거나 지운다.

### 벤치 결과 해석 (사용자 판단용)
- **형식 안정성은 충분하다.** 20/20 유효 JSON, 모순 0, 판정 일치 20/20. ADR 001이 우려한 "JSON 출력 불안정"은 이 프롬프트 형태에서는 재현되지 않았다.
- **속도는 문장당 약 22초**(최대 25.6초)다. 퀴즈 한 문제의 피드백으로는 버틸 만하지만, 스피킹처럼 긴 답변은 더 오래 걸릴 것이다. 그때는 별도로 측정해야 한다.
- **설명 품질이 약점이다.** 틀린 문장 14개 중 2개(14%)에서 문법 용어를 틀렸다 ("a"를 정관사, "bored"를 명사). 학습 앱에서 틀린 문법 설명은 판정 오류보다 해롭다.
- 측정 범위의 한계: 틀린 문장마다 오류가 하나씩만 있는 쉬운 문장이었다. 오류가 여러 개 섞인 문장과 긴 답변은 측정하지 않았다.
- 제안: 판정(`correct`)과 고친 문장(`corrected`)은 E4B 결과를 그대로 쓴다. 설명은 `type`별로 앱에 미리 써 둔 한국어 문구를 보여 주고, 모델이 쓴 설명은 "AI 설명"으로 구분해 보조로만 둔다. 문법 탭 TASK에서 정한다.
