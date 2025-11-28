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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.wordstore.data.Definition
import com.example.wordstore.data.ExamplePair
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
                Text(
                    text = word.english.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.turkish,
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
                Text(
                    text = word.english.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Divider()
                
                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    if (word.definitions.isNotEmpty()) {
                        items(word.definitions) { definition ->
                            DefinitionView(definition = definition)
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // Fallback if no definitions saved (e.g. manual entry without extra details)
                        item {
                            Text(
                                text = "Meaning: ${word.turkish}",
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
fun DefinitionView(definition: Definition) {
    Column {
        Text(
            text = definition.turkishTranslations.joinToString(", "),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (definition.examples.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Examples:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            definition.examples.forEach { example ->
                ExampleView(example = example)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ExampleView(example: ExamplePair) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
        Text(
            text = "EN: ${example.english}",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 18.sp
        )
        Text(
            text = "TR: ${example.turkish}",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
