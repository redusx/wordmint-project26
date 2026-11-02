package com.example.wordstore.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.model.partOfSpeech
import com.example.wordstore.domain.study.LearningStatus
import com.example.wordstore.ui.components.GlassSurface
import com.example.wordstore.ui.components.GlassTube
import com.example.wordstore.ui.components.Hairline
import com.example.wordstore.ui.components.IconAction
import com.example.wordstore.ui.components.LoadingBlock
import com.example.wordstore.ui.components.MessageBlock
import com.example.wordstore.ui.components.PrimaryButton
import com.example.wordstore.ui.components.ScreenTitle
import com.example.wordstore.ui.components.SearchField
import com.example.wordstore.ui.components.TextTab
import com.example.wordstore.ui.format.dueText
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import com.example.wordstore.viewmodel.LibraryRow
import com.example.wordstore.viewmodel.LibraryUiState
import com.example.wordstore.viewmodel.LibraryViewModel
import com.example.wordstore.viewmodel.WordFilter
import com.example.wordstore.viewmodel.WordSort
import com.example.wordstore.viewmodel.appViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun LibraryScreen(container: AppContainer, onOpenWord: (Long) -> Unit, onLookUp: () -> Unit) {
    val viewModel = appViewModel { LibraryViewModel(it.words, it.scheduler, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onSortChange = viewModel::onSortChange,
        onOpenWord = onOpenWord,
        onLookUp = onLookUp
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WordFilter) -> Unit,
    onSortChange: (WordSort) -> Unit,
    onOpenWord: (Long) -> Unit,
    onLookUp: () -> Unit
) {
    if (state.isLoading) {
        LoadingBlock()
        return
    }
    val hazeState = remember { HazeState() }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    // Measured, so large text and small screens can never hide the first row.
    var headerHeight by remember { mutableIntStateOf(0) }
    val headerSpace = with(density) { headerHeight.toDp() }

    Box(Modifier.fillMaxSize()) {
        if (state.rows.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .haze(hazeState)
                    .padding(start = Spacing.xl, end = Spacing.xl, top = headerSpace + Spacing.l)
            ) {
                if (state.totalWords == 0) {
                    MessageBlock(
                        title = stringResource(R.string.library_empty_title),
                        body = stringResource(R.string.library_empty_body),
                        action = { PrimaryButton(stringResource(R.string.today_look_up), onLookUp) }
                    )
                } else {
                    MessageBlock(
                        title = stringResource(R.string.library_no_match_title),
                        body = stringResource(R.string.library_no_match_body)
                    )
                }
            }
        } else {
            val alphabetical = state.sort == WordSort.ALPHABETICAL
            val groups = remember(state.rows, alphabetical) {
                if (alphabetical) {
                    state.rows.groupBy { it.word.term.trim().take(1).uppercase() }
                } else {
                    mapOf("" to state.rows)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().haze(hazeState).testTag("word_list"),
                contentPadding = PaddingValues(
                    start = Spacing.l,
                    end = Spacing.l,
                    top = headerSpace,
                    bottom = navBottom + Spacing.navigationClearance
                )
            ) {
                groups.forEach { (letter, rows) ->
                    if (letter.isNotEmpty()) {
                        item(key = "letter_$letter") {
                            Text(
                                letter,
                                style = MaterialTheme.typography.titleLarge,
                                color = WordMintTheme.colors.inkMuted,
                                modifier = Modifier.padding(top = Spacing.l, bottom = Spacing.xs)
                            )
                            Hairline()
                        }
                    }
                    items(rows, key = { it.word.id }) { row ->
                        WordRow(row, state.nowMillis, onOpenWord)
                        Hairline()
                    }
                }
            }
        }

        ListHeader(
            state = state,
            hazeState = hazeState,
            onQueryChange = onQueryChange,
            onFilterChange = onFilterChange,
            onSortChange = onSortChange,
            modifier = Modifier.onSizeChanged { headerHeight = it.height }
        )
    }
}

@Composable
private fun ListHeader(
    state: LibraryUiState,
    hazeState: HazeState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WordFilter) -> Unit,
    onSortChange: (WordSort) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(hazeState = hazeState, shape = Radii.header, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = Spacing.m)) {
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.m, top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenTitle(stringResource(R.string.nav_words), Modifier.weight(1f))
                SortMenu(state.sort, onSortChange)
            }
            SearchField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.library_search),
                onClear = { onQueryChange("") },
                modifier = Modifier.padding(top = Spacing.xs)
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                WordFilter.entries.forEach { filter ->
                    FilterTab(filter, state.filter == filter, state.counts[filter] ?: 0) { onFilterChange(filter) }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(filter: WordFilter, selected: Boolean, count: Int, onClick: () -> Unit) {
    TextTab(
        label = stringResource(
            when (filter) {
                WordFilter.ALL -> R.string.filter_all
                WordFilter.NEW -> R.string.filter_new
                WordFilter.LEARNING -> R.string.filter_learning
                WordFilter.DUE -> R.string.filter_due
                WordFilter.MASTERED -> R.string.filter_mastered
            }
        ),
        selected = selected,
        onClick = onClick,
        count = count,
        modifier = Modifier.testTag("filter_${filter.name}")
    )
}

@Composable
private fun SortMenu(sort: WordSort, onSortChange: (WordSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val colors = WordMintTheme.colors
    Box {
        IconAction(AppIcons.Sort, stringResource(R.string.library_sort), { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = colors.surface
        ) {
            WordSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (option) {
                                    WordSort.RECENT -> R.string.sort_recent
                                    WordSort.ALPHABETICAL -> R.string.sort_alphabetical
                                    WordSort.NEXT_REVIEW -> R.string.sort_next_review
                                    WordSort.HARDEST -> R.string.sort_hardest
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (option == sort) colors.primary else colors.ink
                        )
                    },
                    trailingIcon = {
                        if (option == sort) {
                            Icon(AppIcons.Check, stringResource(R.string.selected), tint = colors.primary, modifier = Modifier.size(Sizes.iconSmall))
                        }
                    },
                    onClick = {
                        onSortChange(option)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WordRow(row: LibraryRow, nowMillis: Long, onOpenWord: (Long) -> Unit) {
    val colors = WordMintTheme.colors
    val statusColor = statusColor(row.status)
    val statusLabel = statusLabel(row.status)
    val dueLabel = when {
        row.status == LearningStatus.NEW -> statusLabel
        else -> dueText(row.memory.dueAt, nowMillis)
    }
    val description = stringResource(
        R.string.library_row_description,
        row.word.term,
        row.word.translation,
        statusLabel,
        dueLabel
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.s)
            .clickable { onOpenWord(row.word.id) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = Spacing.s, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Text(
                    row.word.term,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                row.word.partOfSpeech?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = WordMintTheme.type.partOfSpeech, color = colors.inkMuted, maxLines = 1)
                }
            }
            Text(
                row.word.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            dueLabel,
            style = MaterialTheme.typography.bodySmall,
            color = if (row.isDue || row.status == LearningStatus.NEW) colors.copper else colors.inkMuted,
            maxLines = 1
        )
        GlassTube(
            level = row.recall,
            liquid = statusColor,
            liquidDeep = statusColor,
            modifier = Modifier.size(width = 10.dp, height = 36.dp)
        )
    }
}

@Composable
fun statusColor(status: LearningStatus): Color {
    val colors = WordMintTheme.colors
    return when (status) {
        LearningStatus.NEW -> colors.statusNew
        LearningStatus.LEARNING -> colors.statusLearning
        LearningStatus.MASTERED -> colors.statusMastered
    }
}

@Composable
fun statusLabel(status: LearningStatus): String = stringResource(
    when (status) {
        LearningStatus.NEW -> R.string.filter_new
        LearningStatus.LEARNING -> R.string.filter_learning
        LearningStatus.MASTERED -> R.string.filter_mastered
    }
)
