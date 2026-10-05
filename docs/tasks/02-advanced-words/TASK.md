# TASK: 고급 영어 단어 추가 (GRE·TOEFL·IELTS)

> 작성: Claude · 승인: [ ] 사용자
> 경로: docs/tasks/02-advanced-words/TASK.md
> 선행: TASK 01 완료 (커밋된 `exports/words.json`)
> 근거: `docs/decisions/001-native-pivot.md` § 단어 데이터, `Opic-App-Blueprint.md:501` (원래 설계: COCA + GRE + AWL)

## 목표
TASK 01에서 쉬운 단어를 빼고 나니 영어가 약 1,100개만 남았다. 원래 설계에 있었지만 구현되지 않은 GRE 계열 고급 어휘를 추가해서, 토익 820~920점 사용자에게 맞는 단어를 늘린다. 기존 단어의 `seq`는 바꾸지 않고, 새 단어는 맨 뒤에 붙인다.

## 출처 (2026-10-05 확인)
- **ECDICT** (`skywind3000/ECDICT`, MIT 라이선스, 별 8.3k)
  - `https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv` (약 66MB, 행마다 단어 하나)
  - 사용할 열: `word`, `tag`, `oxford`, `collins`, `frq`, `definition`, `exchange`
  - `tag`에 시험 분류가 들어 있다: `gre` 7,504개, `toefl` 6,974개, `ielts` 5,040개
- **AWL은 쓰지 않는다.** Coxhead AWL 570개는 대부분 토익 800점 이상이면 아는 학술 기초어이고, 라이선스가 확실한 공개 사본도 찾지 못했다. IELTS·TOEFL 태그가 학술 어휘를 대신한다.
- **발음기호:** kaikki.org (Wiktionary 데이터) `https://kaikki.org/dictionary/English/meaning/{w[0]}/{w[0:2]}/{w}.jsonl`의 `sounds[].ipa` 중 첫 번째 값. 실패하면 ECDICT `phonetic`을 `/…/`로 감싸서 쓰고, 그 단어의 뜻 앞에 `*`를 붙인다.
  - 2026-10-05 확인: `add`, `infrastructure` 모두 200 응답, 2초 이내

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `scripts/collect-advanced-words.ts` | 생성 | ECDICT 다운로드·필터 → kaikki 조회 → `exports/source/words-en-advanced.json` |
| `scripts/lib/advanced-filter.ts` | 생성 | 순수 함수: 후보 필터, 품사·뜻 파싱 |
| `tests/lib/advanced-filter.test.ts` | 생성 | 필터·파싱 단위 테스트 |
| `scripts/export-words.ts` | 수정 | source 입력에 `words-en-advanced.json` 추가 |
| `exports/source/words-en-advanced.json` | 생성 | 수집 원본 (커밋 대상) |
| `exports/words.json` | 수정 | 재생성 |
| `.gitignore` | 수정 | `exports/.cache/` 추가 (ECDICT 원본 66MB는 커밋 금지) |

## 관련 파일 (읽기만)
- `scripts/lib/word-export.ts` — `buildWordExport` (**수정 금지**, seq 추가 규칙을 그대로 쓴다)
- `exports/source/words-en.json` — 기존 영어 단어와 `metadata.excluded` (중복 판정용)

## 요구사항
### 1차 기계 필터 (`advanced-filter.ts`)
아래 조건을 **모두** 만족하는 ECDICT 행만 후보로 삼는다.
- `word`가 영문 소문자로만 이루어짐 (공백, 하이픈, 대문자, 숫자 제외)
- `tag`에 `gre`, `toefl`, `ielts` 중 하나 이상 포함
- `oxford != 1` (Oxford 3000 기초어 제외)
- `collins <= 3` (Collins 별 4~5개는 고빈도 기초어)
- `0 < frq <= 20000` (빈도 정보가 없거나 너무 드문 단어 제외. GRE 전용 희귀어가 OPIc 말하기에 쓸모없기 때문)
- 기존 `words-en.json`의 `words`나 `metadata.excluded`에 이미 있는 단어 제외
- 2026-10-05 측정 기준 약 **5,400개**가 남는다

### 2차 GPT 판정 (TASK 01 기준 그대로)
- 고유명사, 성인·스팸성 단어, 규칙 굴절형(원형이 후보나 기존 목록에 있을 때) 제외
- **토익 800점 이상이면 확실히 아는 단어 제외** (예: `nod`, `athlete`). 애매하면 포함
- 제외 사유는 `words-en-advanced.json`의 `metadata.excluded`에 기록

### 필드
- `phonetic`: 위 발음기호 규칙
- `partOfSpeech`: ECDICT `definition` 첫 줄의 접두어(`n.`, `v.`, `adj.`, `adv.` 등)를 `noun` / `verb` / `adjective` / `adverb`로 변환. 기존 영어 데이터와 같은 표기를 쓴다
- `meaningEn`: `definition` 첫 줄에서 접두어를 뗀 값
- `meaningKo`, `example`, `exampleKo`: GPT가 작성한다 (TASK 01과 같은 규칙, `*` 포함)
- `category` = `partOfSpeech`
- `level`: 남은 고급 단어를 `frq` 오름차순으로 5등분한 구간 번호. 기존 단어 level은 바꾸지 않는다
- `collocations`: `[]` (이번 범위 아님)

### seq
- `export-words.ts`가 기존 영어 source와 고급 source를 합쳐 `buildWordExport`에 넘긴다. 이전 `exports/words.json`을 `previous`로 넘겨서, 기존 단어 seq는 유지되고 고급 단어는 영어 최대 seq 뒤에 시드 셔플로 붙게 한다.
- 따라서 기존 Day는 하나도 바뀌지 않고, 새 Day가 뒤에 생긴다. `dataVersion`은 1 오른다.

### 네트워크
- kaikki 조회: 동시 요청 2개 이하, 요청 간격 200ms 이상, 타임아웃 8초, 재시도 2회
- 이미 조회한 단어는 건너뛴다 (재실행 시 이어서 진행)
- ECDICT CSV는 `exports/.cache/`에 한 번만 받는다

## 수용 기준
- [ ] AC1: 1차 필터는 위 조건 중 하나라도 어긋나는 행을 제외한다 (조건별 테스트)
- [ ] AC2: `definition` 첫 줄 `"v. make less severe"`는 `partOfSpeech: "verb"`, `meaningEn: "make less severe"`가 된다
- [ ] AC3: 고급 단어는 기존 `words-en.json`의 `words`·`excluded`와 겹치는 단어가 0개다
- [ ] AC4: 재생성한 `words.json`에서 TASK 01 단어의 seq와 내용이 이전 커밋과 같다 (`git diff`에 기존 레코드 변경 없음)
- [ ] AC5: 고급 단어 seq는 영어 기존 최대 seq + 1부터 빈 칸 없이 연속이다
- [ ] AC6: `dataVersion`이 TASK 01 출력보다 정확히 1 크다
- [ ] AC7: 고급 단어 중 `meaningKo`가 빈 레코드가 0개다
- [ ] AC8: 고급 단어 level 1~5의 개수 차이가 1 이하다
- [ ] AC9: `exports/.cache/`가 git에 추적되지 않는다
- [ ] AC10: HANDOFF에 1차 필터 후 수, 2차 제외 사유별 개수, 최종 추가 수, kaikki 실패 수, 영어 전체 Day 수, level별 6개씩 샘플 30개를 적는다

## 제약 / 주의
- `scripts/lib/word-export.ts`는 수정하지 않는다
- Anthropic/OpenAI API 호출 금지
- 패키지 설치 금지. CSV는 Node 기본 기능으로 파싱한다 (따옴표 안의 쉼표와 `\n` 처리 주의)
- 중국어 데이터는 건드리지 않는다

## 범위 밖 (하지 말 것)
- 기존 TASK 01 영어 단어의 재판정이나 수정
- collocations 작성
- 웹 코드 수정이나 삭제 (TASK 03)
