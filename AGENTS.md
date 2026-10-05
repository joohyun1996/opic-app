# 응답 언어 (최우선 규칙)
- 모든 응답, 보고, 질문, 문서는 **반드시 한국어**로 작성한다.
- 영어는 코드, 파일명, 명령어, 커밋 타입(feat, fix 등)에만 허용한다.

# AGENTS.md — 모든 AI 에이전트 공통 규칙 (단일 원본)

> 이 파일이 규칙의 유일한 원본이다. Claude Code는 CLAUDE.md의 `@AGENTS.md`로 읽는다.
> 문서 충돌 시 우선순위: **AGENTS.md > 현재 작업의 TASK.md > SPEC.md > docs/design/\***.

## 프로젝트 요약
Kotlin + Jetpack Compose 기반 안드로이드 전용 영어(OPIc) 학습 앱. 서버·로그인 없이 완전 오프라인으로 동작하며 APK를 직접 배포한다. 1차 출시는 영어만, 중국어(HSK)는 후속 기능이다.

### 기술 스택
- Kotlin, Jetpack Compose, Room(SQLite)
- MediaPipe + Gemma 3n E4B: 머니로그 `core/llm`을 복사해 사용
- 음성 인식: 미정
- 세부 버전: TASK 04(안드로이드 골격)에서 확정

### 폴더 구조 (TASK 04에서 생성 예정)
- `app/` — 안드로이드 앱
- `core/{llm,database,model,common}` — 공통 기능과 도메인 로직
- `feature/{words,grammar,shadowing,speaking,analysis}` — 기능별 화면
- `exports/` — 앱 내장 단어 데이터와 원본
- `docs/` — 결정·설계·작업 문서

### 데이터 규칙
- `exports/words.json`(dataVersion 포함)을 앱에 내장한다. 저장된 dataVersion보다 크면 Word만 한 트랜잭션에서 upsert하고 UserWord는 건드리지 않는다.
- `OnConflictStrategy.REPLACE` 사용 금지. 삭제 후 삽입으로 id가 바뀌면 UserWord 기록이 깨진다. `(language, word)`로 찾아 기존 행은 UPDATE, 새 행은 INSERT한다. `@Upsert`는 이 키에 맞게 동작할 때 허용한다.
- 기존 단어의 `seq`는 절대 바꾸지 않는다. 삭제는 행 제거 대신 `deleted = true`로 표시한다.
- Day = `(seq - 1) / 40 + 1`. deleted 단어는 해당 Day에서 숨기기만 한다.
- 단어 저장·비교는 소문자로 한다. 뜻이 불확실하면 값 앞에 `*`를 붙인다.
- 모든 DB 쿼리는 `language` 필드로 필터링한다.
- 단어 수정은 `exports/words.json`을 직접 고치고 dataVersion을 1 올린다. 생성 스크립트는 `web-final` 태그에 보존되어 있다.

### 코드 규칙
- Kotlin 100%, UI는 Jetpack Compose만 사용한다 (XML 레이아웃 금지).
- 채점·Day 계산 같은 도메인 로직은 `core/common`의 순수 Kotlin으로 두고 단위 테스트를 작성한다.
- LLM 프롬프트는 JSON만 반환하도록 작성한다.

## 역할 분담
| 역할 | 담당 | 하는 일 | 하지 않는 일 |
|------|------|---------|-------------|
| 구현 | GPT (Codex) | TASK대로 구현, 테스트, 로컬 커밋, HANDOFF 작성, REVIEW 반영 | TASK 범위 밖 수정, 설계 임의 변경, push |
| 리뷰 | Claude (Claude Code) | TASK 작성, diff 리뷰, REVIEW 작성, SPEC 업데이트 | 코드 구현 |
| 결정 | 사용자 | TASK 승인, 의견 충돌 판정, push | |

## 작업 흐름
1. 사용자가 다음 작업을 결정한다.
2. Claude가 `docs/tasks/<순번>-<slug>/TASK.md`를 작성한다 (순번은 기존 최대 + 1).
3. 사용자가 TASK를 검토·승인한다.
4. GPT가 구현·검증·로컬 커밋 후 HANDOFF.md를 작성한다.
5. Claude가 base..HEAD diff를 리뷰해 REVIEW.md를 작성한다.
6. GPT가 리뷰를 반영해 새 커밋과 HANDOFF의 리뷰 반영 섹션을 추가한다. 재리뷰를 반복한다.
7. Claude가 Approve 후 SPEC.md를 업데이트한다.
8. 사용자가 dev를 push한다.

템플릿: `docs/workflow/templates/{TASK,HANDOFF,REVIEW}.md`.

## 구현 에이전트(GPT) 규칙
### 시작 전
1. 현재 작업의 TASK.md를 읽는다. 없으면 구현하지 말고 사용자에게 요청한다.
2. TASK의 관련 파일과 참고 문서만 읽는다. 저장소 전체 탐색은 하지 않는다.
3. 시작 커밋 해시를 기록해 HANDOFF의 base로 쓴다.

### 파일 수정 권한
- 승인된 TASK의 수정 범위에 적힌 파일은 추가 허락 없이 생성·수정한다.
- 범위 밖 파일 생성·수정·삭제, 패키지 설치·삭제, Room 스키마(엔티티) 변경, Migration 추가, AGENTS.md·CLAUDE.md 수정은 사용자 허락을 먼저 받는다.
- TASK가 모호하거나 틀렸으면 추측으로 진행하지 말고 사용자에게 묻는다.

### 완료 조건
- `./gradlew test`, `./gradlew lint` 통과 (TASK 04부터 적용; 그 전에는 TASK의 검증 기준으로 대신한다).
- TASK의 수용 기준 전부 충족.
- dev 브랜치에 로컬 커밋 (push 금지).
- 템플릿대로 HANDOFF.md 작성.

### REVIEW 반영
- Must-fix는 전부 반영한다. Should-fix는 반영하거나 HANDOFF에 이유를 적는다. Nit은 선택이다.
- 동의하지 않는 지적은 HANDOFF의 리뷰 반영 표에 반론을 적고 사용자 판단을 받는다.
- 수정은 새 커밋으로 한다. commit --amend, rebase, force push 금지.
- 재리뷰 요청 시 HANDOFF에 리뷰 반영 커밋 범위를 적는다.

### 보고 형식
- 변경 부분만 diff 형식으로 (`+` 추가 / `-` 삭제) 보고하고 변경 이유를 한 줄 적는다. 전체 파일은 출력하지 않는다.

## 금지사항
- 모델 다운로드 외 네트워크 권한 사용
- API 키 하드코딩, `.env*`·`local.properties`·keystore 커밋
- `OnConflictStrategy.REPLACE` 사용
- main 브랜치 직접 커밋

## SPEC.md 규칙
- 새 기능은 해당 섹션에 기록하고 변경 사항은 하단 변경 이력에 추가한다 (삭제 금지).
- API 변경 시 이전·이후 URL 모두 기록한다.
- Approve 이후 Claude가 업데이트한다.

## Git 규칙
- main은 안정 코드, dev는 개발 및 커밋용이다.
- 커밋 메시지 형식: `feat(범위): 설명` 뒤에 빈 줄과 `* 세부 설명`을 적는다.
- 타입: feat / fix / test / spec / refactor / chore / review.
- GitHub 이슈는 쓰지 않고 커밋에 이슈 번호를 붙이지 않는다.
