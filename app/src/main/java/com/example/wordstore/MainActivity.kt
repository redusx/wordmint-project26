package com.example.wordstore

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.wordstore.viewmodel.DictionaryViewModel
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.WordStoreTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wordstore.ui.screens.home.HomeScreen
import com.example.wordstore.ui.screens.addword.AddWordScreen
import com.example.wordstore.viewmodel.WordViewModel
import com.example.wordstore.viewmodel.WordViewModelFactory


class MainActivity : AppCompatActivity() {

    private val dictionaryViewModel: DictionaryViewModel by viewModels()
    private val wordViewModel: WordViewModel by viewModels {
        WordViewModelFactory((application as WordStoreApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WordStoreTheme {
                MainScreen(
                    wordViewModel = wordViewModel,
                    dictionaryViewModel = dictionaryViewModel
                )
            }
        }
    }

    @Composable
    fun MainScreen(
        wordViewModel: WordViewModel,
        dictionaryViewModel: DictionaryViewModel
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
                    dictionaryViewModel = dictionaryViewModel
                )
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