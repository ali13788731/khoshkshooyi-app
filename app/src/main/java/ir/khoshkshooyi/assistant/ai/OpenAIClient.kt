package ir.khoshkshooyi.assistant.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object OpenAIClient : AiClient {

    private const val BASE = "https://api.openai.com/v1"
    private const val CHAT_MODEL = "gpt-4o-mini"
    private const val TTS_MODEL = "gpt-4o-mini-tts"
    private const val TTS_VOICE = "shimmer"

    private fun openConn(path: String, apiKey: String): HttpURLConnection {
        val conn = URL("$BASE$path").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
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

    private fun postJsonForBytes(path: String, apiKey: String, body: JSONObject): ByteArray {
        val conn = openConn(path, apiKey)
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body.toString()) }
            if (conn.responseCode !in 200..299) throw AiClientException(errorMessageFrom(conn))
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Sends the running conversation (provider-neutral history of {role, content} messages, no
     * system entry) to ChatGPT and parses the structured JSON reply. Returns the parsed result
     * plus the updated neutral history (with this turn's user + assistant messages appended).
     */
    override fun chat(history: JSONArray, userText: String, apiKey: String): Pair<ParsedAIResult, JSONArray> {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
        for (i in 0 until history.length()) messages.put(history.getJSONObject(i))
        messages.put(JSONObject().put("role", "user").put("content", userText))

        val body = JSONObject().apply {
            put("model", CHAT_MODEL)
            put("messages", messages)
            put("temperature", 0.4)
            put("response_format", JSONObject().put("type", "json_object"))
        }
        val data = postJson("/chat/completions", apiKey, body)
        val text = data.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content", "") ?: ""

        val updatedHistory = JSONArray()
        for (i in 0 until history.length()) updatedHistory.put(history.getJSONObject(i))
        updatedHistory.put(JSONObject().put("role", "user").put("content", userText))
        updatedHistory.put(JSONObject().put("role", "assistant").put("content", text))

        val result = parseAiJson(text)
        return result to updatedHistory
    }

    /** Calls OpenAI's TTS endpoint and returns a ready-to-play MP3 file. */
    override fun synthesizeSpeech(text: String, apiKey: String): ByteArray {
        val body = JSONObject().apply {
            put("model", TTS_MODEL)
            put("voice", TTS_VOICE)
            put("input", text)
            put("response_format", "mp3")
        }
        return postJsonForBytes("/audio/speech", apiKey, body)
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
