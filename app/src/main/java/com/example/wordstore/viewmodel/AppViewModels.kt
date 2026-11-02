package com.example.wordstore.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.wordstore.AppContainer
import com.example.wordstore.WordStoreApplication
import java.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Builds a ViewModel from the application's [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer {
            create((this[APPLICATION_KEY] as WordStoreApplication).container)
        }
    }
)

/** Time as a stream, so that "due" and "today" stay true on an open screen. */
fun Clock.ticks(periodMillis: Long = 30_000L): Flow<Long> = flow {
    while (true) {
        emit(millis())
        delay(periodMillis)
    }
}
