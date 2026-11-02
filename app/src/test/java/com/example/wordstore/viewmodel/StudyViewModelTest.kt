package com.example.wordstore.viewmodel

import com.example.wordstore.domain.scheduling.CardState
import com.example.wordstore.domain.scheduling.Fsrs6Scheduler
import com.example.wordstore.domain.scheduling.Rating
import com.example.wordstore.fakes.FakeStudyRepository
import com.example.wordstore.fakes.MutableClock
import com.example.wordstore.fakes.word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {
    private val clock = MutableClock()
    private val scheduler = Fsrs6Scheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeStudyRepository) = StudyViewModel(repository, scheduler, clock)

    private fun repository(count: Int) = FakeStudyRepository(scheduler, clock, (1L..count).map { word(it) })

    private val StudyViewModel.studying get() = state.value.phase as StudyPhase.Studying

    @Test
    fun `no words shows the empty state`() {
        assertTrue(viewModel(repository(0)).state.value.phase is StudyPhase.Empty)
    }

    @Test
    fun `starts on the question with position and total`() {
        val vm = viewModel(repository(3))
        assertFalse(vm.studying.isRevealed)
        assertEquals(1, vm.state.value.position)
        assertEquals(3, vm.state.value.total)
    }

    @Test
    fun `cannot rate before the answer is revealed`() {
        val repository = repository(1)
        val vm = viewModel(repository)
        vm.rate(Rating.GOOD)
        assertTrue(repository.recorded.isEmpty())
    }

    @Test
    fun `button intervals are exactly what the scheduler applies`() {
        val repository = repository(1)
        val vm = viewModel(repository)
        vm.reveal()
        val memory = vm.studying.card.memory
        val expected = scheduler.preview(memory, clock.millis())
        vm.studying.options.forEach { option ->
            assertEquals(expected.getValue(option.rating).intervalMillis, option.intervalMillis)
        }
        assertEquals(Rating.entries, vm.studying.options.map { it.rating })

        val goodInterval = vm.studying.options.first { it.rating == Rating.EASY }.intervalMillis
        vm.rate(Rating.EASY)
        assertEquals(goodInterval, repository.memories.getValue(memory.cardId).dueAt - clock.millis())
    }

    @Test
    fun `rating saves, reports the result and moves on`() {
        val repository = repository(2)
        val vm = viewModel(repository)
        vm.reveal()
        vm.rate(Rating.EASY)
        assertEquals(listOf(1L to Rating.EASY), repository.recorded)
        assertEquals(CardState.REVIEW, repository.memories.getValue(1).state)
        assertEquals(2L, vm.studying.card.word.id)
        assertFalse(vm.studying.isRevealed)
        assertEquals(Rating.EASY, vm.state.value.feedback?.rating)
    }

    @Test
    fun `a card rated again returns in the same session`() {
        val repository = repository(1)
        val vm = viewModel(repository)
        vm.reveal()
        vm.rate(Rating.AGAIN)
        assertEquals(1L, vm.studying.card.word.id)
        assertEquals(2, vm.state.value.position)
        assertEquals(2, vm.state.value.total)
    }

    @Test
    fun `finishing shows a summary of what really happened`() {
        val repository = repository(2)
        val vm = viewModel(repository)
        clock.nowMillis += 5_000
        vm.reveal()
        clock.nowMillis += 5_000
        vm.rate(Rating.EASY)
        vm.reveal()
        vm.rate(Rating.EASY)
        val summary = (vm.state.value.phase as StudyPhase.Complete).summary
        assertEquals(2, summary.reviewed)
        assertEquals(2, summary.newStarted)
        assertEquals(2, summary.ratings[Rating.EASY])
        assertEquals(10_000L, summary.activeMillis)
        assertTrue(summary.nextDueAt!! > clock.millis())
    }

    @Test
    fun `time on a card left open is capped`() {
        val vm = viewModel(repository(1))
        clock.nowMillis += 60 * 60_000
        vm.reveal()
        vm.rate(Rating.EASY)
        assertEquals(5 * 60_000L, (vm.state.value.phase as StudyPhase.Complete).summary.activeMillis)
    }

    @Test
    fun `a new session continues where the last one stopped`() {
        val repository = repository(2)
        val first = viewModel(repository)
        first.reveal()
        first.rate(Rating.EASY)

        val second = viewModel(repository)
        assertEquals(2L, second.studying.card.word.id)
        assertEquals(1, second.state.value.total)
    }
}
