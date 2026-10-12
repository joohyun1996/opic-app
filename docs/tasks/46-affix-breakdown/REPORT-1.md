# TASK 46 1단계: 접사 후보 추출 결과

## 입력과 기준

- 단어 입력: `exports/words.json` 전체 6,269행, 활성 알파벳 단어 6,260개. `deleted: true` 9개는 어간과 후보 양쪽에서 제외.
- 접사 입력: `exports/source/affixes/affixes.tsv` 59개. 같은 언어의 활성 단어를 어간으로 찾은 경우만 후보로 기록.
- 철자 되돌리기: 직접 결합, y→i, -e 탈락, 자음 겹침, -le→-ly. im-/ir-/il-은 각각 별도 접두사로 검사.
- 어원은 주장하지 않는다. 철자만 맞는 경우는 뜻이 연결된다고 단정하지 않는다.

## 숫자 요약

| 지표 | 개수 |
|------|-----:|
| 전체 후보 분해 | 752 |
| 후보 단어(중복 제거) | 727 |
| 여러 분해가 잡힌 단어 | 25 |
| 투명한 분해로 1차 확인(`true`) | 41 |
| 의미 미확인·애매함(`false`) | 711 |

`false`에는 뜻이 불투명한 분석과 아직 사람이 검수하지 않은 분석이 함께 있다. `review.tsv`의 판단 이유를 보고 확정해야 한다.

## 접사별 후보 분포

| 접사 | 종류 | 후보 분해 |
|------|------|----------:|
| `un-` | prefix | 12 |
| `dis-` | prefix | 14 |
| `non-` | prefix | 1 |
| `in-` | prefix | 36 |
| `im-` | prefix | 13 |
| `ir-` | prefix | 0 |
| `il-` | prefix | 1 |
| `re-` | prefix | 23 |
| `pre-` | prefix | 11 |
| `mis-` | prefix | 3 |
| `over-` | prefix | 3 |
| `under-` | prefix | 1 |
| `anti-` | prefix | 3 |
| `inter-` | prefix | 8 |
| `sub-` | prefix | 4 |
| `super-` | prefix | 2 |
| `trans-` | prefix | 2 |
| `co-` | prefix | 1 |
| `counter-` | prefix | 0 |
| `de-` | prefix | 18 |
| `fore-` | prefix | 1 |
| `post-` | prefix | 1 |
| `semi-` | prefix | 0 |
| `multi-` | prefix | 1 |
| `bi-` | prefix | 1 |
| `tri-` | prefix | 0 |
| `micro-` | prefix | 1 |
| `macro-` | prefix | 0 |
| `auto-` | prefix | 0 |
| `-able` | suffix | 7 |
| `-ible` | suffix | 3 |
| `-less` | suffix | 3 |
| `-ness` | suffix | 8 |
| `-ment` | suffix | 24 |
| `-ful` | suffix | 5 |
| `-ly` | suffix | 50 |
| `-er` | suffix | 44 |
| `-or` | suffix | 19 |
| `-ist` | suffix | 5 |
| `-ize` | suffix | 19 |
| `-ise` | suffix | 1 |
| `-ion` | suffix | 104 |
| `-tion` | suffix | 4 |
| `-ation` | suffix | 32 |
| `-ity` | suffix | 36 |
| `-ous` | suffix | 13 |
| `-ive` | suffix | 26 |
| `-al` | suffix | 48 |
| `-ship` | suffix | 1 |
| `-hood` | suffix | 1 |
| `-en` | suffix | 9 |
| `-ish` | suffix | 3 |
| `-y` | suffix | 53 |
| `-ant` | suffix | 12 |
| `-ent` | suffix | 16 |
| `-ance` | suffix | 9 |
| `-ence` | suffix | 13 |
| `-ism` | suffix | 9 |
| `-ic` | suffix | 14 |

## 검수용 목록

- [review.tsv](review.tsv): 단어·분해·어간·접사·철자 규칙·제안 뜻풀이·투명성·판단 이유를 한 줄씩 기록.
- `transparent=true`도 최종 승인 전에는 앱 데이터로 쓰지 않는다. 한 단어에 후보가 여러 개면 뜻이 가장 정확한 분석 하나를 선택하거나 전부 제외한다.
- 권장 검수: `false` 중 빈도 높은 접사와 친숙한 단어부터 뜻을 대조하고, 각 행에 true/false와 판단 이유를 확정한다. 애매하면 false를 유지한다.

## 자체 검증

- 합성 예시로 happiness→happy, usable→use, impossible→possible, beginner→begin, simply→simple을 확인했다.
- 합성 자료에서 어간이 빠진 discover는 후보가 되지 않고, `cle`이 없는 uncle도 후보가 되지 않는다.
- 실제 결과의 모든 어간·후보 단어는 같은 언어의 활성 단어 목록에 존재한다.

## 다음 단계

- 사용자 검수 방식 결정 전까지 2단계 씨드, DB, 앱 코드는 만들거나 고치지 않는다.
