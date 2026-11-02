package com.example.wordstore.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wordstore.R
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.model.DictionaryStatus
import com.example.wordstore.domain.model.WordLookup
import com.example.wordstore.domain.progress.DayActivity
import com.example.wordstore.domain.repository.ThemeMode
import com.example.wordstore.domain.repository.StudyCard
import com.example.wordstore.domain.scheduling.CardMemory
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.ui.screens.dictionary.DictionaryContent
import com.example.wordstore.ui.screens.library.LibraryContent
import com.example.wordstore.ui.screens.progress.ProgressContent
import com.example.wordstore.ui.screens.study.StudyContent
import com.example.wordstore.ui.screens.today.TodayContent
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.ActivityRange
import com.example.wordstore.viewmodel.DictionaryUiState
import com.example.wordstore.viewmodel.LibraryRow
import com.example.wordstore.viewmodel.LibraryUiState
import com.example.wordstore.viewmodel.LookupState
import com.example.wordstore.viewmodel.ProgressUiState
import com.example.wordstore.viewmodel.RatingOption
import com.example.wordstore.viewmodel.SessionSummary
import com.example.wordstore.viewmodel.StudyPhase
import com.example.wordstore.viewmodel.StudyUiState
import com.example.wordstore.viewmodel.TodayUiState
import com.example.wordstore.viewmodel.WordFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private val word = Word(
        id = 7,
        term = "resilient",
        translation = "dayanıklı",
        sourceLang = "en",
        targetLang = "tr",
        phonetic = "/ɹɪˈzɪl.jənt/",
        hint = "Springs back",
        createdAt = 0,
        updatedAt = 0
    )
    private val card = StudyCard(word, CardMemory(cardId = 7, dueAt = 0))

    private fun options() = listOf(
        RatingOption(Rating.AGAIN, 60_000),
        RatingOption(Rating.HARD, 330_000),
        RatingOption(Rating.GOOD, 600_000),
        RatingOption(Rating.EASY, 8 * 86_400_000L)
    )

    private fun studying(isRevealed: Boolean, isHintShown: Boolean = false, isSaving: Boolean = false) = StudyUiState(
        phase = StudyPhase.Studying(card, isRevealed, isHintShown, "Springs back", if (isRevealed) options() else emptyList(), isSaving),
        position = 3,
        total = 12
    )

    private fun setStudy(
        state: StudyUiState,
        onReveal: () -> Unit = {},
        onRate: (Rating) -> Unit = {},
        onHint: () -> Unit = {},
        onExit: () -> Unit = {}
    ) = compose.setContent {
        WordMintTheme {
            StudyContent(state, 0, onHint, onReveal, onRate, {}, onExit, {})
        }
    }

    @Test
    fun questionHidesTheAnswerUntilRevealed() {
        var revealed = false
        setStudy(studying(isRevealed = false), onReveal = { revealed = true })
        compose.onNodeWithText("resilient").assertIsDisplayed()
        compose.onNodeWithText("/ɹɪˈzɪl.jənt/").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.study_position, 3, 12)).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("dayanıklı").fetchSemanticsNodes().size)
        compose.onNodeWithText(string(R.string.study_show_answer)).performClick()
        assertTrue(revealed)
    }

    @Test
    fun hintIsOfferedAndCanBeRequested() {
        var requested = false
        setStudy(studying(isRevealed = false), onHint = { requested = true })
        compose.onNodeWithText(string(R.string.study_show_hint)).performClick()
        assertTrue(requested)
    }

    @Test
    fun answerShowsFourRatingsWithRealIntervals() {
        val rated = mutableListOf<Rating>()
        setStudy(studying(isRevealed = true), onRate = { rated += it })
        compose.onNodeWithTag("translation").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.study_rating_description, string(R.string.rating_hard), string(R.string.interval_minutes, 6))).assertIsDisplayed()
        Rating.entries.forEach { compose.onNodeWithTag("rate_${it.name}").performClick() }
        assertEquals(Rating.entries, rated)
    }

    @Test
    fun ratingsAreDisabledWhileSaving() {
        var rated = false
        setStudy(studying(isRevealed = true, isSaving = true), onRate = { rated = true })
        compose.onNodeWithTag("rate_GOOD").performClick()
        assertTrue(!rated)
    }

    @Test
    fun leavingASessionIsAlwaysPossible() {
        var left = false
        setStudy(studying(isRevealed = false), onExit = { left = true })
        compose.onNodeWithContentDescription(string(R.string.study_exit)).performClick()
        assertTrue(left)
    }

    @Test
    fun summaryReportsSelfRatedResults() {
        val summary = SessionSummary(12, 3, 125_000, mapOf(Rating.GOOD to 9, Rating.AGAIN to 3), null)
        setStudy(StudyUiState(phase = StudyPhase.Complete(summary)))
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.study_complete_title, 12, 12)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.duration_minutes_seconds, 2, 5)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.study_complete_ratings_note)).assertIsDisplayed()
    }

    @Test
    fun todayLeadsWithWorkWaiting() {
        var started = false
        compose.setContent {
            WordMintTheme {
                TodayContent(
                    TodayUiState(isLoading = false, totalWords = 40, waiting = 14, dueReviews = 11, newCards = 3, dailyNewGoal = 10),
                    onStudy = { started = true }, onLookUp = {}, onSettings = {}, onOpenWord = {}
                )
            }
        }
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.today_waiting, 14, 14)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.today_start)).performClick()
        assertTrue(started)
    }

    @Test
    fun todayOffersToContinueAfterStarting() {
        compose.setContent {
            WordMintTheme {
                TodayContent(TodayUiState(isLoading = false, totalWords = 40, waiting = 4, reviewsToday = 6), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText(string(R.string.today_continue)).assertIsDisplayed()
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.today_left, 4, 4)).assertIsDisplayed()
    }

    @Test
    fun todayEmptyStatePointsToTheDictionary() {
        var lookedUp = false
        compose.setContent {
            WordMintTheme {
                TodayContent(TodayUiState(isLoading = false), {}, { lookedUp = true }, {}, {})
            }
        }
        compose.onNodeWithText(string(R.string.today_empty_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.today_look_up)).performClick()
        assertTrue(lookedUp)
    }

    @Test
    fun todayAllClearSaysWhatComesNext() {
        compose.setContent {
            WordMintTheme {
                TodayContent(TodayUiState(isLoading = false, totalWords = 5, reviewsToday = 5, nowMillis = 0, nextDueAt = 3 * 3_600_000L), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText(string(R.string.today_done_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.today_next_review, string(R.string.due_in, string(R.string.interval_hours, 3)))).assertIsDisplayed()
    }

    private fun libraryRow(id: Long, term: String, status: LearningStatus) = LibraryRow(
        word.copy(id = id, term = term),
        CardMemory(cardId = id, dueAt = 0),
        status,
        false,
        0f
    )

    @Test
    fun libraryListsWordsAndFiltersReport() {
        var opened = 0L
        var filter: WordFilter? = null
        compose.setContent {
            WordMintTheme {
                LibraryContent(
                    LibraryUiState(
                        isLoading = false,
                        totalWords = 2,
                        rows = listOf(libraryRow(1, "alpha", LearningStatus.NEW), libraryRow(2, "beta", LearningStatus.MASTERED)),
                        counts = mapOf(WordFilter.ALL to 2)
                    ),
                    {}, { filter = it }, {}, { opened = it }, {}
                )
            }
        }
        compose.onNodeWithText("alpha").performClick()
        assertEquals(1L, opened)
        compose.onNodeWithTag("filter_DUE").performClick()
        assertEquals(WordFilter.DUE, filter)
    }

    @Test
    fun libraryExplainsNoMatches() {
        compose.setContent {
            WordMintTheme {
                LibraryContent(LibraryUiState(isLoading = false, totalWords = 3, query = "zzz"), {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText(string(R.string.library_no_match_title)).assertIsDisplayed()
    }

    private fun dictionaryState(status: DictionaryStatus = DictionaryStatus.FOUND, saved: Long? = null, draft: String = "kitap") =
        DictionaryUiState(
            query = "book",
            lookup = LookupState.Found(WordLookup("book", "kitap", "/bʊk/", emptyList(), emptyList(), status, "en", "tr")),
            translationDraft = draft,
            savedWordId = saved
        )

    private fun setDictionary(state: DictionaryUiState, onSave: () -> Unit = {}, onOpen: (Long) -> Unit = {}) =
        compose.setContent {
            WordMintTheme {
                DictionaryContent(state, {}, {}, {}, {}, {}, {}, {}, {}, onSave, onOpen)
            }
        }

    @Test
    fun dictionaryResultCanBeSaved() {
        var saved = false
        setDictionary(dictionaryState(), onSave = { saved = true })
        compose.onNodeWithTag("lookup_term").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.dictionary_save)).performClick()
        assertTrue(saved)
    }

    @Test
    fun aSavedWordOpensInsteadOfSavingAgain() {
        var opened = 0L
        setDictionary(dictionaryState(saved = 42), onOpen = { opened = it })
        compose.onNodeWithText(string(R.string.dictionary_in_your_words)).performClick()
        assertEquals(42L, opened)
    }

    @Test
    fun aBlankTranslationCannotBeSaved() {
        setDictionary(dictionaryState(draft = ""))
        compose.onNodeWithText(string(R.string.dictionary_save)).assertIsNotEnabled()
    }

    @Test
    fun dictionaryExplainsWhyDefinitionsAreMissing() {
        setDictionary(dictionaryState(status = DictionaryStatus.OFFLINE))
        compose.onNodeWithText(string(R.string.dictionary_offline)).assertIsDisplayed()
    }

    @Test
    fun progressWithoutReviewsShowsNoCharts() {
        compose.setContent {
            WordMintTheme { ProgressContent(ProgressUiState(isLoading = false), {}, {}) }
        }
        compose.onNodeWithText(string(R.string.progress_empty_title)).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText(string(R.string.progress_activity)).fetchSemanticsNodes().size)
    }

    @Test
    fun progressShowsRealActivityAndSwitchesRange() {
        var range: ActivityRange? = null
        compose.setContent {
            WordMintTheme {
                ProgressContent(
                    ProgressUiState(
                        isLoading = false,
                        totalReviews = 9,
                        activity = listOf(DayActivity(20000, 9, 2, 10)),
                        consistency = listOf(DayActivity(20000, 9, 2, 10))
                    ),
                    { range = it }, {}
                )
            }
        }
        compose.onNodeWithText(string(R.string.progress_activity)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.progress_range_month)).performClick()
        assertEquals(ActivityRange.MONTH, range)
    }

    @Test
    fun darkThemeRendersEveryPrimaryStateWithoutFailure() {
        compose.setContent {
            WordMintTheme(themeMode = ThemeMode.DARK) {
                TodayContent(TodayUiState(isLoading = false, totalWords = 2, waiting = 2), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.today_waiting, 2, 2)).assertIsDisplayed()
    }
}
