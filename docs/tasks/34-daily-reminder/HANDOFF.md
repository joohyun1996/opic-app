# HANDOFF: 하루 한 번 학습 알림

> 작성: GPT · 경로: docs/tasks/34-daily-reminder/HANDOFF.md

## 커밋 범위
- base: `6c8742d`
- 구현 커밋: `5c2148e`
- 리뷰 명령: `git diff 6c8742d..HEAD`

## 변경 요약
| 파일 | 작업 | 한 줄 설명 |
|------|------|-----------|
| `core/common/.../Reminder.kt`와 테스트 | 생성 | 다음 시각과 학습 여부별 문구를 순수 함수로 계산 |
| `app/.../Reminder.kt` | 생성 | 설정 저장, 하루 1회 예약, 학습 기록 확인과 알림 표시 |
| `app/.../MenuScreen.kt`, `OpicRoot.kt` | 수정 | 메뉴에서 시각 선택, 권한 요청과 거부 시 꺼짐 처리 |
| `app/src/main/AndroidManifest.xml` | 수정 | 승인된 알림·예약 권한만 최종 APK에 유지 |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | 수정 | 승인된 WorkManager 의존성 추가 |

## 수용 기준 체크
| AC | 결과 | 검증한 내용 |
|----|------|-------------|
| AC1 | ✅ | `ReminderTest.nextReminderIsTodayOrTomorrow`: 20:00→오늘 21:00, 22:00→내일 21:00 |
| AC2 | ✅ | `ReminderTest.textSkipsWhenStudiedAndFallsBackWhenNothingDue`: 오늘 학습 기록이면 문구가 `null` |
| AC3 | ✅ | 같은 테스트: 남은 단어 Day·문법 복습이 없으면 `오늘도 5분만 해 볼까요?` |
| AC4 | ✅ | S23+에서 권한을 거부하자 메뉴가 `끔`으로 남음. 허용 상태에서는 `매일 21:00` 표시와 권한 부여를 확인 |
| AC5 | ✅ | `./gradlew test lint :app:assembleRelease --quiet` 종료 코드 0 |

## 검증 결과
```
./gradlew test lint :app:assembleRelease --quiet  → 통과
ReminderTest                                  → 2개 통과, 실패 0개
git diff --cached --check                    → 출력 없음
```

S23+(`SM-S916N`, API 36)에 릴리스 APK를 `adb install -r`로 덮어 설치했다. 서명 오류는 없었고 앱 삭제·데이터 초기화는 하지 않았다. 권한 미허용 상태에서 알림을 켜자 Android 권한 대화상자가 표시됐다. `허용 안함`을 선택한 뒤 메뉴 값이 `끔`인 것을 확인했다. 앞선 허용 흐름에서는 메뉴가 `매일 21:00`이 되고 권한이 부여된 것을 확인했다. 테스트 후 알림을 끄고 권한 상태를 테스트 전의 미허용·미선택 상태로 복원했다. 실제 21:00 알림 발송은 기다려서 확인하지 않았다.

최종 APK의 Android 권한은 `INTERNET`, `RECORD_AUDIO`, `MANAGE_EXTERNAL_STORAGE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`다. WorkManager가 자동 추가한 `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`는 매니페스트 병합에서 제거했다. `WAKE_LOCK`과 `RECEIVE_BOOT_COMPLETED` 추가는 사용자에게 별도로 허락받았다. AndroidX가 선언하는 앱 내부 서명 권한은 사용자에게 권한 대화상자를 띄우지 않는다.

## 설계 판단 / 리뷰어가 봐야 할 곳
- `app/.../Reminder.kt` — 설정은 기본 `null`(꺼짐), 시각은 08·12·19·21·22시만 고른다. 알림 작업이 끝날 때 다음 일회성 작업을 이어 붙여 매일 같은 시각에 실행한다.
- `app/.../Reminder.kt` — 앱이 배경에서 처음 켜질 때 문법 책 로딩이 끝나지 않았을 수 있어, 복습 수는 저장된 학습 통계에서 읽는다. 이 수에는 현재 콘텐츠에 없는 문제의 기록이 포함될 수 있다.
- `app/.../Reminder.kt` — Android 12 이하에서는 알림 권한을 요청하지 않는다. Android 13 이상에서 거부하면 설정을 꺼진 상태로 둔다.
- `core/common/.../Reminder.kt` — 오늘 한 활동이 하나라도 있으면 알림을 보내지 않는다. 단어는 이어서 할 Day를 표시한다.

## 범위 밖 변경
- 없음. `POST_NOTIFICATIONS`와 WorkManager 의존성은 TASK 승인 범위이며, 두 예약 권한은 추가 허락을 받았다. `AGENTS.md`는 수정하지 않았다.

## 질문
- 없음.
