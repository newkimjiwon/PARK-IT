package com.nangmanroad.vehicle.pleos

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/** How one turn at the microphone ended. */
sealed interface Heard {
    data class Words(val text: String) : Heard
    /** The microphone opened but nothing recognisable came back. */
    data object Silence : Heard
    data class Failed(val reason: String) : Heard
}

/**
 * One short utterance at a time through the Pleos SpeechToText SDK in ON_DEVICE mode. The SDK only
 * turns speech into text; deciding what the words mean is up to the app. Without the speech service
 * [ready] stays false, and the screens hide their microphone buttons. As with [VoiceGuide], a service
 * whose on-device model is missing (Pleos Connect Emulator 2.1.0) turns [ready] off after its first
 * ON_DEVICE_ENGINE_ERROR and reports it in [problem].
 */
class VoiceInput(context: Context, private val scope: CoroutineScope) {

    private val stt: SpeechToText? = runCatching { SpeechToText(context.applicationContext, Mode.ON_DEVICE) }
        .onFailure { Log.w(TAG, "SpeechToText unavailable", it) }
        .getOrNull()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    /** Set when the service said it cannot recognise speech here at all; stays for the session. */
    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    /** The words so far, updated while the driver is still talking. */
    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial.asStateFlow()

    private var started = false
    private var registered = false
    private var onHeard: ((Heard) -> Unit)? = null
    private var deadline: Job? = null

    private val results = object : ResultListener {
        override fun onReady() {
            if (_problem.value == null) _ready.value = true
        }

        override fun onStartedRecognition() {
            _listening.value = true
        }

        override fun onUpdated(stt: String, completed: Boolean) {
            _partial.value = stt
            if (completed && stt.isNotBlank()) finish(Heard.Words(stt.trim()))
        }

        override fun onUpdatedEpdData(on: Long, off: Long) = Unit

        override fun onUpdatedRms(rms: Double) = Unit

        override fun onEndedRecognition() {
            finish(_partial.value.trim().takeIf { it.isNotEmpty() }?.let { Heard.Words(it) } ?: Heard.Silence)
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "Recognition failed: $errorType")
            if (errorType in ENGINE_MISSING) {
                _problem.value = "이 차량에는 Pleos 음성 인식 모델이 없어요"
                _ready.value = false
                return finish(Heard.Failed(_problem.value!!))
            }
            finish(if (errorType == ErrorType.TIMEOUT) Heard.Silence else Heard.Failed(errorType.toMessage()))
        }
    }

    fun start() {
        val engine = stt ?: return
        if (started) return
        started = true
        runCatching {
            engine.initialize(
                object : OnInitializeListener {
                    override fun onSuccess() {
                        runCatching {
                            if (!registered) {
                                engine.addListener(results)
                                registered = true
                            }
                            engine.setLocale(Locale.KOREA)
                        }.onFailure { Log.w(TAG, "SpeechToText setup failed", it) }
                        if (_problem.value == null) _ready.value = true
                    }

                    override fun onFailure(errorType: ErrorType) {
                        Log.w(TAG, "SpeechToText init failed: $errorType")
                        started = false
                    }
                },
            )
        }.onFailure {
            Log.w(TAG, "SpeechToText init threw", it)
            started = false
        }
    }

    /** Opens the car microphone for one utterance; [onResult] is called exactly once. */
    fun listen(onResult: (Heard) -> Unit) {
        val engine = stt
        if (engine == null || !_ready.value) {
            onResult(Heard.Failed("이 차량에서는 음성 인식을 쓸 수 없어요"))
            return
        }
        cancel()
        onHeard = onResult
        _partial.value = ""
        _listening.value = true
        runCatching { engine.request() }.onFailure {
            Log.w(TAG, "request failed", it)
            finish(Heard.Failed("마이크를 열지 못했어요"))
            return
        }
        // End-point detection normally closes the turn; this only covers a microphone that never does.
        deadline = scope.launch {
            delay(LISTEN_LIMIT_MS)
            finishListening()
        }
    }

    /** The driver tapped the button again: stop and use what was heard so far. */
    fun finishListening() {
        if (onHeard == null) return
        runCatching { stt?.stop() }
        deadline?.cancel()
        // onEndedRecognition should follow the stop; if the service stays quiet, settle it here.
        deadline = scope.launch {
            delay(END_GRACE_MS)
            finish(_partial.value.trim().takeIf { it.isNotEmpty() }?.let { Heard.Words(it) } ?: Heard.Silence)
        }
    }

    /** Drops the current turn without reporting anything. */
    fun cancel() {
        val wasListening = onHeard != null
        onHeard = null
        deadline?.cancel()
        if (wasListening) runCatching { stt?.stop() }
        _listening.value = false
    }

    fun release() {
        cancel()
        runCatching {
            if (registered) stt?.removeListener(results)
            if (started) stt?.release()
        }.onFailure { Log.w(TAG, "SpeechToText release failed", it) }
        registered = false
        started = false
        _ready.value = false
    }

    private fun finish(heard: Heard) {
        val callback = onHeard ?: return
        onHeard = null
        deadline?.cancel()
        _listening.value = false
        scope.launch { callback(heard) }
    }

    private fun ErrorType.toMessage(): String = when (this) {
        ErrorType.MIC_ERROR, ErrorType.AUDIO_ERROR -> "마이크를 쓸 수 없어요"
        ErrorType.PERMISSION_DENIED -> "음성 인식 권한이 없어요"
        ErrorType.UNAVAILABLE_LANGUAGE -> "한국어 음성 인식을 쓸 수 없어요"
        else -> "음성을 알아듣지 못했어요"
    }

    private companion object {
        const val TAG = "VoiceInput"
        const val LISTEN_LIMIT_MS = 10_000L
        const val END_GRACE_MS = 1_500L
        val ENGINE_MISSING = setOf(ErrorType.ON_DEVICE_ENGINE_ERROR, ErrorType.NOT_SUPPORTED_API, ErrorType.UNAVAILABLE_LANGUAGE)
    }
}
