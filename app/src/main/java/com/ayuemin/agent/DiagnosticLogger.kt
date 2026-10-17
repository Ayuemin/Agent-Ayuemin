package com.ayuemin.agent

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Redacted, size-bounded diagnostic events. Never pass request/response bodies or secrets here. */
class DiagnosticLogger(context: Context, private val prefs: SharedPreferences) {
    private val file = File(context.filesDir, "diagnostics.jsonl")
    private val lock = Any()
    fun event(stage: String, code: String, fields: Map<String, Any?> = emptyMap(), detailed: Boolean = false) {
        if (detailed && !prefs.getBoolean("diagnostics_verbose", false)) return
        val allowed = setOf("http_status", "finish_reason", "content_length", "input_tokens",
            "output_tokens", "cached_tokens", "elapsed_ms", "request_type", "error_type",
            "model_length", "indexed_count", "retrieved_count")
        val row = JSONObject().put("at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date()))
            .put("stage", stage.take(48)).put("code", code.take(64))
            .put("version", "0.2.1-debug")
        fields.filterKeys { it in allowed }.forEach { (key, value) -> row.put(key, value) }
        synchronized(lock) {
            try {
                if (file.exists() && file.length() > 450_000) {
                    val tail = file.readText().takeLast(180_000).substringAfter('\n', "")
                    file.writeText(tail)
                }
                file.appendText(row.toString() + "\n")
            } catch (_: Exception) { /* diagnostics must never break chat */ }
        }
    }
    fun exportJsonLines(): String = synchronized(lock) { if (file.exists()) file.readText() else "" }\n    fun exportJson(): String = org.json.JSONArray().apply { exportJsonLines().lineSequence().filter { it.isNotBlank() }.forEach { line -> runCatching { put(JSONObject(line)) } } }.toString(2)
    fun exportTxt(): String = exportJsonLines().lineSequence().filter { it.isNotBlank() }.map {
        try {
            val o = JSONObject(it)
            "${o.optString("at")} ${o.optString("stage")} ${o.optString("code")} ${o.toString()}"
        } catch (_: Exception) { "[malformed diagnostic event]" }
    }.joinToString("\n")
    fun clear() = synchronized(lock) { if (file.exists()) file.writeText("") }
    fun verbose() = prefs.getBoolean("diagnostics_verbose", false)
    fun setVerbose(value: Boolean) { prefs.edit().putBoolean("diagnostics_verbose", value).apply() }
}
