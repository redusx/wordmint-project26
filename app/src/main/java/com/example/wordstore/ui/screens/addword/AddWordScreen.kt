package com.example.wordstore.ui.screens.addword

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wordstore.data.local.entity.Word
import com.example.wordstore.domain.model.NetworkResult
import com.example.wordstore.domain.model.TranslatedDefinition
import com.example.wordstore.domain.model.WordSearchResult
import com.example.wordstore.ui.components.AnimatedListItem
import com.example.wordstore.ui.components.GradientButton
import com.example.wordstore.ui.components.InfoCard
import com.example.wordstore.ui.components.InfoCardType
import com.example.wordstore.ui.components.OutlinedPremiumButton
import com.example.wordstore.ui.components.SectionHeader
import com.example.wordstore.ui.components.TranslationProgress
import com.example.wordstore.ui.theme.AnimationSpecs
import com.example.wordstore.ui.theme.BrandColors
import com.example.wordstore.ui.theme.CornerRadius
import com.example.wordstore.ui.theme.GlassColors
import com.example.wordstore.ui.theme.IconSize
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordStoreIcons
import com.example.wordstore.viewmodel.DictionaryViewModel
import com.example.wordstore.viewmodel.DownloadEvent
import com.example.wordstore.viewmodel.LanguageModelStatus
import com.example.wordstore.viewmodel.WordViewModel

/**
 * AddWordScreen allows users to translate and save new words. Features premium design with enhanced
 * UX.
 *
 * @param navController Navigation controller
 * @param wordViewModel ViewModel for word operations
 * @param dictionaryViewModel ViewModel for dictionary/translation operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
        navController: NavController,
        wordViewModel: WordViewModel,
        dictionaryViewModel: DictionaryViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sharedPrefs = remember {
        context.getSharedPreferences("word_store_prefs", Context.MODE_PRIVATE)
    }

    // Use rememberSaveable for orientation change persistence
    var inputWord by rememberSaveable { mutableStateOf("") }
    var outputWord by rememberSaveable { mutableStateOf("") }

    // Load saved language preferences or use defaults
    var sourceLang by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("last_source_lang", "en") ?: "en")
    }
    var targetLang by rememberSaveable {
        mutableStateOf(sharedPrefs.getString("last_target_lang", "tr") ?: "tr")
    }

    val searchResult by dictionaryViewModel.searchResult.observeAsState()
    val languageStatus by dictionaryViewModel.languageStatus.observeAsState(initial = emptyList())
    val downloadingLanguages by
            dictionaryViewModel.downloadingLanguages.observeAsState(initial = emptySet())
    val downloadEvent by dictionaryViewModel.downloadEvent.observeAsState()

    // Mobile data permission dialog state
    var showDataPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDownloadLangCode by rememberSaveable { mutableStateOf<String?>(null) }

    // Setup network monitoring
    LaunchedEffect(Unit) {
        dictionaryViewModel.checkLanguageStatus()
        dictionaryViewModel.setupNetworkMonitoring(context)
    }

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(downloadEvent) {
        when (val event = downloadEvent) {
            is DownloadEvent.Error -> {
                snackbarHostState.showSnackbar(event.message)
                kotlinx.coroutines.delay(3000)
                dictionaryViewModel.clearDownloadEvent()
            }
            is DownloadEvent.Success -> {
                snackbarHostState.showSnackbar("${event.langCode.uppercase()} dil paketi indirildi")
                kotlinx.coroutines.delay(2000)
                dictionaryViewModel.clearDownloadEvent()
            }
            else -> {}
        }
    }

    // Data permission dialog
    if (showDataPermissionDialog && pendingDownloadLangCode != null) {
        PremiumAlertDialog(
                onDismiss = {
                    showDataPermissionDialog = false
                    pendingDownloadLangCode = null
                },
                onConfirm = {
                    pendingDownloadLangCode?.let { code ->
                        dictionaryViewModel.downloadLanguage(code, allowCellular = true)
                    }
                    showDataPermissionDialog = false
                    pendingDownloadLangCode = null
                },
                title = "Mobil Veri Kullanımı",
                text =
                        "Dil paketi indirmek için mobil veri kullanılacak. Devam etmek istiyor musunuz?",
                confirmText = "İndir",
                dismissText = "İptal"
        )
    }

    Scaffold(
            topBar = { PremiumAddWordTopBar(onBackClick = { navController.popBackStack() }) },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                            snackbarData = data,
                            shape = RoundedCornerShape(CornerRadius.Medium),
                            containerColor = GlassColors.DarkCardBackground,
                            contentColor = GlassColors.TextPrimary
                    )
                }
            },
            containerColor = GlassColors.DarkBackground
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.padding(paddingValues)
                                .fillMaxSize()
                                .background(GlassColors.DarkBackground)
                                .verticalScroll(rememberScrollState())
        ) {
            // Translation Section
            TranslationSection(
                    inputWord = inputWord,
                    outputWord = outputWord,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    onInputChange = { inputWord = it },
                    onOutputChange = { outputWord = it },
                    onSourceLangChange = { newLang ->
                        sourceLang = newLang
                        sharedPrefs.edit().putString("last_source_lang", newLang).apply()
                    },
                    onTargetLangChange = { newLang ->
                        targetLang = newLang
                        sharedPrefs.edit().putString("last_target_lang", newLang).apply()
                    },
                    languageStatus = languageStatus,
                    downloadingLanguages = downloadingLanguages,
                    onDownload = { langCode ->
                        if (isOnWiFi(context)) {
                            dictionaryViewModel.downloadLanguage(langCode, allowCellular = false)
                        } else {
                            pendingDownloadLangCode = langCode
                            showDataPermissionDialog = true
                        }
                    },
                    onRefresh = { dictionaryViewModel.checkLanguageStatus(forceRefresh = true) },
                    focusManager = focusManager
            )

            Spacer(modifier = Modifier.height(Spacing.L))

            // Check if both source and target language models are downloaded
            val isSourceDownloaded =
                    languageStatus.find { it.code == sourceLang }?.isDownloaded ?: false
            val isTargetDownloaded =
                    languageStatus.find { it.code == targetLang }?.isDownloaded ?: false
            val canTranslate = isSourceDownloaded && isTargetDownloaded

            // Warning message if language packs not downloaded
            if (!canTranslate && languageStatus.isNotEmpty()) {
                Surface(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = Spacing.L)
                                        .border(
                                                width = 1.dp,
                                                color = GlassColors.Error.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(14.dp)
                                        ),
                        color = GlassColors.Error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                            modifier = Modifier.padding(Spacing.M),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = WordStoreIcons.Warning,
                                contentDescription = null,
                                tint = GlassColors.Error,
                                modifier = Modifier.size(IconSize.Medium)
                        )
                        Spacer(modifier = Modifier.width(Spacing.S))
                        Text(
                                text = "Çevirmek istediğiniz dil paketini indirin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassColors.Error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.M))
            }

            // Translate Button
            Box(modifier = Modifier.padding(horizontal = Spacing.L)) {
                GradientButton(
                        text = "Çevir",
                        onClick = {
                            if (inputWord.isNotBlank() && canTranslate) {
                                focusManager.clearFocus()
                                dictionaryViewModel.searchWord(inputWord, sourceLang, targetLang)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = WordStoreIcons.Translate,
                        enabled = inputWord.isNotBlank() && canTranslate,
                        isLoading = searchResult is NetworkResult.Loading
                )
            }

            Spacer(modifier = Modifier.height(Spacing.L))

            // Results Area
            AnimatedVisibility(
                    visible = searchResult != null,
                    enter =
                            fadeIn(tween(AnimationSpecs.StandardDuration)) +
                                    slideInVertically(tween(AnimationSpecs.StandardDuration)) {
                                        it / 4
                                    },
                    exit = fadeOut(tween(AnimationSpecs.ShortDuration))
            ) {
                ResultsSection(
                        searchResult = searchResult,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        onOutputWordUpdate = { outputWord = it }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.L))

            // Save Button
            Box(modifier = Modifier.padding(horizontal = Spacing.L)) {
                OutlinedPremiumButton(
                        text = "Kelimeyi Kaydet",
                        onClick = {
                            if (inputWord.isNotBlank() && outputWord.isNotBlank()) {
                                saveWord(
                                        inputWord = inputWord,
                                        outputWord = outputWord,
                                        sourceLang = sourceLang,
                                        searchResult = searchResult,
                                        wordViewModel = wordViewModel,
                                        onComplete = { navController.popBackStack() }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = WordStoreIcons.Save,
                        enabled = inputWord.isNotBlank() && outputWord.isNotBlank()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.XXL))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumAddWordTopBar(onBackClick: () -> Unit) {
    Surface(color = GlassColors.DarkBackground, shadowElevation = 0.dp) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = Spacing.S, vertical = Spacing.S),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = onBackClick,
                    modifier =
                            Modifier.semantics {
                                contentDescription = "Geri dön"
                                role = Role.Button
                            }
            ) {
                Icon(
                        imageVector = WordStoreIcons.Back,
                        contentDescription = null,
                        tint = GlassColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(Spacing.S))

            Column {
                Text(
                        text = "Yeni Kelime Ekle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassColors.TextPrimary
                )
                Text(
                        text = "Kelime dağarcığınızı genişletin",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun TranslationSection(
        inputWord: String,
        outputWord: String,
        sourceLang: String,
        targetLang: String,
        onInputChange: (String) -> Unit,
        onOutputChange: (String) -> Unit,
        onSourceLangChange: (String) -> Unit,
        onTargetLangChange: (String) -> Unit,
        languageStatus: List<LanguageModelStatus>,
        downloadingLanguages: Set<String>,
        onDownload: (String) -> Unit,
        onRefresh: () -> Unit,
        focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(modifier = Modifier.padding(Spacing.L)) {
        // Source Language Card
        TranslationInputCard(
                label = "Kaynak Kelime",
                value = inputWord,
                onValueChange = onInputChange,
                selectedLang = sourceLang,
                onLangSelected = onSourceLangChange,
                languageStatus = languageStatus,
                downloadingLanguages = downloadingLanguages,
                onDownload = onDownload,
                onRefresh = onRefresh,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                icon = WordStoreIcons.Language
        )

        // Swap Languages Button
        Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.S),
                contentAlignment = Alignment.Center
        ) {
            IconButton(
                    onClick = {
                        // Swap languages
                        val tempLang = sourceLang
                        onSourceLangChange(targetLang)
                        onTargetLangChange(tempLang)
                        // Also swap words
                        val tempWord = inputWord
                        onInputChange(outputWord)
                        onOutputChange(tempWord)
                    },
                    modifier =
                            Modifier.clip(CircleShape)
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
                                            shape = CircleShape
                                    )
            ) {
                Icon(
                        imageVector = WordStoreIcons.SwapVert,
                        contentDescription = "Dilleri değiştir",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Target Language Card
        TranslationInputCard(
                label = "Çeviri",
                value = outputWord,
                onValueChange = onOutputChange,
                selectedLang = targetLang,
                onLangSelected = onTargetLangChange,
                languageStatus = languageStatus,
                downloadingLanguages = downloadingLanguages,
                onDownload = onDownload,
                onRefresh = onRefresh,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                icon = WordStoreIcons.Translate
        )
    }
}

@Composable
private fun TranslationInputCard(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        selectedLang: String,
        onLangSelected: (String) -> Unit,
        languageStatus: List<LanguageModelStatus>,
        downloadingLanguages: Set<String>,
        onDownload: (String) -> Unit,
        onRefresh: () -> Unit,
        imeAction: ImeAction,
        onImeAction: () -> Unit,
        icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val cardShape = RoundedCornerShape(20.dp)

    // Glass Card
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    GlassColors.GlassBorderTopDark,
                                                                    GlassColors
                                                                            .GlassBorderBottomDark
                                                            )
                                            ),
                                    shape = cardShape
                            ),
            shape = cardShape,
            color = GlassColors.GlassCardDark
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Label with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.MediumSmall),
                            tint = GlassColors.Accent
                    )
                    Spacer(modifier = Modifier.width(Spacing.S))
                    Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = GlassColors.Accent,
                            fontWeight = FontWeight.Medium
                    )
                }

                // Language Selector
                PremiumLanguageSelector(
                        selectedLang = selectedLang,
                        onLangSelected = onLangSelected,
                        languageStatus = languageStatus,
                        downloadingLanguages = downloadingLanguages,
                        onDownload = onDownload,
                        onRefresh = onRefresh
                )
            }

            Spacer(modifier = Modifier.height(Spacing.MS))

            // Text Input - Glass style
            OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Kelime girin...", color = GlassColors.TextTertiary) },
                    shape = RoundedCornerShape(14.dp),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GlassColors.Accent,
                                    unfocusedBorderColor = GlassColors.DarkGray600,
                                    focusedContainerColor = GlassColors.DarkGray800,
                                    unfocusedContainerColor = GlassColors.DarkGray800,
                                    cursorColor = GlassColors.Accent,
                                    focusedTextColor = GlassColors.TextPrimary,
                                    unfocusedTextColor = GlassColors.TextPrimary
                            ),
                    keyboardOptions =
                            KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    imeAction = imeAction
                            ),
                    keyboardActions =
                            KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
                    singleLine = true
            )
        }
    }
}

@Composable
private fun PremiumLanguageSelector(
        selectedLang: String,
        onLangSelected: (String) -> Unit,
        languageStatus: List<LanguageModelStatus>,
        downloadingLanguages: Set<String>,
        onDownload: (String) -> Unit,
        onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) { if (expanded) onRefresh() }

    val sortedLanguages =
            remember(languageStatus) {
                languageStatus.sortedWith(
                        compareByDescending<LanguageModelStatus> { it.isDownloaded }.thenBy {
                            it.code
                        }
                )
            }

    Box {
        Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(CornerRadius.Full),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier =
                        Modifier.semantics {
                                    contentDescription =
                                            "Dil seç: ${getLanguageDisplayName(selectedLang)}"
                                    role = Role.DropdownList
                                }
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
        ) {
            Row(
                    modifier = Modifier.padding(horizontal = Spacing.MS, vertical = Spacing.S),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = getLanguageDisplayName(selectedLang),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(Spacing.XS))
                Icon(
                        imageVector =
                                if (expanded) WordStoreIcons.ExpandLess
                                else WordStoreIcons.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier =
                        Modifier.heightIn(max = 320.dp)
                                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (sortedLanguages.isEmpty()) {
                DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(Spacing.S))
                                Text("Diller yükleniyor...")
                            }
                        },
                        onClick = {}
                )
            } else {
                sortedLanguages.forEach { status ->
                    LanguageMenuItem(
                            status = status,
                            isDownloading = downloadingLanguages.contains(status.code),
                            isSelected = status.code == selectedLang,
                            onClick = {
                                if (status.isDownloaded) {
                                    onLangSelected(status.code)
                                    expanded = false
                                } else if (!downloadingLanguages.contains(status.code)) {
                                    onDownload(status.code)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageMenuItem(
        status: LanguageModelStatus,
        isDownloading: Boolean,
        isSelected: Boolean,
        onClick: () -> Unit
) {
    DropdownMenuItem(
            text = {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Flag icon
                        Box(
                                modifier =
                                        Modifier.size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (isSelected)
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerHigh
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = getLanguageFlag(status.code),
                                    style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.width(Spacing.MS))

                        Text(
                                text = getLanguageDisplayName(status.code),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight =
                                        if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color =
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    when {
                        status.isDownloaded -> {
                            Icon(
                                    imageVector = WordStoreIcons.CheckCircle,
                                    contentDescription = "İndirildi",
                                    tint = BrandColors.Success,
                                    modifier = Modifier.size(20.dp)
                            )
                        }
                        isDownloading -> {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                            )
                        }
                        else -> {
                            Icon(
                                    imageVector = WordStoreIcons.Download,
                                    contentDescription = "İndir",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            onClick = onClick
    )
}

@Composable
private fun ResultsSection(
        searchResult: NetworkResult<WordSearchResult>?,
        sourceLang: String,
        targetLang: String,
        onOutputWordUpdate: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.L)) {
        when (val result = searchResult) {
            is NetworkResult.Loading -> {
                TranslationProgress()
            }
            is NetworkResult.Error -> {
                InfoCard(
                        title = "Hata",
                        description = result.message ?: "Bilinmeyen hata oluştu",
                        icon = WordStoreIcons.Error,
                        type = InfoCardType.WARNING
                )
            }
            is NetworkResult.Success -> {
                val data = result.data
                if (data != null) {
                    // Update output word
                    LaunchedEffect(data) {
                        when {
                            sourceLang == "en" && targetLang == "tr" -> {
                                data.turkishTranslation?.let { onOutputWordUpdate(it) }
                            }
                            sourceLang == "tr" && targetLang == "en" -> {
                                onOutputWordUpdate(data.englishWord)
                            }
                            targetLang == "en" -> {
                                onOutputWordUpdate(data.englishWord)
                            }
                            else -> {
                                data.turkishTranslation?.let { onOutputWordUpdate(it) }
                            }
                        }
                    }

                    // Show error message if any
                    if (!data.errorMessage.isNullOrBlank()) {
                        InfoCard(
                                title = "Bilgi",
                                description = data.errorMessage,
                                icon = WordStoreIcons.Info,
                                type = InfoCardType.INFO
                        )
                        Spacer(modifier = Modifier.height(Spacing.M))
                    }

                    // Definitions
                    if (!data.translatedDefinitions.isNullOrEmpty()) {
                        SectionHeader(title = "Tanımlar", icon = WordStoreIcons.Description)

                        Spacer(modifier = Modifier.height(Spacing.S))

                        data.translatedDefinitions.forEachIndexed { index, def ->
                            AnimatedListItem(visible = true, index = index) {
                                DefinitionResultCard(def = def, index = index + 1)
                            }
                            Spacer(modifier = Modifier.height(Spacing.S))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun DefinitionResultCard(def: TranslatedDefinition, index: Int) {
    val cardShape = RoundedCornerShape(14.dp)

    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = cardShape
                            ),
            shape = cardShape,
            color = GlassColors.GlassCardDark
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                // Index badge - gray style
                Box(
                        modifier =
                                Modifier.size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                    )
                }

                // Part of speech badge - gray style
                Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.1f)) {
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

            Text(
                    text = def.primaryDefinition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
            )

            if (def.secondaryDefinition.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.XS))
                Text(
                        text = def.secondaryDefinition,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.5f)
                )
            }

            if (def.examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.MS))
                Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.05f)
                ) {
                    Column(modifier = Modifier.padding(Spacing.S)) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = Spacing.XS)
                        ) {
                            Icon(
                                    imageVector = WordStoreIcons.FormatQuote,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSize.Small),
                                    tint = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(Spacing.XS))
                            Text(
                                    text = "Örnekler",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        def.examples.forEach { example ->
                            Column(modifier = Modifier.padding(bottom = Spacing.XS)) {
                                Text(
                                        text = "• ${example.primaryExample}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                        text = "  ${example.secondaryExample}",
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
}

@Composable
private fun PremiumAlertDialog(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit,
        title: String,
        text: String,
        confirmText: String,
        dismissText: String
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(CornerRadius.XLarge),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = WordStoreIcons.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            title = {
                Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                        text,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = onConfirm, shape = RoundedCornerShape(CornerRadius.Medium)) {
                    Text(confirmText)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}

// Utility functions
private fun saveWord(
        inputWord: String,
        outputWord: String,
        sourceLang: String,
        searchResult: NetworkResult<WordSearchResult>?,
        wordViewModel: WordViewModel,
        onComplete: () -> Unit
) {
    val meanings =
            if (searchResult is NetworkResult.Success) {
                searchResult.data?.definitions?.flatMap { it.meanings ?: emptyList() }
                        ?: emptyList()
            } else {
                emptyList()
            }

    val phonetic =
            if (searchResult is NetworkResult.Success) {
                searchResult.data?.definitions?.firstOrNull()?.phonetic
            } else {
                null
            }

    val audio =
            if (searchResult is NetworkResult.Success) {
                searchResult
                        .data
                        ?.definitions
                        ?.firstOrNull()
                        ?.phonetics
                        ?.firstOrNull { !it.audio.isNullOrBlank() }
                        ?.audio
            } else {
                null
            }

    val translatedDefinitions =
            if (searchResult is NetworkResult.Success) {
                searchResult.data?.translatedDefinitions ?: emptyList()
            } else {
                emptyList()
            }

    val (finalEnglish, finalTurkish) =
            if (sourceLang == "en") {
                inputWord.trim() to outputWord.trim()
            } else {
                outputWord.trim() to inputWord.trim()
            }

    val word =
            Word(
                    english = finalEnglish,
                    turkish = finalTurkish,
                    sourceLang = sourceLang,
                    meanings = meanings,
                    translatedDefinitions = translatedDefinitions,
                    phonetic = phonetic,
                    audioUrl = audio
            )
    wordViewModel.insert(word)
    onComplete()
}

fun isOnWiFi(context: Context): Boolean {
    val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun getLanguageDisplayName(code: String): String {
    return when (code) {
        "en" -> "English"
        "tr" -> "Türkçe"
        "de" -> "Deutsch"
        "fr" -> "Français"
        "es" -> "Español"
        "it" -> "Italiano"
        "ru" -> "Русский"
        "ar" -> "العربية"
        "ja" -> "日本語"
        "zh" -> "中文"
        else -> code.uppercase()
    }
}

fun getLanguageFlag(code: String): String {
    return when (code) {
        "en" -> "EN"
        "tr" -> "TR"
        "de" -> "DE"
        "fr" -> "FR"
        "es" -> "ES"
        "it" -> "IT"
        "ru" -> "RU"
        "ar" -> "AR"
        "ja" -> "JP"
        "zh" -> "CN"
        else -> code.uppercase()
    }
}
