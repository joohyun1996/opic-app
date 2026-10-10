# TASK: 섀도잉 4 — 자막이 안 나오는 문제 수정

> 작성·구현: Claude · 승인: [x] 사용자 (2026-10-10, "원인부터 확인 → TASK → 보고 → 구현")
> 경로: docs/tasks/23-shadowing-captions-fix/TASK.md

## 원인 확인 (2026-10-10, S23+, DevTools Network + 화면)
| 영상 | 채널 | 플레이어 요청 `lang` | 열자마자 | 재생 25초 뒤 |
|------|------|------|------|------|
| n68k7k68iLA | Vanessa | en | 자막 없음 | 자막 75문장 |
| Ue9a29y8AsY | Easy English | en | 자막 없음 | 자막 37문장 |
| Nh-TVcNFtVI | TED | en | 자막 없음 | 자막 83문장 |
| z3Y-gsBKChc | TED-Ed | en | 자막 없음 | 자막 45문장 |
| JAUAzf8zBog | BBC | **en-GB** | 자막 없음 | **자막 없음** |

1. **플레이어는 재생을 시작해야 `/api/timedtext`를 요청한다.** 열 때 하는 직접 요청은 빈 응답(ADR·TASK 16) → 곧바로 "자막 없음"이 떠서 실패처럼 보였다. 재생하면 가로채기로 대부분 받아진다
2. **`captionRequest`가 `lang == "en"`만 받는다** (`CaptionClient.kt:27`). BBC 등 `en-GB`·`en-US` 자막은 버려진다 (추천 영상 중 BBC만 32개)
- 참고: 쿠키 없이 같은 주소로 다시 요청해도 내용이 온다 (BBC 6,888바이트) → 다시 요청하는 방식 자체는 문제 아님

## 수정
| 파일 | 내용 |
|------|------|
| `feature/shadowing/.../CaptionClient.kt` + 테스트 | `lang`이 `en` 또는 `en-*`이면 받는다 |
| `feature/shadowing/.../YouTubePlayer.kt` | 플레이어 준비되면 **소리 끄고 잠깐 재생 → 첫 재생 신호에 멈춤·0초로·소리 켜기** (자막 요청을 바로 일으킴, 한 번만) |
| `feature/shadowing/.../ShadowingViewModel.kt`, `CaptionList.kt` | 직접 요청이 비면 바로 "자막 없음" 대신 "자막 불러오는 중"으로 기다리고, 10초 안에 가로채기가 없을 때만 "자막 없음" |

## 수용 기준
- [x] AC1: `captionRequest` — `lang=en`, `en-GB`, `en-US` 통과, `ko`·`es`·`english` 거절, `kind=asr` 유지 (테스트)
- [x] AC2: 위 5개 영상을 **재생하지 않고 열기만 해도** 10초 안에 자막 목록 (전후 비교 기록)
- [x] AC3: 영상을 연 뒤 사용자가 누르기 전에는 소리가 나지 않고 0초에 멈춰 있다
- [x] AC4: `./gradlew test lint :app:assembleDebug :app:assembleRelease` 통과, 권한·의존성 변경 없음

## 결과 (2026-10-10, 수정 후 S23+, 재생하지 않고 열기만 하고 12초 뒤)
| 영상 | 분류 | 수정 전 | 수정 후 | 현재 시각 |
|------|------|------|------|------|
| n68k7k68iLA | conversation | 자막 없음 | 75문장 | 0.0초 |
| Ue9a29y8AsY | interview | 자막 없음 | 37문장 | 0.0초 |
| Nh-TVcNFtVI | ted | 자막 없음 | 83문장 | 0.0초 |
| z3Y-gsBKChc | teded | 자막 없음 | 45문장 | 0.0초 |
| JAUAzf8zBog | learner (BBC, en-GB) | 자막 없음 (재생해도) | 24문장 | 0.0초 |
| rN01ExHHRhk | conversation | — | 86문장 | 0.0초 |
| 1uDsY9i2EbI | learner | — | 26문장 | 0.0초 |
| C4TDI8z3Ej8 | learner | — | 22문장 | 0.0초 |
| AMlG-c1hXEE | ted | — | 92문장 | 0.0초 |
| zzvbUEULKtk | conversation | — | 88문장 | 0.0초 |
| 7WvW2KZm8S0 | interview | — | 114문장 | 0.0초 |
| Ntw6fx8DQUU | conversation | — | 97문장 | 0.0초 |
| -moW9jvvMr4 | ted | — | 93문장 | 0.0초 |
| M7CeTh4XpnI | conversation | — | 39문장 | 0.0초 |
| sMg4vj6ugSs | learner | — | 65문장 | 0.0초 |
- **수정 전 0/5 → 수정 후 15/15** (열기만 했을 때). AC1~AC4 충족 (테스트·lint·빌드 통과)
- 확인 중 Claude 실수: 검사 스크립트에 `--`를 넘겨 `watch?v=--`를 연 일이 있었음 (사용자가 중단) — 앱 문제 아님
