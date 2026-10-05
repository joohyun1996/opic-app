# REVIEW: 고급 영어 단어 추가

> 작성: GPT · 경로: docs/tasks/02-advanced-words/REVIEW.md

## 1차 리뷰 (GPT 교차 검증) — 2026-10-05
- 대상: `git diff cf971e9..4d1e460 -- . ':!exports/.cache'`
- 판정: **Approve**

### 수용 기준
| 항목 | 판정 | 근거 |
|------|------|------|
| CSV·apply·finalize | ✅ | `scripts/lib/advanced-filter.ts:23-42`는 인용 필드의 쉼표·줄바꿈·이중 따옴표를 처리한다. `scripts/collect-advanced-words.ts:63-92`는 재적용 시 판정 중복을 건너뛰고 작성·IPA 값을 같은 값으로 덮어쓴다. `:141-152`는 필수 필드와 IPA 실패를 검사한다. |
| 데이터 | ✅ | 현재 `exports/words.json`: dataVersion 1, 영어 5,517개, seq 1~5517 연속, `(language, word)` 중복 0, 빈 뜻 0. Day 1·70·138 모두 level 1~5 포함. 영어 전용 재생성은 HANDOFF의 추가 변경에 기록됨. |
| 품질 | ✅ | 시드 `20261005`로 50개 추출. 발음·품사·뜻·예문·번역 오류 0개, 확실히 쉬운 단어 0개. |
| 범위·규칙 | ✅ | 지정 diff에서 웹 코드, 금지 import, 비밀 파일 변경 없음. `exports/.cache/`는 추적되지 않음. |
| 검사 | ✅ | `npx tsc --noEmit` 통과, `npm test` 55개 통과. |

### Must-fix
- 없음 (0개)

### Should-fix
- **S1** `scripts/collect-advanced-words.ts:145` — `finalize`가 `meaningEn`, `exampleKo`의 빈 값을 직접 검사하지 않는다. 현재 출력은 `scripts/export-words.ts:47-54`가 막고 있으며 빈 값도 없지만, `finalize` 단독 실행 시 미완성 원본을 완료로 표시할 수 있다. 원본 완성 검사를 확장할 것.

### Nit
- 없음

### 품질 오류
- 없음 (0개). 따라서 `단어: 문제 → 고칠 값` 항목 없음.

### 좋았던 점
- 판정·작성·IPA 배치가 남아 데이터 생성 과정을 추적할 수 있다.
