# ADR 002: Gemma 모델 파일을 여러 앱이 공용 폴더에서 같이 쓴다

- 날짜: 2026-10-10 · 결정: 사용자 · 상태: OPIc 적용(`5aaa688`), 머니로그는 머니로그 세션에서 적용 예정

## 배경
OPIc와 머니로그가 같은 Gemma 3n E4B(`gemma-3n-e4b-it.task`, 4,405,655,031바이트)를 각자 앱 전용 폴더(`no_backup/llm/`)에 두면 4.4GB가 두 번 든다.

## 결정
- 모델 파일 하나를 `내장 저장공간/Develop/Core/llm/gemma-3n-e4b-it.task`에 둔다 (사용자가 만든 `Develop/` 아래, 백업은 `Develop/Opic-app/`, `Develop/Moneylog/`)
- ~~시스템 파일 선택(SAF) + `/proc/self/fd/<fd>`~~ → **실기기 실패 (2026-10-10)**: MediaPipe(LiteRT-LM)가 그 경로를 다시 열 때 MediaProvider가 저장공간 권한을 검사해 거부 (`Permission to access file ... is denied`, `scoped_file_posix.cc:32 open() failed: /proc/self/fd/131`)
- **개정 (2026-10-10, 사용자 승인)**: 각 앱에 `MANAGE_EXTERNAL_STORAGE`("모든 파일 접근") 추가, 사용자가 설정에서 한 번 허용 → `setModelPath("/storage/emulated/0/Develop/Core/llm/gemma-3n-e4b-it.task")`로 직접 연다. 모델 파일 읽기에만 사용
- 공용 파일을 안 골랐으면 기존처럼 앱 전용 파일·다운로드를 쓴다

## 버린 대안
- sharedUserId로 두 앱 묶기: 기존 설치를 지워야 해서 머니로그 데이터 위험, 안드로이드에서 폐기 예정
- 한 앱이 ContentProvider로 빌려주기: 그 앱이 깔려 있어야만 다른 앱이 동작
- 모든 파일 접근 권한(MANAGE_EXTERNAL_STORAGE): 권한이 너무 넓음

## 머니로그에 적용할 것 (머니로그 세션용)
1. Manifest에 `<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />`
2. OPIc의 `core/llm/.../SharedModel.kt`(SharedModel + SharedFirstModelStore)를 머니로그 `core/llm`에 같은 내용으로 (패키지명만)
3. 엔진을 만드는 곳에서 `store = SharedFirstModelStore(기존 store)`, `ModelCatalog.isDownloaded`에 `|| SharedModel.isValid(spec.expectedBytes)`
4. 모델 준비 화면: 권한이 없으면 "공용 모델 쓰기 — 모든 파일 접근 허용" 버튼(`SharedModel.accessSettingsIntent`) → 돌아오면(ON_RESUME) 권한·파일 확인 후 `ensureModelReady()` (OPIc `core/correction/.../LlmPreparationScreen.kt` 참고)
5. 실기기: 공용 파일을 골라 AI 기능이 돌아가는 걸 확인한 **뒤에** 머니로그 전용 복사본(`no_backup/llm/gemma-3n-e4b-it.task`) 삭제
