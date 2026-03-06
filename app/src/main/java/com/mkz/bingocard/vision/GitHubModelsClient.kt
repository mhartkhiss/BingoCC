package com.mkz.bingocard.vision

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight REST client for the GitHub Models (OpenAI-compatible) API.
 * Sends a bitmap + text prompt and returns the model's text response.
 *
 * No third-party HTTP library required — uses [HttpURLConnection].
 */
object GitHubModelsClient {

    private const val ENDPOINT = "https://models.github.ai/inference/chat/completions"
    private const val MODEL = "openai/gpt-4.1-nano"
    private const val JPEG_QUALITY = 80

    /**
     * Sends [bitmap] with the given [prompt] to the GitHub Models vision API.
     * @return The assistant's text reply, or throws on failure.
     */
    suspend fun analyzeImage(
        token: String,
        bitmap: Bitmap,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val base64Image = bitmapToBase64(bitmap)
        val requestBody = buildRequestBody(base64Image, prompt)

        val url = URL(ENDPOINT)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            conn.outputStream.bufferedWriter().use { it.write(requestBody) }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
                throw RuntimeException("GitHub Models API error $responseCode: $errorBody")
            }

            val responseText = conn.inputStream.bufferedReader().readText()
            extractContent(responseText)
        } finally {
            conn.disconnect()
        }
    }

    /** Compresses [bitmap] to JPEG and returns a Base64-encoded string. */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /** Builds the OpenAI-compatible JSON request body with an inline base64 image. */
    private fun buildRequestBody(base64Image: String, prompt: String): String {
        val imageUrl = JSONObject().apply {
            put("url", "data:image/jpeg;base64,$base64Image")
        }
        val imagePart = JSONObject().apply {
            put("type", "image_url")
            put("image_url", imageUrl)
        }
        val textPart = JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        }

        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply {
                put(imagePart)
                put(textPart)
            })
        }
        val systemMessage = JSONObject().apply {
            put("role", "system")
            put("content", "")
        }

        return JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(systemMessage)
                put(userMessage)
            })
        }.toString()
    }

    /** Extracts the assistant text content from the OpenAI-style JSON response. */
    private fun extractContent(responseJson: String): String {
        val root = JSONObject(responseJson)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) {
            throw RuntimeException("GitHub Models returned empty choices")
        }
        return choices
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
