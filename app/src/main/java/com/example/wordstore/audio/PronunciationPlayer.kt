package com.example.wordstore.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackStatus { IDLE, LOADING, PLAYING, FAILED }

data class PlaybackState(val key: String? = null, val status: PlaybackStatus = PlaybackStatus.IDLE)

/**
 * Plays a recorded pronunciation when one exists and otherwise uses the device's speech engine.
 *
 * The recording host only has common English words (`book` works, `resilient` returns 404), and
 * nothing for other languages, so speech synthesis is what makes every term pronounceable.
 */
class PronunciationPlayer(private val context: Context) {
    private val state = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = state.asStateFlow()

    private var player: MediaPlayer? = null
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var pending: (() -> Unit)? = null

    fun play(term: String, lang: String, audioUrl: String? = null) {
        val key = keyOf(term, lang)
        stop()
        state.value = PlaybackState(key, PlaybackStatus.LOADING)
        val url = audioUrl?.takeIf(String::isNotBlank) ?: recordingUrl(term, lang)
        if (url != null) playRecording(url, term, lang, key) else speak(term, lang, key)
    }

    fun stop() {
        player?.release()
        player = null
        speech?.stop()
        state.value = PlaybackState()
    }

    private fun recordingUrl(term: String, lang: String): String? {
        val word = term.trim().lowercase(Locale.ROOT)
        if (lang != "en" || word.isEmpty() || !word.all { it in 'a'..'z' }) return null
        return "https://ssl.gstatic.com/dictionary/static/sounds/20200429/$word--_gb_1.mp3"
    }

    private fun playRecording(url: String, term: String, lang: String, key: String) {
        val media = MediaPlayer()
        player = media
        try {
            media.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            media.setDataSource(url)
            media.setOnPreparedListener {
                if (player === media) {
                    state.value = PlaybackState(key, PlaybackStatus.PLAYING)
                    media.start()
                }
            }
            media.setOnCompletionListener {
                if (player === media) {
                    media.release()
                    player = null
                    state.value = PlaybackState()
                }
            }
            media.setOnErrorListener { _, _, _ ->
                if (player === media) {
                    media.release()
                    player = null
                    speak(term, lang, key)
                }
                true
            }
            media.prepareAsync()
        } catch (_: Exception) {
            media.release()
            player = null
            speak(term, lang, key)
        }
    }

    private fun speak(term: String, lang: String, key: String) {
        val engine = speech
        if (engine != null && speechReady) {
            utter(engine, term, lang, key)
            return
        }
        pending = { speech?.let { utter(it, term, lang, key) } }
        if (engine == null) {
            speech = TextToSpeech(context.applicationContext) { status ->
                speechReady = status == TextToSpeech.SUCCESS
                if (speechReady) {
                    speech?.setOnUtteranceProgressListener(listener)
                    pending?.invoke()
                } else {
                    state.value = PlaybackState(key, PlaybackStatus.FAILED)
                }
                pending = null
            }
        }
    }

    private fun utter(engine: TextToSpeech, term: String, lang: String, key: String) {
        val availability = engine.setLanguage(Locale(lang))
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            state.value = PlaybackState(key, PlaybackStatus.FAILED)
            return
        }
        state.value = PlaybackState(key, PlaybackStatus.PLAYING)
        if (engine.speak(term, TextToSpeech.QUEUE_FLUSH, null, key) != TextToSpeech.SUCCESS) {
            state.value = PlaybackState(key, PlaybackStatus.FAILED)
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            if (state.value.key == utteranceId) state.value = PlaybackState()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (state.value.key == utteranceId) state.value = PlaybackState(utteranceId, PlaybackStatus.FAILED)
        }
    }

    companion object {
        fun keyOf(term: String, lang: String) = "$lang:$term"
    }
}
