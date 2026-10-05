# REVIEW: 단어 DB → words.json 내보내기

> 작성: Claude · 경로: docs/tasks/01-export-words/REVIEW.md

## 1차 리뷰 — 2026-10-04
- 대상: 커밋 안 된 작업 트리 (base `5de210f`), 신규 4개 파일
- 판정: **Request changes** (코드는 문제없음. 데이터와 절차 때문에 막힘)

### 수용 기준
| AC | 판정 | 비고 |
|----|------|------|
| AC1~AC9 | 충족 | `tests/lib/word-export.test.ts:21-132`에 AC별 테스트가 있음 |
| AC3 | 픽스처로만 충족 | 실제 데이터로 확인하지 못함 (아래 M1) |
| AC10 | **미충족** | 출력이 빈 배열 (`exports/words.json`의 `"words": []`) |

### Must-fix
- **M1. 내보낼 단어 데이터가 없다** — `exports/words.json:5`
  - 원인을 확인했다. `8c4c9d7 "단어 DB 추가"`는 단어 데이터가 아니라 씨드 **스크립트**만 추가한 커밋이다. `scripts/seed-words-en.ts:11-14`는 외부 단어 목록, Free Dictionary API, Claude Haiku로 런타임에 단어를 생성한다. 이 스크립트가 실행된 적이 없어서 DB(`opic_db`)가 비어 있다.
  - ADR 001의 "재사용할 것: 단어 DB" 전제가 틀렸다. GPT가 고칠 수 있는 문제가 아니라 **사용자 결정 사항**이다 (아래 "사용자 결정 필요").
- **M2. HANDOFF.md 파일이 없다** — `docs/tasks/01-export-words/`
  - 워크플로우상 리뷰 기준 문서다. 사용자에게 전달한 HANDOFF 내용을 템플릿대로 파일로 저장할 것.

### Should-fix
- 없음

### Nit
- `scripts/lib/word-export.ts:91` — 기존 파일의 `language`가 `en`/`zh`인지 검증하지 않는다. 손으로 고친 JSON에 오타가 있어도 통과한다. 검증 오류 메시지에 추가하면 좋다.

### 린트 실패 (예외 인정)
- `npm run lint` 오류 379개는 기존 파일에서 나온 것이고, 신규 파일은 통과했다 (HANDOFF 기준). 웹 코드는 TASK 02에서 전부 삭제되므로 이번 TASK에서는 완료 조건 예외로 인정한다.

### 좋았던 점
- 셔플 전에 `(language, word)`로 정렬해서 DB 조회 순서에 영향받지 않는다 (`word-export.ts:85-86`).
- 바뀐 게 없으면 `exportedAt`도 유지해서 diff가 생기지 않는다 (`word-export.ts:131`).
- 기존 파일의 seq 단조 증가, 버전, 시드를 검증한다 (`word-export.ts:93-104`).
- 삭제된 단어가 다시 나타나면 복구한다 (AC5 테스트). TASK에 없던 경우까지 처리했다.
- 패키지를 설치하지 않았고 범위 밖 수정도 없다.

### 사용자 결정 필요 (M1)
단어 데이터를 어떻게 만들지 정해야 한다. 웹 코드를 지우기(TASK 02) **전에** 정해야 한다.
1. **기존 씨드 스크립트를 1회 실행:** 영어 5,000개 + 중국어. Haiku 유료 API 호출이 마지막으로 1회 발생하고, 외부 API에 의존한다. 실행 후 TASK 01 스크립트를 다시 돌리면 끝난다.
2. **Claude Code(구독)로 JSON 직접 생성:** API 과금이 없지만 분량이 많아 여러 번에 나눠야 하고, 뜻·발음 품질을 별도로 검수해야 한다. 이 경우 내보내기 입력이 DB가 아니라 JSON이 되므로 TASK를 수정해야 한다.
3. **앱 출시 후 Gemma로 생성:** 기기 내 생성은 속도와 품질이 불확실하다. 추천하지 않는다.

리뷰어 추천: 1번. 스크립트가 이미 있고, 비용은 Haiku 수백 회 호출 수준이다. 이후 유지보수는 `words.json` 직접 수정으로 한다. 단, 씨드 스크립트는 `lib/prisma.ts` 싱글톤을 쓰지 않는 등(`scripts/seed-words-en.ts:6-7`) 규칙 위반이 있으니, 1회 실행용으로만 쓰고 고치지 않는다.

### 1차 리뷰 후속 결정 (2026-10-04, 사용자)
- M1 대응: 선택지 1~3을 모두 버리고, **GPT가 원래 출처에서 직접 조회하고 한국어 뜻을 직접 작성**한다. 유료 API 호출 없음.
- TASK.md에 "추가 요구사항"과 AC11~AC14를 추가했다. 수정 범위에 `scripts/collect-words.ts`와 `exports/source/*.json`을 추가했다.
- M2(HANDOFF.md 저장)는 그대로 유지한다.

---

## 2차 리뷰 (GPT 교차 검증) — 2026-10-05
- 대상: `git diff 5de210f..636d189 -- . ':!exports/.cache'`
- 판정: **Approve**
- 수용 기준: `exports/words.json`의 현재 영어 첫 배포본은 dataVersion 1, 5,517개, seq 1~5517 연속, 중복·빈 뜻 0개다. Day 1·70·138 모두 level 1~5가 섞인다. TASK 01의 1,100개는 TASK 02의 영어 전용 재생성에 포함됐다.
- Must-fix: 없음 (0개)
- Should-fix: 없음 (0개)
- Nit: 없음
- 품질 표본: 시드 `20261005`로 현재 출력에서 50개 추출. 발음·품사·뜻·예문·번역 오류 0개, 토익 800점이면 확실히 알 쉬운 단어 0개.
- 검증: `npx tsc --noEmit` 통과, `npm test` 55개 통과. `scripts/export-words.ts:38-56`은 필수 영어 필드와 번역을 검사한다.
