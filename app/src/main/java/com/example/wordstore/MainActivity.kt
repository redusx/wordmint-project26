package com.example.wordstore

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.wordstore.viewmodel.TranslationViewModel
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordstore.data.Definition
import com.example.wordstore.data.ExamplePair
import com.example.wordstore.data.FetchState
import com.example.wordstore.data.TranslationResult
import com.example.wordstore.data.WordEntry
import com.example.wordstore.ui.theme.WordStoreTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wordstore.ui.HomeScreen
import com.example.wordstore.ui.AddWordScreen
import com.example.wordstore.viewmodel.WordViewModel
import com.example.wordstore.viewmodel.WordViewModelFactory


class MainActivity : AppCompatActivity() {

    private val translationViewModel: TranslationViewModel by viewModels()
    private val wordViewModel: WordViewModel by viewModels {
        WordViewModelFactory((application as WordStoreApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WordStoreTheme {
                MainScreen(
                    wordViewModel = wordViewModel,
                    translationViewModel = translationViewModel
                )
            }
        }
    }

    @Composable
    fun MainScreen(
        wordViewModel: WordViewModel,
        translationViewModel: TranslationViewModel
    ) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController = navController, wordViewModel = wordViewModel)
            }
            composable("add_word") {
                AddWordScreen(
                    navController = navController,
                    wordViewModel = wordViewModel,
                    translationViewModel = translationViewModel
                )
            }
        }
    }

    @Composable
    fun TranslationResultView(result: TranslationResult) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Text(
                    text = "Word: ${result.word.replaceFirstChar { it.uppercase() }}", // İlk harfi büyük yap
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(result.entries) { entry ->
                WordEntryView(entry = entry)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    @Composable
    fun WordEntryView(entry: WordEntry) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = "(${entry.partOfSpeech})",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            entry.definitions.forEachIndexed { index, definition ->
                DefinitionView(definition = definition, index = index + 1)
                if (index < entry.definitions.size - 1) { // Son tanım değilse boşluk bırak
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    @Composable
    fun DefinitionView(definition: Definition, index: Int) {
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "$index. ${definition.turkishTranslations.joinToString(", ")}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (definition.examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Examples:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                definition.examples.forEach { example ->
                    ExampleView(example = example)
                }
            }
        }
    }

    @Composable
    fun ExampleView(example: ExamplePair) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
            Text(
                text = "EN: ${example.english}",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp // Okunabilirliği artırmak için satır yüksekliği
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

    // Preview için (isteğe bağlı)
    @OptIn(ExperimentalMaterial3Api::class)
    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        // Örnek veri ile ViewModel oluşturmak yerine direkt Composable'ı çağırabiliriz
        // Veya sahte bir ViewModel ile de test edilebilir
        val sampleResult = TranslationResult(
            word = "Hello",
            entries = listOf(
                WordEntry(
                    partOfSpeech = "exclamation",
                    definitions = listOf(
                        Definition(
                            turkishTranslations = listOf("merhaba!", "selam!"),
                            examples = listOf(
                                ExamplePair("Hello, John! How are you?", "Merhaba John! Nasılsın?")
                            )
                        )
                    )
                ),
                WordEntry(
                    partOfSpeech = "noun",
                    definitions = listOf(
                        Definition(
                            turkishTranslations = listOf("merhaba (deme)", "selam (deme)"),
                            examples = listOf(
                                ExamplePair("She gave a friendly hello.", "Dostça bir merhaba dedi.")
                            )
                        )
                    )
                )
            )
        )

        WordStoreTheme {
            Scaffold(topBar = { TopAppBar(title = { Text("Preview Translator") }) }) { padding ->
                Column(Modifier.padding(padding).padding(16.dp)) {
                    OutlinedTextField(value = "hello", onValueChange = {}, label = { Text("Enter word") })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { }) { Text("Translate") }
                    Spacer(Modifier.height(16.dp))
                    TranslationResultView(result = sampleResult)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview(showBackground = true)
    @Composable
    fun LoadingPreview() {
        WordStoreTheme {
            Scaffold(topBar = { TopAppBar(title = { Text("Preview Translator") }) }) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview(showBackground = true)
    @Composable
    fun ErrorPreview() {
        WordStoreTheme {
            Scaffold(topBar = { TopAppBar(title = { Text("Preview Translator") }) }) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Error: Network connection failed.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

}