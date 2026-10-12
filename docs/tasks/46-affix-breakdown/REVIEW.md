# REVIEW: TASK 46 접사 분해

> 작성: Claude · 범위: `git diff d96fc82..HEAD` (구현 `f43a6f0` + Claude 실기기 확인 기록)

## 판정: Approve (Must-fix 0)

- **데이터 안전:** v5→v6은 `affixes`·`word_parts` 생성만 한다. words·UserWord는 조회만 하고, 적재는 한 트랜잭션 안에서 `(language, wordId, position)`으로 찾아 UPDATE·INSERT한다. REPLACE는 쓰지 않는다
- **정확성:** 씨드는 `transparent = true` 426개만이고, 적재기는 미검수 행과 deleted 단어·어간을 거부한다. 모든 쿼리를 `language`로 필터링한다
- **노출 규칙:** 분해는 `checked != null`이고 현재 카드가 같을 때만 채워지고, 다음 카드로 넘어가면 비운다 (`StudyViewModel`). 실기기에서 확인 전에는 숨기고 확인 후에만 보이는 것을 확인했다
- **검증:** test·lint·release 재실행 통과. Day 6 `regenerate`로 분해 줄과 관련 단어 화면 2종(re-, generate)을 확인했다

## Nit
- `AffixImporter`가 `meta.insert` 뒤에 `meta.update`를 이어서 부른다. insert가 IGNORE라서 동작은 맞지만, 하나로 합치면 읽기 쉽다
- 관련 단어 화면은 TASK 45 이후 `deck` 구분 없이 seq 순서로 나온다. 지금은 괜찮다
