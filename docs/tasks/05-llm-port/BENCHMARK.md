# TASK 05 — S23+ 문장 교정 실기기 측정

- 측정 날짜: 2026-10-05 (한국 시간)
- 기기: 갤럭시 S23+ (SM-S916N), Android 16/API 36
- 모델: Gemma 3n E4B, `gemma-3n-e4b-it.task`, 4,405,655,031바이트
- 모델 준비: 머니로그 앱 파일을 기기 내부 `run-as` 파이프로 이 앱의 `no_backup/llm`에 복사. 원본은 읽기만 했고 복사 전후 크기가 일치했다.
- 프롬프트: `buildCorrectionPrompt` 첫 버전. 재측정이나 프롬프트 수정 없음.
- 방법: debug 화면에서 20문장을 순서대로 실행. 문장별 제한 120초, `SystemClock.elapsedRealtime()`로 측정.

## 문장별 결과

| 번호 | 문장 | 기대 | 결과 | 소요(ms) | 판정 일치 |
|---:|------|------|------|---------:|-----------|
| 1 | I usually take the bus to work. | 맞음 | Ok | 24,838 | 예 |
| 2 | My sister enjoys cooking on weekends. | 맞음 | Ok | 24,505 | 예 |
| 3 | We visited a museum last Saturday. | 맞음 | Ok | 15,939 | 예 |
| 4 | There are many cafes near my house. | 맞음 | Ok | 13,799 | 예 |
| 5 | I have lived here for three years. | 맞음 | Ok | 14,569 | 예 |
| 6 | The weather was nice, so we went for a walk. | 맞음 | Ok | 16,302 | 예 |
| 7 | Yesterday I go to the movies with my friend. | 틀림 | Ok | 20,426 | 예 |
| 8 | She don't like crowded places. | 틀림 | Ok | 21,881 | 예 |
| 9 | I bought book at the station. | 틀림 | Ok | 20,588 | 예 |
| 10 | He is interested on learning English. | 틀림 | Ok | 21,032 | 예 |
| 11 | We was tired after the trip. | 틀림 | Ok | 22,771 | 예 |
| 12 | I have seen him yesterday. | 틀림 | Ok | 22,091 | 예 |
| 13 | She went to a home after work. | 틀림 | Ok | 23,300 | 예 |
| 14 | I am good in playing tennis. | 틀림 | Ok | 22,521 | 예 |
| 15 | What you did last weekend? | 틀림 | Ok | 21,792 | 예 |
| 16 | I very like this restaurant. | 틀림 | Ok | 24,236 | 예 |
| 17 | The movie was very bored. | 틀림 | Ok | 25,624 | 예 |
| 18 | I made a photo of the sunset. | 틀림 | Ok | 22,992 | 예 |
| 19 | My parents lives in Busan. | 틀림 | Ok | 21,432 | 예 |
| 20 | I will meet her in Monday. | 틀림 | Ok | 22,086 | 예 |

## 요약

- 유효 JSON: 20/20
- InvalidJson: 0
- Contradiction: 0
- `correct` 판정 일치율: 20/20 (100%)
- 문장 소요시간 중앙값: 21,983.5ms
- 문장 소요시간 최댓값: 25,624ms
- 첫 모델 로딩: 11,006ms
- 문장 20개 총 소요: 422,724ms

## 설명 품질 관찰

- 9번: `a`를 정관사라고 설명했으나 부정관사다. 수정 문장과 `correct` 판정은 맞았다.
- 17번: `bored`를 명사라고 설명했으나 형용사다. 수정 문장과 `correct` 판정은 맞았다.

## 판단 근거

- 20/20, InvalidJson 0, Contradiction 0, 판정 일치 20/20, 중앙값 21,983.5ms, 최댓값 25,624ms, 첫 로딩 11,006ms.
