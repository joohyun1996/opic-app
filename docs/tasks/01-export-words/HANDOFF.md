# HANDOFF: 단어 DB → words.json 내보내기

> 작성: GPT(구현) + Claude(마무리) · 경로: docs/tasks/01-export-words/HANDOFF.md
> **GPT 토큰 소진으로, 2026-10-05 사용자 지시에 따라 마무리 작업을 Claude가 수행했다.** 아래 "Claude 마무리 작업" 섹션이 GPT에게 넘기는 기록이다.

## 커밋 범위
- base: `5de210f`
- 이 커밋 하나에 GPT 작업분(미커밋 상태였음)과 Claude 마무리 작업이 함께 들어 있다.

## 변경 요약
```diff
+ scripts/lib/word-export.ts       — 시드 셔플, seq 유지, deleted, dataVersion (GPT)
+ scripts/export-words.ts          — source JSON → buildWordExport → exports/words.json (GPT)
+ scripts/collect-words.ts         — Google 10000 / complete-hsk-vocabulary 수집, 제외 판정 기록 (GPT)
+ tests/lib/word-export.test.ts    — AC1~AC9 + 경계 조건 11개 (GPT)
+ exports/source/words-en.json     — 영어 1,100개 + metadata.excluded 8,784개
+ exports/source/words-zh.json     — 중국어 4,991개
+ exports/words.json               — 최종 출력 (dataVersion 1, seed 20261004)
+ docs/tasks/01-export-words/claude-fill.tsv — Claude가 작성한 111개 원본 (기록용)
```

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1~AC9 | 통과 | 단위 테스트 |
| AC10 | 통과 | 이 커밋 + 이 문서 |
| AC11 | 통과 | 영어·중국어 모두 빈 `meaningKo` 0개. 영어는 사전 필드 5개 모두 채워짐 (`export-words.ts` 검증 통과) |
| AC12 | 통과 | 영어: Google 10000 전체 9,884줄 판정 후 **1,100개**. 중국어: **4,991개** (중복 0, 복수 독음 416개는 `forms[0]` 사용) |
| AC13 | 통과 | 영어 Day 1 level 분포 {1:9, 2:5, 3:12, 4:4, 5:10}. 중국어 Day 1 {1:2, 3:2, 4:2, 5:11, 6:23} |
| AC14 | 통과 | `*` 표시: 영어 2개 (`gage`, `theta`), 중국어 66개 |
| AC15 | 통과 | 한 글자 단어 0개, 제외 목록과 겹침 0개 |
| AC16 | 통과 | 지정 9개 단어 모두 사유 `쉬운 단어(토익 800+)` |
| AC17 | 통과 | 영어 level 1~5 각 220개 |

영어 제외 사유: 쉬운 단어(토익 800+) 4,037 / 표기·약어 1,119 / 고유명사 1,076 / 성인·스팸성 28 / 한 글자 24 / 단순 변화형 나머지

## 검증 결과
- `npx tsc --noEmit --incremental false`: 통과
- `npm test`: 40개 통과
- 신규 파일 `eslint`: 통과
- `npm run lint`: 기존 웹 코드 오류로 실패 (1차 리뷰에서 예외로 인정, TASK 03에서 웹 코드 삭제)
- 실행: `rm exports/words.json && npx vite-node scripts/export-words.ts` (`tsx` 없음, 로컬 `vite-node` 사용)

## Claude 마무리 작업 (GPT 참고용)
GPT가 중단한 시점: 영어 source 1,125개 중 121개가 모든 필드가 빈 상태, `exports/words.json`은 빈 배열(dataVersion 1).

1. **잡음·쉬운 단어 25개 제외** → `metadata.excluded`에 사유와 함께 추가
   - 성인·스팸성: `booty`, `virgin` / 표기: `ciao`
   - 쉬운 단어(토익 800+): `warned smallest stronger earliest licking beautifully excitement bored insects movers glasses anymore somehow differently gently cocktail fewer focuses expires implies decorating assembled`
2. **빈 레코드 111개 작성** (121개 - 제외 10개): phonetic(IPA), partOfSpeech, category(=품사), meaningKo, meaningEn, example, exampleKo
   - 원본: `claude-fill.tsv` (탭 구분 7열). Python으로 source JSON에 병합했다
   - 확실하지 않은 2개에 `*`: `gage`(gauge 변형 철자), `theta`(그리스 문자, 학습 가치 낮음)
3. **level 재계산**: 남은 1,100개를 source 배열 순서(원본 빈도 순위)대로 5등분 → 각 220개
4. **빈 `exports/words.json` 삭제 후 재생성**: 커밋된 적 없는 빈 출력이 `previous`로 들어가 dataVersion이 2가 되는 것을 막고, 첫 배포본을 dataVersion 1로 만들었다
5. `scripts/**`와 테스트 코드는 수정하지 않았다

## 영어 샘플 (level별 6개)
| level | 단어 | 뜻 |
|---|---|---|
| 1 | solo | 혼자의, 독주하는 |
| 1 | allocation | 배분 |
| 1 | jurisdiction | 관할권 |
| 1 | underlying | 근본적인 |
| 1 | integer | 정수 |
| 1 | vinyl | 비닐; 레코드판 |
| 2 | supervision | 감독 |
| 2 | tribal | 부족의 |
| 2 | sacred | 신성한 |
| 2 | silicon | 규소 |
| 2 | composer | 작곡가 |
| 2 | inclusive | 포괄적인 |
| 3 | fragrance | 향기 |
| 3 | trance | 황홀경 |
| 3 | retro | 복고풍의 |
| 3 | optics | 광학 |
| 3 | payroll | 급여 지급 명부 |
| 3 | custody | 보호 관리; 양육권 |
| 4 | ensemble | 합주단 |
| 4 | interference | 간섭; 방해 |
| 4 | tuner | 조율기 |
| 4 | systematic | 체계적인 |
| 4 | aqua | 연한 청록색의 |
| 4 | dispatch | 발송하다 |
| 5 | learners | 학습자들 |
| 5 | impose | 부과하다; 강요하다 |
| 5 | exclusion | 제외 |
| 5 | confidentiality | 기밀 유지 |
| 5 | cubic | 세제곱의; 입방의 |
| 5 | accompanying | 함께 제공되는 |

## 설계 판단 / 리뷰어가 봐야 할 곳
- **중국어 Day 1에 HSK 5~6급이 34/40개다.** HSK 6급이 전체의 절반(2,500개)이라 무작위로 섞으면 자연스럽게 이렇게 된다. 사용자는 중국어 초보이므로 "난이도 섞기"가 중국어에는 맞지 않을 수 있다 → 사용자 결정 필요
- 영어 1,100개 = Day 28개. TASK 02(고급 단어 추가)에서 보강한다

## 범위 밖 변경
없음

## 질문
- 위 중국어 Day 구성 문제
