package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechRecognitionManager(
    private val context: Context,
    private val alertManager: HapticAndVisualAlertManager
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isContinuous: Boolean = false
    private var currentLanguage: String = "ar-SA"
    private var currentSpeaker: String = "المتحدث"

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _statusMessage = MutableStateFlow("جاهز للاستماع")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    var vibrationAlertsEnabled = true
    var screenFlashAlertsEnabled = true

    private val scope = CoroutineScope(Dispatchers.Main)
    private var restartJob: Job? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } else {
            _statusMessage.value = "التعرف على الصوت غير مدعوم على هذا الجهاز"
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _statusMessage.value = "يستمع الآن… تحدّث بوضوح"
            }

            override fun onBeginningOfSpeech() {
                _statusMessage.value = "تم رصد كلام… يجري التحويل"
                alertManager.triggerSpeechStartAlert(vibrationAlertsEnabled, screenFlashAlertsEnabled)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB typically ranges from -2dB to ~10dB
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _soundLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _soundLevel.value = 0f
                alertManager.triggerSpeechEndAlert(vibrationAlertsEnabled)
            }

            override fun onError(error: Int) {
                _soundLevel.value = 0f
                val (msg, canRecover) = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التقاط كلام واضح" to true
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "توقف الكلام مؤقتًا" to true
                    SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت" to true
                    SpeechRecognizer.ERROR_CLIENT -> "خطأ في محرك التعرف" to true
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يتطلب إذن الميكروفون" to false
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطأ في الاتصال بالشبكة" to true
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "محرك الصوت مشغول، جارٍ إعادة التعيين" to true
                    else -> "تنبيه: محرك الصوت قيد الانتظار" to true
                }

                _statusMessage.value = msg

                if (isContinuous && canRecover) {
                    scheduleRestart()
                } else if (!canRecover) {
                    _isListening.value = false
                }
            }

            override fun onResults(results: Bundle?) {
                _soundLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0].trim()
                    if (text.isNotEmpty()) {
                        val prefix = if (currentSpeaker.isNotBlank()) "[$currentSpeaker]: " else ""
                        val newBlock = "$prefix$text"
                        val current = _fullTranscript.value
                        _fullTranscript.value = if (current.isBlank()) newBlock else "$current\n$newBlock"
                    }
                }
                _partialTranscript.value = ""

                if (isContinuous) {
                    scheduleRestart()
                } else {
                    _isListening.value = false
                    _statusMessage.value = "تم الانتهاء من الاستماع"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0]
                    _partialTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(
        language: String = currentLanguage,
        speaker: String = currentSpeaker,
        continuous: Boolean = true
    ) {
        currentLanguage = language
        currentSpeaker = speaker
        isContinuous = continuous
        restartJob?.cancel()

        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            _statusMessage.value = "يستمع الآن… تحدث بوضوح"
        } catch (_: Exception) {
            _statusMessage.value = "تعذر بدء الميكروفون"
            _isListening.value = false
        }
    }

    fun stopListening() {
        isContinuous = false
        restartJob?.cancel()
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _soundLevel.value = 0f
        _partialTranscript.value = ""
        _statusMessage.value = "متوقف"
    }

    fun clearTranscript() {
        _fullTranscript.value = ""
        _partialTranscript.value = ""
    }

    fun setSpeaker(speaker: String) {
        currentSpeaker = speaker
    }

    private fun scheduleRestart() {
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(400)
            if (isContinuous) {
                try {
                    speechRecognizer?.cancel()
                    startListening(currentLanguage, currentSpeaker, true)
                } catch (_: Exception) {
                    initRecognizer()
                    startListening(currentLanguage, currentSpeaker, true)
                }
            }
        }
    }

    fun destroy() {
        isContinuous = false
        restartJob?.cancel()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }
}
