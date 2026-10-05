# REVIEW: 웹앱 코드 제거 + 규칙 문서 Kotlin/Room 기준 개정

> 작성: Claude · 경로: docs/tasks/03-remove-web/REVIEW.md

## 1차 리뷰 — 2026-10-05
- 대상: `git diff fab2047..4b327e0` (`9a2445a`, `4b327e0`)
- 판정: **Approve**

### 수용 기준
| AC | 판정 | 비고 |
|----|------|------|
| AC1~AC8 | 충족 | HANDOFF의 명령 출력과 `git diff --stat`(70 files, -20,905줄)으로 확인 |

### Must-fix
- 없음

### Should-fix
- 없음

### Nit
- `AGENTS.md` "SPEC.md 규칙"의 "API 변경 시 이전·이후 URL 모두 기록"은 서버가 없어진 지금 맞지 않는다. 다음 AGENTS.md 개정 때 "Repository/DAO 변경 시 이전·이후 시그니처 기록"으로 바꾸면 된다.

### 좋았던 점
- 데이터 규칙(REPLACE 금지, seq 불변, Day 공식, dataVersion upsert, language 필터)이 빠짐없이 들어갔다.
- 자동 승인에서 `rm -rf app`이 거부되자 무리하게 진행하지 않고 보고했다. 로컬 잔여물은 Claude가 확인했다. 빈 폴더와 Prisma 생성 파일뿐이었고 사용자 작업물은 없었다. 확인 후 삭제했다 (`.env*` 유지).

### 참고: TASK 02 교차 리뷰 Should-fix
- "`finalize`가 영어 뜻·예문 번역 빈 값을 직접 검사" — TASK 03에서 `scripts/`가 삭제되어 해당 없음. 같은 검사는 앱의 `words.json` 로더 테스트(TASK 04 이후)에서 다룬다.
