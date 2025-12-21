package com.example.wordstore.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wordstore.ui.theme.GlassColors
import com.example.wordstore.ui.theme.WordStoreIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** State enum for pronunciation playback. */
private enum class PlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    ERROR
}

/**
 * Generates the Google Dictionary audio URL for a given English word. Only works for single,
 * lowercase English words.
 *
 * @param word The English word to get pronunciation for
 * @return The audio URL for the word
 */
fun getPronunciationUrl(word: String): String {
    val cleanWord = word.lowercase().trim().replace(" ", "")
    return "https://ssl.gstatic.com/dictionary/static/sounds/20200429/$cleanWord--_gb_1.mp3"
}

/**
 * A composable button that plays pronunciation audio for the given English word. Uses Android
 * MediaPlayer to stream MP3 audio from Google Dictionary API.
 *
 * @param word The English word to pronounce
 * @param modifier Modifier for the button
 * @param size Size of the button
 */
@Composable
fun PronunciationButton(word: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    var playbackState by remember { mutableStateOf(PlaybackState.IDLE) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.9f else 1f,
                    animationSpec = tween(100),
                    label = "pronunciation_scale"
            )

    // Cleanup on dispose
    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }

    val iconTint =
            when (playbackState) {
                PlaybackState.IDLE -> Color.White.copy(alpha = 0.6f)
                PlaybackState.LOADING -> GlassColors.Accent
                PlaybackState.PLAYING -> GlassColors.Accent
                PlaybackState.ERROR -> GlassColors.Error.copy(alpha = 0.6f)
            }

    Box(
            modifier =
                    modifier.size(size)
                            .scale(scale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = playbackState != PlaybackState.LOADING
                            ) {
                                scope.launch {
                                    // Stop current playback if any
                                    mediaPlayer?.apply {
                                        if (isPlaying) {
                                            stop()
                                        }
                                        release()
                                    }
                                    mediaPlayer = null

                                    playbackState = PlaybackState.LOADING

                                    try {
                                        val url = getPronunciationUrl(word)
                                        val player =
                                                withContext(Dispatchers.IO) {
                                                    MediaPlayer().apply {
                                                        setAudioAttributes(
                                                                AudioAttributes.Builder()
                                                                        .setContentType(
                                                                                AudioAttributes
                                                                                        .CONTENT_TYPE_SPEECH
                                                                        )
                                                                        .setUsage(
                                                                                AudioAttributes
                                                                                        .USAGE_MEDIA
                                                                        )
                                                                        .build()
                                                        )
                                                        setDataSource(url)
                                                        prepare()
                                                    }
                                                }

                                        mediaPlayer = player

                                        player.setOnCompletionListener {
                                            playbackState = PlaybackState.IDLE
                                            player.release()
                                            mediaPlayer = null
                                        }

                                        player.setOnErrorListener { _, _, _ ->
                                            playbackState = PlaybackState.ERROR
                                            player.release()
                                            mediaPlayer = null
                                            true
                                        }

                                        withContext(Dispatchers.Main) {
                                            playbackState = PlaybackState.PLAYING
                                            player.start()
                                        }
                                    } catch (e: Exception) {
                                        playbackState = PlaybackState.ERROR
                                        // Reset to idle after showing error briefly
                                        kotlinx.coroutines.delay(1500)
                                        playbackState = PlaybackState.IDLE
                                    }
                                }
                            }
                            .semantics {
                                contentDescription = "$word kelimesinin telaffuzunu dinle"
                                role = Role.Button
                            },
            contentAlignment = Alignment.Center
    ) {
        when (playbackState) {
            PlaybackState.LOADING -> {
                CircularProgressIndicator(
                        modifier = Modifier.size(size * 0.5f),
                        color = GlassColors.Accent,
                        strokeWidth = 2.dp
                )
            }
            else -> {
                Icon(
                        imageVector = WordStoreIcons.VolumeUp,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}
