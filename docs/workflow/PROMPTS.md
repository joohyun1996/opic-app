# 복붙용 프롬프트

`<>` 부분만 바꿔서 붙여넣는다. 규칙은 AGENTS.md / CLAUDE.md에 다 있으니 프롬프트는 짧게 유지한다.

---

## ② Claude — TASK 작성
```
"<기능 이름>" TASK를 docs/tasks/<순번>-<slug>/TASK.md 로 작성해줘.
SPEC.md 관련 섹션과 docs/decisions/ 만 참고해. 순번은 docs/tasks/ 의 최대 순번 + 1.
코드는 쓰지 말고, 수정 범위와 수용 기준을 명확히.
```

## ④ GPT — 구현
```
docs/tasks/<순번>-<slug>/TASK.md 를 구현해줘. (TASK는 승인됨)
AGENTS.md의 "구현 에이전트(GPT) 규칙"을 따르고,
완료 조건을 전부 통과하면 커밋 후 같은 폴더에 HANDOFF.md를 템플릿대로 작성해.
```

## ⑤ Claude — 리뷰
```
docs/tasks/<순번>-<slug>/HANDOFF.md 기준으로 리뷰해서 REVIEW.md 작성해줘.
diff만 보고, 전체 파일은 꼭 필요할 때만.
```

## ⑥ GPT — 리뷰 반영
```
docs/tasks/<순번>-<slug>/REVIEW.md 의 <N>차 리뷰를 반영해줘.
Must-fix는 전부, Should-fix는 반영 또는 이유 기록.
동의 안 되는 항목은 반영하지 말고 HANDOFF "리뷰 반영" 표에 반론으로 적어.
새 커밋으로 하고, HANDOFF에 "리뷰 반영 (<N>차)" 섹션 추가.
```

## ⑤' Claude — 재리뷰
```
docs/tasks/<순번>-<slug>/HANDOFF.md 의 "리뷰 반영 (<N>차)" 커밋 범위만 재리뷰해서
REVIEW.md에 <N+1>차 리뷰 섹션 추가해줘.
```

## ⑦ Claude — 마무리
```
docs/tasks/<순번>-<slug> Approve 됐으니 SPEC.md 업데이트해줘. (변경 이력 포함)
```

---

## GPT가 막혔을 때 (Claude로 넘기기 전에)
```
effort를 high로 올려서 다시 시도해줘.
원인 가설 3개 → 각각 확인 방법 → 확인 결과 순서로.
```
그래도 안 되면 Claude에게는 **에러 메시지 + 관련 diff + GPT가 시도한 것 요약**만 넘긴다. 파일 통째로 넘기지 않는다.

## ChatGPT 웹/앱에서 쓸 때 (Codex가 아닐 때)
웹에서는 AGENTS.md를 자동으로 못 읽는다. 대화 첫 메시지에 붙여넣는다:
```
아래는 이 프로젝트의 에이전트 규칙이다. 너는 "구현 에이전트(GPT)" 역할이다.
이 규칙을 대화 내내 따라라.

<AGENTS.md 전체 붙여넣기>
<TASK.md 붙여넣기>
```
