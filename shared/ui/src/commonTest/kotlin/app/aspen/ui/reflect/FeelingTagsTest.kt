package app.aspen.ui.reflect

import app.aspen.domain.logging.model.FeelingTag
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every feeling tag maps to a distinct localized label (no tag silently shares or drops copy). */
class FeelingTagsTest {

    @Test
    fun every_feeling_tag_has_a_distinct_label() {
        val labels = FeelingTag.entries.map { feelingLabel(it) }.toSet()
        assertEquals(FeelingTag.entries.size, labels.size)
    }
}
