@AGENTS.md
# OPIc · HSK 학습 앱

## 프로젝트 개요
Next.js 16 + Prisma 7 + shadcn/ui 기반 영어/중국어 학습 웹앱

## 기술 스택
- Next.js 16 (App Router, proxy.ts — middleware 아님)
- Prisma 7 (adapter-pg 방식, output: app/generated/prisma)
- shadcn/ui (Radix + Nova 프리셋)
- Tailwind CSS v4
- iron-session (세션 인증)
- bcryptjs (비밀번호 해시)
- Anthropic Claude API (Haiku 4.5 — 교정/채점, Sonnet 4.6 — 씨드 생성)
- OpenAI Whisper API (음성 변환)

## Prisma 사용 규칙
- import 경로: `../app/generated/prisma` (절대 `@prisma/client` 사용 금지)
- PrismaClient 생성 시 반드시 PrismaPg adapter 사용
- lib/prisma.ts 싱글톤 패턴 사용

## 인증
- proxy.ts (middleware.ts 아님 — Next.js 16 변경사항)
- iron-session, 쿠키명: opic_session
- 역할: admin | member

## 폴더 구조
- app/(auth)/login — 로그인
- app/(main)/[lang]/{words,grammar,shadowing,speaking,analysis} — 언어별 탭
- app/api/ — API Routes
- components/{navigation,words,grammar,shadowing,speaking,analysis}
- lib/ — prisma.ts, session.ts, claude.ts, lang.ts
- scripts/ — 씨드 스크립트

## 언어 구조
- lang 파라미터: "en" | "zh"
- 영어: OPIc (IL→AL 목표)
- 중국어: HSK (1~6급 목표)

## 개발 규칙
- 컴포넌트는 shadcn/ui 우선 사용
- API route는 세션 확인 필수
- 오류 응답: { error: string } 형식
- 모든 DB 쿼리는 language 필드로 필터링