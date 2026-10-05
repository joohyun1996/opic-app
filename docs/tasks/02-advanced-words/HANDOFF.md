# HANDOFF: 고급 영어 단어 추가 (GRE·TOEFL·IELTS)

> 작성: Claude (구현 대행, GPT 토큰 소진) · 경로: docs/tasks/02-advanced-words/HANDOFF.md

## 커밋 범위
- base: `cf971e9`
- head: 이 커밋 (단계별 커밋 `edd6c4a`~)

## 진행 상황
| 단계 | 상태 | 비고 |
|------|------|------|
| 02-1 수집·기계 필터 | 완료 | 후보 5,180개 (`edd6c4a`) |
| 02-2 판정 | 완료 | 쉬운 단어 762개, 부적절 표현 1개 제외 → 4,417개 (`judge-01~05.tsv`) |
| 02-3 뜻·예문 작성 | 완료 | 4,417개 (`fill-001~014.tsv`) |
| 02-4 IPA 조회 | 완료 | kaikki 미국식 4,258개 + 직접 작성 159개 (`ipa-01.tsv`) |
| 02-5 출력·검증 | 완료 | `words.json` dataVersion 2 |

## 변경 요약
```diff
+ scripts/lib/advanced-filter.ts        — CSV 파서, 1차 필터, 정의 파싱, 판정·작성 TSV 파서, level 5등분
+ scripts/collect-advanced-words.ts     — collect | apply | ipa [--reset] | finalize | status
+ tests/lib/advanced-filter.test.ts     — 15개
~ scripts/export-words.ts               — 기본 입력에 words-en-advanced.json 추가 (1줄)
+ exports/source/words-en-advanced.json — 4,417개 + metadata (judged, excluded, ipaFailures)
+ exports/source/advanced-batches/      — judge-01~05, fill-001~014, ipa-01 (작업 기록)
~ exports/words.json                    — dataVersion 1 → 2
~ .gitignore                            — exports/.cache/
```

## 수용 기준 체크
| AC | 결과 | 근거 |
|----|------|------|
| AC1 | 통과 | 조건별 테스트 9개 + 통과 케이스 |
| AC2 | 통과 | `parseDefinition` 테스트 |
| AC3 | 통과 | 1차 필터에서 기존 `words`·`excluded` 제외 (테스트) |
| AC4 | 통과 | TASK 01 레코드 1,100 + 4,991개 중 변경 0개 (재생성 전후 비교) |
| AC5 | 통과 | 새 단어 seq 1101~5517 연속 |
| AC6 | 통과 | dataVersion 1 → 2 |
| AC7 | 통과 | 빈 meaningKo 0개 (`finalize`에서 검증) |
| AC8 | 통과 | level별 884/883/884/883/883 |
| AC9 | 통과 | `git check-ignore exports/.cache/ecdict.csv` |
| AC10 | 통과 | 아래 수치 |

## 수치
- 1차 필터 후: **5,180개** (굴절형 243개, 빈 정의 1개는 1차에서 제외)
- 2차 제외: 쉬운 단어(토익 800+) **762**, 부적절한 표현 **1** (`midget`)
- 최종 추가: **4,417개**
- kaikki IPA 실패: **159개** → 직접 작성 (`*` 표시 없음)
- 영어 전체: **5,517개 / Day 138개** (TASK 01: 1,100개 / Day 28)
- 중국어: 4,991개 / Day 125개 (변경 없음)
- `*` 표시: 고급 단어 0개

## 영어 고급 단어 샘플 (level별 6개)
| level | 단어 | 발음 | 뜻 |
|---|---|---|---|
| 1 | crate | /kɹeɪt/ | 나무 상자 |
| 1 | inflict | /ɪnˈflɪkt/ | (피해를) 가하다 |
| 1 | vibration | /vaɪˈbɹeɪʃən/ | 진동 |
| 1 | weary | /ˈwɪɚi/ | 지친, 싫증 난 |
| 1 | usher | /ˈʌʃəɹ/ | 안내하다; (시대를) 열다 |
| 1 | vow | /vaʊ/ | 맹세하다 |
| 2 | improvise | /ˈɪm.pɹə.vaɪz/ | 즉흥적으로 하다 |
| 2 | perverse | /pɚˈvɝs/ | 비뚤어진, 심술궂은 |
| 2 | plump | /plʌmp/ | 통통한 |
| 2 | retrospect | /ˈɹɛtɹəˌspɛkt/ | 회상 (in retrospect: 돌이켜 보면) |
| 2 | rearrange | /ˌɹiːəˈɹeɪndʒ/ | 재배열하다 |
| 2 | override | /oʊ.vɚˈɹaɪd/ | 무효화하다; 우선하다 |
| 3 | populous | /ˈpɑpjələs/ | 인구가 많은 |
| 3 | pertain | /pɚˈteɪn/ | 관련되다 |
| 3 | ascribe | /əˈskɹaɪb/ | (원인을) ~에 돌리다 |
| 3 | fiasco | /fiˈæs.koʊ/ | 대실패 |
| 3 | attrition | /əˈtɹɪʃən/ | 소모, 감소; 자연 감원 |
| 3 | abduct | /æbˈdʌkt/ | 유괴하다, 납치하다 |
| 4 | pang | /ˈpeɪ̯ŋ/ | (갑작스러운) 고통 |
| 4 | philanthropic | /ˌfɪl.ənˈθɹɑ.pɪk/ | 박애주의의, 자선의 |
| 4 | enormity | /ɪˈnoɹmɪti/ | 막대함; 극악함 |
| 4 | croon | /kɹun/ | 나지막이 노래하다 |
| 4 | rescind | /ɹɪˈsɪnd/ | 철회하다 |
| 4 | fondle | /ˈfɒndəl/ | 애무하다, 만지작거리다 |
| 5 | opus | /ˈəʊpəs/ | 작품 |
| 5 | proclivity | /pɹoʊˈklɪvɪti/ | 성향 |
| 5 | allay | /əˈleɪ/ | (걱정을) 가라앉히다 |
| 5 | schism | /ˈskɪzəm/ | 분열 |
| 5 | vex | /vɛks/ | 짜증 나게 하다 |
| 5 | vastness | /ˈvæstnəs/ | 광대함 |

## TASK와 다르게 한 것 (리뷰 필요)
1. **품사·영어 뜻을 직접 작성.** ECDICT `definition`의 첫 줄이 대표 뜻이 아닌 경우가 많았다 (`cling`, `soar`, `tremble`이 명사로 잡힘). 작성 TSV를 6열(word, 품사, 뜻, 영어 뜻, 예문, 번역)로 바꾸고 모두 직접 썼다. ECDICT는 후보 선정과 빈도(level)에만 쓴다.
2. **IPA는 미국식 우선.** kaikki의 첫 발음이 영국식(`/nɒd/`)이라 `General-American`·`US` 태그를 우선했다 (`ipa --reset`으로 전체 재조회).
3. **IPA 실패 시 ECDICT 대체 + `*` 대신 직접 작성.** ECDICT 발음 표기는 표준 IPA가 아니고, 뜻이 정확한데 `*`를 붙이면 검토 대상이 잘못 늘어난다. 실패 159개는 `ipa-01.tsv`에 미국식 IPA로 작성했다. `finalize`는 실패 단어가 남아 있으면 에러를 낸다.
4. **IPA 표기 차이.** TASK 01 단어(GPT 작성)는 `r`, 고급 단어(kaikki)는 `ɹ`를 쓴다. 표기 차이일 뿐 발음은 같다. 통일이 필요하면 앱에서 표시할 때 치환하거나 별도 TASK로 처리한다.

## 검증 결과
- `npx tsc --noEmit --incremental false`: 통과
- `npm test`: 55개 통과
- 변경 파일 eslint: 통과
- 실행: `npx vite-node scripts/collect-advanced-words.ts finalize` → `npx vite-node scripts/export-words.ts`

## 범위 밖 변경
없음

## 질문
- 없음 (중국어 Day 구성 문제는 TASK 01 HANDOFF에 있음)

## 추가 변경 (2026-10-05, 사용자 결정: 영어 우선 출시)
- `scripts/export-words.ts` 기본 입력에서 `words-zh.json` 제외 (원본은 보관)
- 배포 전이라 `exports/words.json`을 처음부터 다시 생성: 영어 5,517개 전체를 고정 시드로 한 번에 섞음, **dataVersion 1**
  - 이전 버전은 Day 1~28이 TASK 01 단어만, Day 29~가 고급 단어만 있었다. 지금은 Day마다 두 출처가 섞인다 (Day 1: TASK 01 단어 6개 + 고급 34개, level 1~5 모두 포함)
- 위 수치 중 AC4·AC5·AC6(이전 출력 대비 seq 유지·연속·버전 +1)은 이번 재생성으로 의미가 없어졌다. seq는 1~5,517 연속이다.
- 중국어 Day 규칙(쉬운 것 → 어려운 것)은 ADR 001 § 언어 범위에 기록
