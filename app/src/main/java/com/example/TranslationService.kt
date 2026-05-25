package com.example

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TranslationService {
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Translates a given text between English and Sinhala using the Gemini API.
     */
    suspend fun translate(text: String, fromEngToSin: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("TranslationService", "GEMINI_API_KEY is not defined or is placeholder.")
            return@withContext "No API Key (Set in Secrets Panel)"
        }

        val prompt = if (fromEngToSin) {
            "Translate this English text to Sinhala. Provide ONLY the translated Sinhala text. Do not include any explanations, introduction, markdown quotes, or extra text. Translating: \"$text\""
        } else {
            "Translate this Sinhala text to English. Provide ONLY the translated English text. Do not include any explanations, introduction, markdown quotes, or extra text. Translating: \"$text\""
        }

        try {
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArray)
            }

            val requestBody = requestBodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: ""
                    Log.e("TranslationService", "HTTP error ${response.code}: $errorMsg")
                    return@withContext "Translation failed: Server error ${response.code}"
                }
                val bodyStr = response.body?.string() ?: return@withContext "Empty response"
                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val translatedText = parts.getJSONObject(0).optString("text")
                            return@withContext translatedText.trim().removeSurrounding("\"")
                        }
                    }
                }
                return@withContext "Translation error: No candidates found"
            }
        } catch (e: Exception) {
            Log.e("TranslationService", "API error during translation", e)
            return@withContext "Translation Offline: Check connection"
        }
    }
}
