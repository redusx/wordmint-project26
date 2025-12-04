package com.example.wordstore.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wordstore.data.NetworkResult
import com.example.wordstore.data.Word
import com.example.wordstore.viewmodel.DictionaryViewModel
import com.example.wordstore.viewmodel.DownloadState
import com.example.wordstore.viewmodel.LanguageModelStatus
import com.example.wordstore.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    navController: NavController,
    wordViewModel: WordViewModel,
    dictionaryViewModel: DictionaryViewModel
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("word_store_prefs", Context.MODE_PRIVATE) }
    
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
    val downloadState by dictionaryViewModel.downloadState.observeAsState(DownloadState.Idle)

    // Mobile data permission dialog state
    var showDataPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDownloadLangCode by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Setup network monitoring (only once)
    LaunchedEffect(Unit) {
        dictionaryViewModel.checkLanguageStatus()
        dictionaryViewModel.setupNetworkMonitoring(context)
    }
    
    // Show download messages as snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                // Auto-reset after error
                kotlinx.coroutines.delay(3000)
                dictionaryViewModel.resetDownloadState()
            }
            is DownloadState.Success -> {
                snackbarHostState.showSnackbar("${state.langCode.uppercase()} dil paketi indirildi")
                // Auto-reset after success
                kotlinx.coroutines.delay(2000)
                dictionaryViewModel.resetDownloadState()
            }
            else -> {}
        }
    }

    // Data permission dialog
    if (showDataPermissionDialog && pendingDownloadLangCode != null) {
        AlertDialog(
            onDismissRequest = { 
                showDataPermissionDialog = false
                pendingDownloadLangCode = null
            },
            title = { Text("Mobil Veri Kullanımı") },
            text = { 
                Text("Dil paketi indirmek için mobil veri kullanılacak. Devam etmek istiyor musunuz?")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDownloadLangCode?.let { code ->
                        dictionaryViewModel.downloadLanguage(code, allowCellular = true)
                    }
                    showDataPermissionDialog = false
                    pendingDownloadLangCode = null
                }) {
                    Text("İndir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDataPermissionDialog = false
                    pendingDownloadLangCode = null
                }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Kelime Ekle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputWord,
                    onValueChange = { inputWord = it },
                    label = { Text("Kelime Girin") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                LanguageSelector(
                    selectedLang = sourceLang, 
                    onLangSelected = { newLang ->
                        sourceLang = newLang
                        // Save to SharedPreferences
                        sharedPrefs.edit().putString("last_source_lang", newLang).apply()
                    },
                    languageStatus = languageStatus,
                    onDownload = { langCode ->
                        // Check if on WiFi
                        if (isOnWiFi(context)) {
                            dictionaryViewModel.downloadLanguage(langCode, allowCellular = false)
                        } else {
                            // Ask permission for mobile data
                            pendingDownloadLangCode = langCode
                            showDataPermissionDialog = true
                        }
                    },
                    downloadState = downloadState,
                    onRefresh = { dictionaryViewModel.checkLanguageStatus(forceRefresh = true) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Output Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = outputWord,
                    onValueChange = { outputWord = it },
                    label = { Text("Çeviri") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                LanguageSelector(
                    selectedLang = targetLang, 
                    onLangSelected = { newLang ->
                        targetLang = newLang
                        // Save to SharedPreferences
                        sharedPrefs.edit().putString("last_target_lang", newLang).apply()
                    },
                    languageStatus = languageStatus,
                    onDownload = { langCode ->
                        if (isOnWiFi(context)) {
                            dictionaryViewModel.downloadLanguage(langCode, allowCellular = false)
                        } else {
                            pendingDownloadLangCode = langCode
                            showDataPermissionDialog = true
                        }
                    },
                    downloadState = downloadState,
                    onRefresh = { dictionaryViewModel.checkLanguageStatus(forceRefresh = true) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Translate Button
            Button(
                onClick = {
                    if (inputWord.isNotBlank()) {
                        dictionaryViewModel.searchWord(inputWord, sourceLang, targetLang)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = inputWord.isNotBlank(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Çevir", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results Area
            when (val result = searchResult) {
                is NetworkResult.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                is NetworkResult.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.message ?: "Bilinmeyen hata",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is NetworkResult.Success -> {
                    val data = result.data
                    if (data != null) {
                        // Show translation error if any
                        if (!data.errorMessage.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "⚠️ ${data.errorMessage}",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Auto-fill output for all language pairs
                        LaunchedEffect(data) {
                            // For EN↔TR, use the specific fields
                            when {
                                sourceLang == "en" && targetLang == "tr" -> {
                                    if (outputWord.isBlank() || outputWord != data.turkishTranslation) {
                                        outputWord = data.turkishTranslation ?: ""
                                    }
                                }
                                sourceLang == "tr" && targetLang == "en" -> {
                                    if (outputWord.isBlank() || outputWord != data.englishWord) {
                                        outputWord = data.englishWord
                                    }
                                }
                                // For other pairs (e.g., DE→TR, FR→EN), turkishTranslation holds the target
                                targetLang == "en" -> {
                                    // Target is English, use englishWord
                                    if (outputWord.isBlank() || outputWord != data.englishWord) {
                                        outputWord = data.englishWord
                                    }
                                }
                                else -> {
                                    // Target is not English, use turkishTranslation
                                    if (outputWord.isBlank() || outputWord != data.turkishTranslation) {
                                        outputWord = data.turkishTranslation ?: ""
                                    }
                                }
                            }
                        }

                        if (!data.translatedDefinitions.isNullOrEmpty()) {
                            Text(
                                text = "Tanımlar:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            data.translatedDefinitions.forEachIndexed { index, def ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "(${def.partOfSpeech})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = "${index + 1}. ${def.primaryDefinition}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "   (${def.secondaryDefinition})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        
                                        // Display examples if available
                                        if (def.examples.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "Örnekler:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                def.examples.forEach { example ->
                                                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                                        Text(
                                                            text = "• ${example.primaryExample}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "  (${example.secondaryExample})",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    if (inputWord.isNotBlank() && outputWord.isNotBlank()) {
                        val currentResult = searchResult
                        val meanings = if (currentResult is NetworkResult.Success) {
                            currentResult.data?.definitions?.flatMap { it.meanings ?: emptyList() } ?: emptyList()
                        } else {
                            emptyList()
                        }
                        
                        val phonetic = if (currentResult is NetworkResult.Success) {
                            currentResult.data?.definitions?.firstOrNull()?.phonetic
                        } else {
                            null
                        }

                        val audio = if (currentResult is NetworkResult.Success) {
                            currentResult.data?.definitions?.firstOrNull()?.phonetics?.firstOrNull { !it.audio.isNullOrBlank() }?.audio
                        } else {
                            null
                        }

                        val translatedDefinitions = if (currentResult is NetworkResult.Success) {
                            currentResult.data?.translatedDefinitions ?: emptyList()
                        } else {
                            emptyList()
                        }

                        // Map input/output to English/Turkish based on selection
                        val finalEnglish: String
                        val finalTurkish: String
                        
                        if (sourceLang == "en") {
                            finalEnglish = inputWord.trim()
                            finalTurkish = outputWord.trim()
                        } else {
                            finalEnglish = outputWord.trim()
                            finalTurkish = inputWord.trim()
                        }

                        val word = Word(
                            english = finalEnglish,
                            turkish = finalTurkish,
                            sourceLang = sourceLang,
                            meanings = meanings,
                            translatedDefinitions = translatedDefinitions,
                            phonetic = phonetic,
                            audioUrl = audio
                        )
                        wordViewModel.insert(word)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = inputWord.isNotBlank() && outputWord.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Kelimeyi Kaydet", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLang: String, 
    onLangSelected: (String) -> Unit,
    languageStatus: List<LanguageModelStatus>,
    onDownload: (String) -> Unit,
    downloadState: DownloadState? = null,
    onRefresh: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Auto-refresh when dropdown opens
    LaunchedEffect(expanded) {
        if (expanded) {
            onRefresh()
        }
    }
    
    // Sort: Downloaded first, then alphabetically
    val sortedLanguages = remember(languageStatus) {
        languageStatus.sortedWith(compareByDescending<LanguageModelStatus> { it.isDownloaded }.thenBy { it.code })
    }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(selectedLang.uppercase(), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 300.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            if (sortedLanguages.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Diller yükleniyor...") },
                    onClick = {}
                )
            } else {
                sortedLanguages.forEach { status ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = getLanguageName(status.code),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                when {
                                    status.isDownloaded -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "İndirildi",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    downloadState is DownloadState.Downloading && downloadState.langCode == status.code -> {
                                        // Show indeterminate progress
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    else -> {
                                        // Download button
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Download",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "⬇",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            if (status.isDownloaded) {
                                onLangSelected(status.code)
                                expanded = false
                            } else {
                                onDownload(status.code)
                                // Don't close menu to show progress
                            }
                        }
                    )
                }
            }
        }
    }
}

fun isOnWiFi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun getLanguageName(code: String): String {
    return when (code) {
        "en" -> "🇬🇧 English"
        "tr" -> "🇹🇷 Türkçe"
        "de" -> "🇩🇪 Deutsch"
        "fr" -> "🇫🇷 Français"
        "es" -> "🇪🇸 Español"
        "it" -> "🇮🇹 Italiano"
        "ru" -> "🇷🇺 Русский"
        "ar" -> "🇸🇦 العربية"
        "ja" -> "🇯🇵 日本語"
        "zh" -> "🇨🇳 中文"
        else -> code.uppercase()
    }
}
