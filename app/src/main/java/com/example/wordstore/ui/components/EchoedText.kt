package com.example.wordstore.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The text a field shows when its state comes back through a flow.
 *
 * That state arrives after the keystroke. Giving a late value to BasicTextField overwrites newer
 * keystrokes and moves the cursor, so the field shows its own edits straight away and only takes
 * a value from state when it is not an echo of one of those edits.
 */
class EchoedText(initial: String) {
    var value by mutableStateOf(initial)
        private set

    private val awaitingEcho = ArrayDeque<String>()

    fun onEdit(text: String) {
        value = text
        awaitingEcho.addLast(text)
    }

    fun onState(text: String) {
        val echo = awaitingEcho.indexOf(text)
        if (echo >= 0) {
            // A conflated flow can skip values, so everything older than this echo is settled too.
            repeat(echo + 1) { awaitingEcho.removeFirst() }
        } else {
            awaitingEcho.clear()
            value = text
        }
    }
}

@Composable
fun rememberEchoedText(state: String): EchoedText {
    val text = remember { EchoedText(state) }
    LaunchedEffect(state) { text.onState(state) }
    return text
}
