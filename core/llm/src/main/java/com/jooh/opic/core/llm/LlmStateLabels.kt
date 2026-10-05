package com.jooh.opic.core.llm

/**
 * [OnDeviceLlmEngine] 상태를 화면에 보여줄 한국어 안내문으로 바꿔주는 공용 유틸.
 * 월결산 AI 요약 화면과 AI 질문 화면이 같은 문구를 쓴다.
 */
fun llmProgressLabel(state: LlmEngineState, generatingLabel: String): String = when (state) {
    is LlmEngineState.Downloading -> "AI 모델을 받는 중… ${(state.progress * 100).toInt()}%"
    is LlmEngineState.Loading -> "AI 모델을 불러오는 중…"
    else -> generatingLabel
}

fun llmFailureLabel(reason: LlmFailureReason): String = when (reason) {
    LlmFailureReason.AUTH_REQUIRED -> "설정에서 Hugging Face 토큰을 먼저 입력해주세요."
    LlmFailureReason.INSUFFICIENT_STORAGE -> "저장 공간이 부족해요."
    LlmFailureReason.INSUFFICIENT_MEMORY -> "이 기기 메모리로는 모델을 실행하기 어려워요."
    LlmFailureReason.DEVICE_UNSUPPORTED -> "이 기기에서는 AI 기능을 지원하지 않아요."
    LlmFailureReason.NETWORK -> "네트워크 연결을 확인해주세요."
    LlmFailureReason.TIMEOUT -> "응답이 너무 오래 걸려 중단됐어요."
    LlmFailureReason.BUSY -> "이전 AI 작업이 아직 끝나지 않았어요. 잠시 후 다시 시도해주세요."
    else -> "지난번 시도가 실패했어요. 다시 시도해볼 수 있어요."
}
