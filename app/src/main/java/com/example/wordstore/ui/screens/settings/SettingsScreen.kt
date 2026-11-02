package com.example.wordstore.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.repository.LanguagePack
import com.example.wordstore.domain.repository.ThemeMode
import com.example.wordstore.ui.components.GoalStepper
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LabelValueRow
import com.example.wordstore.ui.components.LanguagePickerSheet
import com.example.wordstore.ui.components.Notice
import com.example.wordstore.ui.components.QuietButton
import com.example.wordstore.ui.components.ReminderTimeDialog
import com.example.wordstore.ui.components.ScreenTitle
import com.example.wordstore.ui.components.SectionTitle
import com.example.wordstore.ui.components.TextTab
import com.example.wordstore.ui.components.canPostNotifications
import com.example.wordstore.ui.components.openNotificationSettings
import com.example.wordstore.ui.components.rememberNotificationPermissionRequest
import com.example.wordstore.ui.components.rememberPackDownloader
import com.example.wordstore.ui.format.clockText
import com.example.wordstore.ui.format.languageName
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.SettingsViewModel
import com.example.wordstore.viewmodel.appViewModel

private enum class LanguageSide { LEARNING, KNOWN }

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings, it.languagePacks, it.reminderScheduler) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current
    val colors = WordMintTheme.colors

    var side by rememberSaveable { mutableStateOf<LanguageSide?>(null) }
    var isPickingTime by rememberSaveable { mutableStateOf(false) }
    var canNotify by remember { mutableStateOf(canPostNotifications(context)) }
    val download = rememberPackDownloader(viewModel::isOnUnmeteredNetwork, viewModel::downloadPack)

    // The user may change the permission in system settings while this screen is in the background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { canNotify = canPostNotifications(context) }
    }

    val requestPermission = rememberNotificationPermissionRequest { granted ->
        canNotify = granted
        viewModel.setReminderEnabled(granted)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = Spacing.xxl)
    ) {
        IconAction(AppIcons.Back, stringResource(R.string.action_back), onBack, Modifier.padding(horizontal = Spacing.xs))
        Column(
            Modifier.padding(horizontal = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            ScreenTitle(stringResource(R.string.settings_title))

            Column {
                SectionTitle(stringResource(R.string.settings_languages))
                Spacer(Modifier.height(Spacing.s))
                LanguageRow(stringResource(R.string.settings_learning), settings.sourceLang, state.packs) { side = LanguageSide.LEARNING }
                Hairline()
                LanguageRow(stringResource(R.string.settings_known), settings.targetLang, state.packs) { side = LanguageSide.KNOWN }
            }

            Column {
                SectionTitle(stringResource(R.string.settings_goal))
                Spacer(Modifier.height(Spacing.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings_goal_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    GoalStepper(settings.dailyNewGoal, viewModel::setDailyNewGoal)
                }
                Text(
                    stringResource(R.string.settings_goal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
            }

            Column {
                SectionTitle(stringResource(R.string.settings_reminder))
                Spacer(Modifier.height(Spacing.s))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_reminder_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { enabled ->
                            when {
                                !enabled -> viewModel.setReminderEnabled(false)
                                canNotify -> viewModel.setReminderEnabled(true)
                                else -> requestPermission()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.primary,
                            checkedThumbColor = colors.onPrimary,
                            uncheckedTrackColor = colors.surfaceSunken,
                            uncheckedThumbColor = colors.inkMuted,
                            uncheckedBorderColor = colors.inkMuted
                        ),
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }
                if (settings.reminderEnabled) {
                    Hairline()
                    LabelValueRow(
                        label = stringResource(R.string.settings_reminder_time),
                        value = clockText(settings.reminderHour, settings.reminderMinute),
                        onClick = { isPickingTime = true }
                    )
                }
                Text(
                    stringResource(R.string.settings_reminder_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
                if (!canNotify) {
                    Notice(
                        text = stringResource(R.string.settings_notifications_off),
                        icon = AppIcons.Bell,
                        modifier = Modifier.padding(top = Spacing.m),
                        action = {
                            QuietButton(stringResource(R.string.settings_notifications_open), { openNotificationSettings(context) })
                        }
                    )
                }
            }

            Column {
                SectionTitle(stringResource(R.string.settings_appearance))
                Spacer(Modifier.height(Spacing.s))
                Row(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeTab(mode, settings.themeMode == mode, Modifier.weight(1f)) { viewModel.setThemeMode(mode) }
                    }
                }
            }

            Column {
                SectionTitle(stringResource(R.string.settings_about))
                Spacer(Modifier.height(Spacing.s))
                Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
            }
        }
    }

    side?.let { current ->
        LanguagePickerSheet(
            title = stringResource(if (current == LanguageSide.LEARNING) R.string.settings_learning else R.string.settings_known),
            packs = state.packs,
            selected = if (current == LanguageSide.LEARNING) settings.sourceLang else settings.targetLang,
            onSelect = { code ->
                if (current == LanguageSide.LEARNING) viewModel.setLearningLanguage(code) else viewModel.setKnownLanguage(code)
                if (state.packs.firstOrNull { it.code == code }?.isDownloaded != true) download(code)
                side = null
            },
            onDismiss = { side = null }
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

@Composable
private fun LanguageRow(label: String, code: String, packs: List<LanguagePack>, onClick: () -> Unit) {
    val pack = packs.firstOrNull { it.code == code }
    val colors = WordMintTheme.colors
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary, modifier = Modifier.weight(1f))
            Text(languageName(code), style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Icon(AppIcons.Forward, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.padding(start = Spacing.xs).size(Sizes.iconSmall))
        }
        if (pack != null) {
            Text(
                stringResource(
                    when {
                        pack.isDownloading -> R.string.pack_downloading
                        pack.isDownloaded -> R.string.pack_on_device
                        else -> R.string.pack_needs_download
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                modifier = Modifier.align(Alignment.End).padding(end = Sizes.iconSmall + Spacing.xs)
            )
        }
    }
}

@Composable
private fun ThemeTab(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextTab(
        label = stringResource(
            when (mode) {
                ThemeMode.SYSTEM -> R.string.settings_theme_system
                ThemeMode.LIGHT -> R.string.settings_theme_light
                ThemeMode.DARK -> R.string.settings_theme_dark
            }
        ),
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
        modifier = modifier.testTag("theme_${mode.name}")
    )
}
