package ir.khoshkshooyi.assistant.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VoiceRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    // شناسه‌ی سرویس تشخیص گفتار آنلاین گوگل. اگر روی گوشی نصب باشد (تقریباً همیشه هست،
    // چون از طریق اپ گوگل / GMS می‌آید)، صراحتاً همین را صدا می‌زنیم تا تشخیص گفتار
    // «آنلاین» و مبتنی بر ابر انجام شود، نه موتور آفلاینِ گاهی محدود گوشی که ممکن است
    // بسته‌ی زبان فارسی‌اش نصب نباشد. اگر پیدا نشود، به سرویس پیش‌فرض گوشی برمی‌گردیم.
    private val googleRecognitionService = ComponentName(
        "com.google.android.googlequicksearchbox",
        "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
    )

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private fun resolveOnlineComponent(): ComponentName? {
        val pm = context.packageManager
        val intent = Intent("android.speech.RecognitionService")
        val services = try {
            pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }
        val hasGoogleService = services.any { it.serviceInfo?.packageName == googleRecognitionService.packageName }
        return if (hasGoogleService) googleRecognitionService else null
    }

    private fun createRecognizer(): SpeechRecognizer {
        val onlineComponent = resolveOnlineComponent()
        return if (onlineComponent != null) {
            SpeechRecognizer.createSpeechRecognizer(context, onlineComponent)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    /**
     * Runs one listen-and-transcribe turn. Must be called from the main thread.
     * Resolves with the recognized text, or "" on silence / permission / any other error.
     * onError receives a Persian, user-facing message for real failures (not plain silence).
     */
    suspend fun listenOnce(
        onPartial: (String) -> Unit,
        onError: (String) -> Unit = {}
    ): String = suspendCancellableCoroutine { cont ->
        stop()
        val rec = createRecognizer()
        recognizer = rec

        var resumed = false
        fun finish(result: String) {
            if (resumed) return
            resumed = true
            rec.destroy()
            if (recognizer === rec) recognizer = null
            cont.resume(result)
        }

        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                when (error) {
                    // سکوت یا عدم تطبیق: خطای واقعی نیست، فقط یک نوبت خالی است.
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {}

                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                        onError("تشخیص گفتار فارسی روی این گوشی در دسترس نیست. لطفاً از تنظیمات گوشی، در برنامه‌ی «Google» یا «Google Voice Typing»، زبان فارسی را برای تشخیص گفتار آنلاین فعال/دانلود کنید، یا برنامه‌ی Google را به‌روزرسانی کنید.")

                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        onError("اتصال اینترنت برقرار نیست یا ضعیف است. تشخیص گفتار به اینترنت نیاز دارد.")

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        onError("دسترسی میکروفون فعال نیست. آن را در تنظیمات گوشی برای این برنامه فعال کنید.")

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        onError("تشخیص‌گر گفتار مشغول است، لطفاً چند لحظه دیگر دوباره امتحان کنید.")

                    SpeechRecognizer.ERROR_CLIENT ->
                        onError("سرویس تشخیص گفتار گوگل روی این گوشی نصب یا فعال نیست. لطفاً مطمئن شوید اپ «Google» نصب و به‌روز است.")

                    else ->
                        onError("مشکلی در تشخیص گفتار پیش آمد (کد $error). دوباره امتحان کنید.")
                }
                finish("")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                finish(matches?.firstOrNull()?.trim() ?: "")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let(onPartial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // صراحتاً از موتور آفلاین/محلی گوشی استفاده نکن؛ همیشه از سرویس آنلاین
            // (که زبان فارسی را بدون نیاز به بسته‌ی زبان نصب‌شده پشتیبانی می‌کند) استفاده کن.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        cont.invokeOnCancellation { finish("") }

        try {
            rec.startListening(intent)
        } catch (e: Exception) {
            onError("راه‌اندازی تشخیص گفتار با خطا مواجه شد: ${e.message ?: "نامشخص"}")
            finish("")
        }
    }

    fun stop() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (e: Exception) { /* noop */ }
        recognizer = null
    }
}
