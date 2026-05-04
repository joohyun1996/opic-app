# GitHub 이슈 전체 목록
# 아래 내용을 GitHub Issues에 하나씩 등록

==================================================
## Phase 0 — 기반 세팅 (완료)
==================================================

### #1 feat: 프로젝트 초기 세팅
- Next.js 16 + Prisma 7 + shadcn/ui 설치
- 다국어 DB 스키마 (en/zh)
- lib/ 기반 파일 (prisma, session, claude, lang)
- dev/main 브랜치 분리
상태: 완료 ✅

### #2 feat: 인증 시스템
- 로그인 페이지
- proxy.ts (미들웨어)
- /api/auth/login, /api/auth/logout
- iron-session 세션 관리
상태: 완료 ✅

==================================================
## Phase 1 — 단어 탭
==================================================

### #3 feat: 공통 레이아웃
구현 항목:
- app/page.tsx → /home 리다이렉트
- app/(main)/layout.tsx → max-width 430px 중앙 정렬
- components/navigation/Header.tsx → 뒤로가기 + 제목 + 우측 액션
- lib/tts.ts → Web Speech API TTS 헬퍼

### #4 feat: 단어 API
구현 항목:
- GET /api/words?lang=en&day=1 → 40개씩 슬라이싱
- GET /api/words/stats?lang=en → Day별 완료수/오답수
- POST /api/user-words → 정오답 업데이트, 상태 변경
- 단어는 language+id순 정렬 후 40개씩 day 계산

### #5 feat: 홈 페이지
화면: ① 홈 (언어 선택)
구현 항목:
- 영어/중국어 카드
- 각 언어별 진행률 프로그레스바
- 현재 등급 표시 (OPIc IM2 / HSK 4)
- 오늘 복습 단어 수 표시

### #6 feat: Day 인덱스 페이지
화면: ② Day 인덱스
구현 항목:
- 4열 그리드 Day 카드
- 완료수/40 표시
- 완료(검정) / 진행중(테두리) / 미시작(회색) 상태
- 헤더 오답 뱃지 (빨간색)
- 총 단어수/일수 표시

### #7 feat: Day 단어 목록 페이지
화면: ③ Day 단어 목록
구현 항목:
- 상단 진행 프로그레스바
- 단어 + 발음기호 + ♪버튼 + 상태 뱃지 리스트
- ♪ 클릭 시 Web Speech API TTS 실행
- 하단 학습 시작 버튼

### #8 feat: 플래시카드 학습
화면: ④⑤ 플래시카드 (영→한, 한→영)
구현 항목:
- 영→한: 영어단어+♪ 보여주고 → 한국어 뜻 타이핑
- 한→영: 한국어 뜻 보여주고 → 영어 단어 타이핑
- 힌트: 첫글자_마지막글자 마스킹 (co_ _ _ _ _ _t)
- 정답 판정: 소문자 변환 후 완전 일치
- 정답: 초록 테두리 + ✓ + 예문 표시
- 오답: 빨간 테두리 + ✗ + 정답 표시
- 정답/오답 즉시 /api/user-words POST
- 진행 프로그레스바
- 완료 후 결과 요약 화면

### #9 feat: 오답 모음 페이지
화면: ⑥ 오답 모음
구현 항목:
- 전체/Day별 필터 탭
- 빨간 왼쪽 border 카드
- 단어 + ♪버튼 + 뜻 + 틀린 횟수
- 오답만으로 플래시카드 학습 시작

### #10 feat: 단어 씨드 생성 (admin)
구현 항목:
- POST /api/admin/words/seed
- role === 'admin' 만 접근 가능
- Claude Sonnet으로 언어/레벨별 단어 배치 생성
- 영어: COCA+GRE+AWL 기반, 레벨 1~5 혼합, 40개씩
- 중국어: HSK 1~6 기반, 40개씩
- DB upsert (중복 방지)
- 단어 소문자 저장

==================================================
## Phase 2 — 문법 탭
==================================================

### #11 feat: 문법 챕터 목록 페이지
구현 항목:
- 카테고리별 챕터 목록
- 완료/학습중/미시작 상태 표시
- 언어 전환 (EN/ZH) 버튼
- GET /api/grammar?lang=en

### #12 feat: 문법 챕터 상세 페이지
구현 항목:
- 1단계: 핵심 개념 설명 (coreConceptKo)
- 2단계: 구조 테이블
- 3단계: 좋은예/나쁜예 슬라이드
- OPIc/HSK 팁 표시
- 다음 단계 버튼

### #13 feat: 문법 퀴즈
구현 항목:
- fill_blank: 빈칸 채우기
- error_correction: 오류 수정
- sentence_creation: 자유 작문 (Claude Haiku 채점)
- 오답 시 GrammarError DB 저장
- POST /api/grammar/[chapter]/quiz

### #14 feat: 문법 씨드 생성 (admin)
구현 항목:
- scripts/seed-grammar-en.ts
- scripts/seed-grammar-zh.ts
- Claude Sonnet으로 35챕터 생성
- coreConceptKo, structureTable, goodExamples, badExamples, examTip, quizBank

==================================================
## Phase 3 — 섀도잉 탭
==================================================

### #15 feat: 섀도잉 목록 페이지
구현 항목:
- 영상 목록 (썸네일/제목/길이/상태)
- URL 입력 모달
- GET /api/shadowing?lang=en

### #16 feat: yt-dlp 자막 추출
구현 항목:
- lib/ytdlp.ts → execFile 방식
- URL 검증 (Command Injection 방어)
- .vtt 파싱 → JSON [{start, end, text}]
- POST /api/shadowing/extract

### #17 feat: 섀도잉 플레이어
구현 항목:
- YouTube IFrame 플레이어
- 자막 싱크 하이라이트
- 구간 반복 (현재 자막 구간)
- 재생속도 조절 (0.75x / 1x / 1.25x)
- ±5초 이동 버튼
- 자막 번역 토글

### #18 feat: 섀도잉 녹음 및 피드백
구현 항목:
- MediaRecorder로 녹음
- POST /api/speaking/transcribe → Whisper
- 내 발음 vs 원문 비교 표시
- 틀린 단어 하이라이트

### #19 feat: 리텔링 모드
구현 항목:
- 자막 숨김 토글
- 녹음 후 Claude로 원문 비교 피드백

==================================================
## Phase 4 — 스피킹 탭
==================================================

### #20 feat: 스피킹 질문 및 녹음
구현 항목:
- OPIc/HSK 랜덤 질문 출제
- 키워드 메모 (서론/본론/결론)
- MediaRecorder 녹음
- 3초 침묵 감지 → 필러 카드 팝업
- POST /api/speaking/transcribe

### #21 feat: 스피킹 AI 피드백
구현 항목:
- POST /api/speaking/correct → Claude Haiku
- 교정된 답변 표시 (오류 하이라이트)
- 오류 분석 (errorType별)
- 더 나은 표현 제안
- OPIc/HSK 예상 등급
- GrammarError DB 저장

==================================================
## Phase 5 — 오답 분석 탭
==================================================

### #22 feat: 오답 분석 대시보드
구현 항목:
- 오류 유형 통계 (프로그레스바)
- 취약 챕터 추천
- OPIc/HSK 등급 추이 그래프 (Recharts)
- 월별 필터
- GET /api/analysis?lang=en

### #23 feat: 오답 모음 (문법)
구현 항목:
- 문법 오답 목록
- 오류 유형 필터
- 오답 → 해당 챕터 바로가기

==================================================
## Phase 6 — 배포
==================================================

### #24 feat: Docker 이미지 빌드
구현 항목:
- Dockerfile (멀티스테이지)
- start.sh (migrate deploy → node server.js)
- next.config.ts output: standalone

### #25 feat: GitHub Actions CI/CD
구현 항목:
- .github/workflows/deploy.yml
- docker build + push (ghcr.io)
- SSH로 서버 rollout restart
- GitHub Secrets 환경변수 주입

### #26 feat: Oracle Cloud 서버 세팅
구현 항목:
- PM2로 앱 실행
- Nginx 리버스 프록시
- Cloudflare DNS 연결
- PostgreSQL 설치 및 마이그레이션

==================================================
## Phase 7 — 관리자
==================================================

### #27 feat: 관리자 페이지
구현 항목:
- /admin → 사용자 목록
- 사용자 추가 (username + 초기 비밀번호)
- 사용자 삭제
- POST/DELETE /api/admin/users

### #28 feat: API 사용량 대시보드 (admin)
구현 항목:
- Anthropic API 호출 횟수/비용 추적
- OpenAI Whisper 호출 횟수/비용 추적
- 월별 집계
