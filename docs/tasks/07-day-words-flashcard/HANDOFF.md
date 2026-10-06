# HANDOFF: Day 단어 목록 + 플래시카드(영→한, 한→영) + 발음

> 작성: Claude (구현 대행, GPT 토큰 소진 — 2026-10-06 사용자 지시) · 경로: docs/tasks/07-day-words-flashcard/HANDOFF.md

## 커밋 범위
- base: `cf4d727`
- head: 이 커밋 하나

## 변경 요약
```diff
+ core/common/.../Flashcard.kt          — meaningCandidates, gradeMeaning, gradeWord, maskHint
+ core/common/.../FlashcardTest.kt      — 4개 (TASK 예시 7개 + 별표·쉼표·빈 입력, 힌트 4종)
~ core/database/.../WordDao.kt          — getDayWordsWithProgress(language, firstSeq, lastSeq) → WordWithProgress(@Embedded word, correctCount?, wrongCount?)
~ core/database/.../UserWordDao.kt      — correctLastWrong(wordId, now): 트랜잭션, wrong−1·correct+1, 기록 없거나 wrong 0이면 false
~ core/database/.../WordImporter.kt     — N1 CancellationException 재던짐, N2 앞 256자에서 dataVersion만 먼저 읽고 같으면 즉시 UpToDate, N3 주석
~ core/database/src/test/...            — 3개 추가 (같은 버전 파싱 생략, 정정, 진행 기록 조회)
+ feature/words/Theme.kt                — 색, 상태 뱃지, 오답 뱃지
+ feature/words/Speaker.kt              — TextToSpeech(Locale.US) 1개 공유, 사용 불가 시 ♪ 비활성
+ feature/words/DayScreen.kt            — ③ + DayViewModel
+ feature/words/StudyViewModel.kt       — 세션 상태, 기록 쓰기 순서 보장(Job join)
+ feature/words/StudyScreen.kt          — ④⑤ 카드, 결과 화면
~ feature/words/WordsApp.kt             — 경로 day/{day}, study/{day}/{mode}, TTS 1회 안내, 홈·Day 목록 진입 시 refresh, safeBack
~ feature/words/build.gradle.kts        — :core:model 의존
```
스키마·DB version·라이브러리·권한 변화 없음.

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1 | 통과 | `FlashcardTest.meaningMatchesAnyCandidate`, `meaningWithAsteriskAndCommasAndBlank` |
| AC2 | 통과 | `wordIsTrimmedAndCaseInsensitive` |
| AC3 | 통과 | `hints` — `well-being` → `w _ _ _ - _ _ _ _ g` |
| AC4 | 통과 | `WordImporterTest.correctionTurnsLastWrongIntoCorrect` + 실기기 (archipelago 오답 → 맞았어요 → 학습중) |
| AC5 | 통과 | `dayWordsWithProgressSkipsDeletedAndMarksNew` (correct 3 → mastered) + 실기기 |
| AC6 | 통과 | 같은 테스트 (deleted 제외, 기록 없으면 null = 신규) |
| AC7 | 통과 | `sameVersionSkipsFullParse` — 단어 목록이 깨진 같은 버전 파일도 UpToDate |
| AC8 | 통과 | `./gradlew test lint assembleDebug assembleRelease` exit 0. 테스트 21개 (Correction 5, Day 1, Flashcard 4, Dao 3, WordImporter 8) + model. lint 경고는 버전 안내·앱 아이콘뿐 (새 코드 0) |
| AC9 | 통과 | 아래 |

## 실기기 결과 (S23+, debug)
| 항목 | 결과 |
|------|------|
| 재실행 적재 | `UpToDate(version=1) elapsedMs=260` (이전 637ms) |
| ♪ | 탭 시 `com.google.android.tts` AudioTrack `state:started`, `CONTENT_TYPE_SPEECH` 24kHz 확인. 기기가 무음 모드라 **귀로는 확인하지 못함 — 사용자 확인 필요** |
| 기록 반영 | 한→영 2회 + 영→한 일부 후: Day 1 `습득 2 / 40`, Day 목록 `2/40`, 홈 `습득 2 / 5517개`, Day 목록 `오답 1개` — 세 화면 일치 |

스크린샷 (`docs/tasks/07-day-words-flashcard/`)
- `day1-list.png` ③ 학습 전 / `day1-after.png` ③ 학습 후 (습득·학습중 뱃지)
- `en-ko-wrong.png` ④ 오답 (빨간 테두리, 정답, 예문, "맞았어요") / `en-ko-corrected.png` 맞았어요 후
- `ko-en-question.png` ⑤ 입력 중 (키보드 위로 뜻·품사·힌트 보임) / `ko-en-correct.png` ⑤ 정답 (♪ + 발음기호) / `ko-en-wrong.png` ⑤ 오답
- `result.png` 결과 화면 (40/40)
- `ko-en-question-before-fix.png` 아래 버그 2의 수정 전 모습

## 실기기에서 찾아 고친 버그
1. **빈 화면:** 뒤로 버튼을 빠르게 여러 번 누르면 시작 화면까지 pop되어 NavHost가 비었다. `safeBack()` — 이전 화면이 있고 현재 화면이 RESUMED일 때만 pop. 6연타 시 Day 목록에서 멈추는 것 확인.
2. **키보드에 문제가 가려짐:** 카드가 `weight(1f)`라 키보드가 올라오면 카드가 사라졌다. 카드를 `weight(3f, fill = false)` + 내부 스크롤, 아래에 `Spacer(weight(1f))`로 바꿈.
3. **"맞았어요" 순서:** 확인 직후 바로 누르면 정정이 오답 기록보다 먼저 실행될 수 있었다. 직전 쓰기 Job을 `join` 후 실행.

## 확인하지 못한 것
- **영→한 정답 화면:** adb로 한국어를 입력할 수 없어 실기기에서 정답 판정 화면은 못 봤다. 채점은 단위 테스트로 확인했다. 직접 한 번 "군도"를 입력해 초록 테두리가 뜨는지 봐 주세요.
- **TTS 소리:** 위와 같이 재생 시작까지만 확인.

## 설계 판단 / 리뷰어가 봐야 할 곳
- 한→영 입력에 `KeyboardType.Ascii`를 줬지만 삼성 키보드는 마지막 언어(한글)를 유지한다. 사용자가 한/영 전환을 해야 한다. 강제로 영문 키보드를 띄우려면 `KeyboardType.Uri`/`Password` 같은 우회가 필요해 보류했다.
- 힌트 `_` 사이 공백이 폰트에서 붙어 보인다 (`a _ _ _` → `a______`). 읽는 데 문제는 없지만 다음에 글자 간격이나 `·` 구분을 검토할 만하다.
- 플래시카드는 Day의 단어를 매번 seq 순서로 처음부터 낸다 (섞기·SRS는 범위 밖).

## 범위 밖 변경
- 없음

## 질문
- 위 "확인하지 못한 것" 2가지를 직접 확인 부탁드립니다.
