package sk.scednote.events

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.model.Command

class Voice : RecognitionListener {
    private var result = ""
    private var error = ""
    private var handle: (String) -> Unit = fun(_){}
    private var cmd: Command? = null
    private var commit: (Command?, String) -> Unit = fun(_, _) {}

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onEndOfSpeech() {}

    override fun onError(err: Int) {
        error = ScedNoteApp.res.getString(when (err) {
            ERROR_SERVER -> R.string.voice_err_server
            ERROR_AUDIO -> R.string.voice_err_audio
            ERROR_CLIENT -> R.string.voice_err_client
            ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_err_permissions
            ERROR_NETWORK -> R.string.voice_err_network
            ERROR_NETWORK_TIMEOUT -> R.string.voice_err_network
            ERROR_NO_MATCH -> R.string.voice_err_no_match
            ERROR_RECOGNIZER_BUSY -> R.string.voice_err_busy
            ERROR_SPEECH_TIMEOUT -> R.string.time_out
            else -> R.string.voice_not_captured
        })
        handle(error)
    }
    override fun onResults(results: Bundle?) {
        result = results?.getStringArrayList(RESULTS_RECOGNITION)?.get(0) ?: ""
        cmd = Command.identifyCommand(result)
        commit(cmd, result)
    }

    fun setUpCommand(com: (Command?, String)->Unit) {
        commit = com
    }

    fun handleError(fn: (String)->Unit) {
        handle = fn
    }
}