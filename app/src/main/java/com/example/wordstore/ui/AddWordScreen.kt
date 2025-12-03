package com.example.wordstore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wordstore.data.MeaningDto
import com.example.wordstore.data.NetworkResult
import com.example.wordstore.data.Word
import com.example.wordstore.viewmodel.DictionaryViewModel
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

    // Reset state when entering
    LaunchedEffect(Unit) {
        dictionaryViewModel.resetState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Word") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                    label = { Text("Input Word") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LanguageSelector(selectedLang = sourceLang, onLangSelected = { sourceLang = it })
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
                    label = { Text("Output / Translation") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LanguageSelector(selectedLang = targetLang, onLangSelected = { targetLang = it })
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
                Text("Translate")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Area
            when (val result = searchResult) {
                is NetworkResult.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is NetworkResult.Error -> {
                    Text(
                        text = result.message ?: "Unknown Error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is NetworkResult.Success -> {
                    val data = result.data
                    if (data != null) {
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
                                text = "Definitions (Translated):",
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
                                text = "Definitions for: ${data.englishWord}",
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
                            // Assuming TR input
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
                Text("Save Word")
            }
        }
    }
}

@Composable
fun LanguageSelector(selectedLang: String, onLangSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("en" to "EN", "tr" to "TR")

    Box {
        Button(onClick = { expanded = true }) {
            Text(languages.find { it.first == selectedLang }?.second ?: selectedLang)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onLangSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
