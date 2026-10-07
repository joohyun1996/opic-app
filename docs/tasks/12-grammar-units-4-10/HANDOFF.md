# HANDOFF: 문법 탭 3 — 단원 4~10 + TASK 11 S1

> 작성: Claude (구현 대행) · 경로: docs/tasks/12-grammar-units-4-10/HANDOFF.md

## 커밋 범위
- base: `f317584`
- head: 이 커밋 하나

## 변경 요약
```diff
+ units-4-10.json (초안 원본, 단원 7개 × 문제 10개)
~ exports/grammar.json — 단원 4~10 추가, dataVersion 2 (단원 1~3은 그대로)
~ GrammarCatalogTest — 10단원, 100문제, id 110개, order 1~10
~ GrammarWritingViewModel — retry: 그 칸이 아직 같은 Failed일 때만 교체 / correct 콜백: 취소됐으면 결과를 붙이지 않음 (ensureActive)
```

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1 | 통과 | `GrammarCatalogTest.catalogHasHundredValidExercises` + 병합 스크립트 검증 (id 중복, choice 범위, blank `___`) |
| AC2 | 통과 (코드) | `GrammarWritingViewModel.retry`·`startCorrection` 수정. ViewModel 단위 테스트는 `HfTokenStore`(Android 암호화 저장소) 의존 때문에 추가하지 않았다 — 기존 `CorrectionCoordinatorTest`는 통과 |
| AC3 | 통과 | `./gradlew test lint :app:assembleDebug :app:assembleRelease` exit 0 |
| AC4 | 통과 | 홈 "단원 10개", 목록 끝까지 단원 10 표시(`unit-list.png`), 단원 10 설명(`unit10-explanation.png`), u10-05 1차 오답 → 힌트(`unit10-hint.png`) → 2차 정답 "두 번째 시도에 맞혔어요". 문제는 2개만 풀었다 |

## 자체 콘텐츠 검토 (GPT 검토 대신)
- 빈칸 뒤 축약형 충돌: 빈칸 정답이 `I`·`you` 바로 뒤에 오는 문제(u4-01, u4-09)는 정답을 비축약형만 두고 prompt에 "현재완료로"를 명시
- 정답이 둘 이상인 문맥은 answers에 함께 넣음: u4-06(has worked / has been working), u5-04(much/a lot/far), u6-06(How much is …), u8-06(walk to work), u10-04·u10-06(although/even though/despite the rain), u10-07(Then/Next/After that)
- 해석이 갈릴 수 있는 문제는 prompt로 문맥 고정: u4-03(지금도 살고 있음), u4-06(지금도 일함), u7-06(개 전체 이야기), u9-06(틀린 곳 두 군데)
- 초안 단계에서 버린 문제: "Let's meet ___ the bus stop."(at/by/near 모두 가능), "What time ___ the store open tomorrow?"(will도 가능), "Neither of my brothers ___"(like/likes 둘 다 쓰임)
- 실기기에서 고친 것: u10-05 설명 `However,.` → `However, ~`

## 리뷰어가 봐야 할 곳
- u5-04 `than yesterday`는 엄밀히는 `than it was yesterday`지만 구어에서 자연스러워 그대로 둠
- GPT 토큰이 돌아오면 단원 4~10 콘텐츠 교차 검토를 권장 (TASK 10처럼 CONTENT-REVIEW.md)
