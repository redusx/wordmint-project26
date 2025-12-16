package com.example.wordstore.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.data.remote.model.MeaningDto
import com.example.wordstore.domain.model.TranslatedDefinition
import com.example.wordstore.domain.model.TranslatedExample
import com.example.wordstore.ui.components.EmptyState
import com.example.wordstore.ui.components.GlassFab
import com.example.wordstore.ui.components.SectionHeader
import com.example.wordstore.ui.theme.AnimationSpecs
import com.example.wordstore.ui.theme.GlassColors
import com.example.wordstore.ui.theme.IconSize
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordStoreIcons
import com.example.wordstore.viewmodel.WordViewModel

/**
 * HomeScreen displays the list of saved words with premium Material 3 styling. Features a modern
 * header, animated cards, and intuitive interactions.
 *
 * @param navController Navigation controller for app navigation
 * @param wordViewModel ViewModel providing word data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, wordViewModel: WordViewModel) {
    val allWords by wordViewModel.allWords.observeAsState(initial = emptyList())
    var selectedWord by remember { mutableStateOf<Word?>(null) }

    // Show detail dialog when a word is selected
    if (selectedWord != null) {
        WordDetailDialog(word = selectedWord!!, onDismiss = { selectedWord = null })
    }

    val isDarkTheme = isSystemInDarkTheme()

    // Solid dark gray background (no gradient)
    val backgroundColor =
            if (isDarkTheme) GlassColors.DarkBackground else GlassColors.LightBackground

    Scaffold(
            floatingActionButton = {
                GlassFab(
                        onClick = { navController.navigate("add_word") },
                        icon = WordStoreIcons.Add,
                        accentColor = GlassColors.Accent
                )
            },
            containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(backgroundColor)
                                .padding(paddingValues)
                                .statusBarsPadding()
        ) {
            // Fixed header - won't scroll with content
            HomeHeader(
                    wordCount = allWords.size,
                    modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S)
            )

            AnimatedContent(
                    targetState = allWords.isEmpty(),
                    transitionSpec = {
                        fadeIn(tween(AnimationSpecs.StandardDuration)) togetherWith
                                fadeOut(tween(AnimationSpecs.ShortDuration))
                    },
                    label = "content_animation",
                    modifier = Modifier.weight(1f)
            ) { isEmpty ->
                if (isEmpty) {
                    // Premium Empty state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                                icon = WordStoreIcons.AutoStories,
                                title = "Henüz kelime yok",
                                description =
                                        "Kelime dağarcığınızı oluşturmaya başlamak için yeni kelimeler ekleyin",
                                actionText = "İlk Kelimeyi Ekle",
                                onAction = { navController.navigate("add_word") }
                        )
                    }
                } else {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                    PaddingValues(
                                            start = Spacing.M,
                                            end = Spacing.M,
                                            top = Spacing.S,
                                            bottom = 100.dp // Space for FAB
                                    ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.MS)
                    ) {
                        // Section header
                        item {
                            SectionHeader(
                                    title = "Kelimelerim",
                            )
                        }

                        itemsIndexed(items = allWords, key = { _, word -> word.id }) { _, word ->
                            PremiumWordCard(
                                    word = word,
                                    onClick = { selectedWord = word },
                                    onDelete = { wordViewModel.delete(word) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Fixed home header with branding and statistics - Liquid Glass style. */
@Composable
private fun HomeHeader(wordCount: Int, modifier: Modifier = Modifier) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(modifier = modifier.fillMaxWidth()) {
        // Branding row
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Logo - Glass style
                Box(
                        modifier =
                                Modifier.size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GlassColors.Accent.copy(alpha = 0.2f))
                                        .border(
                                                width = 1.dp,
                                                brush =
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                GlassColors.Accent
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        ),
                                                                                Color.Transparent
                                                                        )
                                                        ),
                                                shape = RoundedCornerShape(14.dp)
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = WordStoreIcons.Translate,
                            contentDescription = null,
                            tint = GlassColors.Accent,
                            modifier = Modifier.size(IconSize.Medium)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.MS))

                Column {
                    Text(
                            text = "WordStore",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) GlassColors.TextPrimary else Color.Black
                    )
                    Text(
                            text = "Kelime Öğrenme Asistanı",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) GlassColors.TextTertiary else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.M))

        // Statistics card - Liquid Glass style
        StatisticsCard(wordCount = wordCount)
    }
}

/** Statistics card showing word count and progress - Liquid Glass style. */
@Composable
private fun StatisticsCard(wordCount: Int) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(20.dp)
    val borderTop =
            if (isDarkTheme) GlassColors.GlassBorderTopDark else GlassColors.GlassBorderTopLight
    val borderBottom =
            if (isDarkTheme) GlassColors.GlassBorderBottomDark
            else GlassColors.GlassBorderBottomLight
    val glassSurface = if (isDarkTheme) GlassColors.GlassCardDark else GlassColors.GlassCardLight
    val glassHighlight =
            if (isDarkTheme) GlassColors.GlassHighlightDark else GlassColors.GlassHighlightLight

    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassColors.Accent.copy(alpha = 0.2f))
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    GlassColors.Accent.copy(
                                                                            alpha = 0.5f
                                                                    ),
                                                                    Color.Transparent
                                                            )
                                            ),
                                    shape = RoundedCornerShape(14.dp)
                            ),
            shape = cardShape
    ) {
        Box(
                modifier =
                        Modifier.background(
                                        Brush.linearGradient(
                                                colors =
                                                        listOf(
                                                                glassHighlight,
                                                                glassSurface,
                                                                glassSurface.copy(
                                                                        alpha =
                                                                                glassSurface.alpha *
                                                                                        0.5f
                                                                )
                                                        ),
                                                start = Offset(0f, 0f),
                                                end =
                                                        Offset(
                                                                Float.POSITIVE_INFINITY,
                                                                Float.POSITIVE_INFINITY
                                                        )
                                        )
                                )
                                .padding(Spacing.L)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                        icon = WordStoreIcons.AutoStories,
                        value = wordCount.toString(),
                        label = "Toplam Kelime"
                )

                // Glass divider
                Box(
                        modifier =
                                Modifier.width(1.dp)
                                        .height(48.dp)
                                        .background(
                                                if (isDarkTheme) Color.White.copy(alpha = 0.1f)
                                                else Color.Black.copy(alpha = 0.1f)
                                        )
                )

                StatItem(icon = WordStoreIcons.School, value = "Aktif", label = "Öğrenme")
            }
        }
    }
}

@Composable
private fun StatItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        value: String,
        label: String
) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassColors.Accent,
                modifier = Modifier.size(IconSize.Medium)
        )
        Spacer(modifier = Modifier.height(Spacing.XS))
        Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) GlassColors.TextPrimary else Color.Black
        )
        Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) GlassColors.TextTertiary else Color.Gray
        )
    }
}

/** Premium FAB with gradient and animation. */
@Composable
private fun PremiumFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = tween(AnimationSpecs.ShortDuration),
                    label = "fab_scale"
            )

    Box(
            modifier =
                    Modifier.size(64.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassColors.Accent.copy(alpha = 0.2f))
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    GlassColors.Accent.copy(
                                                                            alpha = 0.5f
                                                                    ),
                                                                    Color.Transparent
                                                            )
                                            ),
                                    shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onClick
                            )
                            .semantics {
                                contentDescription = "Yeni kelime ekle"
                                role = Role.Button
                            },
            contentAlignment = Alignment.Center
    ) {
        Icon(
                imageVector = WordStoreIcons.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
        )
    }
}

/** Premium word card with pure gray Liquid Glass styling. */
@Composable
fun PremiumWordCard(word: Word, onClick: () -> Unit, onDelete: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.98f else 1f,
                    animationSpec = tween(150),
                    label = "card_scale"
            )

    // Determine display order based on source language
    val (title, subtitle) =
            if (word.sourceLang == "tr") {
                word.turkish to word.english
            } else {
                word.english to word.turkish
            }

    val languageFlag = if (word.sourceLang == "tr") "TR" else "EN"

    // Pure gray glass colors
    val glassSurface = GlassColors.GlassCardDark
    val borderColor = Color.White.copy(alpha = 0.1f)
    val cardShape = RoundedCornerShape(16.dp)

    // Delete confirmation dialog - Gray glass style
    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Surface(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(20.dp)
                                    ),
                    shape = RoundedCornerShape(20.dp),
                    color = GlassColors.DarkDialogBackground
            ) {
                Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    Box(
                            modifier =
                                    Modifier.size(56.dp)
                                            .clip(CircleShape)
                                            .background(GlassColors.Error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                imageVector = WordStoreIcons.Delete,
                                contentDescription = null,
                                tint = GlassColors.Error,
                                modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                            text = "Kelimeyi Sil",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                            text = "\"${title}\" kelimesini silmek istediğinizden emin misiniz?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button - glass style
                        Surface(
                                modifier =
                                        Modifier.weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { showDeleteDialog = false }
                                                .border(
                                                        width = 1.dp,
                                                        color = Color.White.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(12.dp)
                                                ),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.05f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                        text = "Vazgeç",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Delete button - muted red
                        Surface(
                                modifier =
                                        Modifier.weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    showDeleteDialog = false
                                                    onDelete()
                                                },
                                shape = RoundedCornerShape(12.dp),
                                color = GlassColors.Error.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                        text = "Sil",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = GlassColors.Error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Glass Card - No levitation, pure gray
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .scale(scale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassColors.Accent.copy(alpha = 0.2f))
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    GlassColors.Accent.copy(
                                                                            alpha = 0.5f
                                                                    ),
                                                                    Color.Transparent
                                                            )
                                            ),
                                    shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onClick
                            )
                            .semantics {
                                contentDescription =
                                        "$title, çeviri: $subtitle. Detayları görmek için dokunun."
                                role = Role.Button
                            },
            shape = cardShape,
            color = Color.White.copy(alpha = 0.02f)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.M),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Language indicator - gray style
            Box(
                    modifier =
                            Modifier.size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = WordStoreIcons.Translate,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(IconSize.MediumSmall)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.MS))

            // Word content
            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                ) {
                    Text(
                            text = title.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )

                    // Language badge - gray style
                    Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                                text = languageFlag,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // Delete button - subtle gray/muted red
            IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier =
                            Modifier.size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                        imageVector = WordStoreIcons.Delete,
                        contentDescription = "$title kelimesini sil",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Premium word detail dialog - Gray Glass style. */
@Composable
fun WordDetailDialog(word: Word, onDismiss: () -> Unit) {
    Dialog(
            onDismissRequest = onDismiss,
            properties =
                    DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            usePlatformDefaultWidth = false
                    )
    ) {
        Surface(
                modifier =
                        Modifier.fillMaxWidth(0.95f)
                                .fillMaxHeight(0.85f)
                                .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(24.dp)
                                ),
                shape = RoundedCornerShape(24.dp),
                color = GlassColors.DarkDialogBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header - Glass style
                DialogHeader(word = word, onDismiss = onDismiss)

                // Content
                LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = Spacing.L),
                        verticalArrangement = Arrangement.spacedBy(Spacing.M)
                ) {
                    // Phonetic section
                    if (!word.phonetic.isNullOrBlank()) {
                        item { PhoneticCard(phonetic = word.phonetic) }
                    }

                    // Definitions
                    if (word.translatedDefinitions.isNotEmpty()) {
                        item {
                            // Section header - glass style
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = Spacing.S)
                            ) {
                                Icon(
                                        imageVector = WordStoreIcons.Description,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(IconSize.MediumSmall)
                                )
                                Spacer(modifier = Modifier.width(Spacing.S))
                                Text(
                                        text = "Tanımlar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                )
                            }
                        }

                        itemsIndexed(
                                items = word.translatedDefinitions,
                                key = { index, _ -> "def_$index" }
                        ) { index, def -> DefinitionCard(def = def, index = index + 1) }
                    } else if (word.meanings.isNotEmpty()) {
                        item {
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = Spacing.S)
                            ) {
                                Icon(
                                        imageVector = WordStoreIcons.Description,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(IconSize.MediumSmall)
                                )
                                Spacer(modifier = Modifier.width(Spacing.S))
                                Text(
                                        text = "Anlamlar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                )
                            }
                        }

                        itemsIndexed(
                                items = word.meanings,
                                key = { index, _ -> "meaning_$index" }
                        ) { _, meaning -> MeaningCard(meaning = meaning) }
                    } else {
                        item { EmptyDefinitionsState() }
                    }

                    item { Spacer(modifier = Modifier.height(Spacing.L)) }
                }

                // Footer with close button
                DialogFooter(onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun DialogHeader(word: Word, onDismiss: () -> Unit) {
    val title = if (word.sourceLang == "tr") word.turkish else word.english
    val subtitle = if (word.sourceLang == "tr") word.english else word.turkish
    val subtitleLabel = if (word.sourceLang == "tr") "EN" else "TR"

    Box(modifier = Modifier.fillMaxWidth().background(GlassColors.DarkGray850)) {
        Column(modifier = Modifier.padding(Spacing.L)) {
            // Close button - glass style
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                        onClick = onDismiss,
                        modifier =
                                Modifier.size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                            imageVector = WordStoreIcons.Close,
                            contentDescription = "Kapat",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title
            Text(
                    text = title.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
            )

            Spacer(modifier = Modifier.height(Spacing.S))

            // Translation chip - glass style
            Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.08f)) {
                Row(
                        modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            imageVector = WordStoreIcons.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.Small),
                            tint = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.XS))
                    Text(
                            text = "$subtitleLabel: $subtitle",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.M))
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
private fun PhoneticCard(phonetic: String) {
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                            ),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
                modifier = Modifier.padding(Spacing.M),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = WordStoreIcons.VolumeUp,
                    contentDescription = "Telaffuz",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(IconSize.Medium)
            )
            Spacer(modifier = Modifier.width(Spacing.MS))
            Text(
                    text = phonetic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
            )
        }
    }
}

@Composable
private fun DefinitionCard(def: TranslatedDefinition, index: Int) {
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(14.dp)
                            ),
            shape = RoundedCornerShape(14.dp),
            color = GlassColors.GlassCardDark
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            // Header with index and part of speech
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                // Number badge - gray style
                Box(
                        modifier =
                                Modifier.size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                    )
                }

                // Part of speech badge - gray style
                Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.08f)) {
                    Text(
                            text = def.partOfSpeech,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.MS))

            // Primary definition
            Text(
                    text = def.primaryDefinition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
            )

            // Secondary definition
            if (def.secondaryDefinition.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.XS))
                Text(
                        text = def.secondaryDefinition,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Examples
            if (def.examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.MS))
                ExamplesSection(examples = def.examples)
            }
        }
    }
}

@Composable
private fun ExamplesSection(examples: List<TranslatedExample>) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.04f)
    ) {
        Column(modifier = Modifier.padding(Spacing.MS)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = Spacing.S)
            ) {
                Icon(
                        imageVector = WordStoreIcons.FormatQuote,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Small),
                        tint = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(Spacing.XS))
                Text(
                        text = "Örnekler",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.5f)
                )
            }

            examples.forEach { example ->
                Column(modifier = Modifier.padding(bottom = Spacing.S)) {
                    Row {
                        Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(Spacing.S))
                        Column {
                            Text(
                                    text = example.primaryExample,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                    text = example.secondaryExample,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeaningCard(meaning: MeaningDto) {
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(14.dp)
                            ),
            shape = RoundedCornerShape(14.dp),
            color = GlassColors.GlassCardDark
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            // Part of speech badge
            Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.1f)) {
                Text(
                        text = meaning.partOfSpeech,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.MS))

            meaning.definitions.forEachIndexed { index, def ->
                Row(modifier = Modifier.padding(bottom = Spacing.S)) {
                    Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.S))
                    Column {
                        Text(
                                text = def.definition,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                        )

                        if (!def.example.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Spacing.XS))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                        imageVector = WordStoreIcons.FormatQuote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.width(Spacing.XS))
                                Text(
                                        text = def.example,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDefinitionsState() {
    Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.XL),
            contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                    imageVector = WordStoreIcons.InfoOutlined,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.XLarge),
                    tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(Spacing.M))
            Text(
                    text = "Tanım bulunamadı",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DialogFooter(onDismiss: () -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth().background(GlassColors.DarkGray850).padding(Spacing.M),
            contentAlignment = Alignment.CenterEnd
    ) {
        // Close button - glass style
        Surface(
                modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onDismiss)
                                .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                ),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f)
        ) {
            Row(
                    modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        imageVector = WordStoreIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(Spacing.S))
                Text(
                        "Kapat",
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
