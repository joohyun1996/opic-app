package com.jooh.opic.core.llm

import android.content.Context
import java.io.File

/**
 * Gemma 3n E4B 단일 후보. E2B/1B 폴백은 의도적으로 넣지 않았다 — 실기기(S23+)에서 E4B가
 * 이미 정상 동작 확인됐고, 굳이 더 낮은 사양으로 물러날 이유가 없다는 판단(사용자 결정).
 * E4B 로딩 자체가 실패하면(파일 손상·기기 미지원 등) [DefaultLlmEngine]이 다른 모델로
 * 재시도하지 않고 그대로 [LlmEngineState.Failed] 를 낸다 — 호출부가 숫자요약 폴백으로 넘어간다.
 *
 * 바이트 크기는 Hugging Face 저장소 메타데이터 기준(2026-09-26 조회, README.md 참고) —
 * 2026-09-26 실기기 다운로드로 실제 검증됨(정확히 이 크기로 다운로드 완료). SHA-256 은 공개
 * 메타데이터에 없어 비워뒀다 — 크기 불일치로만 손상 감지가 가능하고, 같은 크기의 손상 파일은
 * 잡아내지 못한다.
 *
 * 다운로드 URL은 게이트된 저장소라 Hugging Face 토큰(Authorization 헤더)이 필요하다 —
 * [HfTokenStore] 에 저장된 토큰을 [HttpModelStore]의 `headers` 람다에서 채워준다.
 */
object ModelCatalog {
    private const val E4B_URL =
        "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task"
    private const val E4B_BYTES = 4_405_655_031L

    fun config(context: Context): LlmConfig {
        val dir = File(context.noBackupFilesDir, "llm").apply { mkdirs() }
        return LlmConfig(
            models = listOf(
                ModelSpec(
                    id = "gemma-3n-e4b",
                    file = File(dir, "gemma-3n-e4b-it.task"),
                    downloadUrl = E4B_URL,
                    expectedBytes = E4B_BYTES,
                    sha256 = null,
                ),
            ),
            // 기본값(1024)이 컨텍스트 전체(입력+출력 합산) 한도라, 종목분석처럼 지표가 많이
            // 들어가는 프롬프트에선 출력이 중간에 끊겼다 — 2048로 확장. 이 엔진 인스턴스를
            // 월결산 요약·AI질문도 같이 쓰지만, 늘린 건 상한선일 뿐 목표 길이가 아니라서
            // 원래 짧게 끝나던 답변이 억지로 길어지지는 않는다.
            maxTokens = 2048,
        )
    }

    /**
     * 디스크에 모델이 이미 온전히 받아져 있는지 — 크기만 비교(SHA-256 없음).
     * [OnDeviceLlmEngine.state]는 앱 프로세스가 새로 시작하면 항상 [LlmEngineState.NotDownloaded]로
     * 초기화돼서(이번 실행에서 아직 확인 안 해봤을 뿐, 실제로 파일이 있어도) 이걸로 다운로드 동의
     * 다이얼로그를 띄울지 판단하면 안 된다 — 디스크를 직접 봐야 한다.
     */
    fun isDownloaded(context: Context): Boolean {
        val spec = config(context).models.first()
        return (spec.file.isFile && spec.file.length() == spec.expectedBytes) || SharedModel.isValid(spec.expectedBytes)
    }

    /** 앱 안(noBackup)에 받아 둔 복사본. 공용 파일을 쓰게 되면 지워서 용량을 아낀다. */
    fun privateCopy(context: Context): File = config(context).models.first().file
}
