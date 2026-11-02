package com.example.wordstore.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.ui.components.GlassTube
import com.example.wordstore.ui.components.GoalStepper
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LabelValueRow
import com.example.wordstore.ui.components.LanguagePickerSheet
import com.example.wordstore.ui.components.PrimaryButton
import com.example.wordstore.ui.components.ReminderTimeDialog
import com.example.wordstore.ui.components.SecondaryButton
import com.example.wordstore.ui.components.TubeOrientation
import com.example.wordstore.ui.components.rememberNotificationPermissionRequest
import com.example.wordstore.ui.components.rememberPackDownloader
import com.example.wordstore.ui.format.clockText
import com.example.wordstore.ui.format.languageName
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Motion
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.DictionaryViewModel
import com.example.wordstore.viewmodel.SettingsViewModel
import com.example.wordstore.viewmodel.SettingsUiState
import com.example.wordstore.viewmodel.appViewModel

private enum class LanguageSide { LEARNING, KNOWN }

private const val STEPS = 3

@Composable
fun OnboardingScreen(container: AppContainer, onFinished: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings, it.languagePacks, it.reminderScheduler) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var side by rememberSaveable { mutableStateOf<LanguageSide?>(null) }
    val download = rememberPackDownloader(viewModel::isOnUnmeteredNetwork, viewModel::downloadPack)

    fun finish() {
        viewModel.completeOnboarding()
        onFinished()
    }

    fun downloadIfNeeded() {
        val settings = state.settings
        DictionaryViewModel.requiredPacks(settings.sourceLang, settings.targetLang)
            .filter { code -> state.packs.none { it.code == code && (it.isDownloaded || it.isDownloading) } }
            .forEach(download)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.xs, end = Spacing.xl).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                IconAction(AppIcons.Back, stringResource(R.string.action_back), { step-- })
            } else {
                Spacer(Modifier.padding(start = Spacing.l))
            }
            GlassTube(
                level = (step + 1f) / STEPS,
                orientation = TubeOrientation.HORIZONTAL,
                modifier = Modifier.weight(1f).height(12.dp)
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(Motion.STANDARD)) togetherWith fadeOut(tween(Motion.QUICK)) },
            label = "onboarding_step",
            modifier = Modifier.weight(1f)
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl)
                    .padding(top = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl)
            ) {
                when (current) {
                    0 -> LanguagesStep(state, onPick = { side = it })
                    1 -> GoalStep(state, viewModel::setDailyNewGoal)
                    else -> ReminderStep(state, viewModel)
                }
            }
        }

        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            when (step) {
                0 -> PrimaryButton(
                    stringResource(R.string.onboarding_continue),
                    {
                        downloadIfNeeded()
                        step = 1
                    },
                    Modifier.fillMaxWidth().testTag("onboarding_next")
                )
                1 -> PrimaryButton(stringResource(R.string.onboarding_continue), { step = 2 }, Modifier.fillMaxWidth().testTag("onboarding_next"))
                else -> PrimaryButton(stringResource(R.string.onboarding_finish), ::finish, Modifier.fillMaxWidth().testTag("onboarding_finish"))
            }
        }
    }

    side?.let { current ->
        LanguagePickerSheet(
            title = stringResource(if (current == LanguageSide.LEARNING) R.string.settings_learning else R.string.settings_known),
            packs = state.packs,
            selected = if (current == LanguageSide.LEARNING) state.settings.sourceLang else state.settings.targetLang,
            onSelect = { code ->
                if (current == LanguageSide.LEARNING) viewModel.setLearningLanguage(code) else viewModel.setKnownLanguage(code)
                side = null
            },
            onDismiss = { side = null }
        )
    }
}

@Composable
private fun Heading(title: String, body: String) {
    val colors = WordMintTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(title, style = MaterialTheme.typography.displayMedium, color = colors.ink)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary)
    }
}

@Composable
private fun LanguagesStep(state: SettingsUiState, onPick: (LanguageSide) -> Unit) {
    Heading(stringResource(R.string.onboarding_languages_title), stringResource(R.string.onboarding_languages_body))
    Column {
        LanguageChoice(stringResource(R.string.settings_learning), state.settings.sourceLang, state.packs) { onPick(LanguageSide.LEARNING) }
        Hairline()
        LanguageChoice(stringResource(R.string.settings_known), state.settings.targetLang, state.packs) { onPick(LanguageSide.KNOWN) }
    }
    Text(
        stringResource(R.string.onboarding_languages_note),
        style = MaterialTheme.typography.bodySmall,
        color = WordMintTheme.colors.inkMuted
    )
}

@Composable
private fun LanguageChoice(label: String, code: String, packs: List<LanguagePack>, onClick: () -> Unit) {
    val colors = WordMintTheme.colors
    val onDevice = packs.firstOrNull { it.code == code }?.isDownloaded == true
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.m)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(languageName(code), style = MaterialTheme.typography.headlineSmall, color = colors.ink, modifier = Modifier.weight(1f))
            if (onDevice) {
                Text(stringResource(R.string.pack_on_device), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Icon(AppIcons.Forward, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.padding(start = Spacing.xs).size(Sizes.iconSmall))
        }
    }
}

@Composable
private fun GoalStep(state: SettingsUiState, onChange: (Int) -> Unit) {
    Heading(stringResource(R.string.onboarding_goal_title), stringResource(R.string.onboarding_goal_body))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GoalStepper(state.settings.dailyNewGoal, onChange)
    }
    Text(
        stringResource(R.string.settings_goal_note),
        style = MaterialTheme.typography.bodySmall,
        color = WordMintTheme.colors.inkMuted
    )
}

@Composable
private fun ReminderStep(state: SettingsUiState, viewModel: SettingsViewModel) {
    var isPickingTime by rememberSaveable { mutableStateOf(false) }
    var wasDenied by rememberSaveable { mutableStateOf(false) }
    val settings = state.settings
    val colors = WordMintTheme.colors
    val requestPermission = rememberNotificationPermissionRequest { granted ->
        wasDenied = !granted
        viewModel.setReminderEnabled(granted)
    }

    Heading(stringResource(R.string.onboarding_reminder_title), stringResource(R.string.onboarding_reminder_body))
    LabelValueRow(
        label = stringResource(R.string.settings_reminder_time),
        value = clockText(settings.reminderHour, settings.reminderMinute),
        onClick = { isPickingTime = true }
    )
    when {
        settings.reminderEnabled -> Text(
            stringResource(R.string.onboarding_reminder_on, clockText(settings.reminderHour, settings.reminderMinute)),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.primary
        )
        wasDenied -> Text(
            stringResource(R.string.onboarding_reminder_denied),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary
        )
        else -> SecondaryButton(
            text = stringResource(R.string.onboarding_reminder_enable),
            onClick = requestPermission,
            icon = AppIcons.Bell,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_enable_reminder")
        )
    }

    if (isPickingTime) {
        ReminderTimeDialog(
            hour = settings.reminderHour,
            minute = settings.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                isPickingTime = false
            },
            onDismiss = { isPickingTime = false }
        )
    }
}
