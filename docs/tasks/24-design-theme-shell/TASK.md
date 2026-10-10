# TASK: 디자인 개편 1 — 공용 테마(밝게·어둡게) + 하단 바 + 전체 메뉴 + 새 홈

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10)
> 경로: docs/tasks/24-design-theme-shell/TASK.md
> 시안: https://claude.ai/artifact/4UkaAdg75DhYj7u4CJTyZN

## 사용자 결정 (2026-10-10)
- 밝은 모드 = 시안 A(Babbel 톤: 흰 배경·주황), 어두운 모드 = 시안 B(Speak 톤: 남색·파랑). 기본은 기기 설정, 전체 메뉴에서 밝게/어둡게/기기 설정 따름
- 홈 "이어서 하기"는 B의 목록형, 글꼴은 IBM Plex Sans KR
- 하단 바 5개(홈·단어·문법·섀도잉·스피킹), 오른쪽 위 ≡ 전체 메뉴. 애니메이션·슬라이드 효과 없음

## 디자인 토큰 (`core/ui/Theme.kt`)
| 토큰 | 밝게 | 어둡게 |
|------|------|--------|
| background | #FFFFFF | #0E1430 |
| surface (카드) | #F7F5F1 | #151D40 |
| surfaceHigh (아이콘 칸·트랙) | #E7E3DC | #24306A |
| onBackground | #1C1C1E | #F3F5FF |
| onSurfaceVariant (보조 글자) | #5B5B60 | #A3ACD3 |
| primary (버튼·강조 채움) | #C8460F | #4C5BF0 |
| onPrimary | #FFFFFF | #FFFFFF |
| accent (강조 글자·선택 탭) | #C8460F | #8EA0FF |
| outline (구분선) | #ECE9E4 | #1F2853 |
| 정답 / 오답 / 경고 | #1E7A3C / #B3261E / #A15C00 | #6FD39A / #FF8A80 / #FFC46B |
- 모서리: 카드 16dp, 큰 카드 20dp, 버튼 999(알약), 터치 영역 ≥ 48dp
- 글꼴: IBM Plex Sans KR 400·600·700 (`core/ui/src/main/res/font/`, OFL 라이선스 파일 함께) — 새 라이브러리 없음, APK 약 +8.5MB

## 수정 범위
| 파일 | 내용 |
|------|------|
| `core/ui/**` | `OpicTheme`(light/dark), 토큰, 글꼴, `ThemeSetting`(밝게/어둡게/기기 — SharedPreferences), 상태 색 `OpicColors` |
| `app/.../OpicRoot.kt`, `MainActivity.kt` | 하단 바 5개 + 탭 안의 화면은 기존 경로 유지, 상단 ≡ → `menu`, 시스템 바 색 테마에 맞춤 |
| `app/.../HomeScreen.kt`, `MenuScreen.kt` (새 파일) | 새 홈(날짜·오늘 할 일·이어서 하기 목록), 전체 메뉴(학습·설정·데이터·앱 정보), 백업 창은 메뉴로 이동 |
| `feature/words/**` | 홈 화면 역할을 앱으로 넘기고 단어 탭 첫 화면 = Day 목록. `WordsTheme` 삭제, 하드코딩 색 → 테마 |
| `feature/grammar/**`, `feature/shadowing/**`, `feature/speaking/**`, `core/correction/**`, `core/ui/PronunciationHints.kt` | 하드코딩 색(흰색·빨강·회색·주황 등) → 테마·상태 색 |
| `feature/words/.../Speaker.kt` | TTS 속도 설정 (느리게 0.8 / 보통 1.0 / 빠르게 1.2) |

## 홈
- 위: 날짜(`10월 10일 금요일`), 제목 "오늘의 학습", 오른쪽 ≡
- "오늘 할 일" 큰 카드: 문법 복습이 있으면 "영문법 복습 N문제" + 복습 시작, 없으면 "오늘 복습 끝!" + 영단어 이어서
- "이어서 하기" 목록(아이콘 + 제목 + 한 줄): 스피킹 모의고사 / 영단어 Day N(첫 미습득 Day, 진행 막대) / 섀도잉 최근 연습 영상(없으면 추천 영상)
## 전체 메뉴
- 학습: 오답노트(N개), 스피킹 기록, 중국어(HSK) 준비 중
- 설정: AI 모델(Gemma 공용 모델 상태·모든 파일 접근 허용, Whisper 상태), TTS 속도, 화면 테마
- 데이터: 학습 기록 백업, 백업에서 복원 (TASK 22 기능)
- 앱 정보(버전)

## 수용 기준
- [ ] AC1: 밝게·어둡게·기기 설정 전환이 즉시 반영되고 앱을 다시 켜도 유지 (실기기 스크린샷 밝게·어둡게 홈 각 1장)
- [ ] AC2: 하단 바 5개로 탭 이동, 각 탭 안 화면(Day·단원·영상·질문)에서 뒤로 가면 탭 첫 화면, 다른 탭 갔다 와도 그 탭 위치 유지
- [ ] AC3: 홈 오늘 할 일·이어서 하기가 실제 데이터(복습 수·Day·최근 영상)로 채워짐
- [ ] AC4: 전체 메뉴에서 오답노트·스피킹 기록·백업·테마·TTS 속도 동작
- [ ] AC5: 어두운 모드에서 흰 배경 카드·안 보이는 글자 없음 (주요 화면 확인: Day 목록, 플래시카드, 문법 연습, 섀도잉, 스피킹 결과)
- [ ] AC6: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, 권한·DB 변경 없음
