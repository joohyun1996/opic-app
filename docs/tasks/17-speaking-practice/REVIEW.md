# REVIEW: 스피킹 1 — 질문 은행 + 답변 녹음 + 받아 적기 + 즉시 지표

> 작성: Claude · 경로: docs/tasks/17-speaking-practice/REVIEW.md

## 1차 리뷰 — 2026-10-10
- 판정: **Approve** (Claude 구현 — 독립 리뷰 아님)
- 데이터 안전: Room·Migration 변경 없음. 녹음은 `filesDir/speaking-last.pcm` 1개
- 권한·네트워크: Manifest 그대로, 새 네트워크 없음 (모델 다운로드는 기존 경로)
- 섀도잉 영향: 녹음 루프를 `PcmRecorder`로 옮김 — 30초 상한·입력 크기·짧은 녹음 판정·정리 순서 동일. 빌드·단위 테스트 통과, 실기기 섀도잉 녹음은 다음 사용 때 확인

### Must-fix / Should-fix
- 없음

### Nit
- **N1** 콘텐츠는 작성자=검토자. GPT 사용량이 돌아오면 `CONTENT-REVIEW.md`의 남은 포인트로 교차 검토
- **N2** 결과 화면을 나갔다 들어오면 결과가 사라진다 (저장은 TASK 19)
- **N3** `.kotlin/` 빌드 캐시 폴더가 git 미추적으로 남는다 → `.gitignore`에 추가 권장 (chore)
