# TASK: 문법 탭 3 — 단원 4~10 콘텐츠 + TASK 11 Should-fix

> 작성: Claude · 승인: [x] 사용자 (2026-10-07) · 구현: Claude (GPT 토큰 소진)
> 경로: docs/tasks/12-grammar-units-4-10/TASK.md
> 선행: TASK 11 Approve (`f317584`)

## 목표
1. 단원 4~10(현재완료, 비교급, 의문문, 관사, 전치사, 수 일치, 연결어)의 설명·문제 70개·쓰기 과제를 써서 `exports/grammar.json`에 추가한다 (dataVersion 2)
2. TASK 11 REVIEW S1 — 다시 시도 중 취소 → 다시 쓰기 시 `IndexOutOfBounds` 가능성을 없앤다

원래는 "Claude 초안 → GPT 검토·반영"이지만 GPT 토큰이 없어 Claude가 작성과 자체 검토를 함께 한다. TASK 10에서 GPT가 찾은 패턴을 검토 기준으로 쓴다: 빈칸 뒤 축약형 충돌, 힌트가 정답을 그대로 말함, 정답이 둘 이상 가능한 문맥.

## 수정 범위
| 파일 | 작업 |
|------|------|
| `docs/tasks/12-grammar-units-4-10/units-4-10.json` | 생성 (초안 원본) |
| `exports/grammar.json` | 수정 (단원 4~10 추가, dataVersion 1 → 2) |
| `feature/grammar/src/test/.../GrammarCatalogTest.kt` | 수정 (10단원·100문제·110 id) |
| `feature/grammar/.../GrammarWritingViewModel.kt` | 수정 (S1) |

## 수용 기준
- [ ] AC1: 단원 10개(order 1~10), 문제 100개, 단원·문제 id 110개 중복 없음, choice answer 범위 안, blank에 `___` (테스트·병합 스크립트)
- [ ] AC2: S1 — 늦게 끝난 다시 시도 결과는 그 칸이 같은 실패 결과일 때만 교체, 취소 뒤 늦게 끝난 교정 결과는 붙이지 않는다
- [ ] AC3: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과
- [ ] AC4: S23+ — 단원 목록 10개, 새 단원 설명 화면, 새 단원 문제 2개(두 번 시도 흐름)
