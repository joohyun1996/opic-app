# TASK: 실전 영문법 장마다 영작 연습 (Gemma 교정)

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10)
> 경로: docs/tasks/32-core-writing/TASK.md

## 목표
실전 영문법 1~18장에 "직접 써 보기" 과제를 1개씩 넣는다. 이미 있는 영작 화면과 Gemma 교정을 그대로 쓴다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/grammar-core.json` | 수정 | c1~c18에 `writingTask` 추가 (c0 제외) |
| `feature/grammar/src/test/.../GrammarCatalogTest.kt` | 수정 | order ≥ 1인 core 장은 모두 writingTask를 가진다 |

## 관련 파일 (읽기만)
- `GrammarCatalog.kt` — GrammarWritingTask 필드 형식
- `exports/grammar.json` — 기존 writingTask 작성 예

## 요구사항
- 과제는 그 장 문법을 써야만 풀리는 한국어 상황 + 조건으로 쓴다
  - 예) 9장: "주말에 즐겨 하는 일 2가지를 동명사로 써 보세요 (enjoy, be into)"
- 모범 답안 1~2개를 쓴다
- OPIc 주제(집, 취미, 여행, 일상)와 연결한다

## 수용 기준
- [x] AC1: c1~c18의 writingTask가 파싱되고 비어 있지 않다 (테스트)
- [x] AC2: c0에는 writingTask가 없고 "직접 써 보기"가 숨겨진다
- [ ] AC3: 실기기에서 2개 장의 영작 → Gemma 교정 결과 표시를 확인한다
- [x] AC4: `./gradlew test lint :app:assembleRelease` 통과

## 범위 밖
- 교정 프롬프트 변경

## 구현 메모
- c1~c18 writingTask(한국어 과제·영어 질문·최소 문장 수)와 예시 답안 `sample`(선택 필드, 기존 grammar.json은 영향 없음)
- 쓰기 화면에 "예시 답안 보기" 토글
- 범위 추가: `GrammarCatalog.kt`(sample 필드), `GrammarWritingScreen.kt`(토글)
- AC3(Gemma 교정 실기기) 미확인
