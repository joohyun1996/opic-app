# ADR 001 — Kotlin 네이티브 안드로이드 앱으로 전환

- 상태: 승인 (2026-10-04)
- 결정자: 사용자
- 작성: Claude (리뷰어)

## 배경
- 본인 + 소수 지인용 비공개 앱이라 서버 운영, 로그인, 유료 AI API(Anthropic/OpenAI) 비용이 효용 대비 크다.
- 머니로그 앱에서 기기 내 LLM 모듈(`core/llm`, MediaPipe + Gemma 3n E4B)을 이미 운영 중이다.
- 배포는 APK 직접 전달이면 충분하다.

## 결정
- Kotlin + Jetpack Compose, **안드로이드 전용**, 완전 오프라인
- 데이터: Room(SQLite), 기기 내 저장. 서버·로그인·PostgreSQL 없음
- LLM: 머니로그 `core/llm` 재사용 (MediaPipe + Gemma 3n E4B). 모델 파일은 **이 앱 전용 폴더에 별도 다운로드** (머니로그와 공유하지 않음)
- 음성 인식: 미정 (아래 "미결 사항")
- ~~새 저장소로 시작~~ → **이 저장소를 계속 사용** (2026-10-04 변경, 아래 "저장소" 참고)
- `core/llm`은 **복사로 시작**. 두 앱에서 안정화되면 별도 라이브러리 분리 검토
- `UserWord`(학습 기록)는 이전하지 않고 새로 시작

## 저장소 (2026-10-04 변경)
- 처음에는 새 저장소를 계획했으나, **이 저장소를 계속 쓰기로** 변경했다.
- 이유:
  - **이력 유지:** 요구사항, 결정, SPEC 변경 이력, 웹앱의 Day 오류 근거 커밋이 한 저장소에 이어진다.
  - **관리 단순화:** 저장소 하나, 워크플로우 문서 하나로 운영하고 문서 복사본이 갈라지지 않는다.
- 웹 코드는 `web-final` 태그(`620b400`)로 보존되므로 dev에서 삭제한다 (TASK 02).
- 남길 것: `docs/`, `SPEC.md`, `exports/words.json`, `.github/`
- 죽은 웹 코드가 남아 에이전트가 잘못 참고할 위험은, 삭제와 AGENTS.md 개정으로 없앤다.

## 이슈 관리
- GitHub 이슈는 쓰지 않는다. 작업 단위는 `docs/tasks/<순번>-<slug>/` (두 자리 순번)
- 커밋 형식: `feat(범위): 설명` (이슈 번호 없음)

## 버리는 것
- Next.js, `proxy.ts`, `app/api/**` 전체
- Prisma 7, PostgreSQL
- iron-session, 로그인, admin/member 역할, `POST /api/admin/words/seed`
- Anthropic / OpenAI Whisper API 호출
- shadcn/ui, Tailwind 웹 UI, Vitest 테스트 코드

## 재사용할 것
| 자산 | 방식 |
|------|------|
| 단어 DB | `words.json`으로 내보내 앱에 내장 (아래 "단어 데이터") |
| Day 계산 | **규칙 변경** — `seq` 기준 (아래 "Day 구성") |
| 채점 로직 | `input.trim().lowercase() == answer.lowercase()`, correctCount ≥ 3 → mastered, wrongCount > 0 → 오답 모음 |
| 와이어프레임 | `docs/design/words-ui.md` |
| SPEC.md | 도메인 규칙 유지. API 섹션은 Repository/DAO 명세로 교체 (변경 이력에 이전 API 기록) |
| AI 프롬프트 | JSON만 반환 원칙 유지, Gemma에 맞게 단순화 |
| 워크플로우 | TASK / HANDOFF / REVIEW, `docs/` 구조 |

## Day 구성 (요구사항 정정)
**원래 요구사항: "난이도를 섞어서 하루 40개".**
현재 웹앱은 `level ASC, id ASC` 정렬 후 40개씩 슬라이싱한다 (`app/api/words/route.ts:34`, `app/api/words/stats/route.ts:25`). 그래서 앞쪽 Day는 쉬운 단어만, 뒤쪽 Day는 어려운 단어만 나온다. **Phase 1부터 요구사항과 어긋나 있었다.** SPEC.md의 "Day 구조"도 이 구현을 그대로 옮겨 적은 것이다.

또 정렬 기반 계산은 단어가 추가·수정되면 Day 구성이 통째로 밀린다.

새 규칙:
- 단어마다 언어별로 **고정 순서 번호 `seq`**(1부터)를 부여한다.
- 최초 부여: 언어별 전체 단어를 **고정 시드로 섞어서** 매번 같은 결과가 나오게 한다. 시드 값은 내보내기 스크립트에 상수로 둔다.
- 새 단어: 해당 언어의 기존 최대 `seq` 뒤로 추가한다. 기존 단어의 `seq`는 절대 바꾸지 않는다.
- 단어 삭제: 행을 지우지 않고 `deleted` 표시만 한다.
- **Day 계산: `day = (seq - 1) / 40 + 1`** (정수 나눗셈). Day N = `WHERE language = ? AND seq BETWEEN (N-1)*40+1 AND N*40 AND deleted = 0 ORDER BY seq`
- deleted 단어는 해당 Day에서 숨기기만 한다. 그 Day가 39개 이하가 되어도 괜찮다. 대신 어떤 Day도 절대 밀리지 않는다.
- 전체 Day 수 = 해당 언어 `MAX(seq)`를 40으로 올림 나눗셈한 값.

## 단어 데이터 (JSON 내장 방식)
`createFromAsset`은 쓰지 않는다. 이 방식은 첫 설치 때만 복사되기 때문에, `*` 표시된 뜻을 고치는 것 같은 데이터 수정을 반영하려면 마이그레이션을 짜야 하고, 잘못하면 학습 기록까지 잃는다.

- 내보내기: 현재 저장소에 `scripts/export-words.ts`를 둔다. Prisma로 `Word`를 읽어 `words.json`을 만든다.
- 형식 (개요): `{ dataVersion: number, words: [{ language, word, seq, phonetic, meaningKo, meaningEn, example, exampleKo, level, category, partOfSpeech, collocations, deleted }] }`
- 식별 키: `(language, word)`. `word`는 소문자로 저장하며, 기존 `@@unique([language, word])`와 같다. Postgres `id`는 넘기지 않는다.
- `*` 표시(뜻 불확실)는 그대로 유지한다.
- 앱 실행 시: 저장된 dataVersion보다 JSON의 dataVersion이 크면 **Word만 upsert**(한 트랜잭션)하고, **UserWord는 건드리지 않는다**.
- **Room upsert 규칙** (UserWord는 Room이 매긴 Word의 자동 id를 참조한다):
  - `OnConflictStrategy.REPLACE` **사용 금지.** 기존 행을 지우고 다시 넣는 방식이라 id가 바뀐다. 외래키가 CASCADE면 학습 기록이 삭제되고, 아니어도 기록이 엉뚱한 단어를 가리키게 된다.
  - `(language, word)`로 기존 행을 찾아 있으면 UPDATE, 없으면 INSERT한다. Room `@Upsert`는 UPDATE 우선이라 써도 된다. 단, 매칭 기준은 `(language, word)` 유니크 인덱스여야 한다.
  - 기존 행의 `seq`는 UPDATE하지 않는다.
  - 단어 철자를 고치면 `(language, word)`가 바뀌므로 새 단어로 취급한다. 기존 단어는 `deleted = true`로 두고, 새 단어는 맨 뒤 `seq`로 추가한다. 고치기 전 단어의 학습 기록은 이어지지 않는다.
- 이후 단어 수정은 새 저장소의 `words.json`을 직접 고치고 dataVersion을 올린다. 변경 내용은 git diff로 검토한다. 내보내기 스크립트는 최초 1회용이다.

## 미결 사항
### 섀도잉 — yt-dlp 대체 필요
앱 안에서 yt-dlp를 실행하는 것은 사실상 불가능하다. Python 런타임을 넣어야 하고, YouTube가 바뀔 때마다 깨지며, 약관 문제도 있다. 후보:
- A. 기기 오디오/영상 파일 가져오기 + 공유 인텐트
- B. 섀도잉 소재를 앱에 묶어 배포
- C. YouTube 재생만 하고 구간 반복은 앱이 제어 (온라인 필요)

결정 시점: 섀도잉 Phase 직전.

### 음성 인식 — 별도 검증
E4B는 텍스트 전용으로 사용한다. 후보: whisper.cpp(JNI), sherpa-onnx(Whisper/Paraformer, 중국어), Vosk, Android 기기 내 SpeechRecognizer(API 31 이상).
측정 기준: 30초 음성 RTF, 영어·중국어 정확도, 모델 크기. 검증은 스피킹 Phase 직전에 한다 (후속 작업 6).

## 위험
- 모델 다운로드 용량이 수 GB이고, E4B 기준 RAM 약 8GB가 권장된다 → 저사양 기기에서는 사용이 제한된다.
- 작은 모델은 채점 결과가 스스로 모순되거나 JSON 출력이 불안정할 수 있다 (머니로그에서 이미 겪음) → `core/llm` 이식 단계(후속 작업 4)에서 일찍 검증한다.
- 기기 간 동기화가 없고, 앱을 삭제하면 학습 기록이 사라진다 → 내보내기/백업은 추후 검토한다.

## 후속 작업 순서 (2026-10-04 갱신)
1. TASK 01 단어 내보내기 (`exports/words.json`, `seq`·dataVersion 포함)
2. TASK 02 웹 코드 제거 + AGENTS.md / CLAUDE.md를 Kotlin/Room 기준으로 개정
3. TASK 03 안드로이드 골격 (모듈 구조, Room)
4. `core/llm` 이식 + 채점 품질·속도 검증
5. 단어 탭 이식
6. STT 검증 (스피킹 직전)

완료: `web-final` 태그 (`620b400`).
