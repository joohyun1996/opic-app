# TASK: OPIc 유형별 답변 템플릿

> 작성: Claude · 승인: [ ] 사용자
> 경로: docs/tasks/33-answer-templates/TASK.md

## 목표
스피킹 탭에 "답변 템플릿"을 추가해 OPIc 문항 유형별 답변 뼈대·연결어·예시 답변을 보여 주고, 템플릿을 보고 바로 그 유형 문제를 녹음하게 한다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/templates.json` | 생성 | 유형 6개 (dataVersion 포함) |
| `feature/speaking/.../TemplateCatalog.kt` | 생성 | 파싱·검증 |
| `feature/speaking/.../TemplateScreen.kt` | 생성 | 목록·상세 |
| `feature/speaking/src/test/.../TemplateCatalogTest.kt` | 생성 | |
| `feature/speaking/.../SpeakingScreen.kt` | 수정 | 진입 버튼, 유형으로 문제 고르기 |
| `app/build.gradle.kts`, `OpicApplication.kt` | 수정 | 파일 내장·로드 |

## 요구사항
- 유형: 묘사(장소·사람), 습관·루틴, 과거 경험, 비교(과거 vs 현재), 롤플레이(질문하기), 롤플레이(문제 해결)
- 항목마다 다음을 둔다:
  - 단계별 뼈대 3~5개 (예: 도입 → 특징 2개 → 느낌 → 마무리)
  - 단계마다 쓸 표현 3개 이상
  - 예시 답변 1개(100~150단어)와 한국어 뜻
  - 관련 실전 영문법 장 id (예: 비교 → c16, 과거 경험 → c4·c5)
- 상세 화면에서 관련 장 버튼(TASK 29 진입점 재사용)과 "이 유형 문제 연습"을 누를 수 있게 한다

## 수용 기준
- [ ] AC1: templates.json이 파싱되고 유형 6개, 각 뼈대 3개 이상, 예시 답변 100~150단어다 (테스트)
- [ ] AC2: 관련 장 id가 grammar-core.json에 모두 존재한다 (테스트)
- [ ] AC3: "이 유형 문제 연습"은 해당 유형 질문 중 하나로 녹음 화면을 연다
- [ ] AC4: `./gradlew test lint :app:assembleRelease` 통과, 실기기 확인

## 제약 / 주의
- 기존 스피킹 질문 데이터에 유형 필드가 없으면 매핑 표를 TemplateCatalog에 두고 HANDOFF에 적는다
- 이 TASK는 TASK 29 다음에 한다

## 범위 밖
- 템플릿 기반 자동 채점
