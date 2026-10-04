# 단어 탭 화면 설계

> CLAUDE.md에서 이동 (2026-09-26). 단어 탭 UI 작업 시에만 읽는다.

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


