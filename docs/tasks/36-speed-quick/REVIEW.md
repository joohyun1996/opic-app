# REVIEW: TASK 36 속도 개선

> 리뷰: Claude · 범위: `git diff 274eaeb..f9f85a4` · 판정: **Approve** (Must-fix 0)

## 확인
- 앱 전체 재구성 문제: `OpicRoot.kt:133-134`의 `coreUnits`와 `grammarLink`를 `remember(grammarResult)`로 감쌌다. 이제 `LocalGrammarLink`는 문법 데이터가 바뀔 때만 바뀐다. 통계 화면의 `titles`도 같은 방식이다
- 단어 파일 읽기: `WordImporter.kt:17-32`는 앞 4KB만 읽어 버전을 확인한다
  - 버퍼 기본 크기 8KB ≥ 4KB라서 mark/reset이 안전하다
  - 버전이 같으면 전체를 읽지 않고, 못 찾거나 버전이 더 크면 기존 전체 파싱 경로로 간다
  - UserWord를 건드리지 않는다 (테스트 `sameVersionStreamReadsOnlyPrefixAndKeepsProgress`)
- 목록: 섀도잉과 스피킹 첫 화면을 LazyColumn 하나로 바꿨고 스크롤이 중첩되지 않는다
  - 섀도잉 key는 `recent:`/`library:` 접두사를 붙여, 같은 영상이 두 목록에 있어도 key가 겹치지 않는다
  - 스피킹 key는 topic.id(고유)다
  - 실기기 스크린샷 2장 있음

## Nit
1. `WordImporter.importWords(raw: String)`의 256자 버전 확인과 새 스트림 경로의 4KB 확인이 중복이다. 지금은 테스트용으로 String 경로를 남겨도 된다.
