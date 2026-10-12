package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AffixTest {
    @Test fun formatsVerifiedPiecesInWordOrder() {
        assertEquals("dis(아닌) + similar(비슷한) → 비슷하지 않은",
            formatAffixBreakdown(listOf(AffixPiece("dis-", "아닌", 1, null), AffixPiece("similar", "비슷한", null, 2)), "비슷하지 않은"))
    }
}
