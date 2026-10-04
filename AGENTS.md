<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# AGENTS.md — 모든 AI 에이전트 공통 규칙 (단일 원본)

> 이 파일이 규칙의 **유일한 원본**이다. Codex(GPT)는 이 파일을 자동으로 읽고, Claude Code는 CLAUDE.md의 `@AGENTS.md`로 읽는다.
> 규칙을 바꿀 때는 이 파일만 고친다. CLAUDE.md에는 Claude 역할 규칙만 둔다.
>
> 문서 충돌 시 우선순위: **AGENTS.md > 현재 작업의 TASK.md > SPEC.md > docs/design/\* > Opic-App-Blueprint.md**
> (Blueprint는 초기 설계 초안이라 Next.js 14 / middleware.ts 등 구버전 내용이 있다. 참고만 하고 그대로 따르지 않는다.)

## 프로젝트 요약
Next.js 16 + Prisma 7 + shadcn/ui 기반 영어(OPIc)/중국어(HSK) 학습 웹앱. 본인 + 소수 지인용 비공개 앱.

### 기술 스택
- Next.js 16 (App Router, `proxy.ts` — middleware 아님)
- Prisma 7 (adapter-pg 방식, output: `app/generated/prisma`)
- shadcn/ui (Radix + Nova 프리셋), Tailwind CSS v4
- iron-session (세션 인증, 쿠키명 `opic_session`), bcryptjs
- Anthropic API: Haiku 4.5 (런타임 교정/채점), Sonnet 4.6 (씨드 생성)
- OpenAI Whisper API (음성 변환), Web Speech API (TTS)
- 테스트: Vitest (`npm test`)

### Prisma 규칙
- import 경로: `app/generated/prisma` (`@prisma/client` 직접 import 금지)
- PrismaClient 생성 시 반드시 PrismaPg adapter 사용
- `lib/prisma.ts` globalThis 싱글톤만 사용 (새 PrismaClient 생성 금지)
- 모든 DB 쿼리는 `language` 필드로 필터링

### 인증
- `proxy.ts` 하나로 전체 차단. 공개 경로는 로그인 페이지 + `/api/auth/login`뿐
- 역할: `admin` | `member`. 회원가입 없음 (admin만 계정 추가)
- 모든 API route는 세션 확인 필수

### 폴더 구조
- `app/(auth)/login` — 로그인
- `app/(main)/[lang]/{words,grammar,shadowing,speaking,analysis}` — 언어별 탭
- `app/api/` — API Routes
- `components/{navigation,words,grammar,shadowing,speaking,analysis}`
- `lib/` — prisma.ts, session.ts, claude.ts, lang.ts, tts.ts
- `scripts/` — 씨드 스크립트
- `tests/` — Vitest 테스트
- `docs/design/` — 화면 설계 (해당 탭 작업 시에만 읽기. 예: 단어 탭 → `docs/design/words-ui.md`)
- `docs/tasks/<이슈번호>-<slug>/` — 작업별 TASK / HANDOFF / REVIEW
- `docs/workflow/` — 워크플로우 템플릿과 프롬프트

### 언어 구조
- lang 파라미터: `"en"` | `"zh"`
- 영어: OPIc (IL→AL), 발음기호 IPA / 중국어: HSK (1~6급), 병음

### 코드 규칙
- TypeScript 100%, 함수형 컴포넌트만
- 컴포넌트는 shadcn/ui 우선
- API 응답은 항상 JSON, 오류는 `{ error: string }`
- AI API 프롬프트는 JSON만 반환하도록 작성
- 단어 데이터: 뜻이 불확실하면 값 앞에 `*` (예: `"*난해한"`) → 추후 수동 검토 대상
- 단어 저장/비교는 소문자 처리

---

## 역할 분담

| 역할 | 담당 | 하는 일 | 하지 않는 일 |
|------|------|---------|-------------|
| 구현 | GPT (Codex) | TASK대로 구현, 테스트, 커밋, HANDOFF 작성, REVIEW 반영 | TASK 범위 밖 수정, 설계 임의 변경, push |
| 리뷰 | Claude (Claude Code) | TASK 작성, diff 리뷰, REVIEW 작성, SPEC 업데이트 | 코드 구현 |
| 결정 | 사용자 | TASK 승인, 의견 충돌 판정, push | |

## 작업 흐름

```
① 사용자  이슈 번호 결정 (.github/ISSUES.md)
② Claude  docs/tasks/<번호>-<slug>/TASK.md 작성
③ 사용자  TASK 검토·승인
④ GPT     구현 → 테스트 → 로컬 커밋 → HANDOFF.md 작성
⑤ Claude  HANDOFF의 base..HEAD diff 리뷰 → REVIEW.md 작성
⑥ GPT     REVIEW 반영 → 커밋 → HANDOFF "리뷰 반영" 섹션 추가 → ⑤로
⑦ Claude  Approve → SPEC.md 업데이트
⑧ 사용자  push (dev)
```

템플릿: `docs/workflow/templates/{TASK,HANDOFF,REVIEW}.md`
복붙용 프롬프트: `docs/workflow/PROMPTS.md`

---

## 구현 에이전트(GPT) 규칙

### 시작 전
1. `docs/tasks/<현재 작업>/TASK.md`를 읽는다. **TASK.md가 없으면 구현하지 말고 사용자에게 요청한다.**
2. TASK의 "관련 파일"과 "참고 문서"만 읽는다. 저장소 전체 탐색은 하지 않는다.
3. Next.js API를 쓰기 전 `node_modules/next/dist/docs/`의 해당 가이드를 확인한다.
4. 시작 커밋 해시를 기록해 둔다 (`git rev-parse --short HEAD`) → HANDOFF의 base.

### 파일 수정 권한
- 사용자가 승인한 TASK의 **"수정 범위"에 적힌 파일은 추가 허락 없이 생성/수정**한다.
- 아래는 **반드시 사용자 허락**을 먼저 받는다:
  - 수정 범위 밖 파일의 생성/수정/삭제
  - 패키지 설치·삭제
  - `prisma/schema.prisma` 변경, 마이그레이션 실행
  - AGENTS.md / CLAUDE.md 수정
- TASK가 모호하거나 틀렸다고 판단되면 추측으로 진행하지 말고 사용자에게 묻는다.

### 완료 조건 (전부 통과해야 HANDOFF 작성)
- [ ] `npx tsc --noEmit` 통과
- [ ] `npm run lint` 통과
- [ ] `npm test` 통과 (기능마다 테스트 작성, 기능 변경 시 테스트도 수정)
- [ ] TASK의 수용 기준 전부 충족
- [ ] dev 브랜치에 로컬 커밋 (**push 금지**)
- [ ] `docs/tasks/<현재 작업>/HANDOFF.md` 작성 (템플릿 그대로)

### REVIEW 반영
- **Must-fix**: 전부 반영한다.
- **Should-fix**: 반영하거나, 안 하면 HANDOFF에 이유를 적는다.
- **Nit**: 선택.
- 리뷰에 동의하지 않으면 반영하지 말고 HANDOFF "리뷰 반영" 표에 반론을 적는다 → 사용자가 판단.
- 수정은 **새 커밋**으로 한다. `commit --amend`, rebase, force push 금지 (리뷰 diff 추적용).
- 재리뷰 요청 시 HANDOFF에 "리뷰 반영 커밋 범위"를 적는다.

### 보고 형식 (사용자에게)
- 변경된 부분만 diff 형식으로 (`+` 추가 / `-` 삭제) + 변경 이유 한 줄
- 전체 파일 출력 금지

---

## 금지사항 (모든 에이전트)
- `@prisma/client` 직접 import
- `middleware.ts` 파일명 사용 (`proxy.ts` 사용)
- API 키 하드코딩, `.env*` 커밋
- `ANTHROPIC_API_KEY` / `OPENAI_API_KEY`를 시스템 환경변수로 설정 (구독 대신 API 과금됨. 키는 `.env.local`에만)
- yt-dlp를 `exec` 문자열로 호출 (반드시 `execFile` + 인자 배열). 유튜브 URL은 정규식으로 사전 검증
- 세션 확인 없는 API route
- main 브랜치 직접 커밋

## SPEC.md 규칙
- 새 기능 추가 시 해당 섹션에 기록, 변경 시 하단 변경 이력에 추가 (삭제 금지)
- API 변경 시 이전/이후 URL 모두 기록
- Approve 이후 Claude가 업데이트한다

## Git 규칙
- main: 배포용 (안정 코드만) / dev: 개발 및 커밋
- 커밋 메시지:
```
feat(기능이름): 세부설명 (#이슈번호)

* 세부 설명 1
* 세부 설명 2
```
- 타입: feat / fix / test / spec / refactor / chore / review(리뷰 반영)
- 예: `feat(words/day-index): Day 인덱스 페이지 구현 (#6)`
