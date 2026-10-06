# REVIEW: 단어 데이터 적재 + 홈·Day 목록 화면

> 작성: Claude · 경로: docs/tasks/06-words-import-day-index/REVIEW.md

## 1차 리뷰 — 2026-10-06
- 대상: `git diff 5bad698..7594feb`
- 판정: **Approve**
- 참고: 마무리(실기기 검증, debug 자원 이동, 커밋)를 Claude가 했으므로 독립 리뷰가 아니다. GPT 교차 리뷰는 선택 사항으로 남긴다.

### 수용 기준
| AC | 판정 | 비고 |
|----|------|------|
| AC1~AC11 | 충족 | HANDOFF 표 참고. 실기기 첫 적재 3,283ms(3초 초과, TASK 지시대로 보고만), 재실행 637ms |

### Must-fix
- 없음

### Should-fix
- 없음

### Nit (TASK 07에서 처리)
- **N1** `WordImporter.kt` `catch (error: Exception)` — `CancellationException`까지 `Failed`로 바꾼다. 취소는 다시 던져야 한다.
- **N2** `WordImporter.kt` — 버전이 같아도 2.4MB 전체를 파싱한다 (재실행 637ms). `dataVersion`만 먼저 확인하고, 저장된 값 이상이면 전체 파싱을 건너뛴다.
- **N3** `WordImporter.kt` — `metadata.insert` 다음 `metadata.update`를 연달아 부른다. 동작은 맞지만 "없으면 insert, 있으면 update" 의도가 코드에서 바로 보이지 않는다. 주석 한 줄이면 충분하다.

### 좋았던 점
- 검증 → 트랜잭션 → 예외를 결과로 변환하는 구조가 단순하고, 롤백 테스트(`collisionRollsBackEarlierUpdatesAndVersion`)가 TASK 04 N2를 정확히 막는다.
- `words.json`을 저장소에 복사하지 않고 Gradle `Sync`로 assets에 넣었다.
- debug/release `DebugContent` 분리로 release dex에서 `LlmBenchScreen`이 완전히 빠졌다.
