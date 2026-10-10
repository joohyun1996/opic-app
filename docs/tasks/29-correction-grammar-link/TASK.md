# TASK: 교정 결과에서 해당 문법 장으로 바로 가기

> 작성: Claude · 승인: [ ] 사용자
> 경로: docs/tasks/29-correction-grammar-link/TASK.md

## 목표
스피킹·영작 교정 카드의 오류마다 "관련 문법 보기" 버튼을 달아, 오류 종류에 맞는 실전 영문법 장 설명으로 바로 이동한다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `core/common/src/main/kotlin/com/jooh/opic/core/common/Writing.kt` | 수정 | `errorTypeChapters(type): List<String>` 추가 |
| `core/common/src/test/.../WritingTest.kt` (또는 새 테스트) | 생성/수정 | 매핑 테스트 |
| `core/correction/.../CorrectionResultCard.kt` | 수정 | 오류마다 버튼, `onOpenGrammar: (unitId) -> Unit` 콜백 (기본값 null이면 버튼 숨김) |
| `feature/speaking/.../SpeakingScreen.kt` | 수정 | 콜백 전달 |
| `feature/grammar/...` (GrammarFlow, 진입 함수) | 수정 | 외부에서 unitId로 설명 화면을 여는 진입점 |
| `app/.../OpicRoot.kt` | 수정 | 스피킹 → 문법 탭 해당 장 이동 연결 |

## 관련 파일 (읽기만)
- `exports/grammar-core.json` — 장 id(c0~c18)와 제목
- `feature/grammar/.../GrammarWritingScreen.kt` — 영작 교정 카드 사용처

## 요구사항
- 매핑 (core/common 순수 Kotlin):
  - tense → c4, c5
  - article → c18
  - preposition → c18
  - agreement → c3, c18
  - word_order → c14, c2
  - word_choice·기타 → 빈 목록 (버튼 숨김)
- 버튼 문구: "관련 문법: 4장 시제" (장이 2개면 2개)
- 이동하면 문법 탭의 실전 영문법 트랙과 그 장 설명을 연다. 뒤로 가면 원래 스피킹 화면으로 돌아온다

## 수용 기준
- [ ] AC1: `errorTypeChapters("tense")`는 `["c4","c5"]`를 반환한다
- [ ] AC2: `errorTypeChapters("word_choice")`와 모르는 값은 빈 목록을 반환한다
- [ ] AC3: 매핑된 모든 id가 grammar-core.json에 존재한다 (테스트)
- [ ] AC4: 콜백이 null이면 버튼이 보이지 않는다 (기존 호출처 동작 유지)
- [ ] AC5: 실기기에서 교정 카드의 버튼을 누르면 장 설명이 열리고, 뒤로 가기로 스피킹에 돌아온다
- [ ] AC6: `./gradlew test lint :app:assembleRelease` 통과

## 제약 / 주의
- DB·엔티티 변경 없음
- 매핑 표는 언어별로 다를 수 있으니 `language` 인자를 받되 지금은 "en"만 처리한다

## 범위 밖
- 오류 기록 저장·통계 (TASK 30)
