# REVIEW: TASK 37 학습 언어 하나로 통일

> 리뷰: Claude · 범위: `git diff 4609beb..0949138` · 판정: **Approve** (Must-fix 0)

## 확인
- 데이터 안전:
  - 엔티티는 Kotlin 생성자의 기본값만 지웠고, Room 스키마 4번은 같다 (해시를 HANDOFF에 기록) → Migration이 필요 없다
  - 이제 언어를 빠뜨리면 컴파일 오류가 난다 (테스트도 `language = "en"`을 명시하도록 바뀜)
- 언어 정의 중복 해소: `core/model`의 `enum Language`를 삭제했고, `StudyLanguages` 하나만 남았다
- `WordImporter.kt`: 언어 목록을 생성자로 받는다
  - 영어 필수 필드 검사가 `requiresRichWordFields` 설정값으로 바뀌어 언어별로 열려 있다
  - 가짜 언어 `xx`를 넣는 테스트가 있다 (AC3)
- 자막 `pickTrack(tracks, language)`에 스페인어 테스트가 있다. 섀도잉 캐시 키에 언어를 포함했다 (HANDOFF)
- `WORDS_PER_DAY`는 `core/common/Day.kt` 한 곳에만 있다 (AC2). main 코드의 `"en"`은 grep 결과 0이다 (AC1)
- 범위 밖 수정(ShadowingScreen, YouTubePlayer)은 사용자가 승인했고 HANDOFF에 기록되어 있다

## Nit
1. `WordImporter`의 `(enabledLanguages + StudyLanguages.ZH)`: 중국어 호환을 위한 특례가 코드에 박혀 있다. 중국어를 정식으로 추가할 때 `all`로 옮기고 이 특례를 지우면 된다.
2. 기기의 UserWord가 0개라 실제 기록이 있는 상태로 전후를 비교하지는 못했다 (HANDOFF). 스키마가 같고 Migration·백업 테스트가 통과해서 위험은 낮다.
