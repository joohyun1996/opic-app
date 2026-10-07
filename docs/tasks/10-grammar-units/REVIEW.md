# REVIEW: 문법 탭 1 — 콘텐츠 검토·적재 + 단원 목록·설명·연습 문제 화면

> 작성: Claude · 경로: docs/tasks/10-grammar-units/REVIEW.md

## 1차 리뷰 — 2026-10-07
- 대상: `git diff e59c125..645f63f` (+ HANDOFF `9f0421a`)
- 판정: **Approve**

### 수용 기준
| AC | 판정 | 비고 |
|----|------|------|
| AC1~AC7 | 충족 | HANDOFF 표와 테스트 이름으로 확인. 결과 화면은 실기기 대신 `GrammarViewModelTest`로 검증 (단원당 5문제 제한 준수) |

### 콘텐츠 검토 (CONTENT-REVIEW.md)
- 30문제 전수 기록, 7문제 수정. 수정 이유가 모두 타당하다.
  - u1-05: `I ___` + `'m taking` → `I 'm taking`이 되는 문제를 잡아 answers에서 뺀 것, 현재진행형을 prompt에 명시한 것
  - u2-09, u3-04, u3-10: 다른 해석(had, would의 조건·추측)이 가능한 문맥을 prompt로 고정한 것
  - u1-06, u1-09: 힌트가 정답 단어를 그대로 말하던 것을 고친 것

### Must-fix / Should-fix
- 없음

### Nit (TASK 11에서 함께 처리 권장)
- **N1** `WordsApp.kt:38-43`, `GrammarFlow.kt:16-19` — `grammar.json`을 Compose 안(메인 스레드)에서 두 번 읽고 파싱한다. 20KB라 체감은 없지만, TASK 11에서 앱 쪽 연결(LLM 엔진 전달)을 손볼 때 `OpicApplication`에서 한 번 읽어 넘기면 된다.
- **N2** `feature/words/build.gradle.kts` — 단어 모듈이 문법 모듈에 의존한다. TASK 11은 `:core:llm` 엔진을 문법 화면에 넘겨야 하므로, 이때 내비게이션(NavHost)을 `:app`으로 옮기면 단어·문법 모듈이 서로 몰라도 된다. HANDOFF의 판단(지금 옮길 필요 없음)은 TASK 10 범위에서는 맞다.
- **N3** `GrammarExerciseScreen.kt` — 선택된 choice 버튼 색이 Material 기본 `primaryContainer`(보라 계열)다. 단어 탭의 흑백·상태색 팔레트와 맞추면 좋다.

### 좋았던 점
- 두 번 시도 규칙을 `GrammarAttempt`라는 순수 상태 기계로 만들어 테스트하기 쉽다.
- `fix` 문제에서 입력칸을 원문으로 미리 채워 수정만 하게 한 것이 TASK 의도와 맞다.
- 작업 중 다른 커밋(e59c125)이 끼어든 것을 HANDOFF에 명시하고 리뷰 범위를 정확히 적었다.
