# REVIEW: TASK 45 수능 어휘 744개 추가

> 작성: Claude · 범위: `git diff 2e51df4..900dbea`

## 판정: Request changes (Must-fix 1)

코드(v4→v5 Migration, `deck` 적재, `language`·`deck` 필터, 라벨)는 문제없다. REPLACE를 쓰지 않았고 기존 seq 해시도 일치한다. 콘텐츠 1건만 고친다.

## Must-fix

1. **실제로 안 쓰는 어근을 표제어로 넣었다** — `exports/words.json` seq 5545 `aggress`, 5703 `destruct`, 5750 `environ`, 5757 `evitable`, 5866 `illude`, 5999 `oversea`, 6136 `sophisticate`
   - 원인: TASK 2단계 결정의 "괄호 표기는 표제어만" 규칙(Claude 작성)이 틀렸다. 고시 원문은 `environ (environment)`처럼 어근을 표제어로 두고, 학습할 단어는 괄호 안에 둔다
   - 결과: `*`가 붙은 추측 뜻으로 거의 안 쓰는 단어를 외우게 되고, 정작 environment·inevitable·aggressive 같은 단어는 빠졌다
   - 수정 방향 (seq 규칙 유지, dataVersion 3):
     - 위 7개는 행을 지우지 말고 `deleted: true`로 바꾼다 (이미 설치된 기기에 있음)
     - 괄호 안 단어(aggressive, destruction, environment, inevitable, illusion, overseas)를 seq 6265부터 뒤에 추가한다 (Day 157 남은 16칸 안). `sophisticated`는 core에 이미 있으므로 추가하지 않는다
     - 괄호 표기 48줄을 다시 본다. 표제어가 학습용으로 단독 사용되지 않는 경우(예: `datum (data)`, `opportune (opportunity)`)도 같은 방식으로 처리한다. 괄호 안 단어가 이미 words.json에 있으면 추가하지 않는다
     - 바꾼 목록을 HANDOFF에 표로 적는다 (deleted 처리 / 추가 / 유지)
   - 테스트: WordImporterTest에 "deleted 7개는 해당 Day에서 숨는다, 새 seq 고유, 기존 seq 해시 불변"을 추가한다

## Nit
- `level` 4 일괄은 괜찮다

## 2차 리뷰 (`1eb4809..9cb715e`)

### 판정: Approve
- Must-fix 1 반영: 어근 9개 `deleted: true`(행 유지), 실제 단어 8개 seq 6265~6272 추가, dataVersion 3. seq 중복 없음, 기존 seq·Day 해시 불변 테스트 확인
- `수능` 라벨 제거는 사용자 요청으로 기록됨. `deck` 데이터는 유지

## 후속 (2026-10-13, 사용자 요청: 수능 단어 섞기)
- 사용자 요청으로 수능 752행의 seq를 5521~6272 안에서 섞었다(시드 20261013). dataVersion 4. core 5,517개 seq는 그대로다
- 적재기는 `REORDER_DECKS = {suneung}`만 파일 seq로 다시 배치한다. 옮길 행은 먼저 `seq = -id`로 비켜 두어 `(language, seq)` unique 충돌을 피한다. wordId가 바뀌지 않으므로 UserWord는 유지된다
- 테스트: `reorderDeckMovesSequenceButCoreKeepsIt` 추가(맞바꾸기·core 유지·학습 기록 유지). test·lint·release 통과, 실기기 `install -r` 후 클론 삭제
