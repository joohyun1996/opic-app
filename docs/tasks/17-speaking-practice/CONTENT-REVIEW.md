# CONTENT-REVIEW: 스피킹 질문 은행 (speaking.json dataVersion 1)

> 작성: Claude (GPT 사용량 부족으로 사용자가 Claude 구현 요청, 2026-10-10) — 초안 작성자와 검토자가 같다. **GPT 교차 검토 권장**
> 대상: `docs/tasks/17-speaking-practice/speaking-draft.json` → `exports/speaking.json`

## 결과
- 15주제, 43문항 유지 (추가·삭제 없음). id 중복 없음, type은 6종 안
- 확인한 것: 영어 질문 말투(OPIc 실제 문항 형식 "You indicated that ~ / Tell me about ~ / Describe ~"), 한국어 번역, 팁이 유형에 맞는지

## 바꾼 문항
| id | 전 | 후 | 이유 |
|----|----|----|------|
| `roleplay_solve-3` | type: roleplay_solve | type: experience | 롤플레이 뒤에 붙는 경험 질문이라 답하는 방식이 과거 경험담. 팁도 과거 시제 |
| `home-1` | You indicated that you live in an apartment. Describe your home to me. What does it look like? How many rooms are there? | You indicated that you live in an apartment. Can you describe your home? What does it look like, and how many rooms does it have? | 실제 OPIc 말투(Can you describe ~?)에 가깝게, 문장 연결 자연스럽게 |
| `weather-2` | How is the weather today different from when you were a child? | How is the weather these days different from when you were a child? | "today"는 오늘 하루 날씨로 읽힘 → these days |
| `travel_abroad-1` | 현재완료(have visited) + 과거 묘사 | 현재완료(I have been to ~)로 시작 → 그때 일은 과거 시제로 묘사 | 질문 문구 "have visited"보다 말할 때 흔한 have been to로 안내 |

## 남은 검토 포인트 (GPT 교차 검토 때)
- 롤플레이 질문의 "Call ~ and ask three or four questions" 표현이 실제 시험 문구와 같은지
- 팁이 너무 짧은 문항(자기소개 등)에 예시 문장을 더할지
