# HANDOFF: 오답 모음 + 오답만 학습

> 작성: Claude (구현 대행, GPT 토큰 소진) · 경로: docs/tasks/08-wrong-answers/HANDOFF.md

## 커밋 범위
- base: `0ef2679`
- head: 이 커밋 하나

## 변경 요약
```diff
~ core/database/.../WordDao.kt        — dayStats.wrong = wrongCount > 0 AND correctCount < 3, getWrongWords(language, firstSeq, lastSeq) 추가
~ core/database/src/test/...          — 기존 기대값 수정 (습득한 오답은 wrong에서 빠짐), 오답 조회 테스트 1개
+ feature/words/WrongScreen.kt        — ⑥ 화면 + WrongViewModel (Day 필터 칩, 빨간 세로선 카드, 틀린 횟수, 학습 버튼)
~ feature/words/StudyViewModel.kt     — StudySource(day, wrongOnly), 시작 시 카드 목록 고정, 쓰기 실패 로그 + saveFailed
~ feature/words/StudyScreen.kt        — StudySource 인자, 뒤로 라벨, "기록 저장 실패" 표시
~ feature/words/DayScreen.kt          — "오답 N" 표시를 습득 전 단어에만
~ feature/words/WordsApp.kt           — wrong, study/wrong/{day}/{mode} 경로, Day 목록 뱃지·홈 "오답 N개 다시 보기 →" 진입
```
스키마·라이브러리·권한 변경 없음.

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1 | 통과 | `wrongWordsExcludeMasteredDeletedAndOtherLanguage` — 습득(correct 3) 단어 제외, dayStats wrong 동일 |
| AC2 | 통과 | 같은 테스트 — deleted 제외, zh 분리, Day 1 범위 필터 |
| AC3 | 통과 | `StudyViewModel.load()`가 `init`에서 한 번만 실행 (주석 표시). 실기기에서 오답 학습 중 습득해도 결과 화면까지 카드 유지 |
| AC4 | 통과 | `./gradlew test lint assembleDebug assembleRelease` exit 0, lint 경고는 기존 버전 안내·아이콘뿐 |
| AC5 | 통과 | 아래 |

## 실기기 결과 (S23+)
1. Day 2 한→영에서 일부러 오답 → Day 목록 뱃지 `오답 8개 →` (`days-badge.png`)
2. 뱃지 → ⑥: `전체`, `Day 1 (1)`, `Day 2 (7)` 칩 (`wrong-all.png`), Day 1 필터 (`wrong-day1.png`)
3. Day 1 오답(rustle) 한→영 학습 2회 → 결과 `1 / 1 정답` (`wrong-study-result.png`)
4. rustle 정답 3회가 되어 오답에서 빠짐: ⑥ `오답 7개`, Day 1 칩 사라지고 필터는 `전체`로 (`wrong-after-mastered.png`). Day 목록 `오답 7개`, Day 1 `3/40`, 홈 `습득 3`, `오답 7개 다시 보기 →` — 모두 일치

## 설계 판단 / 리뷰어가 봐야 할 곳
- 오답 정의를 바꿔 이전 SPEC(v0.7.0)의 `wrong = wrongCount > 0`과 달라졌다. SPEC에 이전·이후를 기록한다.
- 선택한 Day 필터의 오답이 모두 사라지면 필터를 "전체"로 되돌린다.
- 실기기 테스트 중 스크립트가 같은 뜻을 가진 다른 단어로 답해 의도보다 오답이 4개 더 생겼다 (데이터상 뜻이 겹치는 단어가 있음 — 앱 버그 아님).

## 범위 밖 변경
- 없음
