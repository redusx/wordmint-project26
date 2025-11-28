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
import com.example.wordstore.data.FetchState
import com.example.wordstore.data.Word
import com.example.wordstore.viewmodel.TranslationViewModel
import com.example.wordstore.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    navController: NavController,
    wordViewModel: WordViewModel,
    translationViewModel: TranslationViewModel
) {
    var englishWord by remember { mutableStateOf("") }
    var turkishWord by remember { mutableStateOf("") }
    
    val translationState by translationViewModel.translationState.observeAsState()

    // Reset translation state when entering the screen
    LaunchedEffect(Unit) {
        translationViewModel.resetState()
    }

    // Update turkishWord when translation is successful
    LaunchedEffect(translationState) {
        if (translationState is FetchState.Success) {
            val result = (translationState as FetchState.Success).result
            // Take the first translation of the first definition of the first entry as a default
            val firstTranslation = result.entries.firstOrNull()?.definitions?.firstOrNull()?.turkishTranslations?.firstOrNull()
            if (firstTranslation != null) {
                turkishWord = firstTranslation
            }
        }
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
            OutlinedTextField(
                value = englishWord,
                onValueChange = { englishWord = it },
                label = { Text("English Word") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (englishWord.isNotBlank()) {
                        translationViewModel.getTranslation(englishWord)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = englishWord.isNotBlank()
            ) {
                Text("Translate (Auto-fill)")
            }

            if (translationState is FetchState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = turkishWord,
                onValueChange = { turkishWord = it },
                label = { Text("Turkish Translation") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (englishWord.isNotBlank() && turkishWord.isNotBlank()) {
                        val currentTranslationState = translationState
                        val definitions = if (currentTranslationState is FetchState.Success) {
                            currentTranslationState.result.entries.flatMap { it.definitions }
                        } else {
                            emptyList()
                        }
                        
                        val examples = definitions.flatMap { it.examples }

                        val word = Word(
                            english = englishWord.trim(),
                            turkish = turkishWord.trim(),
                            definitions = definitions,
                            examples = examples
                        )
                        wordViewModel.insert(word)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = englishWord.isNotBlank() && turkishWord.isNotBlank()
            ) {
                Text("Save Word")
            }
        }
    }
}
