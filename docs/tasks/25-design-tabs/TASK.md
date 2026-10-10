# TASK: 디자인 개편 2 — 탭별 화면 다듬기

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10, "어 하자")
> 경로: docs/tasks/25-design-tabs/TASK.md · 앞 작업: TASK 24 "TASK 25로 넘김"

## 할 일
1. **탭 첫 화면 머리글 통일**: 단어(Day 목록)·문법(단원 목록)·섀도잉·스피킹(주제 목록)의 옛 "← 홈" 버튼 삭제, 제목을 홈과 같은 크기·위치(오른쪽 ≡와 같은 줄)로. 제목: 영단어 / 영문법 / 섀도잉 / 스피킹
2. **전체 메뉴**: 항목 사이 구분선
3. **어두운 모드 점검**: 플래시카드·문법 연습·섀도잉·스피킹 화면을 어둡게로 열어 흰 칸·안 보이는 글자가 없는지 (스크린샷)
4. 화면 안 뒤로 가기(Day → Day 목록 등)는 그대로

## 수정 범위
`feature/words/.../WordsApp.kt`, `feature/grammar/.../GrammarUnitListScreen.kt`, `feature/shadowing/.../ShadowingScreen.kt`, `feature/speaking/.../SpeakingScreen.kt`, `app/.../MenuScreen.kt`, 점검에서 발견한 화면

## 수용 기준
- [x] AC1: 네 탭 첫 화면에 "← 홈"이 없고 제목이 ≡와 같은 줄
- [x] AC2: 메뉴 항목 사이 구분선
- [x] AC3: 어두운 모드 스크린샷 4장(플래시카드·문법 연습·섀도잉·스피킹)에서 흰 칸·안 보이는 글자 없음
- [x] AC4: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과

## 결과 (2026-10-10, S23+ release 빌드, 어둡게)
- AC1 ✅ 단어·문법·섀도잉·스피킹 첫 화면에 "← 홈" 없음, 제목이 ≡와 같은 줄 (`dark-words.png`, `dark-shadow.png`, `dark-speak2.png`)
- AC2 ✅ 메뉴 항목 사이 구분선
- AC3 ✅ 플래시카드(`dark-card.png`), 문법 설명(`dark-gexp.png`), 섀도잉·스피킹 목록 — **점검 중 발견해 고친 것**: ① Day·오답 화면의 "영→한/한→영 학습"·플래시카드 "확인·다음·목록으로" 버튼이 옛 잉크색(어둡게에서 흰색) 바탕 + 흰 글자라 안 보였음 → 테마 기본 버튼 ② 진행 막대 색 → 강조색 ③ Material Card 기본색(surfaceContainerHighest)이 시안보다 밝았음 → 시안 카드색(#F7F5F1 / #151D40)
- AC4 ✅ test·lint·debug/release 빌드 통과
- 테마 설정은 확인 후 "기기 설정 따름"으로 되돌림
