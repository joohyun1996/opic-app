# HANDOFF: 웹앱 코드 제거 + 규칙 문서 개정

> 작성: GPT · 경로: docs/tasks/03-remove-web/HANDOFF.md

## 커밋 범위
- base: `65eb5f1`
- head: `9a2445a` (이 HANDOFF는 별도 문서 커밋)
- 리뷰 명령: `git diff 65eb5f1..9a2445a`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| 웹 코드·설정 64개 | 삭제 | 최종본은 `web-final` 태그에 보존 |
| `AGENTS.md` | 수정 | Kotlin·Compose·Room 데이터 및 구현 규칙으로 개정 |
| `CLAUDE.md` | 수정 | 안드로이드 리뷰 관점으로 개정 |
| `README.md` | 수정 | 앱 소개, 상태, 문서 위치 정리 |
| `.gitignore` | 수정 | Android·Gradle 무시 규칙 추가 |

## 수용 기준 체크
| AC | 결과 | 검증 명령과 출력 |
|----|------|----------------|
| AC1 | ✅ | `git ls-files | rg '^(app|components|lib|prisma|public|scripts|tests)/|^(proxy.ts|package.json|package-lock.json|tsconfig.json|next.config.ts|eslint.config.mjs|postcss.config.mjs|vitest.config.ts|prisma.config.ts|components.json)$'` → 0줄 |
| AC2 | ✅ | `git ls-files exports/words.json exports/source/words-zh.json exports/source/words-en.json exports/source/words-en-advanced.json SPEC.md docs/decisions/001-native-pivot.md` → 지정 파일 6개 출력 |
| AC3 | ✅ | `git diff 65eb5f1 9a2445a -- exports/` → 0줄 |
| AC4 | ✅ | `rg -n 'REPLACE|\(seq - 1\) / 40 \+ 1|dataVersion|language' AGENTS.md` → 27·28·30·32·33·88줄 |
| AC5 | ✅ | `rg -n 'Next.js|Prisma|proxy.ts|iron-session|middleware|Blueprint' AGENTS.md CLAUDE.md` → 0줄 |
| AC6 | ✅ | `rg -n 'local.properties|build/|\.gradle/' .gitignore` → 3:local.properties, 4:.gradle/, 5:build/ |
| AC7 | ✅ | `git log -1 --format='%h %s'` → `9a2445a chore(pivot): 웹앱 코드 제거` |
| AC8 | ✅ | `test -e .env && test -e .env.local` → 종료 코드 0 |

## 검증 결과
```text
TASK 03에는 빌드·테스트가 없다. AC1~AC8로 검증했다.
git tag -l web-final → web-final
git show web-final --stat | head → 태그 대상 620b400 확인
```

## 설계 판단 / 리뷰어가 봐야 할 곳
- `AGENTS.md:27-33` — Word만 upsert하고 기존 seq·UserWord를 보존하는 규칙.
- `CLAUDE.md:38-44` — 데이터 안전, 정확성, 보안·권한 순의 리뷰 관점.

## 범위 밖 변경
- 없음

## 질문
- 없음
