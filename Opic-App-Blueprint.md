# 다국어 학습 앱 — 전체 설계 뼈대

> 작성 기준: 2026년 5월
> 목적: 개인 + 지인 소수 사용 / 영어·중국어 학습 / OPIc AL + HSK 달성 포함

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 앱 유형 | 웹앱 (모바일 반응형) |
| 사용자 | 나(관리자) + 초대한 친구들 (소수) |
| 지원 언어 | 영어 (`en`) / 중국어 (`zh`) — 추후 확장 가능한 구조 |
| 목표 | 영어: OPIc IM2 → AL / 중국어: HSK 목표 달성 |
| 핵심 탭 | 단어, 섀도잉, 문법, 오답분석 (언어별 독립) |
| 공개 여부 | 비공개 (세션 없으면 전체 차단) |
| 학습 철학 | 어휘(Accuracy) → 문법(Accuracy) → 인풋(Fluency) → 아웃풋(Coherence) → 분석(Assessment) |

---

## 2. 기술 스택

### 프론트엔드
- **Next.js 14** (App Router)
- **Tailwind CSS**
- **shadcn/ui** (컴포넌트)

### 백엔드
- **Next.js API Routes** (프론트와 동일 프로젝트)
- **iron-session** (세션 관리)
- **bcryptjs** (비밀번호 해시)

### 데이터베이스
- **PostgreSQL**
- **Prisma** (ORM)

### 외부 API
- **OpenAI Whisper API** — 음성 → 텍스트 변환 + 단어 단위 타임스탬프 (`timestamp_granularities=["word"]`)
- **Claude Haiku 4.5 API** — 문법 교정, 문제 생성, 채점, OPIc 등급 평가, 유창성 진단
- **Claude Sonnet 4.6 API** — 문법 콘텐츠 최초 씨드 생성 (1회성, 품질 우선)
- **Web Speech API** — TTS (무료, 브라우저 내장)
- **yt-dlp** — 유튜브 자막 추출 (서버에 설치, execFile 방식 적용)

### 인프라
- **Oracle Cloud Free Tier** — ARM 4코어 / 24GB (무료)
- **k3s** — 경량 쿠버네티스
- **Nginx** — 리버스 프록시
- **GitHub Actions** — CI/CD
- **Cloudflare** — DNS + 도메인 연결

### 개발 환경
- **맥북** (로컬 개발)
- **VS Code + Claude Code 확장** (Claude Pro 구독으로 연결, 추가 비용 없음)
- **Docker Desktop + OrbStack** (로컬 k8s 테스트)

---

## 3. 프로젝트 폴더 구조

```
opic-app/
├── app/
│   ├── (auth)/
│   │   └── login/
│   │       └── page.tsx              # 로그인 페이지
│   ├── (main)/
│   │   ├── layout.tsx                # 공통 레이아웃 + 네비게이션
│   │   ├── page.tsx                  # 언어 선택 홈 (영어 / 중국어)
│   │   └── [lang]/                   # lang = "en" | "zh"
│   │       ├── layout.tsx            # 언어별 레이아웃 (탭 네비게이션)
│   │       ├── words/
│   │       │   ├── page.tsx          # 단어 메인
│   │       │   ├── [id]/page.tsx     # 단어 상세
│   │       │   └── quiz/page.tsx     # 단어 퀴즈
│   │       ├── shadowing/
│   │       │   ├── page.tsx          # 섀도잉 목록
│   │       │   └── [id]/page.tsx     # 섀도잉 플레이어
│   │       ├── grammar/
│   │       │   ├── page.tsx          # 문법 챕터 목록
│   │       │   └── [chapter]/
│   │       │       ├── page.tsx      # 챕터 설명 + 예문
│   │       │       ├── quiz/page.tsx # 챕터 퀴즈
│   │       │       └── speaking/page.tsx
│   │       ├── speaking/
│   │       │   └── page.tsx          # 스피킹 연습
│   │       └── analysis/
│   │           └── page.tsx          # 오답 분석 대시보드
│   ├── admin/
│   │   ├── page.tsx                  # 관리자 메인
│   │   └── users/page.tsx            # 사용자 관리 (ID 추가/삭제)
│   └── api/
│       ├── auth/
│       │   ├── login/route.ts
│       │   └── logout/route.ts
│       ├── words/
│       │   ├── route.ts              # GET ?lang=en&level=3
│       │   └── [id]/route.ts
│       ├── shadowing/
│       │   ├── route.ts              # GET ?lang=en
│       │   └── [id]/route.ts
│       ├── shadowing/extract/route.ts
│       ├── grammar/
│       │   ├── route.ts              # GET ?lang=en
│       │   └── [chapter]/
│       │       ├── route.ts
│       │       └── quiz/route.ts
│       ├── speaking/
│       │   ├── transcribe/route.ts   # lang 파라미터로 Whisper 언어 지정
│       │   └── correct/route.ts      # lang에 따라 OPIc/HSK 평가
│       ├── analysis/route.ts         # GET ?lang=en
│       └── admin/
│           └── users/route.ts
├── components/
│   ├── navigation/
│   │   ├── LangSelector.tsx          # 언어 선택 홈 컴포넌트
│   │   └── TabNav.tsx
│   ├── words/
│   │   ├── WordCard.tsx
│   │   ├── WordList.tsx
│   │   └── LevelBadge.tsx
│   ├── shadowing/
│   │   ├── VideoPlayer.tsx
│   │   ├── SubtitleSync.tsx
│   │   └── RecordButton.tsx
│   ├── grammar/
│   │   ├── ChapterCard.tsx
│   │   ├── ConceptExplainer.tsx
│   │   ├── ExampleCard.tsx
│   │   └── QuizCard.tsx
│   ├── speaking/
│   │   ├── MicRecorder.tsx
│   │   ├── FeedbackDisplay.tsx
│   │   └── ErrorHighlight.tsx
│   └── analysis/
│       ├── ErrorChart.tsx
│       └── WeakPointList.tsx
├── lib/
│   ├── session.ts
│   ├── prisma.ts
│   ├── claude.ts
│   ├── whisper.ts                    # lang 파라미터 지원
│   ├── ytdlp.ts
│   └── lang.ts                       # 언어별 설정 (시험명, 레벨, 단어소스 등)
├── middleware.ts
├── prisma/
│   └── schema.prisma
├── scripts/
│   ├── seed-words-en.ts              # 영어 단어 씨드 (COCA+GRE+AWL)
│   ├── seed-words-zh.ts              # 중국어 단어 씨드 (HSK 1~6)
│   ├── seed-grammar-en.ts            # 영어 문법 35챕터 씨드
│   └── seed-grammar-zh.ts            # 중국어 문법 챕터 씨드
└── public/
```

---

## 4. 데이터베이스 스키마 (Prisma)

```prisma
// prisma/schema.prisma

generator client {
  provider = "prisma-client"
  output   = "../app/generated/prisma"
}

datasource db {
  provider = "postgresql"
}

// ─── 사용자 ───────────────────────────────────────

model User {
  id          Int       @id @default(autoincrement())
  username    String    @unique
  password    String                    // bcrypt 해시
  role        String    @default("member")  // "admin" | "member"
  createdAt   DateTime  @default(now())

  userWords        UserWord[]
  grammarProgress  GrammarProgress[]
  grammarErrors    GrammarError[]
  shadowingVideos  ShadowingVideo[]
  speakingSessions SpeakingSession[]
}

// ─── 단어 ─────────────────────────────────────────

model Word {
  id           Int       @id @default(autoincrement())
  language     String                    // "en" | "zh"
  word         String
  phonetic     String?                   // 영어: IPA /prəˌnʌnsiˈeɪʃən/ / 중국어: 병음 pīnyīn
  meaningKo    String                    // 한국어 뜻
  meaningEn    String?                   // 영어 정의
  example      String?                   // 예문
  exampleKo    String?                   // 예문 번역
  level        Int                       // 영어: 1~5(COCA→논문) / 중국어: 1~6(HSK급수)
  category     String                    // 영어: "coca"|"gre"|"awl" / 중국어: "hsk1"~"hsk6"
  partOfSpeech String?
  collocations String[]
  createdAt    DateTime  @default(now())

  userWords    UserWord[]

  @@unique([language, word])            // 언어별 단어 중복 방지
  @@index([language, level])
  @@index([language, category])
  @@index([language, level, category])
}

model UserWord {
  userId       Int
  wordId       Int
  status       String    @default("new")  // "new" | "learning" | "mastered"
  correctCount Int       @default(0)
  wrongCount   Int       @default(0)
  nextReview   DateTime?
  lastStudied  DateTime?
  createdAt    DateTime  @default(now())

  user         User      @relation(fields: [userId], references: [id])
  word         Word      @relation(fields: [wordId], references: [id])

  @@id([userId, wordId])
  @@index([userId, status, nextReview])  // 오늘 복습할 단어 조회용
  @@index([userId, status])
}

// ─── 문법 ─────────────────────────────────────────

model GrammarChapter {
  id             String    @id          // "en_tense_present_perfect" | "zh_aspect_particle"
  language       String                 // "en" | "zh"
  category       String                 // 영어: "시제"|"조동사" / 중국어: "어기조사"|"보어" 등
  title          String
  order          Int
  coreConceptKo  String
  structureTable Json
  goodExamples   Json
  badExamples    Json
  examTip        String?                // 영어: OPIc 팁 / 중국어: HSK 팁
  quizBank       Json
  createdAt      DateTime  @default(now())

  progress       GrammarProgress[]

  @@index([language, category])
  @@index([language, order])
}

model GrammarProgress {
  userId      Int
  chapterId   String
  status      String    @default("not_started")
  lastScore   Int?
  lastStudied DateTime?
  doneAt      DateTime?

  user        User           @relation(fields: [userId], references: [id])
  chapter     GrammarChapter @relation(fields: [chapterId], references: [id])

  @@id([userId, chapterId])
  @@index([userId, status])
  @@index([userId, lastStudied])
}

// ─── 오답 기록 ────────────────────────────────────

model GrammarError {
  id          Int       @id @default(autoincrement())
  userId      Int
  language    String                    // "en" | "zh"
  source      String                    // "speaking" | "grammar_quiz" | "word_quiz"
  original    String
  corrected   String
  errorType   String                    // 영어: "시제오류"|"관사누락" / 중국어: "성조"|"어순" 등
  errorDetail String?
  chapterId   String?
  createdAt   DateTime  @default(now())

  user        User      @relation(fields: [userId], references: [id])

  @@index([userId, language, errorType])
  @@index([userId, language, createdAt])
}

// ─── 스피킹 세션 ──────────────────────────────────

model SpeakingSession {
  id             Int       @id @default(autoincrement())
  userId         Int
  language       String                  // "en" | "zh"
  questionType   String                  // "exam_random" | "grammar_practice" | "free"
  question       String
  originalText   String
  correctedText  String
  feedback       Json
  examLevel      String?                 // 영어: "IL"~"AL" / 중국어: "HSK1"~"HSK6"
  pauseCount     Int       @default(0)
  wordTimestamps Json?
  audioPath      String?
  createdAt      DateTime  @default(now())

  user           User      @relation(fields: [userId], references: [id])

  @@index([userId, language, createdAt])
  @@index([userId, language, examLevel])
}

// ─── 섀도잉 ───────────────────────────────────────

model ShadowingVideo {
  id          Int       @id @default(autoincrement())
  userId      Int
  language    String                    // "en" | "zh"
  youtubeUrl  String
  title       String
  thumbnail   String?
  duration    Int?
  subtitles   Json
  isShared    Boolean   @default(false)
  createdAt   DateTime  @default(now())

  user        User      @relation(fields: [userId], references: [id])

  @@index([language])
}
```

---

## 5. Prisma 싱글톤 패턴

Next.js dev 모드는 파일 변경 시마다 모듈을 재로드해요. 단순히 `new PrismaClient()`를 호출하면 매번 새 DB 커넥션이 생겨서 PostgreSQL 커넥션 풀이 금방 고갈돼요. `globalThis`로 싱글톤을 유지해야 해요.

```typescript
// lib/prisma.ts

import { PrismaClient } from '@prisma/client'

const globalForPrisma = globalThis as unknown as {
  prisma: PrismaClient | undefined
}

export const prisma =
  globalForPrisma.prisma ?? new PrismaClient()

// 개발 환경에서만 전역에 저장 (프로덕션은 매번 새 인스턴스가 정상)
if (process.env.NODE_ENV !== 'production') {
  globalForPrisma.prisma = prisma
}
```

---

## 6. 인증 구조

### 5-1. 미들웨어 (전체 차단)

```typescript
// middleware.ts

import { NextRequest, NextResponse } from 'next/server'
import { getIronSession } from 'iron-session'

const PUBLIC_PATHS = ['/login', '/api/auth/login']

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // 로그인 페이지 + 로그인 API는 통과
  if (PUBLIC_PATHS.some(p => pathname.startsWith(p))) {
    return NextResponse.next()
  }

  // 정적 파일 통과
  if (pathname.startsWith('/_next') || pathname.startsWith('/favicon')) {
    return NextResponse.next()
  }

  // 세션 확인
  const session = await getIronSession(request, NextResponse.next(), {
    cookieName: 'opic_session',
    password: process.env.SESSION_SECRET!,
  })

  // 세션 없으면 전부 차단
  if (!session.userId) {
    // API 요청이면 401
    if (pathname.startsWith('/api/')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }
    // 페이지 요청이면 로그인으로 리다이렉트
    return NextResponse.redirect(new URL('/login', request.url))
  }

  // 어드민 전용 경로
  if (pathname.startsWith('/admin') || pathname.startsWith('/api/admin')) {
    if (session.role !== 'admin') {
      return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
```

### 5-2. 세션 타입 정의

```typescript
// lib/session.ts

import { SessionOptions } from 'iron-session'

export interface SessionData {
  userId: number
  username: string
  role: 'admin' | 'member'
}

export const sessionOptions: SessionOptions = {
  cookieName: 'opic_session',
  password: process.env.SESSION_SECRET!,  // 최소 32자 랜덤 문자열
  cookieOptions: {
    secure: process.env.NODE_ENV === 'production',
    maxAge: 60 * 60 * 24 * 7,  // 7일
  },
}
```

### 5-3. 로그인 API

```typescript
// app/api/auth/login/route.ts

import { NextRequest, NextResponse } from 'next/server'
import { getIronSession } from 'iron-session'
import bcrypt from 'bcryptjs'
import { prisma } from '@/lib/prisma'
import { sessionOptions, SessionData } from '@/lib/session'

export async function POST(request: NextRequest) {
  const { username, password } = await request.json()

  const user = await prisma.user.findUnique({ where: { username } })

  if (!user || !(await bcrypt.compare(password, user.password))) {
    return NextResponse.json({ error: '아이디 또는 비밀번호가 틀렸습니다' }, { status: 401 })
  }

  const response = NextResponse.json({ ok: true })
  const session = await getIronSession<SessionData>(request, response, sessionOptions)

  session.userId = user.id
  session.username = user.username
  session.role = user.role as 'admin' | 'member'
  await session.save()

  return response
}
```

### 5-4. 관리자 전용: ID 추가 API

```typescript
// app/api/admin/users/route.ts

export async function POST(request: NextRequest) {
  const { username, password } = await request.json()

  const hashedPassword = await bcrypt.hash(password, 12)

  const user = await prisma.user.create({
    data: { username, password: hashedPassword, role: 'member' }
  })

  return NextResponse.json({ ok: true, userId: user.id })
}
```

---

## 7. 기능별 상세 설계

---

### 6-1. 단어 탭

#### 단어 DB 구성

```
총 단어 수: 약 7,000~10,000개

소스:
- COCA 5000    → 레벨 1~3 (일상/비즈니스)
- AWL 570      → 레벨 3~4 (학술)
- GRE 3500     → 레벨 4~5 (고급/논문)

레벨 기준:
- 1: 중학교 수준 (make, time, know)
- 2: 고등학교 수준 (analyze, significant)
- 3: 대학교 수준 (phenomenon, coherent)
- 4: GRE/TOEFL 수준 (esoteric, obfuscate)
- 5: 논문/학술 수준 (epistemological, solipsistic)
```

#### 단어 씨드 스크립트

```typescript
// scripts/seed-words.ts
// 공개 데이터셋 CSV 파싱 후 DB에 일괄 저장
// COCA: https://www.wordfrequency.info
// AWL:  https://www.victoria.ac.nz/lals/resources/academicwordlist
// GRE:  공개 GRE 단어 리스트 다수 존재

import { parse } from 'csv-parse/sync'
import { prisma } from '../lib/prisma'

async function seedWords() {
  const cocaWords = parse(fs.readFileSync('data/coca5000.csv'))
  // ... 파싱 후 level 자동 매핑
  await prisma.word.createMany({ data: words, skipDuplicates: true })
}
```

#### 단어 학습 화면 흐름

```
단어 목록
├── 필터: 레벨(1~5) / 카테고리 / 상태(new/learning/mastered)
├── 검색
└── 정렬: 난이도 / 알파벳 / 최근 추가

단어 카드 클릭
├── 단어 + 발음기호
├── 품사 + 한국어 뜻
├── 영어 정의
├── 예문 (영어 + 한국어)
├── 콜로케이션 목록
├── [TTS 듣기] 버튼 → Web Speech API
└── [내 단어장에 추가] 버튼

학습 상태 관리 (스페이스드 리피티션)
- new → 처음 만난 단어
- learning → 틀린 적 있는 단어 (1~3일 후 복습)
- mastered → 연속 3회 정답 (7일 후 복습)
```

#### 단어 퀴즈 유형

```
1. 뜻 맞추기: 단어 보여주고 → 한국어 뜻 4지선다
2. 단어 맞추기: 한국어 뜻 보여주고 → 영어 단어 입력
3. 예문 빈칸: 예문에서 단어 빈칸 → 4지선다
4. 발음 퀴즈: TTS 듣고 → 어떤 단어인지 선택
```

#### 단어 관련 API

```
GET  /api/words?level=3&status=new&limit=20   단어 목록
GET  /api/words/:id                            단어 상세
POST /api/words/:id/status                     학습상태 변경
GET  /api/words/quiz?level=2,3&count=10        퀴즈용 단어 반환
POST /api/words/quiz/result                    퀴즈 결과 저장
GET  /api/words/review                         오늘 복습할 단어
```

---

### 6-2. 섀도잉 탭

#### 영상 추가 흐름

```
유저가 유튜브 URL 입력
        ↓
서버에서 yt-dlp 실행
        ↓
자막 추출 (영어 자막 우선, 없으면 자동생성 자막)
        ↓
자막 JSON 파싱 → [{start: 1.2, end: 3.5, text: "Hello everyone"}]
        ↓
DB 저장 (ShadowingVideo)
        ↓
플레이어 화면으로 이동
```

#### 자막 추출 서버 코드

```typescript
// lib/ytdlp.ts

import { execFile } from 'child_process'
import { promisify } from 'util'
import fs from 'fs'

const execFileAsync = promisify(execFile)

// 유튜브 URL 형식 검증 (Command Injection 방어 1차)
const YOUTUBE_URL_REGEX = /^https:\/\/(www\.)?(youtube\.com\/watch\?v=|youtu\.be\/)[a-zA-Z0-9_-]{11}/

export async function extractSubtitles(youtubeUrl: string) {
  // URL 검증 — 통과 못하면 즉시 차단
  if (!YOUTUBE_URL_REGEX.test(youtubeUrl)) {
    throw new Error('유효하지 않은 유튜브 URL입니다')
  }

  const outputPath = `/tmp/subtitle_${Date.now()}`

  // execFile로 인자 배열 분리 전달 (Command Injection 방어 2차)
  // exec('... "${youtubeUrl}"') 방식은 shell injection 취약점 있음
  await execFileAsync('yt-dlp', [
    '--write-sub',
    '--write-auto-sub',
    '--sub-lang', 'en',
    '--skip-download',
    '--output', outputPath,
    youtubeUrl,           // 인자 분리로 shell 해석 없음
  ])

  // .vtt 파일 파싱 → JSON 변환
  const subtitles = parseVTT(fs.readFileSync(`${outputPath}.en.vtt`, 'utf8'))
  return subtitles
  // [{start: 1.2, end: 3.5, text: "Hello everyone"}, ...]
}
```

#### 섀도잉 플레이어 UI

```
┌──────────────────────────────────────────┐
│           유튜브 영상 (IFrame)            │
│                                          │
└──────────────────────────────────────────┘

재생바 ──────────●──────────────────── 03:42

[◀ 5초] [재생/정지] [▶ 5초]  [0.75x] [1x] [1.25x]

┌──────────────────────────────────────────┐
│ ▶ "The most important thing is to keep  │  ← 현재 구간 하이라이트
│   learning every single day."           │
│                                          │
│   가장 중요한 것은 매일 계속 배우는 것   │  ← 번역 토글
└──────────────────────────────────────────┘

이전 자막                         다음 자막

[이 구간 반복] [발음 녹음] [녹음 재생]

─────────── 녹음 피드백 ───────────
내 발음: "The most importent thing..."
                 ↑ 틀림
정답:   "The most important thing..."
```

#### 섀도잉 관련 API

```
GET  /api/shadowing                  영상 목록
POST /api/shadowing                  영상 추가 (URL 입력)
GET  /api/shadowing/:id              영상 + 자막 데이터
POST /api/shadowing/extract          yt-dlp 자막 추출
POST /api/speaking/transcribe        녹음 → Whisper 변환
```

---

### 6-3. 문법 탭

#### 챕터 목록 (35개)

```
카테고리 1: 시제 (6개)
├── tense_present_perfect      현재완료 vs 과거
├── tense_past_perfect         과거완료 (had p.p)
├── tense_perfect_continuous   현재완료진행
├── tense_future               미래 표현
├── tense_sequence             시제 일치
└── tense_reported_speech      화법 전환의 시제

카테고리 2: 조동사 (4개)
├── modal_must_should          must / have to / should
├── modal_possibility          could / might / may
├── modal_would                would (과거습관/공손)
└── modal_past_inference       should have / could have / must have

카테고리 3: 가정법 (4개)
├── conditional_2nd            if 2형식
├── conditional_3rd            if 3형식
├── conditional_wish            I wish / as if
└── conditional_unless         unless / provided / in case

카테고리 4: 관계사 (4개)
├── relative_basic             who / which / that
├── relative_nonrestrictive    비제한적 관계사
├── relative_what              what vs which
└── relative_adverb            where / when / why

카테고리 5: 수동태 (3개)
├── passive_basic              기본 수동태
├── passive_modal              조동사 수동태
└── passive_get                get 수동태

카테고리 6: 관사 (3개)
├── article_a_the              a vs the
├── article_zero               무관사
└── article_advanced           고급 관사 용법

카테고리 7: 전치사 (3개)
├── prep_time_place            in / on / at
├── prep_duration              for / since / during / while
└── prep_confusing             헷갈리는 전치사

카테고리 8: 문장구조 (5개)
├── structure_inversion        도치
├── structure_cleft            강조구문 (It is ~ that)
├── structure_participle       분사구문
├── structure_parallel         병렬구조
└── structure_comparison       비교급 고급

카테고리 9: 접속사 (3개)
├── conj_coord_sub             등위 vs 종속 접속사
├── conj_concession            양보 (although / despite)
└── conj_advanced              고급 접속사
```

#### 문법 콘텐츠 최초 생성 스크립트

```typescript
// scripts/seed-grammar.ts
// 딱 한 번만 실행. 결과를 DB에 저장.

import Anthropic from '@anthropic-ai/sdk'
import { prisma } from '../lib/prisma'

const client = new Anthropic()

const CHAPTERS = [
  { id: 'tense_present_perfect', category: '시제', title: '현재완료 vs 과거', order: 1 },
  // ... 35개
]

async function generateChapter(chapter: typeof CHAPTERS[0]) {
  const response = await client.messages.create({
    model: 'claude-sonnet-4-6',   // 최초 생성이라 좋은 모델 사용 (Haiku보다 품질 좋음, Opus보다 저렴)
    max_tokens: 4000,
    messages: [{
      role: 'user',
      content: `
        한국인 영어 학습자 (OPIc IM2, AL 목표)를 위한 문법 챕터를 작성해줘.
        챕터: "${chapter.title}"

        아래 JSON 형식으로만 응답해. 다른 말 없이 JSON만.
        {
          "coreConceptKo": "핵심 개념 설명 (한국어 4~5문장. 왜 이 문법이 중요한지, 한국인이 자주 틀리는 이유 포함)",
          "structureTable": [
            {
              "형태": "have + p.p",
              "의미": "현재와 연결된 과거 경험/결과",
              "키워드": ["ever", "never", "just", "already", "yet"],
              "예시": "I have just finished my work."
            }
          ],
          "goodExamples": [
            {
              "sentence": "I have visited Paris three times.",
              "explanation": "경험 표현 → 현재완료",
              "highlight": "have visited"
            }
          ],
          "badExamples": [
            {
              "wrong": "I have visited Paris last year.",
              "correct": "I visited Paris last year.",
              "reason": "last year처럼 명확한 과거 시점이 있으면 과거시제만 가능",
              "highlight": "last year"
            }
          ],
          "opicTip": "OPIc AL 수준에서 이 문법을 자연스럽게 쓰는 방법 (실전 팁 2~3문장)",
          "quizBank": [
            {
              "type": "fill_blank",
              "question": "I _______ (never / visit) New York before this trip.",
              "answer": "had never visited",
              "explanation": "이 여행 이전의 경험 → 과거완료",
              "difficulty": 2
            },
            {
              "type": "error_correction",
              "wrong": "She has graduated last year.",
              "correct": "She graduated last year.",
              "explanation": "last year = 명확한 과거 시점 → 과거시제",
              "difficulty": 1
            },
            {
              "type": "sentence_creation",
              "prompt": "현재완료를 써서 지금까지의 여행 경험을 한 문장으로",
              "sampleAnswer": "I have been to five different countries so far.",
              "difficulty": 3
            }
          ]
        }
      `
    }]
  })

  const content = response.content[0]
  if (content.type !== 'text') throw new Error('Unexpected response type')

  return JSON.parse(content.text)
}

async function main() {
  for (const chapter of CHAPTERS) {
    console.log(`생성 중: ${chapter.title}`)
    const data = await generateChapter(chapter)

    await prisma.grammarChapter.upsert({
      where: { id: chapter.id },
      create: {
        id: chapter.id,
        category: chapter.category,
        title: chapter.title,
        order: chapter.order,
        coreConceptKo: data.coreConceptKo,
        structureTable: data.structureTable,
        goodExamples: data.goodExamples,
        badExamples: data.badExamples,
        opicTip: data.opicTip,
        quizBank: data.quizBank,
      },
      update: {
        coreConceptKo: data.coreConceptKo,
        structureTable: data.structureTable,
        goodExamples: data.goodExamples,
        badExamples: data.badExamples,
        opicTip: data.opicTip,
        quizBank: data.quizBank,
      }
    })

    // API 레이트 리밋 방지
    await new Promise(r => setTimeout(r, 2000))
  }
  console.log('완료!')
}

main()
```

#### 문법 챕터 화면 흐름

```
1단계: 개념 설명 화면
─────────────────────
- coreConceptKo 렌더링
- structureTable 테이블 형태로 표시
- [다음: 예문 보기] 버튼

2단계: 예문 카드 화면
─────────────────────
- goodExamples 슬라이드 형태
- badExamples (틀린 문장 → 클릭하면 정답 + 이유 표시)
- [다음: 퀴즈 풀기] 버튼

3단계: 퀴즈 화면
─────────────────────
fill_blank:
  → 문장 표시 + 빈칸 입력
  → 정답 비교 (오답이면 오답DB 저장)

error_correction:
  → 틀린 문장 표시
  → 어디가 틀렸는지 직접 수정
  → Claude Haiku가 채점 + 해설

sentence_creation:
  → 프롬프트 표시
  → 자유 작문
  → Claude Haiku가 문법 체크 + 더 나은 표현 제안

4단계: 스피킹 연습 (선택)
─────────────────────
- 이 문법 챕터 관련 OPIc 질문 랜덤 출제
- 마이크 녹음 → Whisper → Claude 피드백
- [완료] → 진행도 업데이트
```

#### 문법 퀴즈 AI 채점 API

```typescript
// app/api/grammar/[chapter]/quiz/route.ts

export async function POST(request: NextRequest) {
  const { type, question, userAnswer, sampleAnswer } = await request.json()

  // fill_blank / error_correction은 DB 정답과 비교 (AI 불필요)
  if (type === 'fill_blank') {
    const isCorrect = userAnswer.trim().toLowerCase() === correctAnswer.toLowerCase()
    return NextResponse.json({ isCorrect, explanation })
  }

  // sentence_creation은 AI 채점
  const response = await claude.messages.create({
    model: 'claude-haiku-4-5',
    max_tokens: 500,
    messages: [{
      role: 'user',
      content: `
        영어 문장을 채점해줘. JSON으로만 응답.

        과제: ${question}
        학습자 답변: ${userAnswer}
        모범 답안: ${sampleAnswer}

        {
          "isCorrect": true/false,
          "grammarScore": 0~100,
          "errors": [{"original": "...", "fixed": "...", "reason": "..."}],
          "betterExpression": "더 자연스러운 표현",
          "feedback": "전체 피드백 한 문장"
        }
      `
    }]
  })

  return NextResponse.json(JSON.parse(response.content[0].text))
}
```

---

### 6-4. 오답 분석 탭

#### 대시보드 구성

```
┌─────────────────────────────────────────┐
│  이번 달 오류 요약                       │
│                                         │
│  시제 오류    ████████░░  23회  (38%)   │
│  관사 누락    █████░░░░░  15회  (25%)   │
│  전치사       ███░░░░░░░   8회  (13%)   │
│  조동사       ██░░░░░░░░   6회  (10%)   │
│  기타         ███░░░░░░░   9회  (14%)   │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  취약 문법 챕터 추천                     │
│                                         │
│  1위. 현재완료 vs 과거  →  [지금 공부]  │
│  2위. 관사 a vs the    →  [지금 공부]   │
│  3위. 전치사 for/since →  [지금 공부]   │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  내 오답 모음                            │
│  필터: [전체] [시제] [관사] [전치사]    │
│                                         │
│  ❌ "I have visited Paris last year."   │
│  ✓  "I visited Paris last year."        │
│  → last year = 명확한 과거시점          │
│                                    2일전 │
│ ─────────────────────────────────────── │
│  ❌ "She has graduated last year."      │
│  ✓  "She graduated last year."          │
│  → 동일 패턴 반복 주의!                │
└─────────────────────────────────────────┘
```

#### 오답 분석 API

```
GET /api/analysis                    전체 오류 통계
GET /api/analysis?type=시제오류      유형별 오답 목록
GET /api/analysis/recommend          취약 챕터 추천
GET /api/analysis/monthly            월별 추이
```

---

### 6-5. 스피킹 연습 (각 탭에 연결)

#### 전체 흐름

```
질문 출제 (OPIc 빈출 주제 랜덤)
        ↓
마이크 녹음 (MediaRecorder API)
        ↓
녹음 파일 → /api/speaking/transcribe
        ↓
Whisper API → 텍스트 변환
        ↓
텍스트 → /api/speaking/correct
        ↓
Claude Haiku → 문법 교정 + 피드백
        ↓
화면에 결과 표시 + 오답 DB 저장
```

#### Whisper 변환 API

```typescript
// app/api/speaking/transcribe/route.ts

export async function POST(request: NextRequest) {
  const formData = await request.formData()
  const audioFile = formData.get('audio') as File

  const response = await openai.audio.transcriptions.create({
    file: audioFile,
    model: 'whisper-1',
    language: 'en',
  })

  return NextResponse.json({ text: response.text })
}
```

#### Claude 문법 교정 API

```typescript
// app/api/speaking/correct/route.ts

export async function POST(request: NextRequest) {
  const { text, context } = await request.json()
  // context: "opic_speaking" | "grammar_practice"

  const response = await claude.messages.create({
    model: 'claude-haiku-4-5',
    max_tokens: 1000,
    messages: [{
      role: 'user',
      content: `
        OPIc 스피킹 답변을 교정해줘. JSON으로만 응답.

        원문: "${text}"

        {
          "corrected": "교정된 전체 문장",
          "errors": [
            {
              "original": "틀린 부분",
              "fixed": "고친 부분",
              "errorType": "시제오류|관사누락|전치사|조동사|기타",
              "reason": "왜 틀렸는지 한 문장"
            }
          ],
          "betterExpressions": [
            {
              "original": "원래 표현",
              "better": "더 AL다운 표현",
              "reason": "왜 더 좋은지"
            }
          ],
          "opicLevel": "IM1|IM2|IM3|IH|AL",
          "opicFeedback": "이 답변의 OPIc 수준 평가 한 문장"
        }
      `
    }]
  })

  const result = JSON.parse(response.content[0].text)

  // 오류 DB 저장
  for (const error of result.errors) {
    await prisma.grammarError.create({
      data: {
        userId: session.userId,
        source: context,
        original: error.original,
        corrected: error.fixed,
        errorType: error.errorType,
        errorDetail: error.reason,
      }
    })
  }

  return NextResponse.json(result)
}
```

---

## 8. 관리자 화면

```
/admin
├── 사용자 관리
│   ├── 현재 등록된 유저 목록
│   ├── [ID 추가] → username + 초기 비밀번호 입력
│   └── [ID 삭제]
├── 단어 관리
│   ├── 단어 추가/수정/삭제
│   └── 씨드 데이터 상태 확인
└── 문법 콘텐츠 관리
    ├── 챕터 목록 + 생성 상태
    └── [콘텐츠 재생성] 버튼 (개별 챕터)
```

---

## 9. 환경변수

```env
# .env.local

# 데이터베이스
DATABASE_URL="postgresql://user:password@localhost:5432/opic_db"

# 세션 시크릿 (최소 32자 랜덤)
SESSION_SECRET="your-super-secret-key-minimum-32-characters"

# AI API
ANTHROPIC_API_KEY="sk-ant-..."
OPENAI_API_KEY="sk-..."          # Whisper용

# 앱
NODE_ENV="development"
NEXT_PUBLIC_APP_URL="https://your-domain.com"
```

---

## 10. 배포 구성 (Oracle Cloud + k3s)

### k3s 디플로이먼트

```yaml
# k8s/deployment.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: opic-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: opic-app
  template:
    spec:
      containers:
      - name: opic-app
        image: ghcr.io/yourname/opic-app:latest
        ports:
        - containerPort: 3000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: opic-secrets
              key: database-url
        # ... 나머지 환경변수

---
apiVersion: v1
kind: Service
metadata:
  name: opic-app-service
spec:
  selector:
    app: opic-app
  ports:
  - port: 80
    targetPort: 3000
```

### GitHub Actions CI/CD

```yaml
# .github/workflows/deploy.yml

name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Docker 이미지 빌드 + 푸시
      run: |
        docker build -t ghcr.io/${{ github.repository }}:latest .
        docker push ghcr.io/${{ github.repository }}:latest

    - name: k3s 배포
      run: |
        kubectl rollout restart deployment/opic-app
```

---

## 11. 개발 순서 및 예상 일정

> **학습 원리 순서:** 어휘(Accuracy) → 문법(Accuracy) → 인풋/섀도잉(Fluency) → 아웃풋/스피킹(Coherence) → 분석(Assessment) → 인프라 → 관리

| Phase | 작업 | 핵심 내용 | 예상 시간 |
|-------|------|-----------|-----------|
| **0** | **Core 세팅** | Next.js + Prisma 싱글톤 + 전역 인증 미들웨어 | 1~2일 |
| **1** | **단어 (Vocabulary)** | COCA+GRE+AWL 씨드 + SR 알고리즘 기반 퀴즈 4종 + TTS | 2~3일 |
| **2** | **문법 (Grammar)** | Sonnet 4.6으로 35챕터 씨딩 + 개념/예문/퀴즈 UI + AI 채점 | 3~4일 |
| **3** | **섀도잉 (Input)** | yt-dlp 자막 추출 + 구간반복 플레이어 + 블라인드 리텔링 모드 | 2~3일 |
| **4** | **스피킹 (Output)** | 마인드맵 UI + 녹음 + Whisper 타임스탬프 + 3초 침묵감지 + 필러 추천 + Claude 교정 + OPIc 등급 평가 | 3~4일 |
| **5** | **오답 분석 (Analysis)** | 오류 유형 통계 + 스피킹 등급 변화 추이 그래프 + 취약 챕터 추천 | 1~2일 |
| **6** | **인프라 (Infra)** | Oracle Cloud + k3s + GitHub Actions CI/CD | 1~2일 |
| **7** | **관리자 (Admin)** | 계정 관리 (ID 추가/삭제) + API 사용량 대시보드 | 1일 |
| **합계** | | | **약 2~3주** |

---

### Phase별 신규 기능 상세

**Phase 3 — 섀도잉: 블라인드 리텔링 모드**
```
1. 구간 반복으로 충분히 듣기
2. [리텔링 모드] 버튼 클릭 → 자막 숨김
3. 들은 내용을 내 말로 녹음
4. Whisper 변환 + Claude가 원문과 비교 피드백
→ 단순 따라읽기에서 → 내재화 단계로 진입
```

**Phase 4 — 스피킹: 3단계 구조**
```
[준비] 마인드맵 UI (15초)
  → 키워드 3개 입력: 서론 / 본론 / 결론

[녹음] 말하기
  → 3초 이상 침묵 감지 시 필러 카드 팝업
     "You know..." / "I mean..." / "Actually..."

[피드백] AI 분석
  → 문법 오류 교정
  → 침묵 횟수 (pauseCount)
  → 구조화 점수 (마인드맵 키워드 사용 여부)
  → OPIc 예상 등급 (IL ~ AL)
```

**Phase 5 — 분석 대시보드**
```
┌─ 문법 오류 통계 ─────────────────┐
│ 시제 23회 / 관사 15회 / 전치사 8회│
│ → 취약 챕터 자동 추천             │
└───────────────────────────────────┘
┌─ 스피킹 등급 변화 추이 ───────────┐
│ IM2 → IM2 → IM3 → IH → ...       │
│ (날짜별 꺾은선 그래프, Recharts)  │
└───────────────────────────────────┘
```

---

## 12. 예상 월 비용

| 항목 | 비용 | 비고 |
|------|------|------|
| Oracle Cloud 서버 | $0 | 영구 무료 |
| Whisper API (주 3회 × 20분) | ~$0.50 | 단어 타임스탬프 포함 |
| Claude Haiku 4.5 (교정 + 채점 + 등급평가) | ~$0.20 | 이전보다 호출 증가 |
| Claude Sonnet 4.6 (문법 씨딩, 최초 1회) | ~$0.50 | 일회성 |
| 도메인 | ~$0.85 | $10/년 기준 |
| **월 합계** | **약 $1.50** | 씨딩 이후엔 $0.70 수준 |

---

## 13. 잠재적 문제점 및 리스크

| 항목 | 리스크 | 해결 방법 |
|------|--------|-----------|
| yt-dlp 자막 | 자막 없는 영상 | Whisper로 자동 생성 |
| yt-dlp 법적 이슈 | 개인 사용 목적 | 비공개 서버, 개인 학습용 |
| Whisper 정확도 | 발음 나쁘면 인식 실패 | 오류 메시지 + 재녹음 안내 |
| Oracle Cloud 회수 | 무료 인스턴스 간헐적 이슈 | 주기적 백업 (pg_dump) |
| 세션 만료 | 7일 후 로그아웃 | 자동 갱신 or 기간 늘리기 |
| 단어 저작권 | COCA 유료 데이터 | 무료 버전 사용 or AWL/GRE 대체 |
| **중국어 Whisper 정확도** | **성조 인식 오류** | **language: "zh" 명시, 재녹음 안내** |
| **중국어 TTS** | **Web Speech API 중국어 지원 브라우저마다 다름** | **크롬 기준 작동, 폴백으로 외부 TTS 고려** |
| **API 키 GitHub 노출** | **Anthropic/OpenAI 계정 털림** | **아래 보안 섹션 필수 적용** |

---

## 14. GitHub 공개 레포 보안 가이드

### 핵심 원칙
```
GitHub에 절대 올라가면 안 되는 것
├── .env, .env.local          ← API 키, DB 비밀번호
├── prisma.config.ts의 하드코딩된 URL
└── SESSION_SECRET
```

### .gitignore 필수 확인
```bash
# 아래 항목이 .gitignore에 있는지 반드시 확인
.env
.env.local
.env*.local
.env*
```
Next.js 기본 `.gitignore`에 `.env*` 포함돼 있음. 첫 커밋 전 눈으로 확인 필수.

### GitHub Secrets 설정 (배포 시)
```
GitHub 레포 → Settings → Secrets and variables → Actions → New secret

등록할 값:
├── DATABASE_URL
├── SESSION_SECRET
├── ANTHROPIC_API_KEY
└── OPENAI_API_KEY
```

GitHub Actions에서 이렇게 주입:
```yaml
env:
  DATABASE_URL: ${{ secrets.DATABASE_URL }}
  ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
  OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
  SESSION_SECRET: ${{ secrets.SESSION_SECRET }}
```

### Claude Code (VS Code 확장) 사용 시 주의
```
Claude Code는 Claude Pro 구독으로 인증
→ ANTHROPIC_API_KEY 환경변수가 시스템에 설정돼 있으면
  구독 대신 API 키로 인증되어 API 비용이 별도 청구됨

확인 방법:
echo $ANTHROPIC_API_KEY

값이 나오면:
unset ANTHROPIC_API_KEY       ← 현재 터미널 세션만
~/.zshrc 에서도 해당 줄 삭제  ← 영구 제거
```

### 만약 API 키가 실수로 GitHub에 올라갔다면
```
1. 즉시 Anthropic Console에서 해당 키 삭제
   → console.anthropic.com → API Keys

2. 즉시 OpenAI에서 해당 키 삭제
   → platform.openai.com → API Keys

3. 새 키 발급 후 .env에 교체

4. git history에서 제거 (선택)
   → git filter-branch 또는 BFG Repo-Cleaner 사용
   → 이미 털렸을 가능성 있으니 키 재발급이 우선
```

### 커밋 전 체크리스트
```bash
# 매번 커밋 전 습관적으로 실행
git status          # .env 파일이 없는지 확인
git diff --staged   # 스테이징된 내용에 키값 없는지 육안 확인
```
