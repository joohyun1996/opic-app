# TASK: 발음 힌트 — 불명확하게 들린 단어 + 한국인 발음 팁 + 연음·약화

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "18 19 20 진행") · 구현: Claude
> 경로: docs/tasks/19-pronunciation-hints/TASK.md
> 근거: 사용자 질문 "발음 체크는 힘들겠지? 외국인처럼 말하는 팁 같은 거" → 채점 대신 힌트 (A 불명확 단어, B 한국인 발음 팁, C 리듬·연음·약화)

## 목표
음소 채점 모델 없이, Whisper 신호와 고정 규칙으로 "어디를 고치면 원어민처럼 들리는지" 힌트를 준다. 스피킹 결과와 섀도잉 비교 결과 둘 다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/common/.../Pronunciation.kt` + 테스트 | 생성 | 불명확 단어 고르기, 발음 규칙 매칭, 연음·약화·t 약화 찾기 (순수 함수) |
| `core/ui/**` (새 모듈) | 생성 | `PronunciationHintsCard` — 스피킹·섀도잉 공용 Compose (디자인 개편 때 공용 테마도 여기로) |
| `feature/speaking/**` | 수정 | 결과 화면 "발음 힌트" |
| `feature/shadowing/**` | 수정 | 비교 결과 아래 "발음 힌트", TTS 람다 받기 |
| `app/.../OpicRoot.kt`, `settings.gradle.kts`, `build.gradle.kts`들 | 수정 | 모듈 연결, 섀도잉에 `speaker::speak` |

## 요구사항
### A. 불명확하게 들린 단어
- 스피킹: `SpokenWord.confidence < 0.5`인 단어(머뭇거림·숫자·1글자 제외), 확신도 낮은 순 최대 5개
- 섀도잉: 원문 대비 `Substitute`(원문 → 들린 말)·`Delete`(빠뜨림) 단어, 최대 5개
- 칩을 누르면 TTS로 원어민 발음(원문 단어). 스피킹은 "내 발음 듣기"(그 단어 구간)도

### B. 한국인 발음 팁 (고정 규칙, 말한·원문 단어에 나온 것만)
| id | 조건 (소문자 단어) | 제목 |
|----|------|------|
| `r_l` | r 또는 l 포함 | R과 L |
| `f_p` | f·ph 포함 | F는 P가 아니다 |
| `v_b` | v 포함 | V는 B가 아니다 |
| `th` | th 포함 | TH 소리 |
| `z_j` | z 포함 | Z는 J가 아니다 |
| `final` | 자음(b d g k p t ch sh)으로 끝남, 3글자 이상 | 끝소리에 '으' 붙이지 않기 |
| `w` | w로 시작 + 뒤에 o/u (work, world, would, wood, woman) | W는 입술을 먼저 |
| `ee_i` | ee·ea 포함 | 긴 '이'와 짧은 '이' |
- 각 팁: 설명 1~2문장 + 내 답변에서 찾은 예시 단어 최대 4개(누르면 TTS). 팁은 많이 걸린 순 최대 4개

### C. 리듬·연음·약화 (말한 문장·원문에서 찾은 것)
- 연음: 자음으로 끝나는 단어 + 모음(a e i o u)으로 시작하는 단어 → "pick it up → 피끼럽" 식으로 이어 읽기 (최대 4쌍)
- 약화: `want to`→wanna, `going to`→gonna, `got to`→gotta, `kind of`→kinda, `a lot of`→a lotta, `have to`→hafta (말할 때 이렇게 들림)
- t 약화: 단어 가운데 모음 + t(t) + 모음/y (water, better, city, getting) → "ㄹ처럼 부드럽게"
- 리듬 팁 1줄 고정: "내용어(명사·동사·형용사)는 세고 길게, 기능어(a, the, to, of)는 약하고 짧게"

## 수용 기준
- [x] AC1: `unclearWords` — 임계값·머뭇거림·1글자 제외·최대 5개·낮은 순 (테스트)
- [x] AC2: `pronunciationTips` — 규칙별 매칭(really→r_l, coffee→f_p, very→v_b, think→th, zoo→z_j, milk→final, work→w, sheep→ee_i), 많이 걸린 순, 예시 중복 제거·최대 4개 (테스트)
- [x] AC3: `linkingPairs`("pick it up" → pick it, it up), `reductions`("I want to go" → want to→wanna), `flapWords`(water, city / 제외: time, top) (테스트)
- [x] AC4: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, 권한·Room 변경 없음
- [ ] AC5: 실기기 — 스피킹·섀도잉 결과에 발음 힌트가 보이고 칩을 누르면 TTS (사용자 확인)

## 범위 밖
- 음소 단위 발음 채점, 억양 그래프, 녹음 파형
