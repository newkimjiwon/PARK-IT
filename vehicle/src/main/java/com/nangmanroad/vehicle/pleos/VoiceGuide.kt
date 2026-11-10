package com.nangmanroad.vehicle.pleos

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Speaks through the car's own speakers with the Pleos TextToSpeech SDK. ON_DEVICE mode uses the
 * head unit's engine, so it needs neither a network nor a My Project registration.
 *
 * The speech service can be present while its on-device voice is not: the Pleos Connect Emulator
 * 2.1.0 binds fine but answers every sentence with ON_DEVICE_ENGINE_ERROR because the voice files
 * were never installed. After that answer [ready] turns false for the session and [problem] says
 * why, so the screens drop their speaker buttons instead of offering silence.
 */
class VoiceGuide(context: Context) {

    private val tts: TextToSpeech? = runCatching { TextToSpeech(context.applicationContext, Mode.ON_DEVICE) }
        .onFailure { Log.w(TAG, "TextToSpeech unavailable", it) }
        .getOrNull()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    /** Set when the service said it cannot speak here at all; stays for the rest of the session. */
    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    private var started = false
    private var listening = false

    /** Asked for before the engine came up; only the latest is worth saying. */
    private var pending: String? = null

    /** A stop() we issued to replace one sentence with another, whose onStop must not clear [speaking]. */
    private var replacing = false

    private val events = object : EventListener {
        override fun onReady() {
            markReady()
        }

        override fun onStart() {
            _speaking.value = true
        }

        override fun onDone() {
            _speaking.value = false
        }

        override fun onStop() {
            if (replacing) replacing = false else _speaking.value = false
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "Speech failed: $errorType")
            replacing = false
            _speaking.value = false
            if (errorType in ENGINE_MISSING) {
                _problem.value = "이 차량에는 Pleos 음성 합성 모델이 없어요"
                _ready.value = false
            }
        }

        override fun onUpdatedRms(rms: Double) = Unit
    }

    fun start() {
        val engine = tts ?: return
        if (started) return
        started = true
        runCatching {
            engine.initialize(
                object : OnInitializeListener {
                    override fun onSuccess() {
                        runCatching {
                            if (!listening) {
                                engine.addEventListener(events)
                                listening = true
                            }
                            engine.setLocale(Locale.KOREA)
                        }.onFailure { Log.w(TAG, "TextToSpeech setup failed", it) }
                        markReady()
                    }

                    override fun onFailure(errorType: ErrorType) {
                        Log.w(TAG, "TextToSpeech init failed: $errorType")
                        started = false
                    }
                },
            )
        }.onFailure {
            Log.w(TAG, "TextToSpeech init threw", it)
            started = false
        }
    }

    /** Says [text], cutting off whatever was being said; a newer sentence always wins. */
    fun say(text: String) {
        val engine = tts ?: return
        if (_problem.value != null) return
        if (!_ready.value) {
            pending = text
            return
        }
        runCatching {
            if (_speaking.value) {
                replacing = true
                engine.stop()
            }
            engine.speak(text, true)
            _speaking.value = true
        }.onFailure {
            Log.w(TAG, "speak failed", it)
            _speaking.value = false
        }
    }

    fun stop() {
        pending = null
        replacing = false
        runCatching { tts?.stop() }
        _speaking.value = false
    }

    fun release() {
        runCatching {
            if (listening) tts?.removeEventListener(events)
            if (started) tts?.release()
        }.onFailure { Log.w(TAG, "TextToSpeech release failed", it) }
        listening = false
        started = false
        _ready.value = false
    }

    private fun markReady() {
        // The service says "ready" before every sentence; that does not bring back a missing voice.
        if (_ready.value || _problem.value != null) return
        _ready.value = true
        pending?.let {
            pending = null
            say(it)
        }
    }

    private companion object {
        const val TAG = "VoiceGuide"
        /** Errors that mean the on-device voice is missing or unusable, rather than one bad sentence. */
        val ENGINE_MISSING = setOf(ErrorType.ON_DEVICE_ENGINE_ERROR, ErrorType.NOT_SUPPORTED_API, ErrorType.UNAVAILABLE_LANGUAGE)
    }
}
