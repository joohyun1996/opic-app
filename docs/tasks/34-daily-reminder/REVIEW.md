# REVIEW: TASK 34 하루 한 번 학습 알림 (+ 토큰 저장소 종료 수정)

> 리뷰: Claude · 범위: `git diff 6c8742d..04192d4` (5c2148e 알림, 34cd29d HANDOFF, 04192d4 토큰 저장소 fix)
> 판정: **Approve** (Must-fix 0)

## 확인한 것
- 데이터 안전: DB·엔티티·Migration 변경 없음. 알림 설정은 별도 SharedPreferences(`opic_reminder`)
- 정확성:
  - `nextReminderAt`·`reminderText`는 core/common의 순수 함수이고, 테스트가 수용 기준 AC1~AC3을 그대로 확인한다 (`ReminderTest.kt`)
  - 오늘 학습 여부는 `loadStats(...).week.last()`로 판단하고 language 필터를 거친다 (`app/.../Reminder.kt:58-61`)
  - 워커 안에서 다음 알림을 `APPEND_OR_REPLACE`로 다시 예약한다 (`Reminder.kt:43,63`)
    - 실행 중인 자기 자신을 취소하지 않으므로 올바르다
    - `ExistingWorkPolicy`는 WorkManager 정책이라 Room의 REPLACE 금지 규칙과는 관계없다
- 권한:
  - `POST_NOTIFICATIONS`·`WAKE_LOCK`·`RECEIVE_BOOT_COMPLETED`는 사용자가 승인했다 (HANDOFF)
  - WorkManager가 추가하는 `ACCESS_NETWORK_STATE`·`FOREGROUND_SERVICE`는 `tools:node="remove"`로 뺐다 (`AndroidManifest.xml:11-12`)
  - 알림은 네트워크를 쓰지 않는다
- 권한 거부 흐름: 거부하면 `set(app, null)`이 예약을 취소하고 메뉴가 "끔"으로 남는다 (`OpicRoot.kt:255-259`, 실기기 확인됨)
- 토큰 fix (04192d4):
  - 복호화에 실패해도 기존 암호 파일은 지우지 않고, 별도 키와 파일(recovery)로 전환한다 (`HfTokenStore.kt:31-37`)
  - 저장에 실패하면 false를 돌려주고 화면에 안내한다
  - 키를 코드에 직접 넣지 않았고, 토큰은 로그에 남기지 않는다

## Must-fix
- 없음

## Should-fix
- 없음

## Nit
1. `HfTokenStore.kt:31-32`: recovery 파일이 한 번 생기면 이후로는 항상 recovery만 쓴다. 기본 파일이 일시적인 오류로 한 번만 실패한 경우에도 기존 토큰으로 돌아가지 않는다. 데이터 손실은 아니지만(파일은 남아 있음) 토큰을 다시 입력해야 한다. 지금 동작으로도 충분하다.
2. `Reminder.kt:49-50`: minSdk가 34라서 `SDK_INT < TIRAMISU` 분기는 실행될 일이 없다.
3. 04192d4는 TASK 34 범위 밖의 긴급 수정이다. 다음에는 별도 TASK나 HANDOFF에 범위 밖 수정 사유를 한 줄 남기면 추적하기 쉽다.

## 남은 확인 (사용자)
- 예약한 시각에 실제로 알림이 오는지 실기기 확인 (HANDOFF 미확인 항목)
- AGENTS.md 금지사항의 허용 권한 목록에 `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`를 추가해야 한다. Claude는 AGENTS.md를 수정할 수 없어서 사용자가 직접 넣어야 한다.
