# TASK: 속도 개선 — 불필요한 재구성·목록·시작 읽기

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "태스크로 잡고 지피티에 넘기자") · 구현: GPT

## 목표
코드를 조금만 고쳐 체감 속도를 올린다. 동작은 바뀌지 않는다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `app/.../OpicRoot.kt` | 수정 | `coreUnits`·`grammarLink`·통계 `titles`를 `remember(grammarResult)`로 감싼다 |
| `feature/shadowing/.../ShadowingScreen.kt` | 수정 | 추천 목록(카테고리·최근·영상 카드)을 LazyColumn으로 |
| `feature/speaking/.../SpeakingScreen.kt` | 수정 | TopicsPage 주제 목록을 LazyColumn으로 (다른 페이지는 그대로) |
| `core/database/.../WordImporter.kt` | 수정 | 파일 앞부분만 읽어 dataVersion 확인 → 같으면 전체를 읽지 않음 |
| `app/.../OpicApplication.kt` | 수정 | 위 방식에 맞게 단어 파일 열기 (InputStream 전달 등) |
| `core/database/src/test/...` | 수정 | 버전이 같으면 전체 파싱 없이 UpToDate 반환 테스트 |

## 요구사항
- `staticCompositionLocalOf`로 넘기는 `GrammarLink`는 문법 데이터가 바뀔 때만 새로 만든다
- LazyColumn으로 바꿀 때 바깥 `verticalScroll`과 중첩하지 않는다. 화면 전체를 LazyColumn 하나로 바꾸고, 머리글은 `item {}`으로 넣는다
- dataVersion 확인은 JSON 앞부분(예: 처음 4KB)에서 `"dataVersion": N`을 찾는다. 못 찾으면 지금처럼 전체를 읽는다

## 수용 기준
- [ ] AC1: 저장된 버전과 파일 버전이 같으면 `importWords`가 `UpToDate`를 반환하고, Word·UserWord 행이 그대로다 (테스트)
- [ ] AC2: 버전이 더 크면 지금과 같이 upsert된다 (기존 테스트 통과)
- [ ] AC3: 섀도잉 추천·스피킹 주제 목록이 지금과 같은 순서·내용으로 보인다 (실기기 스크린샷)
- [ ] AC4: `./gradlew test lint :app:assembleRelease` 통과

## 범위 밖
- R8 (TASK 40), 통계 쿼리 (TASK 39)
