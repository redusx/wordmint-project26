package com.example.wordstore.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var inputWord by remember { mutableStateOf("") }
    var outputWord by remember { mutableStateOf("") }
    
    var sourceLang by remember { mutableStateOf("en") }
    var targetLang by remember { mutableStateOf("tr") }
    
    val searchResult by dictionaryViewModel.searchResult.observeAsState()
    val languageStatus by dictionaryViewModel.languageStatus.observeAsState(initial = emptyList())
    val downloadState by dictionaryViewModel.downloadState.observeAsState()
    val downloadProgress by dictionaryViewModel.downloadProgress.observeAsState(0)

    // Mobile data permission dialog state
    var showDataPermissionDialog by remember { mutableStateOf(false) }
    var pendingDownloadLangCode by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

    // Reset state when entering
    LaunchedEffect(Unit) {
        dictionaryViewModel.resetState()
        dictionaryViewModel.checkLanguageStatus()
    }
    
    // Show download error as snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            is DownloadState.Success -> {
                snackbarHostState.showSnackbar("${state.langCode.uppercase()} dil paketi indirildi")
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
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
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LanguageSelector(
                    selectedLang = sourceLang, 
                    onLangSelected = { sourceLang = it },
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
                    downloadProgress = downloadProgress
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Output Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = outputWord,
                    onValueChange = { outputWord = it },
                    label = { Text("Çeviri") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LanguageSelector(
                    selectedLang = targetLang, 
                    onLangSelected = { targetLang = it },
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
                    downloadProgress = downloadProgress
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translate Button
            Button(
                onClick = {
                    if (inputWord.isNotBlank()) {
                        dictionaryViewModel.searchWord(inputWord, sourceLang, targetLang)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputWord.isNotBlank()
            ) {
                Text("Çevir")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Area
            when (val result = searchResult) {
                is NetworkResult.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is NetworkResult.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
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
                                )
                            ) {
                                Text(
                                    text = "⚠️ ${data.errorMessage}",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Auto-fill output if empty or matching result
                        LaunchedEffect(data) {
                            if (sourceLang == "en" && targetLang == "tr") {
                                if (outputWord.isBlank() || outputWord != data.turkishTranslation) {
                                    outputWord = data.turkishTranslation ?: ""
                                }
                            } else if (sourceLang == "tr" && targetLang == "en") {
                                if (outputWord.isBlank() || outputWord != data.englishWord) {
                                    outputWord = data.englishWord
                                }
                            }
                        }

                        if (!data.translatedDefinitions.isNullOrEmpty()) {
                             Text(
                                text = "Tanımlar:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            data.translatedDefinitions.forEachIndexed { index, def ->
                                Text(
                                    text = "(${def.partOfSpeech})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
                                )
                                Text(
                                    text = "${index + 1}. ${def.primaryDefinition}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                                )
                                Text(
                                    text = "   (${def.secondaryDefinition})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                                )
                            }
                        } else if (!data.definitions.isNullOrEmpty()) {
                            Text(
                                text = "Tanımlar: ${data.englishWord}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            data.definitions.forEach { entry ->
                                entry.meanings?.forEach { meaning ->
                                    Text(
                                        text = "(${meaning.partOfSpeech})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
                                    )
                                    meaning.definitions.forEachIndexed { index, def ->
                                        Text(
                                            text = "${index + 1}. ${def.definition}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                modifier = Modifier.fillMaxWidth(),
                enabled = inputWord.isNotBlank() && outputWord.isNotBlank()
            ) {
                Text("Kelimeyi Kaydet")
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
    downloadProgress: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Sort: Downloaded first, then alphabetically
    val sortedLanguages = remember(languageStatus) {
        languageStatus.sortedWith(compareByDescending<LanguageModelStatus> { it.isDownloaded }.thenBy { it.code })
    }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedLang.uppercase())
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp)
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
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = getLanguageName(status.code),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (status.isDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "İndirildi",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        // Check if this language is currently downloading
                                        val isDownloading = downloadState is DownloadState.Downloading && 
                                                          downloadState.langCode == status.code
                                        
                                        if (isDownloading) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "%$downloadProgress",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                LinearProgressIndicator(
                                                    progress = downloadProgress / 100f,
                                                    modifier = Modifier.width(60.dp).height(4.dp)
                                                )
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { 
                                                    onDownload(status.code)
                                                    expanded = false
                                                }
                                            ) {
                                                Text("İndir", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onClick = {
                            if (status.isDownloaded) {
                                onLangSelected(status.code)
                                expanded = false
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
