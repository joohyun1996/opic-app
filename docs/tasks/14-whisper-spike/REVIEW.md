# REVIEW: 스피킹 0 — Whisper 기기 내 음성 인식 검증

> 작성: Claude · 경로: docs/tasks/14-whisper-spike/REVIEW.md

## 1차 리뷰 — 2026-10-07
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)

### 데이터 안전
- DB·엔티티·Migration 변경 없음. 녹음은 앱 내부 `files/stt/`, 모델은 `no_backup/stt/` (백업 제외)

### 보안·권한
- 권한 추가는 `RECORD_AUDIO`만 (AGENTS.md 허용 범위). 네트워크는 모델 다운로드만, 녹음·결과를 밖으로 보내지 않음
- `:core:stt`는 `debugImplementation` → release APK에 네이티브 라이브러리·검증 화면 없음 (APK 목록으로 확인)

### 정확성
- 측정표의 숫자는 logcat `SttBench` 원본과 일치. 최적화 빌드 수정 **후** 세 모델 모두 다시 잼 (수정 전 large-v3 측정은 버림)

### Must-fix / Should-fix
- 없음

### Nit
- **N1** `SttModels`가 `:core:llm`의 `ModelSpec`·`HttpModelStore`를 빌려 쓴다. 스피킹 TASK에서 다운로드 코드를 `core/common`이나 별도 모듈로 옮길지 정한다
- **N2** 즉흥 답변 WER은 재지 않았다 (추천의 남은 위험, HANDOFF 참고)
