# TASK: 섀도잉 3 — 추천 영상 100개

> 작성: Claude · 승인: [x] 사용자 (2026-10-10, "추천 링크 100개, 누르면 바로 들어가게") · 구현: Claude
> 경로: docs/tasks/21-shadowing-library/TASK.md
> 번호: 원래 21이던 "스피킹 기록 저장"은 22로 미룬다

## 목표
섀도잉 화면에 학습하기 좋은 YouTube 영상 100개를 분류별로 보여 주고, 누르면 바로 연다. 링크를 따로 찾지 않아도 된다.

## 영상 고르는 기준 (2026-10-10 Claude가 실제 검색·확인)
- YouTube 검색(자막 있는 영상 필터)으로 후보 352개 → 길이 2~20분 → oEmbed로 **앱 안 재생(임베드) 허용** 확인 349개 → 채널·주제 균형으로 100개
- 채널: BBC Learning English, VOA Learning English, EnglishClass101, Bob the Canadian, Speak English With Vanessa, English with Lucy, mmmEnglish, Easy Languages·Easy British English, Rachel's English, TED·TEDx, TED-Ed 등
- 영상 ID를 기억으로 지어내지 않는다 (모두 검색 결과에서 나온 실제 ID)

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `exports/shadowing.json` | 생성 | `{dataVersion, checkedAt, videos:[{id, title, channel, category, level, minutes}]}` |
| `core/common/.../ShadowingLibrary.kt` + 테스트 | 생성 | 파싱·검증 |
| `feature/shadowing/**` | 수정 | 추천 목록(분류 칩), 누르면 열기, "다른 영상 고르기" |
| `app/build.gradle.kts`, `OpicApplication.kt`, `OpicRoot.kt` | 수정 | assets 포함·파싱·전달 |

## 분류
| category | 이름 | 기본 등급 |
|----------|------|----------|
| learner | 학습자용 (천천히·쉬운 말) | IM |
| conversation | 일상 대화·말하기 연습 | IM~IH |
| interview | 길거리 인터뷰 (실제 속도) | IH |
| pronunciation | 발음 | IM |
| teded | TED-Ed (짧은 설명) | IH |
| ted | TED 강연 | IH~AL |

## 수용 기준
- [ ] AC1: `parseShadowingLibrary` — 실제 파일 100개 통과, ID 11자·중복·분류·등급 검사 (테스트)
- [ ] AC2: 섀도잉 화면 처음에 추천 목록, 분류 칩으로 거르기, 누르면 그 영상이 열리고 링크 칸에 주소가 채워진다
- [ ] AC3: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, 권한·Room 변경 없음

## 범위 밖
- 썸네일 이미지 (이미지 라이브러리 추가 필요), 영상 추가·삭제 UI, 시청 기록
