package ir.khoshkshooyi.assistant.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import ir.khoshkshooyi.assistant.ai.AiProviderType
import ir.khoshkshooyi.assistant.ai.clientFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class TtsPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var androidTts: TextToSpeech? = null
    private var androidTtsReady = false
    private var noPersianVoiceWarned = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        androidTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) androidTtsReady = true
        }
    }

    /** Returns a user-facing warning/error message to surface, or null if playback was clean. */
    suspend fun speak(text: String, provider: AiProviderType, apiKey: String): String? {
        if (text.isBlank()) return null
        if (apiKey.isNotBlank()) {
            try {
                val audio = withContext(Dispatchers.IO) { clientFor(provider).synthesizeSpeech(text, apiKey) }
                playMp3(audio)
                return null
            } catch (e: Exception) {
                val warn = speakWithDeviceVoice(text)
                val cloudMsg = "پخش صدای آنلاین با مشکل مواجه شد (${e.message ?: "خطای نامشخص"})؛ از صدای خود دستگاه استفاده می‌شود."
                return cloudMsg + (warn?.let { " $it" } ?: "")
            }
        }
        return speakWithDeviceVoice(text)
    }

    private suspend fun playMp3(bytes: ByteArray): Unit = suspendCancellableCoroutine { cont ->
        val file = File(context.cacheDir, "tts_${UUID.randomUUID()}.mp3")
        file.writeBytes(bytes)
        var resumed = false
        fun finish() {
            if (resumed) return
            resumed = true
            try { mediaPlayer?.release() } catch (e: Exception) { /* noop */ }
            mediaPlayer = null
            file.delete()
            cont.resume(Unit)
        }
        try {
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ -> finish(); true }
            mp.prepare()
            mp.start()
            val timeoutMs = maxOf(4000L, bytes.size / 16L) // rough duration-based ceiling
            mainHandler.postDelayed({ finish() }, timeoutMs)
            cont.invokeOnCancellation { finish() }
        } catch (e: Exception) {
            finish()
        }
    }

    private suspend fun speakWithDeviceVoice(text: String): String? {
        var warning: String? = null
        val tts = androidTts
        if (tts == null || !androidTtsReady) return null

        val result = tts.setLanguage(Locale("fa", "IR"))
        val hasPersian = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!hasPersian && !noPersianVoiceWarned) {
            noPersianVoiceWarned = true
            warning = "صدای فارسی روی این دستگاه نصب نیست، به همین دلیل ممکنه گفتار درست ادا نشه. از تنظیمات گوشی، بسته‌ی زبان/صدای فارسی رو برای Text-to-Speech نصب کنید."
        }

        suspendCancellableCoroutine<Unit> { cont ->
            val utteranceId = UUID.randomUUID().toString()
            var resumed = false
            fun finish() {
                if (resumed) return
                resumed = true
                cont.resume(Unit)
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { finish() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { finish() }
                override fun onError(utteranceId: String?, errorCode: Int) { finish() }
            })
            mainHandler.postDelayed({ finish() }, maxOf(3000L, text.length * 150L))
            cont.invokeOnCancellation { finish() }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
        return warning
    }

    fun stop() {
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (e: Exception) { /* noop */ }
        mediaPlayer = null
        try { androidTts?.stop() } catch (e: Exception) { /* noop */ }
    }

    fun destroy() {
        stop()
        try { androidTts?.shutdown() } catch (e: Exception) { /* noop */ }
    }
}
