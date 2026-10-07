# REVIEW: 문법 탭 2 — 직접 써 보기 + Gemma 교정

> 작성: Claude · 경로: docs/tasks/11-grammar-writing/REVIEW.md

## 1차 리뷰 — 2026-10-07
- 대상: `git diff 93880bd..8efdf8e` (+ HANDOFF `1401275`)
- 판정: **Approve** (Should-fix 1개는 다음 TASK에서 처리)

### 수용 기준
| AC | 판정 | 비고 |
|----|------|------|
| AC1~AC5 | 충족 | `WritingTest`, `CorrectionCoordinatorTest`(순서·실패 상태·취소 후 호출 1회), release DEX에 `LlmBenchScreen` 없음, words→grammar 의존 0 |
| AC6 | 충족 | S23+ 단원 2, 3문장: 맞음 11.6초 / 맞음 12.5초 / 틀림(`visit → visited`, tense) 17.9초. TASK 05 측정(약 22초)보다 빠름 |

### Must-fix
- 없음

### Should-fix
- **S1** `GrammarWritingViewModel.kt` `retry()` — 다시 시도 중 "취소"를 누르면 `cancel()`이 `isCorrecting = false`를 즉시 세워 "다시 쓰기"가 켜진다. 그 사이 `generate`가 막 끝나면(네이티브 생성은 중간에 멈추지 않음) 코루틴이 취소 확인 지점 없이 `results[index] = replacement`를 실행하는데, "다시 쓰기"로 목록이 비어 있으면 `IndexOutOfBoundsException`으로 앱이 종료된다.
  - 수정 방향: 교체 전에 `index < results.size`이고 그 칸이 같은 문장의 `Failed`인지 확인한다. 또는 `isCorrecting = false`를 `cancel()`이 아니라 작업의 `finally`에서만 세운다 (`startCorrection`의 늦게 도착한 결과 추가도 같은 방식으로 막힌다).

### Nit
- **N1** 취소해도 진행 중인 네이티브 생성은 끝까지 돈다. 바로 "다시 시도"를 누르면 엔진이 바빠 `ENGINE` 실패가 날 수 있다. 안내 문구("이전 교정이 끝나는 중이에요")로 충분하다.
- **N2** 정보: 실기기 검증에서 GPT가 머니로그 앱 데이터의 모델 파일(4.4GB)을 adb로 읽어 OPIc 앱 폴더에 복사했다 (다운로드 버튼은 누르지 않음, 머니로그 쪽은 읽기만). 결과는 같지만 다른 앱의 데이터를 다룬 것은 TASK 범위 밖이었다. 다음부터 이런 우회는 먼저 사용자에게 묻는다.

### 좋았던 점
- `CorrectionCoordinator`를 엔진 인터페이스에만 의존하게 만들어 가짜 엔진으로 순서·실패·취소를 테스트했다.
- TASK 10 Nit 3개를 모두 처리했다 (grammar.json 1회 파싱, NavHost를 `:app`의 `OpicRoot`로, choice 색).
- 토큰 입력칸에 `PasswordVisualTransformation`, 저장 후 값을 다시 보여 주지 않음.
