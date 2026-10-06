# TASK: 오답 모음 + 오답만 학습

> 작성: Claude · 승인: [x] 사용자 (2026-10-06, "다음거 ㄱㄱ") · 구현: Claude (GPT 토큰 소진)
> 경로: docs/tasks/08-wrong-answers/TASK.md
> 근거: `docs/design/words-ui.md` ⑥ / `SPEC.md` § Day 단어 목록 + 플래시카드 / TASK 07 REVIEW N3
> 선행: TASK 07 Approve (`6374234`)

## 목표
단어 탭 이식의 마지막 TASK. **⑥ 오답 모음** 화면을 만들고, 오답 단어만으로 영→한·한→영 플래시카드를 할 수 있게 한다. Day 목록·홈의 오답 뱃지를 누르면 이 화면으로 간다.

## 오답의 정의 (변경)
| | 이전 (TASK 06~07) | 이후 |
|---|---|---|
| 오답 단어 | `wrongCount > 0` | `wrongCount > 0` **그리고** `correctCount < 3` (아직 습득 전) |

- 이유: 이전 정의로는 한 번 틀린 단어가 습득한 뒤에도 오답 모음에 영원히 남는다. 스키마(마지막 결과 컬럼 등)를 바꾸지 않고 "다시 공부해서 습득하면 오답에서 빠진다"를 표현한다.
- `DayStats.wrong`, 홈·Day 목록 뱃지, ③의 "오답 N" 표시, ⑥ 목록이 모두 이 정의를 쓴다. ③ 단어 줄의 "오답 N"은 오답 단어일 때만 보인다 (습득한 단어는 숨김).

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/database/.../WordDao.kt` | 수정 | `dayStats`의 wrong 정의 변경, 오답 단어 조회 쿼리 추가 |
| `core/database/src/test/...` | 수정 | 새 정의·조회 테스트 |
| `feature/words/WrongScreen.kt` | 생성 | ⑥ 화면 |
| `feature/words/StudyViewModel.kt` | 수정 | 학습 대상을 "Day N" 또는 "오답(전체/Day N)"으로 일반화, 쓰기 실패 로그 (TASK 07 N3) |
| `feature/words/StudyScreen.kt`, `DayScreen.kt`, `WordsApp.kt` | 수정 | 경로, 뱃지 클릭, 오답 표시 조건 |

스키마·라이브러리·권한 변경 없음.

## 요구사항
### ⑥ 오답 모음 (`wrong`)
- 상단: "← 뒤로", "오답 모음", 오답 단어 총개수
- Day 필터: 가로 스크롤 칩. "전체" + 오답 단어가 있는 Day만 ("Day 3 (5)"). 기본은 "전체"
- 목록: 왼쪽에 빨간 세로선이 있는 카드. 단어, 발음기호, ♪, 한국어 뜻, "틀린 횟수 N", "Day N". seq 순서
- 하단: "영→한 학습", "한→영 학습" — 지금 필터의 오답 단어만으로 플래시카드 시작
- 오답이 0개면 "오답이 없습니다" 안내와 함께 학습 버튼 비활성
- 진입: Day 목록의 "오답 N개" 뱃지, 홈 영어 카드의 "오답 N개" 표시(0이면 숨김)

### 오답 학습
- 경로 `study/wrong/{day}/{mode}` (`day` = 0이면 전체). 카드 순서는 seq 순
- 학습 대상은 **시작할 때의 목록으로 고정**한다 (풀다가 습득해도 세션 중에는 카드가 빠지지 않는다)
- 채점·기록·결과 화면은 TASK 07과 같다. 결과 화면의 "목록으로"는 ⑥으로 돌아간다

### 기록 쓰기 실패 (TASK 07 N3)
- `recordResult` / `correctLastWrong`에서 예외가 나면 `Log.e`로 남기고, 화면에 "기록 저장 실패" 한 줄을 띄운다. 앱이 죽지 않는다.

## 수용 기준
- [ ] AC1: wrongCount 1·correctCount 0, wrongCount 1·correctCount 3, wrongCount 0·correctCount 1인 단어가 있을 때 오답 조회는 첫 번째만 돌려주고, `dayStats`의 wrong도 1이다
- [ ] AC2: 오답 조회는 deleted 단어를 빼고, `language` 조건을 지키며, Day 필터(seq 범위)를 적용하면 그 Day의 오답만 돌려준다
- [ ] AC3: 오답 학습을 시작할 때의 카드 목록은 세션 중 기록이 바뀌어도 그대로다 (ViewModel이 시작 시 한 번만 읽는다 — 코드로 확인)
- [ ] AC4: `./gradlew test`, `./gradlew lint` 통과
- [ ] AC5: S23+ 확인 — Day 목록 뱃지 → ⑥ 진입, Day 필터 전환, 오답 학습 후 결과, 습득 후 오답에서 빠지는지. 스크린샷을 이 폴더에 저장

## 범위 밖
- SRS(간격 반복), 오답 기록 초기화 버튼, 중국어
