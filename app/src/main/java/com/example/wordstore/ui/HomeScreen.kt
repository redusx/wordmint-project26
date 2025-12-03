package com.example.wordstore.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.wordstore.data.MeaningDto
import com.example.wordstore.data.Word
import com.example.wordstore.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    wordViewModel: WordViewModel
) {
    val allWords by wordViewModel.allWords.observeAsState(initial = emptyList())
    var selectedWord by remember { mutableStateOf<Word?>(null) }

    if (selectedWord != null) {
        WordDetailDialog(word = selectedWord!!, onDismiss = { selectedWord = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Word Notebook") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_word") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Word")
            }
        }
    ) { paddingValues ->
        if (allWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No words saved yet. Add one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(allWords) { word ->
                    WordCard(
                        word = word,
                        onClick = { selectedWord = word },
                        onDelete = { wordViewModel.delete(word) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WordCard(
    word: Word,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val title = if (word.sourceLang == "tr") word.turkish else word.english
                val subtitle = if (word.sourceLang == "tr") word.english else word.turkish
                
                Text(
                    text = title.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Word",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun WordDetailDialog(word: Word, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f) // Occupy 80% of screen height
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val title = if (word.sourceLang == "tr") word.turkish else word.english
                val subtitle = if (word.sourceLang == "tr") word.english else word.turkish
                val subtitleLabel = if (word.sourceLang == "tr") "EN" else "TR"

                Text(
                    text = title.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "$subtitleLabel: $subtitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (!word.phonetic.isNullOrBlank()) {
                    Text(
                        text = "Phonetic: ${word.phonetic}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Divider()
                
                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    if (word.translatedDefinitions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Definitions (Translated):",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(word.translatedDefinitions) { def ->
                            Column {
                                Text(
                                    text = "(${def.partOfSpeech})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• ${def.primaryDefinition}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "   (${def.secondaryDefinition})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    } else if (word.meanings.isNotEmpty()) {
                        items(word.meanings) { meaning ->
                            MeaningView(meaning = meaning)
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // Fallback if no definitions saved
                        item {
                            Text(
                                text = "No definitions available.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun MeaningView(meaning: MeaningDto) {
    Column {
        Text(
            text = "(${meaning.partOfSpeech})",
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        meaning.definitions.forEachIndexed { index, def ->
            Text(
                text = "${index + 1}. ${def.definition}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal
            )
            if (!def.example.isNullOrBlank()) {
                Text(
                    text = "Ex: ${def.example}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
