package com.opendroid.ai.core.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Continuously restarts SpeechRecognizer in a loop to listen for the wake word.
 *
 * IMPORTANT: SpeechRecognizer plays an audible "beep" tone on the system stream
 * every time startListening()/stopListening() fires. Since this loop restarts
 * every ~1s (after each timeout/error), that beep repeats constantly — this is
 * the "tadang tadang" sound. We mute STREAM_SYSTEM for the lifetime of the wake
 * word loop (not STREAM_MUSIC, so TTS/media playback is unaffected) and only
 * unmute it while a real user query is being captured, so the mic keeps working
 * silently in the background and only becomes audible/visible after the wake
 * word actually fires.
 */
class WakeWordDetector(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var intent: Intent? = null
    private var onWakeWordDetectedCallback: (() -> Unit)? = null
    private var isListening = false
    private var isStreamMuted = false

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val handler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable {
        startSpeechListening()
    }

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    fun startListening(onWakeWordDetected: () -> Unit) {
        if (isListening) return
        this.onWakeWordDetectedCallback = onWakeWordDetected
        isListening = true
        muteSystemStream()
        startSpeechListening()
    }

    private fun startSpeechListening() {
        if (!isListening) return

        // Clean up previous instance before creating a new one
        cleanupRecognizer()

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                scheduleRestart()
                return
            }
        } else {
            scheduleRestart()
            return
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                // Restart listening loop on error or timeout after a delay
                scheduleRestart()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    for (match in matches) {
                        if (match.contains("opendroid", ignoreCase = true) || match.contains("open droid", ignoreCase = true)) {
                            triggerWakeWord()
                            break
                        }
                    }
                }
                scheduleRestart()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    for (match in matches) {
                        if (match.contains("opendroid", ignoreCase = true) || match.contains("open droid", ignoreCase = true)) {
                            triggerWakeWord()
                            break
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            scheduleRestart()
        }
    }

    private fun triggerWakeWord() {
        // Wake word fired — restore normal system sound for the upcoming query capture
        unmuteSystemStream()
        onWakeWordDetectedCallback?.invoke()
    }

    private fun scheduleRestart() {
        handler.removeCallbacks(restartRunnable)
        if (isListening) {
            // Post restart with 1000ms delay to let the audio system settle and prevent rapid flickering/beeping
            handler.postDelayed(restartRunnable, 1000)
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {}
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        speechRecognizer = null
    }

    private fun muteSystemStream() {
        if (isStreamMuted) return
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            isStreamMuted = true
        } catch (e: Exception) {}
    }

    private fun unmuteSystemStream() {
        if (!isStreamMuted) return
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
        } catch (e: Exception) {}
        isStreamMuted = false
    }

    fun stopListening() {
        isListening = false
        handler.removeCallbacks(restartRunnable)
        cleanupRecognizer()
        unmuteSystemStream()
        onWakeWordDetectedCallback = null
    }

    fun destroy() {
        stopListening()
    }
}
