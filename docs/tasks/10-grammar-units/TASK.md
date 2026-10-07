# TASK: 문법 탭 1 — 콘텐츠 검토·적재 + 단원 목록·설명·연습 문제 화면

> 작성: Claude · 승인: [x] 사용자 (2026-10-07)
> 경로: docs/tasks/10-grammar-units/TASK.md
> 선행: TASK 09 Approve (`53bf0a9`)
> 근거: 이 문서 "설계 근거", `SPEC.md` § 기기 내 LLM + 문장 교정 (TASK 05)

## 목표
문법 탭의 첫 TASK. Claude가 쓴 단원 1~3 콘텐츠 초안(`grammar-draft.json`)을 **GPT가 검토하고 고쳐서** `exports/grammar.json`으로 확정한다. 그 데이터로 **단원 목록 → 설명 → 연습 문제(틀리면 기회 한 번 더) → 결과** 화면을 만든다.

## 설계 근거 (사용자 결정 2026-10-07)
| 원리 | 연구 | 이 TASK에서 |
|---|---|---|
| 명시적 문법 설명 | Norris & Ortega (2000) | 단원마다 짧은 한국어 설명 + 예문 + 흔한 실수 |
| 인출 연습 | Roediger & Karpicke (2006) | 설명 직후 문제 10개 (고치기·빈칸·고르기 섞음) |
| 스스로 고치게 하는 피드백 | Lyster & Saito (2010) | 틀리면 정답 대신 힌트를 주고 **한 번 더** 기회 |

직접 쓰기 + Gemma 교정은 TASK 11, 단원 4~10은 TASK 12, 간격 복습(스키마 변경)은 TASK 13.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/grammar.json` | 생성 | 초안을 검토·수정한 확정본 |
| `docs/tasks/10-grammar-units/CONTENT-REVIEW.md` | 생성 | 콘텐츠 검토 기록 (아래 1) |
| `core/common/.../GrammarGrading.kt` + 테스트 | 생성 | 정답 비교 순수 함수 |
| `feature/grammar/**` | 생성 | 새 모듈: 데이터 파싱, 화면, ViewModel |
| `settings.gradle.kts`, `app/build.gradle.kts` | 수정 | 모듈 추가, `grammar.json`을 assets에 포함 (words.json과 같은 방식) |
| `feature/words/.../WordsApp.kt` | 수정 | 홈에 "영어 문법" 카드, 문법 경로 연결 (또는 앱 내비게이션을 app 쪽으로 옮겨도 됨 — HANDOFF에 이유 기록) |

**Room 스키마 변경 없음.** 진행 기록은 저장하지 않는다 (TASK 13에서 테이블 추가). 새 라이브러리·권한 없음.

## 요구사항

### 1. 콘텐츠 검토 (GPT)
`docs/tasks/10-grammar-units/grammar-draft.json`을 읽고 확인한다:
- 설명·예문·흔한 실수가 문법적으로 맞는지, 한국어 설명이 정확하고 자연스러운지
- 문제마다 **정답이 하나로 정해지는지**, 다른 정답도 가능하면 `answers`에 추가했는지 (예: 축약형 `I'm`, 쉼표 유무)
- `hint`가 정답을 그대로 알려 주지 않는지 (기회 한 번 더의 의도)
- 수준: 토익 800~900, OPIc IH~AL 목표에 맞는지
고친 내용은 `CONTENT-REVIEW.md`에 "문제 id: 원문 → 수정, 이유" 표로 남기고, 확정본을 `exports/grammar.json`에 저장한다. **형식(키 이름)은 바꾸지 않는다.**

### 2. 형식 (`exports/grammar.json`)
- `dataVersion`, `units[]`: `id, order, title, opicUse, errorType, explanation{summary, points[], examples[{en,ko}], commonMistakes[{wrong,right,note}]}, exercises[], writingTask{promptKo, promptEn, minSentences}`
- 문제 `kind`: `fix`(문장 전체 다시 쓰기, `answers[]`), `blank`(빈칸 단어, `answers[]`), `choice`(`choices[]`, `answer` = 정답 인덱스). 모두 `id, prompt, sentence, hint, explanation`
- 적재 시 검증: id 중복 없음, `fix`/`blank`는 answers 1개 이상, `choice`는 answer가 범위 안. 실패하면 문법 탭에 "문법 데이터를 불러오지 못했습니다"를 띄우고 앱은 죽지 않는다.

### 3. 채점 (`core:common/GrammarGrading.kt`)
- `normalizeAnswer`: 앞뒤 공백 제거, 연속 공백 하나로, 소문자, 끝의 `. ! ?` 제거, 곡선 따옴표(’)를 `'`로
- `gradeText(input, answers)`: 정규화 후 answers 중 하나와 같으면 정답
- `choice`는 인덱스 비교

### 4. 화면 (`:feature:grammar`, Compose)
- **홈:** 기존 홈에 "영어 문법" 카드 추가 (단원 수 표시). 누르면 단원 목록
- **단원 목록:** order 순. 제목, `opicUse`, 문제 수
- **설명:** summary, points, 예문(영어 + 한국어), 흔한 실수(빨간 ✗ wrong → 초록 ✓ right + note). 하단 "문제 풀기"
- **연습 문제:** 단원의 문제 10개를 **섞어서** 한 장씩 ("3 / 10")
  - `fix`: 원문을 보여 주고 고친 문장 전체를 입력. 입력칸은 원문으로 미리 채워 수정만 하게 한다
  - `blank`: `___`가 있는 문장 + 빈칸 입력
  - `choice`: 선택지 버튼
  - **1차 오답:** "다시 생각해 보세요" + `hint`. 입력을 지우지 않고 다시 풀게 한다
  - **2차 오답:** 정답(첫 번째 answer 또는 정답 선택지) + `explanation`
  - **정답:** 초록 표시 + `explanation`. 1차에 맞혔는지 2차에 맞혔는지 구분해 기록
  - "다음"으로 넘어감. 키보드가 문제를 가리지 않게 (TASK 07의 카드 레이아웃 방식)
- **결과:** 1차 정답 수 / 2차 정답 수 / 틀린 문제 수, 틀린 문제 목록(문장 + 정답). "다시 풀기"(다시 섞기), "단원 목록"
- 직접 쓰기(`writingTask`)는 결과 화면 아래에 "직접 써 보기 — 다음 업데이트" 비활성 버튼으로만 둔다 (TASK 11)

## 수용 기준
- [ ] AC1: `CONTENT-REVIEW.md`가 있고, 검토한 문제 30개 각각에 "수정 없음" 또는 수정 내용이 적혀 있다
- [ ] AC2: `exports/grammar.json` 파싱 테스트 — 단원 3개, 문제 30개, id 중복 없음, 모든 choice의 answer가 범위 안
- [ ] AC3: `gradeText`가 `"I'm taking"`/`"I’m taking"`/`"  am taking. "` 등 정규화 케이스와 오답 케이스를 맞게 판정한다 (테스트)
- [ ] AC4: 1차 오답 → 힌트, 2차 오답 → 정답 공개, 1차/2차 정답 구분이 ViewModel 테스트 또는 순수 함수 테스트로 확인된다
- [ ] AC5: 검증 실패 JSON(중복 id, 범위 밖 answer)이면 화면에 안내만 뜨고 예외를 던지지 않는다 (테스트)
- [ ] AC6: `./gradlew test lint` 통과, release APK assets에 `grammar.json` 포함
- [ ] AC7: S23+에서 확인 — **단원마다 문제 5개까지만 푼다** (40장·10문제 전체 풀기 금지): 단원 1 설명 화면, 1차 오답 힌트, 2차 오답 정답 공개, 결과 화면(5문제 후 뒤로 나가지 말고 결과까지 필요하면 단원 3의 문제를 빠르게 넘김) 스크린샷

## 제약 / 주의
- Room 스키마 변경 금지, `REPLACE` 금지, 새 권한 금지
- 채점 함수는 순수 Kotlin + 단위 테스트
- 화면 파일은 기능별로 나눈다
- Gemma(LLM)를 이 TASK에서 쓰지 않는다

## 범위 밖
- 직접 쓰기 + Gemma 교정 (TASK 11), 단원 4~10 (TASK 12), 진행 기록·간격 복습 (TASK 13)
