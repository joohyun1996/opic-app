# TASK: 스피킹 3 — 문항 확대(설문 전 주제·돌발·고난도) + 모의고사

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "18 19 20 진행") · 구현: Claude
> 경로: docs/tasks/20-speaking-bank-mock/TASK.md
> 근거: 사용자 결정 (2026-10-10) — 현재 IM2, 목표 1차 IH → 2차 AL. 설문 주제는 "전부 넓게", 모의고사 넣기

## 목표
질문 은행을 실제 OPIc 구성(사전 설문 주제 + 돌발 + 롤플레이 + 고난도)에 맞춰 약 150문항으로 늘리고, 문항마다 목표 등급(IM/IH/AL)을 붙인다. 실제 시험 순서의 15문항 모의고사를 만든다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/speaking.json` | 수정 | dataVersion 2, 주제·문항 확대, `category`·`level` 추가 (기존 43문항 id 유지) |
| `docs/tasks/20-speaking-bank-mock/CONTENT-REVIEW.md` | 생성 | 추가 주제 목록·등급 기준 |
| `core/common/.../Speaking.kt` + 테스트 | 수정 | `category`·`level`·type `issue` 검증, `buildMockExam` |
| `feature/speaking/**` | 수정 | 주제 목록 구역 나누기·등급 표시, 모의고사 화면 |

## 데이터 형식 (dataVersion 2)
- `SpeakingTopic.category` ∈ `intro | survey | unexpected | roleplay` (기본 survey)
- `SpeakingQuestion.level` ∈ `IM | IH | AL` (기본 IM), type에 `issue`(사회 이슈·의견) 추가
- 등급 기준: describe·routine = IM, experience·compare·roleplay = IH, issue·고난도 compare = AL
- 롤플레이는 상황 하나가 주제 하나: [roleplay_ask, roleplay_solve, experience] 3문항 (실제 시험 11~13번 형식)

## 모의고사 (실제 OPIc 순서, 15문항)
| 번호 | 내용 | 고르는 법 |
|------|------|----------|
| 1 | 자기소개 | intro |
| 2~4 | 설문 주제 A 3문항 | survey 주제 무작위, 앞 3문항 |
| 5~7 | 설문 주제 B 3문항 | A와 다른 survey 주제 |
| 8~10 | 돌발 주제 3문항 | unexpected 주제 |
| 11~13 | 롤플레이 1세트 | roleplay 주제 (질문하기 → 문제 해결 → 관련 경험) |
| 14 | 비교·변화 | 위에서 안 쓴 주제의 compare 문항 |
| 15 | 사회 이슈 | issue 문항 |
- 흐름: 문항마다 TTS 자동 재생·다시 듣기 1회·최대 2분 녹음(`mock-01.pcm`~`mock-15.pcm`), 다음 문항으로. 15번 뒤 한꺼번에 받아 적기(진행 n/15, 취소 가능) → 요약 목록(문항별 말한 시간·분당 단어·머뭇거림) → 누르면 기존 결과 화면(고치기·교정·발음 힌트)
- `buildMockExam(catalog, random)`: 위 규칙. 조건에 맞는 주제가 없으면 null

## 수용 기준
- [x] AC1: 새 speaking.json 통과 — 140문항 이상, 기존 43개 id 모두 유지, category 4종·level 3종 모두 사용 (테스트)
- [x] AC2: 잘못된 category·level·type은 null (테스트)
- [x] AC3: `buildMockExam` — 15문항, 1번 intro, 2~4·5~7 서로 다른 survey 주제, 8~10 unexpected, 11~13 roleplay 한 주제 순서(ask, solve, experience), 14 compare, 15 issue, 같은 문항 중복 없음, 고정 seed로 결정적 (테스트)
- [x] AC4: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, 권한·Room 변경 없음
- [ ] AC5: 실기기 — 주제 목록 구역·등급 표시, 모의고사 2문항 녹음 후 "끝내기"로 요약까지 (사용자 확인)

## 범위 밖
- 모의고사 기록 저장·등급 예측 (TASK 21 이후), 시험 전체 시간 제한, Ava 아바타
