# OPIc · HSK 학습 앱 기능 명세서

> 마지막 업데이트: 2026-05-04

## 변경 이력
| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2026-05-04 | v0.1.0 | 초기 세팅 (인증, 스키마) |
| 2026-05-04 | v0.2.0 | Phase 1 단어 탭 구현 |

## 인증
### POST /api/auth/login
- 기능: 로그인
- 요청: { username, password }
- 응답: { ok: true }
- 세션: userId, username, role 저장

### POST /api/auth/logout
- 기능: 로그아웃
- 세션 삭제

## 단어 (Phase 1)

### 화면 구조
| 화면 | 경로 | 설명 |
|------|------|------|
| ① 홈 | /home | 언어 선택 카드 + 전체 진행률 |
| ② Day 인덱스 | /[lang]/words | 4열 Day 그리드 + 오답 뱃지 |
| ③ Day 단어 목록 | /[lang]/words/[day] | 단어 리스트 + 학습 시작 버튼 |
| ④ 플래시카드 영→한 | /[lang]/words/[day]/study?mode=en-ko | 영어→한국어 타이핑 |
| ⑤ 플래시카드 한→영 | /[lang]/words/[day]/study?mode=ko-en | 한국어→영어 타이핑 |
| ⑥ 오답 모음 | /[lang]/words/wrong | Day 필터 + 오답 카드 리스트 |

### GET /api/words
- 기능: Day 단위 단어 목록 조회
- 요청: ?lang=en|zh&day=1
- 응답: { words: Word[], totalDays: number }
- 규칙: level ASC, id ASC 정렬 후 40개씩 슬라이싱

### GET /api/words/stats
- 기능: Day별 진행 통계
- 요청: ?lang=en|zh
- 응답: { totalDays, days: [{ day, total, mastered, learning, wrong }] }
- wrong = wrongCount > 0 인 UserWord 수

### POST /api/user-words
- 기능: 단어 학습 결과 기록
- 요청: { wordId: number, result: 'correct' | 'wrong' }
- 응답: { userWord }
- 규칙: correctCount >= 3 → mastered, 그 외 → learning

### POST /api/admin/words/seed (admin 전용)
- 기능: Claude Sonnet 4.6으로 단어 생성 후 DB upsert
- 요청: { lang, level, category, count? }
- 응답: { created, skipped, words }

### 플래시카드 규칙
- 정답 판정: `input.trim().toLowerCase() === answer.toLowerCase()`
- 영→한: 영어 단어 + 발음기호 + ♪ → 한국어 뜻 타이핑
- 한→영: 한국어 뜻 + 품사 + 힌트(c _ _ _ _ _ t) → 영어 단어 타이핑
- 정답: 초록 테두리 + ✓, 오답: 빨간 테두리 + ✗ + 정답 표시

### 단어 상태
| 상태 | 조건 | 배지 배경색 |
|------|------|------------|
| new | 미학습 | #f1efe8 |
| learning | correctCount < 3 | #faeeda |
| mastered | correctCount >= 3 | #eaf3de |

### Day 구조
- Day N = 언어별 단어를 level/id ASC 정렬 후 skip (N-1)*40, take 40

## 문법
(기능 추가 시 여기에 작성)

## 섀도잉
(기능 추가 시 여기에 작성)

## 스피킹
(기능 추가 시 여기에 작성)

## 분석
(기능 추가 시 여기에 작성)
