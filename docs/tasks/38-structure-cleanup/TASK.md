# TASK: 구조 정리 — 중복 제거와 OpicRoot 나누기

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "태스크로 잡고 지피티에 넘기자") · 구현: GPT

## 목표
동작 변경 없이 중복 코드와 문자열 상수를 정리하고, OpicRoot(370줄)를 읽기 쉽게 나눈다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `feature/grammar/.../GrammarCatalog.kt` | 수정 | `GrammarUnit.displayTitle()` 추가 ("실전 4장 …" / "OPIc 1단원 …"), `sourceLabels`가 이를 사용. 트랙 `"opic"`/`"core"` → 상수(`GrammarTracks`) |
| `app/.../OpicRoot.kt` | 수정 | 장 제목 계산 3곳 → displayTitle. 스피킹·섀도잉 ViewModel 팩토리를 각 모듈로 이동. `pending*` 전역 3개 → 하나의 요청 타입(sealed class) |
| `feature/speaking/...`, `feature/shadowing/...` | 수정 | `XxxViewModel.Factory` 추가 (WordsViewModel.Factory와 같은 방식) |
| `feature/grammar/...` (GrammarFlow, GrammarUnitListScreen 등) | 수정 | 트랙 상수 사용 |
| `core/correction/...` | 수정 | 문법 영작·스피킹 ViewModel에 똑같이 있는 saveToken 로직 한 곳으로 |
| 관련 테스트 | 수정/생성 | displayTitle 테스트 |

## 수용 기준
- [ ] AC1: 장 제목 문자열 만드는 코드가 한 곳뿐이다
- [ ] AC2: main 코드에 `"opic"`/`"core"` 트랙 문자열이 상수 정의 외에 없다
- [ ] AC3: OpicRoot.kt가 250줄 이하다
- [ ] AC4: 교정·템플릿 → 장 이동, 홈 복습 시작, 메뉴 → 스피킹 기록 이동이 그대로 된다 (실기기)
- [ ] AC5: `./gradlew test lint :app:assembleRelease` 통과

## 범위 밖
- 화면 디자인 변경
