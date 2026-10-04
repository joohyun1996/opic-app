# TASK: 단어 DB → words.json 내보내기

> 작성: Claude · 승인: [x] 사용자 (2026-10-04)
> 경로: docs/tasks/01-export-words/TASK.md
> 근거: `docs/decisions/001-native-pivot.md` § Day 구성, § 단어 데이터

## 목표
PostgreSQL의 `Word` 테이블을 네이티브 앱에 내장할 `words.json`으로 내보낸다. 단어마다 고정 `seq`를 부여하고, 파일에 `dataVersion`을 포함한다. 웹 코드 제거(TASK 02) 전에 이 저장소의 Prisma 환경에서 실행하는 마지막 Node 작업이다.

## 수정 범위
> GPT는 아래 파일만 허락 없이 생성/수정할 수 있다. 그 외는 사용자 허락 필요.

| 파일 | 작업 | 내용 |
|------|------|------|
| `scripts/lib/word-export.ts` | 생성 | 순수 함수: 시드 셔플, seq 부여, 레코드 변환 (DB 접근 없음) |
| `scripts/export-words.ts` | 생성 | Prisma로 Word 조회 → 순수 함수 호출 → JSON 파일 쓰기 |
| `tests/lib/word-export.test.ts` | 생성 | 순수 함수 단위 테스트 |
| `exports/words.json` | 생성 | 실행 결과물 (커밋 대상) |
| `scripts/collect-words.ts` | 생성 | (추가 2026-10-04) 원본 출처 조회 → `exports/source/words-{en,zh}.json` |
| `exports/source/words-en.json`, `exports/source/words-zh.json` | 생성 | (추가) 수집 원본, 커밋 대상 |

## 관련 파일 (읽기만)
- `prisma/schema.prisma` § `model Word` — 필드 목록
- `lib/prisma.ts` — 싱글톤 사용법
- `scripts/seed-words-en.ts` — 기존 스크립트 실행 방식(dotenv, tsx 등) 참고

## 참고 문서
- `docs/decisions/001-native-pivot.md` § Day 구성, § 단어 데이터

## 요구사항
### 출력 형식 (`exports/words.json`)
```
{ "dataVersion": 1, "seed": <number>, "exportedAt": "<ISO>",
  "words": [ { language, word, seq, phonetic, meaningKo, meaningEn,
               example, exampleKo, level, category, partOfSpeech,
               collocations, deleted } ] }
```
- Postgres `id`, `createdAt`는 포함하지 않는다.
- nullable 필드는 `null`로 쓴다(키 생략 금지). `collocations`는 항상 배열이다.
- `words`는 `language` 다음 `seq` 순으로 정렬해서 쓴다 (diff 안정성). JSON은 2칸 들여쓰기.

### seq 부여 규칙
- 언어별로 따로 1부터 연속 번호를 매긴다.
- 셔플 전에 입력을 `(language, word)` 기준으로 정렬한다. 그래야 DB 조회 순서와 상관없이 결과가 같다.
- 셔플: 고정 시드 PRNG(예: mulberry32) + Fisher–Yates. 시드는 `scripts/lib/word-export.ts`의 상수로 둔다. **새 패키지 설치 금지.**
- **기존 `exports/words.json`이 있으면** 기존 단어의 `seq`를 그대로 유지한다.
  - 새 단어는 해당 언어의 기존 최대 seq 뒤에 붙인다. 새 단어끼리는 같은 시드로 셔플한다.
  - DB에서 사라진 단어는 `deleted: true`로 남긴다.
  - 내용이 바뀌었으면 `dataVersion`을 1 올리고, 바뀐 게 없으면 그대로 둔다.

### 변환 규칙
- `word`는 `trim().toLowerCase()`
- 뜻 앞의 `*` 표시는 그대로 유지한다 (가공 금지).
- 정규화 후 `(language, word)`가 중복되면 스크립트를 실패 처리하고 중복 목록을 출력한다.

### 실행
- `npx tsx scripts/export-words.ts` (기존 seed 스크립트와 같은 방식이면 그것을 따른다)
- 완료 시 언어별 단어 수, 최대 seq, Day 수(`ceil(maxSeq/40)`), dataVersion을 출력한다.

## 추가 요구사항 (2026-10-04, 1차 리뷰 M1 대응)
DB가 비어 있다. 기존 씨드 스크립트는 한 번도 실행된 적이 없다. **DB와 유료 API를 거치지 않고**, GPT가 씨드 스크립트의 원래 출처에서 직접 조회해 단어 데이터를 만든다.

### 출처 (기존 씨드와 동일)
- 영어: `scripts/seed-words-en.ts:11-14`의 Google 10000 목록 상위 5,000개
  - level = `min(5, floor(index/1000)+1)` (`:70`과 동일)
  - phonetic, partOfSpeech, meaningEn, example은 Free Dictionary API로 조회
  - category = partOfSpeech, 없으면 `general`
- 중국어: ~~`scripts/seed-words-zh.ts:13`의 gigamorph/hsk-vocabulary~~ (저장소가 사라짐, 404) → **`drkameleon/complete-hsk-vocabulary`** (MIT)
  - URL: `https://raw.githubusercontent.com/drkameleon/complete-hsk-vocabulary/main/wordlists/exclusive/old/{1..6}.json`
  - 2026-10-04 확인 기준 급수별 150/147/298/598/1298/2500, 합계 4,991개
  - 매핑:
    - `word` = `simplified`, `phonetic` = `forms[0].transcriptions.pinyin`
    - `meaningEn` = `forms[0].meanings`를 `"; "`로 연결, `partOfSpeech` = `pos[0]` (없으면 null)
    - `level` = 파일 번호, `category` = `HSK<n>`
  - 여러 독음(`forms`가 2개 이상)인 단어는 `forms[0]`만 사용한다. 개수를 HANDOFF에 적는다
  - 중국어 예문은 출처에 없으므로 `example` / `exampleKo`는 null

### 영어 수집 방식 변경 (2026-10-04, 사용자 승인)
- ~~Free Dictionary API 조회~~ → **사용하지 않는다.** 기본 단어(`add`, `all`, `be`, `city` 등)가 매번 522 타임아웃으로 실패해 쓸 수 없다. 위 "출처"의 Free Dictionary API 항목과 이전 "사전 API 실패 처리" 규칙은 폐기한다.
- 영어 `phonetic`(IPA), `partOfSpeech`, `meaningEn`, `example`도 GPT가 직접 작성한다. 확실하지 않은 값은 `*` 규칙을 그대로 따른다.
- `category` = `partOfSpeech` (기존과 동일)
- **학습 가치 없는 항목 제외.** Google 10000 목록을 순서대로 읽으면서 아래 항목은 건너뛰고, 다음 순위 단어로 채워 5,000개를 만든다.
  - 한 글자 단어 (`a`, `i`는 유지)
  - 약어, 웹·기술 용어 (`www`, `com`, `pdf`, `html`, `inc`, `usa`, `dvd`, `mp3` 등)
  - 고유명사 (지명, 회사명, 인명)
  - 같은 단어의 단순 굴절형(복수형, 3인칭 단수형 등)은 원형이 목록에 이미 있으면 제외한다 (예: `books` → `book`이 있으면 제외)
- `level`은 제외한 뒤의 순위로 계산한다: `min(5, floor(index/1000)+1)`
- 제외한 단어와 사유는 `exports/source/words-en.json`의 `metadata.excluded`에 기록하고, 개수를 HANDOFF에 적는다.
- 이미 수집한 영어 200개도 이 방식으로 다시 작성한다. 중국어는 그대로 둔다.

### 한국어 뜻 (`meaningKo`)·예문 번역 (`exampleKo`)
- **Anthropic/OpenAI API 호출 금지.** GPT(구현 에이전트)가 직접 작성한다.
- 뜻이 확실하지 않으면 값 앞에 `*`를 붙인다 (AGENTS.md 규칙). 사전 조회에 실패한 단어도 `*`를 붙인다.
- 분량이 많으므로 배치로 나눠 작성해도 된다. 중간 결과는 같은 source 파일에 이어서 쓴다.

### 흐름
1. `scripts/collect-words.ts`: 출처 조회 → source JSON에 저장 (`meaningKo`는 비워 둠)
2. GPT가 source JSON의 `meaningKo` / `exampleKo`를 채운다
3. `scripts/export-words.ts`에 source JSON 입력 경로를 추가한다. Prisma 조회 대신 source JSON을 읽어 `buildWordExport`에 넘긴다. `buildWordExport`는 수정하지 않는다.

### 추가 수용 기준
- [ ] AC11: source JSON의 모든 레코드는 `WordInput` 필드를 전부 갖고, `meaningKo`가 빈 문자열인 레코드는 0개다
- [ ] AC12: 영어 5,000개(제외 항목을 뺀 뒤), 중국어는 HSK 1~6 원본 합계(4,991)에서 정규화 중복을 뺀 수와 같다. HANDOFF에 수치를 적는다
- [ ] AC13: `exports/words.json`에서 언어별 Day 1에 2개 이상의 level이 섞여 있다 (실제 데이터로 AC3 확인)
- [ ] AC15: 영어 source에 한 글자 단어(`a`, `i` 제외)와 `metadata.excluded`에 있는 단어가 0개다 (테스트 또는 검증 스크립트로 확인)
- [ ] AC14: `*` 표시된 단어 수를 언어별로 HANDOFF에 적는다

## 수용 기준
- [ ] AC1: 같은 입력과 같은 시드면, 입력 순서를 바꿔도 seq 결과가 같다
- [ ] AC2: 언어별 seq가 1부터 빈 칸 없이 연속이다 (최초 내보내기 기준)
- [ ] AC3: 셔플 결과, 앞 40개(Day 1)에 2개 이상의 level이 섞여 있다. 테스트는 level 1~3 단어 각 40개짜리 픽스처로 확인한다
- [ ] AC4: 기존 데이터가 주어지면 기존 단어의 seq는 바뀌지 않고, 새 단어는 해당 언어의 `maxSeq+1`부터 붙는다
- [ ] AC5: 기존에 있던 단어가 입력에 없으면 `deleted: true`로 남고 seq는 유지된다
- [ ] AC6: 변경이 있으면 dataVersion이 1 오르고, 변경이 없으면 그대로다
- [ ] AC7: `word`는 소문자·trim 처리되고, `*`로 시작하는 뜻은 그대로 보존된다
- [ ] AC8: 정규화 후 `(language, word)`가 중복되면 에러를 던진다
- [ ] AC9: 출력 레코드에 `id`, `createdAt` 키가 없고, nullable 필드는 `null`로 존재한다
- [ ] AC10: 실제 DB로 실행한 `exports/words.json`이 커밋되어 있고, HANDOFF에 언어별 단어 수, Day 수, dataVersion이 기록되어 있다

## 제약 / 주의
- Prisma import는 `app/generated/prisma`, 클라이언트는 `lib/prisma.ts` 싱글톤만 쓴다.
- DB는 읽기만 한다. 쓰기, 스키마 변경, 마이그레이션 금지.
- 테스트는 `scripts/lib/word-export.ts`의 순수 함수만 대상으로 한다 (DB 모킹 불필요).
- UserWord는 내보내지 않는다 (ADR 001: 학습 기록은 새로 시작).

## 범위 밖 (하지 말 것)
- 안드로이드 프로젝트, Room 엔티티 작성
- 웹앱 코드(`app/**`, `components/**`) 수정. 기존 Day 로직도 고치지 않는다 (웹은 보관 상태)
- 뜻이 불확실한(`*`) 단어의 내용 수정
- 패키지 설치
