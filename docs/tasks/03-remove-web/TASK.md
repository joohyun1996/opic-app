# TASK: 웹앱 코드 제거 + 규칙 문서 Kotlin/Room 기준 개정

> 작성: Claude · 승인: [x] 사용자 (2026-10-05)
> 경로: docs/tasks/03-remove-web/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § 저장소, § 버리는 것, § Day 구성, § 단어 데이터, § 언어 범위
> 선행: TASK 01·02 교차 리뷰에서 Must-fix 0개

## 목표
웹 코드는 `web-final` 태그(`620b400`)에 보존되어 있으므로 dev에서 지운다. 남은 저장소가 안드로이드(Kotlin + Compose + Room) 프로젝트를 시작할 수 있는 상태가 되도록 AGENTS.md, CLAUDE.md, README.md, .gitignore를 개정한다. **한 커밋으로 끝낸다.**

## 수정 범위
> 이 TASK는 사용자가 AGENTS.md / CLAUDE.md 수정과 대량 삭제를 승인했다.

| 대상 | 작업 |
|------|------|
| `app/`, `components/`, `lib/`, `prisma/`, `public/`, `scripts/`, `tests/` | 삭제 |
| `proxy.ts`, `package.json`, `package-lock.json`, `tsconfig.json`, `next.config.ts`, `eslint.config.mjs`, `postcss.config.mjs`, `vitest.config.ts`, `prisma.config.ts`, `components.json` | 삭제 |
| `Opic-App-Blueprint.md` | 삭제 (구버전 웹 설계 초안. 태그에 보존됨) |
| `AGENTS.md` | 전면 개정 (아래 요구사항) |
| `CLAUDE.md` | 개정 (아래 요구사항) |
| `README.md` | 다시 작성 (짧게: 앱 소개, 상태, 문서 위치) |
| `.gitignore` | Node/Next 항목 제거, Android/Gradle 항목 추가 |

**남길 것 (건드리지 않음):** `docs/`, `SPEC.md`, `exports/` 전체 (`words.json`, `source/`, `advanced-batches/` — 중국어 원본은 나중에 쓴다), `.github/`, `.claude/`

**로컬에서만 정리 (커밋 대상 아님):** `node_modules/`, `.next/`, `next-env.d.ts`, `tsconfig.tsbuildinfo`는 삭제해도 된다. `.env`, `.env.local`은 **삭제하지 말고 그대로 둔다** (사용자 로컬 파일).

## 요구사항

### AGENTS.md (전면 개정)
맨 위 "응답 언어" 규칙과 "역할 분담", "작업 흐름", "구현 에이전트 규칙"의 구조는 유지하고, 웹 관련 내용을 아래로 바꾼다. Next.js 안내 문구("This is NOT the Next.js you know")와 Blueprint 언급은 삭제한다.

- **프로젝트 요약:** Kotlin + Jetpack Compose 안드로이드 전용 앱. 서버·로그인 없음, 완전 오프라인. APK 직접 배포. 1차 출시는 영어(OPIc)만, 중국어(HSK)는 추가 기능.
- **기술 스택:** Kotlin, Jetpack Compose, Room(SQLite), MediaPipe + Gemma 3n E4B(머니로그 `core/llm` 복사), 음성 인식 미정. 세부 버전은 TASK 04(안드로이드 골격)에서 확정한다고 적는다.
- **폴더 구조 (예정):** `app/`, `core/{llm,database,model,common}`, `feature/{words,grammar,shadowing,speaking,analysis}`, `exports/`, `docs/`. TASK 04에서 생성된다고 적는다.
- **데이터 규칙 (필수 포함):**
  - 단어 데이터는 `exports/words.json`(dataVersion 포함)을 앱에 내장하고, 저장된 dataVersion보다 크면 Word만 upsert한다. UserWord는 건드리지 않는다.
  - `OnConflictStrategy.REPLACE` 사용 금지. 행을 지우고 다시 넣어 id가 바뀌기 때문에 UserWord가 깨진다. `(language, word)`로 찾아 UPDATE, 없으면 INSERT (`@Upsert` 허용).
  - 기존 단어의 `seq`는 절대 바꾸지 않는다. 단어 삭제는 `deleted = true`.
  - **Day = `(seq - 1) / 40 + 1`**. deleted 단어는 그 Day에서 숨기기만 한다.
  - 단어 저장·비교는 소문자. 뜻이 불확실하면 값 앞에 `*`.
  - 모든 DB 쿼리는 `language`로 필터링한다.
  - `words.json` 수정 방법: 파일을 직접 고치고 dataVersion을 1 올린다 (생성 스크립트는 `web-final` 태그에 있다).
- **코드 규칙:** Kotlin 100%, Compose만 사용(XML 레이아웃 금지), 도메인 로직(채점, Day 계산)은 `core/common`의 순수 Kotlin으로 두고 단위 테스트를 작성한다. LLM 프롬프트는 JSON만 반환하도록 작성한다.
- **완료 조건:** `./gradlew test`, `./gradlew lint` 통과 (TASK 04부터 적용. 그 전에는 TASK에 적힌 검증으로 대신한다고 명시).
- **금지사항:** 네트워크 권한은 모델 다운로드 외에 사용 금지, API 키 하드코딩 금지, `.env*`·`local.properties`·keystore 커밋 금지, main 브랜치 직접 커밋 금지, `OnConflictStrategy.REPLACE` 금지. 웹 관련 금지사항(`@prisma/client`, `middleware.ts`, yt-dlp `execFile`, 세션 확인)은 삭제한다.
- **파일 수정 권한:** "`prisma/schema.prisma` 변경, 마이그레이션 실행"을 "Room 스키마(엔티티) 변경, Migration 추가"로 바꾼다.
- **Git 규칙, SPEC.md 규칙:** 그대로 유지.

### CLAUDE.md (개정)
- "리뷰 관점"을 바꾼다:
  1. **데이터 안전** — Room Migration 누락, `REPLACE` 사용, seq 변경, UserWord 손실 가능성
  2. **정확성** — 수용 기준, Day 계산, `language` 필터 누락, 채점 규칙
  3. **보안·권한** — 불필요한 안드로이드 권한, 네트워크 사용, 키 노출
  4. **규칙 위반**, 5. **테스트**, 6. **유지보수성** (기존 순서 유지)
- "리뷰 방법"의 `app/generated/**` 언급을 `build/`, `*.lock` 등 생성 파일로 바꾼다.
- 나머지(역할, 하는 일/하지 않는 일, 판정)는 유지한다.

### .gitignore
- 삭제: node_modules, .next, next-env, vercel, typescript tsbuildinfo, pnp/yarn 관련 항목
- 추가: `.gradle/`, `build/`, `local.properties`, `*.iml`, `.idea/`, `captures/`, `*.keystore`, `*.jks`, `.externalNativeBuild/`, `.cxx/`
- 유지: `.DS_Store`, `.env*`, `exports/.cache/`

### SPEC.md
- 수정하지 않는다 (Approve 후 Claude가 업데이트).

## 수용 기준
- [ ] AC1: `git ls-files`에 `app/`, `components/`, `lib/`, `prisma/`, `public/`, `scripts/`, `tests/` 아래 파일과 삭제 대상 설정 파일이 0개다
- [ ] AC2: `git ls-files`에 `exports/words.json`, `exports/source/words-zh.json`, `exports/source/words-en.json`, `exports/source/words-en-advanced.json`, `SPEC.md`, `docs/decisions/001-native-pivot.md`가 모두 있다
- [ ] AC3: `exports/words.json`이 이 커밋에서 변경되지 않았다 (`git diff <base> HEAD -- exports/` 결과 없음)
- [ ] AC4: AGENTS.md에 `REPLACE` 금지, `(seq - 1) / 40 + 1`, dataVersion upsert 규칙, `language` 필터 규칙이 모두 있다
- [ ] AC5: AGENTS.md·CLAUDE.md에 `Next.js`, `Prisma`, `proxy.ts`, `iron-session`, `middleware`, `Blueprint`라는 단어가 0개다 (`grep -n` 결과 첨부)
- [ ] AC6: `.gitignore`에 `local.properties`, `build/`, `.gradle/`가 있다
- [ ] AC7: 커밋은 하나이고 메시지는 `chore(pivot): 웹앱 코드 제거`다
- [ ] AC8: 로컬 `.env`, `.env.local`이 남아 있다

## 제약 / 주의
- 삭제 전에 `git tag -l web-final`과 `git show web-final --stat | head`로 태그가 있는지 먼저 확인한다. 없으면 중단하고 보고한다.
- 다른 브랜치나 태그는 건드리지 않는다. push 금지.
- 이 TASK에는 빌드·테스트가 없다 (Node 도구가 사라진다). 완료 조건은 위 AC로 대신한다.

## 범위 밖 (하지 말 것)
- 안드로이드 프로젝트 생성 (TASK 04)
- `exports/`, `docs/` 안의 파일 수정 (이 TASK의 HANDOFF.md 작성은 제외)
- SPEC.md 수정
