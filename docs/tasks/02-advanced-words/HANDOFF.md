# HANDOFF: 고급 영어 단어 추가 (GRE·TOEFL·IELTS)

> 작성: Claude (구현 대행, GPT 토큰 소진) · 경로: docs/tasks/02-advanced-words/HANDOFF.md

## 커밋 범위
- base: `cf971e9`
- 단계별 커밋 (아래 진행 상황)

## 진행 상황
| 단계 | 상태 | 커밋 | 비고 |
|------|------|------|------|
| 02-1 수집·기계 필터 | 완료 | (이 커밋) | 후보 5,180개 |
| 02-2 판정 | 대기 | | |
| 02-3 뜻·예문 작성 | 대기 | | |
| 02-4 IPA 조회 | 대기 | | |
| 02-5 출력·검증 | 대기 | | |

## 이어하는 방법 (GPT·Claude 공통)
- 진행 상황 확인: `npx vite-node scripts/collect-advanced-words.ts status`
  - `unjudged` > 0 → 02-2: 후보 `words` 배열 순서(frq 오름차순)대로 판정해 `exports/source/advanced-batches/judge-NN.tsv` 작성 → `apply`
  - `unfilled` > 0 → 02-3: `fill-NN.tsv` 작성 → `apply`
  - `noIpa` > 0 → 02-4: `ipa` (재실행 시 이어서 진행)
  - 모두 0 → 02-5: `finalize` → `export-words.ts` 기본 입력에 advanced 추가 → 재생성
- ECDICT 원본이 없으면 `collect`가 `exports/.cache/`에 다시 받는다 (`collect`를 다시 돌리면 source가 초기화되므로 주의)

## 검증 결과 (02-1)
- `npx tsc --noEmit --incremental false`: 통과
- `npm test`: 55개 통과 (신규 15개)
- 신규 파일 eslint: 통과
