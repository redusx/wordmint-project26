package com.example.wordstore.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.wordstore.ui.components.GradientButton
import com.example.wordstore.ui.components.SectionHeader
import com.example.wordstore.ui.components.StatusBadge
import com.example.wordstore.ui.components.BadgeType
import com.example.wordstore.ui.theme.AnimationSpecs
import com.example.wordstore.ui.theme.BrandColors
import com.example.wordstore.ui.theme.CornerRadius
import com.example.wordstore.ui.theme.Elevation
import com.example.wordstore.ui.theme.Gradients
import com.example.wordstore.ui.theme.IconSize
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordStoreIcons
import com.example.wordstore.viewmodel.WordViewModel

/**
 * HomeScreen displays the list of saved words with premium Material 3 styling.
 * Features a modern header, animated cards, and intuitive interactions.
 *
 * @param navController Navigation controller for app navigation
 * @param wordViewModel ViewModel providing word data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    wordViewModel: WordViewModel
) {
    val allWords by wordViewModel.allWords.observeAsState(initial = emptyList())
    var selectedWord by remember { mutableStateOf<Word?>(null) }

    // Show detail dialog when a word is selected
    if (selectedWord != null) {
        WordDetailDialog(word = selectedWord!!, onDismiss = { selectedWord = null })
    }

    Scaffold(
        floatingActionButton = {
            PremiumFab(
                onClick = { navController.navigate("add_word") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = WordStoreIcons.AutoStories,
                            title = "Henüz kelime yok",
                            description = "Kelime dağarcığınızı oluşturmaya başlamak için yeni kelimeler ekleyin",
                            actionText = "İlk Kelimeyi Ekle",
                            onAction = { navController.navigate("add_word") }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
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
                                icon = WordStoreIcons.Book
                            )
                        }
                        
                        itemsIndexed(
                            items = allWords,
                            key = { _, word -> word.id }
                        ) { _, word ->
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

/**
 * Fixed home header with branding and statistics.
 */
@Composable
private fun HomeHeader(
    wordCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Branding row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(CornerRadius.Medium))
                        .background(brush = Gradients.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = WordStoreIcons.Translate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(IconSize.Medium)
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.MS))
                
                Column {
                    Text(
                        text = "WordStore",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kelime Öğrenme Asistanı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Word count badge
            if (wordCount > 0) {
                Surface(
                    shape = RoundedCornerShape(CornerRadius.Full),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$wordCount kelime",
                        modifier = Modifier.padding(horizontal = Spacing.MS, vertical = Spacing.XS),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.M))
        
        // Statistics card
        StatisticsCard(wordCount = wordCount)
    }
}

/**
 * Statistics card showing word count and progress.
 */
@Composable
private fun StatisticsCard(wordCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.L),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = WordStoreIcons.AutoStories,
                value = wordCount.toString(),
                label = "Toplam Kelime"
            )
            
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            StatItem(
                icon = WordStoreIcons.School,
                value = "Aktif",
                label = "Öğrenme"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(IconSize.Medium)
        )
        Spacer(modifier = Modifier.height(Spacing.XS))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Premium FAB with gradient and animation.
 */
@Composable
private fun PremiumFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(AnimationSpecs.ShortDuration),
        label = "fab_scale"
    )
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .shadow(
                elevation = Elevation.Level4,
                shape = RoundedCornerShape(CornerRadius.Large),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(CornerRadius.Large))
            .background(brush = Gradients.Primary)
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

/**
 * Premium word card with enhanced visuals and interactions.
 */
@Composable
fun PremiumWordCard(
    word: Word,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) Elevation.Level4 else Elevation.Level1,
        animationSpec = tween(AnimationSpecs.ShortDuration),
        label = "card_elevation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(AnimationSpecs.ShortDuration),
        label = "card_scale"
    )
    
    // Determine display order based on source language
    val (title, subtitle) = if (word.sourceLang == "tr") {
        word.turkish to word.english
    } else {
        word.english to word.turkish
    }
    
    val languageFlag = if (word.sourceLang == "tr") "TR" else "EN"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = "$title, çeviri: $subtitle. Detayları görmek için dokunun."
                role = Role.Button
            },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(CornerRadius.Large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(CornerRadius.Medium))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WordStoreIcons.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSize.Medium)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.M))
            
            // Word content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.XXS)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                ) {
                    Text(
                        text = title.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Language badge
                    Surface(
                        shape = RoundedCornerShape(CornerRadius.ExtraSmall),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = languageFlag,
                            modifier = Modifier.padding(horizontal = Spacing.XS, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = WordStoreIcons.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.XS))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics {
                    contentDescription = "$title kelimesini sil"
                    role = Role.Button
                }
            ) {
                Icon(
                    imageVector = WordStoreIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Premium word detail dialog with modern styling.
 */
@Composable
fun WordDetailDialog(word: Word, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(CornerRadius.XXLarge),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Elevation.Level5
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with gradient
                DialogHeader(word = word, onDismiss = onDismiss)
                
                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.L),
                    verticalArrangement = Arrangement.spacedBy(Spacing.M)
                ) {
                    // Phonetic section
                    if (!word.phonetic.isNullOrBlank()) {
                        item {
                            PhoneticCard(phonetic = word.phonetic)
                        }
                    }
                    
                    // Definitions
                    if (word.translatedDefinitions.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Tanımlar",
                                icon = WordStoreIcons.Description
                            )
                        }
                        
                        itemsIndexed(
                            items = word.translatedDefinitions,
                            key = { index, _ -> "def_$index" }
                        ) { index, def ->
                            DefinitionCard(def = def, index = index + 1)
                        }
                    } else if (word.meanings.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Anlamlar",
                                icon = WordStoreIcons.Description
                            )
                        }
                        
                        itemsIndexed(
                            items = word.meanings,
                            key = { index, _ -> "meaning_$index" }
                        ) { _, meaning ->
                            MeaningCard(meaning = meaning)
                        }
                    } else {
                        item {
                            EmptyDefinitionsState()
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(Spacing.L))
                    }
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
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.L)
        ) {
            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = WordStoreIcons.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Title
            Text(
                text = title.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(Spacing.S))
            
            // Translation chip
            Surface(
                shape = RoundedCornerShape(CornerRadius.Full),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = WordStoreIcons.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Small),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(Spacing.XS))
                    Text(
                        text = "$subtitleLabel: $subtitle",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.M))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun PhoneticCard(phonetic: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = WordStoreIcons.VolumeUp,
                contentDescription = "Telaffuz",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(IconSize.Medium)
            )
            Spacer(modifier = Modifier.width(Spacing.MS))
            Text(
                text = phonetic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DefinitionCard(def: TranslatedDefinition, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Level1)
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            // Header with index and part of speech
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                // Number badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                // Part of speech badge
                StatusBadge(
                    text = def.partOfSpeech,
                    type = BadgeType.PRIMARY
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.MS))
            
            // Primary definition
            Text(
                text = def.primaryDefinition,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Secondary definition
            if (def.secondaryDefinition.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.XS))
                Text(
                    text = def.secondaryDefinition,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        shape = RoundedCornerShape(Spacing.S),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
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
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.XS))
                Text(
                    text = "Örnekler",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            examples.forEach { example ->
                Column(modifier = Modifier.padding(bottom = Spacing.S)) {
                    Row {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.S))
                        Column {
                            Text(
                                text = example.primaryExample,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = example.secondaryExample,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Level1)
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            StatusBadge(
                text = meaning.partOfSpeech,
                type = BadgeType.PRIMARY
            )
            
            Spacer(modifier = Modifier.height(Spacing.MS))
            
            meaning.definitions.forEachIndexed { index, def ->
                Row(
                    modifier = Modifier.padding(bottom = Spacing.S)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.S))
                    Column {
                        Text(
                            text = def.definition,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (!def.example.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Spacing.XS))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = WordStoreIcons.FormatQuote,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.XS))
                                Text(
                                    text = def.example,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = WordStoreIcons.InfoOutlined,
                contentDescription = null,
                modifier = Modifier.size(IconSize.XLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Spacing.M))
            Text(
                text = "Tanım bulunamadı",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DialogFooter(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Elevation.Level2
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            contentAlignment = Alignment.CenterEnd
        ) {
            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(CornerRadius.Medium),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = WordStoreIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.S))
                Text(
                    "Kapat",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
