package com.nangmanroad.phone

import com.nangmanroad.core.data.Comment
import com.nangmanroad.phone.data.LocalComment
import com.nangmanroad.phone.data.mergeComments
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeCommentsTest {

    private fun local(id: String, parent: String?, at: Long) = LocalComment(id, "s", parent, "text $id", false, at)

    @Test
    fun `new comments come first and replies attach to their parent`() {
        val base = listOf(Comment("c1", "밤하늘수집가", "질문", "어제", isQuestion = true))
        val merged = mergeComments(
            base,
            listOf(local("mine-old", null, 1), local("mine-new", null, 2), local("r1", "c1", 3), local("r2", "mine-old", 4)),
            myName = "윤슬을따라",
        )
        assertEquals(listOf("mine-new", "mine-old", "c1"), merged.map { it.id })
        assertEquals(listOf("r1"), merged.single { it.id == "c1" }.replies.map { it.id })
        assertEquals(listOf("r2"), merged.single { it.id == "mine-old" }.replies.map { it.id })
        assertEquals("윤슬을따라", merged.first().author)
    }
}
