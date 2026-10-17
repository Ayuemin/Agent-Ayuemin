package com.ayuemin.agent

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ApiResult(val reply: String, val input: Int, val output: Int, val cached: Int, val cost: Double)

class ApiClient(private val prefs: SharedPreferences, private val logger: DiagnosticLogger? = null) {
    @Volatile private var activeConnection: HttpURLConnection? = null
    fun cancel() { activeConnection?.disconnect() }

    fun call(messages: JSONArray, limit: Int = 0): ApiResult {
        val base = (prefs.getString("base", "https://api.deepseek.com/v1") ?: "").trim().trimEnd('/')
        require(base.startsWith("https://")) { "HTTPS base URL required" }
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val body = JSONObject().put("model", prefs.getString("model", "deepseek-chat"))
            .put("messages", messages).put("stream", false)
        // No client-side max_tokens. The provider controls the output limit.
        val connection = URL(url).openConnection() as HttpURLConnection
        activeConnection = connection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.readTimeout = 90000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer " + (prefs.getString("key", "") ?: ""))
        val start = System.currentTimeMillis()
        logger?.event("api", "start")
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            logger?.event("api", "http", mapOf("http_status" to status, "elapsed_ms" to (System.currentTimeMillis() - start)))
            if (status >= 400) throw IOException("API request failed (HTTP $status)")
            val raw = connection.inputStream?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("Empty API response")
            val parsed = try {
                ApiResponseParser.parse(raw)
            } catch (e: EmptyCompletionException) {
                logger?.event("api", e.kind, mapOf("finish_reason" to e.finishReason))
                throw e
            }
            logger?.event("api", "success", mapOf("finish_reason" to parsed.reason,
                "content_length" to parsed.text.length, "input_tokens" to parsed.input,
                "output_tokens" to parsed.output, "cached_tokens" to parsed.cached))
            val cost = ((parsed.input - parsed.cached).coerceAtLeast(0) * price("input_price", 0.20) +
                parsed.cached * price("cached_price", 0.02) + parsed.output * price("output_price", 0.60)) / 1000000.0
            return ApiResult(parsed.text, parsed.input, parsed.output, parsed.cached, cost)
        } catch (e: Exception) {
            logger?.event("api", "failed", mapOf("error_type" to (e.javaClass.simpleName.take(60))))
            throw e
        } finally {
            connection.disconnect()
            if (activeConnection === connection) activeConnection = null
        }
    }
    private fun price(key: String, default: Double) = prefs.getString(key, "$default")?.toDoubleOrNull() ?: default
}
