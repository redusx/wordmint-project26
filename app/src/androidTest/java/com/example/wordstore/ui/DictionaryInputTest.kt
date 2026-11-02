package com.example.wordstore.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wordstore.ui.components.SearchField
import com.example.wordstore.ui.screens.dictionary.DictionaryContent
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.DictionaryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryInputTest {
    @get:Rule
    val compose = createComposeRule()

    private val field get() = compose.onNodeWithTag("search_field")

    @Test
    fun searchFieldKeepsEveryCharacterWhileItsStateIsLate() {
        var state by mutableStateOf("")
        val sent = mutableListOf<String>()
        compose.setContent {
            WordMintTheme {
                SearchField(value = state, onValueChange = { sent += it }, placeholder = "Search", onClear = {})
            }
        }

        field.performTextInput("a")
        compose.waitForIdle()
        field.performTextInput("b")
        compose.waitForIdle()
        assertEquals(listOf("a", "ab"), sent)
        field.assertTextEquals("ab")

        state = "a"
        compose.waitForIdle()
        field.assertTextEquals("ab")

        state = "ab"
        compose.waitForIdle()
        field.assertTextEquals("ab")
    }

    @Test
    fun searchFieldFollowsAChangeMadeOutsideIt() {
        var state by mutableStateOf("book")
        compose.setContent {
            WordMintTheme {
                SearchField(value = state, onValueChange = { state = it }, placeholder = "Search", onClear = {})
            }
        }
        field.assertTextEquals("book")

        state = ""
        compose.waitForIdle()
        field.assertTextEquals("")
    }

    @Test
    fun lookUpClosesTheKeyboardSoTheResultIsVisible() {
        var searched = false
        compose.setContent {
            WordMintTheme {
                DictionaryContent(
                    DictionaryUiState(query = "book"),
                    {}, { searched = true }, {}, {}, {}, {}, {}, {}, {}, {}
                )
            }
        }

        field.performClick()
        field.assertIsFocused()

        compose.onNodeWithTag("look_up").performClick()
        compose.waitForIdle()

        assertTrue(searched)
        field.assertIsNotFocused()
    }
}
