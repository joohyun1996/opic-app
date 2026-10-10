# ADR 002: Gemma 모델 파일을 여러 앱이 공용 폴더에서 같이 쓴다

- 날짜: 2026-10-10 · 결정: 사용자 · 상태: OPIc 적용(`5aaa688`), 머니로그는 머니로그 세션에서 적용 예정

## 배경
OPIc와 머니로그가 같은 Gemma 3n E4B(`gemma-3n-e4b-it.task`, 4,405,655,031바이트)를 각자 앱 전용 폴더(`no_backup/llm/`)에 두면 4.4GB가 두 번 든다.

## 결정
- 모델 파일 하나를 `내장 저장공간/Develop/Core/llm/gemma-3n-e4b-it.task`에 둔다 (사용자가 만든 `Develop/` 아래, 백업은 `Develop/Opic-app/`, `Develop/Moneylog/`)
- 각 앱은 **시스템 파일 선택(ACTION_OPEN_DOCUMENT)으로 한 번 고르고** `takePersistableUriPermission(READ)`로 권한을 유지한다 → **새 권한 없음**
- MediaPipe `setModelPath`는 경로만 받으므로 `ParcelFileDescriptor`를 열어 둔 채 `/proc/self/fd/<fd>`를 넘긴다 (프로세스에 하나만 열어 둠)
- 공용 파일을 안 골랐으면 기존처럼 앱 전용 파일·다운로드를 쓴다

## 버린 대안
- sharedUserId로 두 앱 묶기: 기존 설치를 지워야 해서 머니로그 데이터 위험, 안드로이드에서 폐기 예정
- 한 앱이 ContentProvider로 빌려주기: 그 앱이 깔려 있어야만 다른 앱이 동작
- 모든 파일 접근 권한(MANAGE_EXTERNAL_STORAGE): 권한이 너무 넓음

## 머니로그에 적용할 것 (머니로그 세션용)
1. OPIc의 `core/llm/src/main/java/com/jooh/opic/core/llm/SharedModel.kt`(SharedModel + SharedFirstModelStore)를 머니로그 `core/llm`에 같은 내용으로 추가 (패키지명만 맞춤)
2. 엔진을 만드는 곳에서 `store = SharedFirstModelStore(context, 기존 store)`
3. `ModelCatalog.isDownloaded`에 `|| SharedModel.isValid(context, spec.expectedBytes)`
4. 모델 다운로드/동의 화면에 "공용 모델 파일 선택 (Develop/Core/llm)" 버튼 → `OpenDocument` → `SharedModel.save(context, uri, expectedBytes)` 성공 시 `ensureModelReady()`
5. 실기기: 공용 파일을 골라 AI 기능이 돌아가는 걸 확인한 **뒤에** 머니로그 전용 복사본(`no_backup/llm/gemma-3n-e4b-it.task`) 삭제
