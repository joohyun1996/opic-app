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

## 단어 탭 화면 설계

### 공통 규칙
- max-width: 430px, 중앙 정렬, 라이트모드 전용
- 컬러: 흑백 베이스 (#1a1a18, #f8f7f4, #e8e6e0)
- 상태 뱃지: 습득=green(#eaf3de), 학습중=amber(#faeeda), 신규=gray(#f1efe8), 오답=red(#fcebeb)
- 모든 단어 DB 저장 및 비교 시 소문자 처리

### 화면 목록

**① 홈** — 언어 카드 2개 (영어/중국어), 각 진행률 프로그레스바

**② Day 인덱스** — 4열 그리드, Day 1~N, 완료수/40 표시, 헤더에 오답 뱃지(빨간색)

**③ Day 단어 목록** — 상단 프로그레스바, 단어+발음기호+♪버튼+상태뱃지 리스트, 하단 학습 시작 버튼

**④ 플래시카드 영→한**
- 상단: 영어 단어 + ♪버튼(Web Speech API) + 발음기호
- 하단: 한국어 뜻 타이핑 input
- 확인 후: 정답(초록 테두리 + ✓) / 오답(빨간 테두리 + ✗ + 정답 표시)
- 정답 판정: 소문자 변환 후 완전 일치

**⑤ 플래시카드 한→영**
- 상단: 한국어 뜻 + 품사
- 힌트: 첫글자_마지막글자 마스킹 (co_ _ _ _ _ _t)
- 하단: 영어 단어 타이핑 input
- 확인 후: 정답(초록 + ♪버튼 + 발음기호) / 오답(빨간 + 정답 표시)
- 정답 판정: 소문자 변환 후 완전 일치

**⑥ 오답 모음** — Day 필터 탭, 빨간 왼쪽 border 카드, 틀린 횟수 표시, 오답만으로 학습 시작 버튼

### ♪ 발음 버튼 구현
- Web Speech API의 SpeechSynthesis 사용 (무료, 브라우저 내장)
- 영어: lang="en-US", 중국어: lang="zh-CN"
- 코드 예시:
  const speak = (word: string, lang: string) => {
    const utter = new SpeechSynthesisUtterance(word)
    utter.lang = lang === 'en' ? 'en-US' : 'zh-CN'
    window.speechSynthesis.speak(utter)
  }