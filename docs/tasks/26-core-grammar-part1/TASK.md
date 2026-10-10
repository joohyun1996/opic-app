# TASK: 실전 영문법 1 — 탭 나누기 + 새 문제 유형 + 1부(문장의 뼈대) 3장

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10, "진행하자")
> 경로: docs/tasks/26-core-grammar-part1/TASK.md
> 근거: 사용자 실력 — 단어·독해는 강하지만 문장 구조를 못 보고 문법 오류를 찾지 못함. 목차 18장 6부 (메모리·2026-10-10 계획)

## 목표
영문법 탭을 **OPIc 문법 | 실전 영문법** 두 탭으로 나누고, 실전 영문법 1부 "문장의 뼈대" 3장을 넣는다. 1장은 사용자가 읽고 깊이를 정한 뒤 2부부터 진행한다.

## 데이터 (`exports/grammar-core.json`, 형식은 grammar.json과 같고 선택 필드 추가)
- 단원(장) 추가 필드: `track`("core", 없으면 "opic"), `part`(부 이름), 설명에 `concept`(왜 이렇게 쓰는지), `table`(형태 표: 첫 줄 머리글), `koreanNote`(한국어와 비교해 헷갈리는 이유), `breakdowns`(문장 구조 분해: `sentence`, `parts:[{text, role}]`, `note`)
- `writingTask`는 선택 (없으면 결과 화면에 "직접 써 보기" 없음)
- 새 문제 유형: `spot`(틀린 곳 찾기 — `choices` = 문장의 단어들, `answer` = 틀린 단어 위치, `explanation`에 고친 문장), `structure`(구조 찾기 — 같은 형식, `prompt`가 "진짜 주어를 고르세요" 등)
- 문제 id `c1-01`…, 간격 복습은 OPIc 문법과 같은 저장소를 쓴다

## 1부 목차
1. 품사와 문장 성분 — 단어의 품사 vs 문장 속 역할(주어·동사·목적어·보어·수식어)
2. 문장의 5형식 — 동사가 뒤에 무엇을 요구하는지
3. 주어 찾기 — 긴 주어(수식어가 붙은 주어), 가주어 it, there is/are

## 수용 기준
- [x] AC1: grammar-core.json 파싱 통과(3장·30문제), spot·structure 검증, 잘못된 answer 위치는 실패 (테스트)
- [x] AC2: 영문법 탭 위 "OPIc 문법 | 실전 영문법" 전환, 실전 영문법에 1부 3장
- [x] AC3: 장 설명에 개념·형태 표·한국어 비교·문장 구조 분해(역할별 색)·흔한 실수
- [x] AC4: 틀린 곳 찾기·구조 찾기 문제를 단어를 눌러 풀고, 1차 오답 힌트·2차 정답 공개가 기존과 같다. 틀린 문제는 오늘의 복습에 들어간다
- [x] AC5: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과

## 결과 (2026-10-10, S23+ release 빌드)
- AC1 ✅ `GrammarCatalogTest.coreGrammarParsesWithNewKinds`(3장·30문제·spot/structure·잘못된 위치 실패), `mergeKeepsBothTracksAndRejectsDuplicateIds`
- AC2 ✅ 영문법 탭 위 "OPIc 문법 | 실전 영문법" (`list.png`)
- AC3 ✅ 개념·형태 표·한국어와 다른 점·문장 구조 분해·핵심 규칙·예문·흔한 실수 (`chapter2-breakdown.png`). 역할 색: S 파랑, V 초록, O 주황, C 보라, M 회색 (밝기도 달라 구분)
- AC4 ✅ 단어 칩을 눌러 고르는 문제 (`spot-question.png`) — 채점·두 번 시도·복습은 기존 선택형과 같은 경로(`TAP_KINDS`). 실기기에서 답 제출은 하지 않음(복습 기록 안 남기려고)
- AC5 ✅ test·lint·debug/release 빌드 통과
- 콘텐츠는 Claude 작성 — **사용자가 1장을 읽고 깊이·말투를 정한 뒤 2부(동사) 진행**. GPT 교차 검토 권장
