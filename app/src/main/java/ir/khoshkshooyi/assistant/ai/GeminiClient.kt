package ir.khoshkshooyi.assistant.ai

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object GeminiClient : AiClient {

    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    private const val CHAT_MODEL = "gemini-2.5-flash"
    private const val TTS_MODEL = "gemini-2.5-flash-preview-tts"
    private const val TTS_VOICE = "Kore"

    private fun openConn(path: String, apiKey: String): HttpURLConnection {
        val conn = URL("$BASE$path?key=${apiKey.trim()}").openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        return conn
    }

    private fun errorMessageFrom(conn: HttpURLConnection): String {
        return try {
            val err = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            if (err.isNullOrBlank()) "خطای نامشخص از سرویس"
            else JSONObject(err).optJSONObject("error")?.optString("message") ?: err
        } catch (e: Exception) {
            "خطای نامشخص از سرویس"
        }
    }

    private fun postJson(path: String, apiKey: String, body: JSONObject): JSONObject {
        val conn = openConn(path, apiKey)
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            if (conn.responseCode !in 200..299) throw AiClientException(errorMessageFrom(conn))
            val text = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    /** Converts the provider-neutral {role: "user"|"assistant", content} history into Gemini's contents shape. */
    private fun toGeminiContents(history: JSONArray, userText: String): JSONArray {
        val contents = JSONArray()
        for (i in 0 until history.length()) {
            val entry = history.getJSONObject(i)
            val role = if (entry.optString("role") == "assistant") "model" else "user"
            val parts = JSONArray().put(JSONObject().put("text", entry.optString("content", "")))
            contents.put(JSONObject().put("role", role).put("parts", parts))
        }
        val userParts = JSONArray().put(JSONObject().put("text", userText))
        contents.put(JSONObject().put("role", "user").put("parts", userParts))
        return contents
    }

    override fun chat(history: JSONArray, userText: String, apiKey: String): Pair<ParsedAIResult, JSONArray> {
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            put("contents", toGeminiContents(history, userText))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("responseMimeType", "application/json")
            })
        }
        val data = postJson("/models/$CHAT_MODEL:generateContent", apiKey, body)
        val candidate = data.optJSONArray("candidates")?.optJSONObject(0)
        val text = candidate?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""

        val updatedHistory = JSONArray()
        for (i in 0 until history.length()) updatedHistory.put(history.getJSONObject(i))
        updatedHistory.put(JSONObject().put("role", "user").put("content", userText))
        updatedHistory.put(JSONObject().put("role", "assistant").put("content", text))

        val result = parseAiJson(text)
        return result to updatedHistory
    }

    /** Calls Gemini's TTS-capable generateContent endpoint and returns a ready-to-play WAV file. */
    override fun synthesizeSpeech(text: String, apiKey: String): ByteArray {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))
            ))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().put(
                    "voiceConfig", JSONObject().put(
                        "prebuiltVoiceConfig", JSONObject().put("voiceName", TTS_VOICE)
                    )
                ))
            })
        }
        val data = postJson("/models/$TTS_MODEL:generateContent", apiKey, body)
        val part = data.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
        val inlineData = part?.optJSONObject("inlineData")
        val b64 = inlineData?.optString("data", "") ?: ""
        if (b64.isBlank()) throw AiClientException("پاسخ صوتی خالی از سرویس دریافت شد")
        val mimeType = inlineData?.optString("mimeType", "") ?: ""
        val sampleRate = Regex("rate=(\\d+)").find(mimeType)?.groupValues?.get(1)?.toIntOrNull() ?: 24000
        val pcm = Base64.decode(b64, Base64.DEFAULT)
        return wrapPcmAsWav(pcm, sampleRate)
    }

    /** Gemini's TTS returns raw 16-bit mono PCM; wrap it in a minimal WAV header so MediaPlayer can play it. */
    private fun wrapPcmAsWav(pcm: ByteArray, sampleRate: Int, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val out = ByteArrayOutputStream(44 + pcm.size)
        fun writeLeInt(v: Int) {
            out.write(v and 0xff); out.write((v shr 8) and 0xff); out.write((v shr 16) and 0xff); out.write((v shr 24) and 0xff)
        }
        fun writeLeShort(v: Int) {
            out.write(v and 0xff); out.write((v shr 8) and 0xff)
        }
        out.write("RIFF".toByteArray(StandardCharsets.US_ASCII))
        writeLeInt(36 + pcm.size)
        out.write("WAVE".toByteArray(StandardCharsets.US_ASCII))
        out.write("fmt ".toByteArray(StandardCharsets.US_ASCII))
        writeLeInt(16) // PCM fmt chunk size
        writeLeShort(1) // audio format = PCM
        writeLeShort(channels)
        writeLeInt(sampleRate)
        writeLeInt(byteRate)
        writeLeShort(blockAlign)
        writeLeShort(bitsPerSample)
        out.write("data".toByteArray(StandardCharsets.US_ASCII))
        writeLeInt(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }

    /** Cheap request used to confirm the key works before starting a voice session. */
    override fun validateApiKey(apiKey: String) {
        val conn = openConn("/models", apiKey)
        try {
            conn.requestMethod = "GET"
            if (conn.responseCode !in 200..299) throw AiClientException(errorMessageFrom(conn))
        } finally {
            conn.disconnect()
        }
    }
}
