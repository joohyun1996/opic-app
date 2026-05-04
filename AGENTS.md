<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# Agent 가이드

CLAUDE.md를 먼저 읽고 시작할 것.

## 작업 순서
1. 관련 파일 먼저 읽기
2. 구현
3. 터미널에서 동작 확인

## 금지사항
- @prisma/client 직접 import 금지
- middleware.ts 파일명 사용 금지 (proxy.ts 사용)
- 하드코딩된 API 키 금지

## 파일 수정 규칙
- 파일 생성/수정/삭제 전 반드시 사용자 허락 요청
- 허락 없이 파일을 수정하는 것은 절대 금지
- 수정할 파일 목록과 변경 내용을 먼저 설명하고 승인 후 진행

## 파일 수정 후 보고 규칙
- 수정 완료 후 변경된 부분만 diff 형식으로 보여줄 것
- 추가된 줄: + 표시
- 삭제된 줄: - 표시
- 변경 이유 한 줄 설명

## 개발 워크플로우

### 기능 완성 시 순서
1. 기능 구현
2. 테스트 코드 작성 및 실행 (`npm test`)
3. 테스트 통과 확인
4. SPEC.md 업데이트
5. git commit & 사용자에게 push 여부 물어봄 (dev 브랜치)

### 테스트 규칙
- 기능 하나 완성될 때마다 테스트 코드 작성
- 테스트 통과 전까지 다음 기능 진행 금지
- 기능 변경 시 관련 테스트 코드도 함께 수정

### SPEC.md 작성 규칙
- 새 기능 추가 시 해당 섹션에 기록
- 기능 변경 시 변경 내역 하단에 추가 (삭제 금지)
- API 변경 시 이전/이후 URL 모두 기록

## Git 커밋 규칙

### 브랜치 전략
- main: 배포용 (안정 코드만)
- dev: 개발 및 커밋 (브랜치 분기 없음)

### 커밋 메시지 형식
```
feat(기능이름): 세부설명 (#이슈번호)

* 세부 설명 1
* 세부 설명 2
```

### 커밋 타입
- feat: 새 기능
- fix: 버그 수정
- test: 테스트 추가/수정
- spec: 명세서 업데이트
- refactor: 코드 개선
- chore: 설정/기타

### 예시
```
feat(words/day-index): Day 인덱스 페이지 구현 (#6)

* 4열 그리드 Day 카드
* 완료수/40 표시
* 오답 뱃지 헤더 추가
```