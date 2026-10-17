package com.ayuemin.agent

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class ParsedCompletion(val text: String, val reason: String, val input: Int, val output: Int, val cached: Int)

object ApiResponseParser {
    fun parse(raw: String): ParsedCompletion {
        val response = try { JSONObject(raw) } catch (e: Exception) { throw IOException("Invalid API JSON", e) }
        val choice = response.optJSONArray("choices")?.optJSONObject(0) ?: throw IOException("Missing completion choices")
        val message = choice.optJSONObject("message") ?: throw IOException("Missing assistant message")
        val reason = choice.optString("finish_reason", "unknown")
        val content = message.opt("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val part = content.opt(i)
                    if (part is String) append(part)
                    if (part is JSONObject) {
                        val partText = part.opt("text")
                        when (partText) {
                            is String -> append(partText)
                            is JSONObject -> append(partText.optString("value", ""))
                        }
                    }
                }
            }
            else -> ""
        }
        if (text.isBlank() || text.trim() == "null") {
            val kind = when {
                message.optJSONArray("tool_calls") != null -> "tool_calls"
                message.has("reasoning_content") -> "reasoning_only"
                reason == "length" -> "generation_limit"
                else -> "empty_content"
            }
            throw EmptyCompletionException(reason, kind)
        }
        val usage = response.optJSONObject("usage")
        return ParsedCompletion(text, reason,
            usage?.optInt("prompt_tokens", 0) ?: 0,
            usage?.optInt("completion_tokens", 0) ?: 0,
            usage?.optJSONObject("prompt_tokens_details")?.optInt("cached_tokens", 0)
                ?: usage?.optInt("prompt_cache_hit_tokens", 0) ?: 0)
    }
}
class EmptyCompletionException(val finishReason: String, val kind: String) :
    IOException("No assistant text ($kind, finish_reason=$finishReason). Check provider and generation settings.")
