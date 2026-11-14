package com.nangmanroad.phone

import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.TipKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareDraftTest {
    @Test
    fun `an unfinished optional tip does not block sharing`() {
        val draft = ShareDraft(
            step = ShareDraft.STEP_COUNT - 1,
            title = "테스트 장소",
            category = Category.SEA_SUNSET,
            tips = mapOf(TipKind.TOILET to DraftTip()),
        )

        assertTrue(draft.canContinue())
        assertFalse(draft.copy(title = " ").canContinue())
        assertFalse(draft.copy(publishing = true).canContinue())
    }
}
