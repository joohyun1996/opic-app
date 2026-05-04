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