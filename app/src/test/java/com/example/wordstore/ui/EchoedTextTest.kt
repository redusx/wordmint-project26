package com.example.wordstore.ui

import com.example.wordstore.ui.components.EchoedText
import org.junit.Assert.assertEquals
import org.junit.Test

class EchoedTextTest {
    @Test
    fun `a late echo of an earlier keystroke does not overwrite newer text`() {
        val text = EchoedText("")
        text.onEdit("j")
        text.onEdit("jo")
        text.onEdit("jou")
        text.onState("j")
        assertEquals("jou", text.value)
    }

    @Test
    fun `echoes that were skipped by a conflated flow are still recognized`() {
        val text = EchoedText("")
        text.onEdit("a")
        text.onEdit("ab")
        text.onState("ab")
        text.onState("")
        assertEquals("", text.value)
    }

    @Test
    fun `a change made outside the field replaces the text`() {
        val text = EchoedText("")
        text.onEdit("resilient")
        text.onState("resilient")
        text.onState("")
        assertEquals("", text.value)
    }

    @Test
    fun `a change made outside the field wins over keystrokes still waiting for an echo`() {
        val text = EchoedText("")
        text.onEdit("res")
        text.onState("kitap")
        assertEquals("kitap", text.value)
        text.onEdit("kitaplar")
        text.onState("kitaplar")
        assertEquals("kitaplar", text.value)
    }

    @Test
    fun `typing then deleting back to an earlier value keeps the latest text`() {
        val text = EchoedText("")
        text.onEdit("a")
        text.onEdit("")
        text.onEdit("b")
        text.onState("a")
        assertEquals("b", text.value)
        text.onState("")
        assertEquals("b", text.value)
        text.onState("b")
        assertEquals("b", text.value)
    }
}
