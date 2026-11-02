package com.example.wordstore.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.R
import com.example.wordstore.audio.PlaybackStatus
import com.example.wordstore.audio.PronunciationPlayer
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Sizes
import com.example.wordstore.ui.theme.WordMintTheme

val LocalPronunciationPlayer = compositionLocalOf<PronunciationPlayer?> { null }

@Composable
fun PronounceButton(
    term: String,
    lang: String,
    modifier: Modifier = Modifier,
    audioUrl: String? = null
) {
    val player = LocalPronunciationPlayer.current ?: return
    val playback by player.playback.collectAsStateWithLifecycle()
    val isThisTerm = playback.key == PronunciationPlayer.keyOf(term, lang)
    val status = if (isThisTerm) playback.status else PlaybackStatus.IDLE
    val colors = WordMintTheme.colors

    if (status == PlaybackStatus.LOADING) {
        Box(modifier.size(Sizes.touchTarget), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(Sizes.iconSmall), color = colors.primary, strokeWidth = 2.dp)
        }
    } else {
        IconAction(
            icon = if (status == PlaybackStatus.FAILED) AppIcons.SpeakerOff else AppIcons.Speaker,
            description = stringResource(
                if (status == PlaybackStatus.FAILED) R.string.pronounce_unavailable else R.string.pronounce_word,
                term
            ),
            onClick = { player.play(term, lang, audioUrl) },
            modifier = modifier,
            tint = when (status) {
                PlaybackStatus.PLAYING -> colors.primary
                PlaybackStatus.FAILED -> colors.danger
                else -> colors.inkSecondary
            }
        )
    }
}
