# TASK: 하루 한 번 학습 알림

> 작성: Claude · 승인: [ ] 사용자 — **권한 추가(`POST_NOTIFICATIONS`)는 승인 시 함께 허락 필요**
> 경로: docs/tasks/34-daily-reminder/TASK.md

## 목표
사용자가 정한 시각에 "오늘 단어 N개 · 문법 복습 N문제" 알림을 하루 한 번 보낸다. 알림을 누르면 홈이 열린다.

## 수정 범위
| 파일 | 작업 | 내용 |
|------|------|------|
| `app/src/main/AndroidManifest.xml` | 수정 | `POST_NOTIFICATIONS` 권한 (승인 후) |
| `app/.../ReminderWorker.kt` | 생성 | WorkManager 하루 주기 작업 |
| `core/common/.../Reminder.kt` | 생성 | 다음 알림 시각 계산, 문구 생성 (순수 함수) |
| `core/common/src/test/.../ReminderTest.kt` | 생성 | |
| `app/.../MenuScreen.kt`, UiSettings | 수정 | 설정: 알림 켜기/끄기, 시각 (기본 끔, 21:00) |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | 수정 | WorkManager 의존성 (승인 후) |

## 요구사항
- 기본은 꺼짐이다. 켤 때 Android 13 이상이면 권한을 요청하고, 거부하면 설정을 끈 상태로 되돌린다
- 오늘 이미 학습 기록(단어·문법·스피킹·섀도잉 중 하나)이 있으면 알림을 보내지 않는다
- 네트워크 사용 없음

## 수용 기준
- [ ] AC1: 현재 20:00, 설정 21:00이면 다음 알림은 오늘 21:00이다. 현재 22:00이면 내일 21:00이다
- [ ] AC2: 오늘 학습 기록이 있으면 알림을 보내지 않는다 (문구 생성 함수가 null을 반환한다)
- [ ] AC3: 단어 0개·복습 0개면 "오늘도 5분만 해 볼까요?" 문구를 쓴다
- [ ] AC4: 권한을 거부하면 설정이 꺼짐으로 남는다 (실기기)
- [ ] AC5: `./gradlew test lint :app:assembleRelease` 통과

## 범위 밖
- 홈 화면 위젯

## 넘김 메모 (2026-10-10, Claude → GPT)
- 사용자가 TASK 34 승인(권한·WorkManager 포함). Claude는 사용량 부족으로 구현 전 중단, 파일 변경 없음
- 설계 제안:
  - `core/common/Reminder.kt`: `nextReminderAt(now, at)`, `reminderText(studiedToday, nextDay, grammarDue)` (+ ReminderTest)
  - `app/.../Reminder.kt`: ReminderSettings(SharedPreferences, 시각 null=꺼짐, 08·12·19·21·22시 선택) + ReminderWorker(OneTimeWork, 보낸 뒤 다음 날 재예약)
  - 오늘 학습 여부는 `feature.analysis.loadStats(...).week.last().count > 0`, 다음 Day는 `wordDao().dayStats("en",40)`에서 mastered < total인 첫 Day
  - 메뉴 설정 "학습 알림" → 선택 창, 켤 때 POST_NOTIFICATIONS 요청, 거부하면 꺼짐
  - 의존성: `androidx.work:work-runtime-ktx:2.10.0` (libs.versions.toml)
