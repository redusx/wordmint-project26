package com.example.wordstore.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.wordstore.WordStoreApplication
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.repository.SaveWordResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the real app container, with real WorkManager and the real notification manager. */
@RunWith(AndroidJUnit4::class)
class ReminderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val container = (context as WordStoreApplication).container
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val createdWords = mutableListOf<Long>()

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        notifications.cancelAll()
        container.settings.lastReminderDay = null
        container.settings.setDailyNewGoal(10)
    }

    @After
    fun tearDown() = runBlocking {
        createdWords.forEach { container.words.delete(it) }
        container.settings.setReminder(false, 19, 0)
        container.reminderScheduler.sync()
        notifications.cancelAll()
    }

    private fun pending(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME).get()
            .filter { !it.state.isFinished }

    private fun addWord(term: String) = runBlocking {
        val now = System.currentTimeMillis()
        val result = container.words.save(
            Word(term = term, translation = "t", sourceLang = "en", targetLang = "zz", createdAt = now, updatedAt = now)
        )
        createdWords += (result as SaveWordResult.Saved).wordId
    }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<ReminderWorker>(context).build().doWork()
    }

    private fun reminderShown() = notifications.activeNotifications.any { it.notification.channelId == ReminderNotifier.CHANNEL_ID }

    @Test
    fun enablingSchedulesOneReminderAndDisablingCancelsIt() {
        container.settings.setReminder(true, 19, 0)
        container.reminderScheduler.sync()
        assertEquals(1, pending().size)

        container.settings.setReminder(true, 7, 30)
        container.reminderScheduler.sync()
        assertEquals(1, pending().size)

        container.settings.setReminder(false, 7, 30)
        container.reminderScheduler.sync()
        assertTrue(pending().isEmpty())
    }

    @Test
    fun appStartDoesNotDuplicateAPendingReminder() {
        container.settings.setReminder(true, 19, 0)
        container.reminderScheduler.sync()
        val id = pending().single().id
        container.reminderScheduler.ensureScheduled()
        assertEquals(id, pending().single().id)
    }

    @Test
    fun remindsWhenWordsAreWaitingAndNotAgainThatDay() {
        addWord("reminder-test-a")
        container.settings.setReminder(true, 19, 0)

        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertTrue(reminderShown())

        notifications.cancelAll()
        runWorker()
        assertTrue("A second run on the same day must stay quiet", !reminderShown())
    }

    @Test
    fun staysSilentWhenDisabled() {
        addWord("reminder-test-b")
        container.settings.setReminder(false, 19, 0)
        runWorker()
        assertTrue(!reminderShown())
        assertTrue("A disabled reminder must not reschedule itself", pending().isEmpty())
    }

    @Test
    fun workerReschedulesTheNextDay() {
        container.settings.setReminder(true, 19, 0)
        runWorker()
        assertEquals(1, pending().size)
    }
}
