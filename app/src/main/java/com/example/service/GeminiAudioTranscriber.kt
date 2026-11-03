package com.example.service

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class TranscriptionResult {
    data class Success(val text: String, val audioDurationSec: Int? = null) : TranscriptionResult()
    data class Error(val message: String) : TranscriptionResult()
}

class GeminiAudioTranscriber(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudioUri(uri: Uri): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: detectMimeType(uri)

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext TranscriptionResult.Error("تعذر قراءة ملف الصوت المحدد")
            }

            val audioBytes = inputStream.use { it.readBytes() }
            if (audioBytes.isEmpty()) {
                return@withContext TranscriptionResult.Error("الملف الصوتي فارغ")
            }

            // Size check: limit up to 15MB for inline base64
            if (audioBytes.size > 15 * 1024 * 1024) {
                return@withContext TranscriptionResult.Error("حجم الملف الصوتي كبير جداً (الحد الأقصى 15 ميجابايت)")
            }

            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext TranscriptionResult.Error(
                    "مفتاح Gemini API غير مفعّل. يمكنك إدخاله عبر لوحة الأسرار (Secrets) لتحويل المقاطع الصوتية بالذكاء الاصطناعي."
                )
            }

            // Call Gemini 2.5 Flash for audio transcription
            val promptText = "أنت مساعد مخصص للأشخاص الصم وضعاف السمع. قم بتفريغ هذا التسجيل الصوتي بدقة متناهية إلى نص عربي فصيح وواضح، واكتب علامات الترقيم واسم المتحدث إذا أمكن. لا تضف أي مقدمة أو تعليقات خارجية، فقط النص المنطوق."

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Audio part
                val audioPart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mime_type", if (mimeType.startsWith("audio/")) mimeType else "audio/ogg")
                        put("data", base64Audio)
                    }
                    put("inline_data", inlineData)
                }
                partsArray.put(audioPart)

                // Instruction prompt part
                val textPart = JSONObject().apply {
                    put("text", promptText)
                }
                partsArray.put(textPart)

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "خطأ من خادم الذكاء الاصطناعي (${response.code})"
                }
                return@withContext TranscriptionResult.Error("فشل تحويل الصوت: $errorMsg")
            }

            val parsedJson = JSONObject(responseString)
            val candidates = parsedJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext TranscriptionResult.Error("لم يتم العثور على نص منطوق في هذا التسجيل")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val resultBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    resultBuilder.append(part.getString("text")).append("\n")
                }
            }

            val text = resultBuilder.toString().trim()
            if (text.isEmpty()) {
                TranscriptionResult.Error("الملف الصوتي لا يحتوي على كلام واضح")
            } else {
                TranscriptionResult.Success(text)
            }
        } catch (e: Exception) {
            TranscriptionResult.Error("حدث خطأ أثناء معالجة الصوت: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    private fun detectMimeType(uri: Uri): String {
        val path = uri.path?.lowercase() ?: ""
        return when {
            path.endsWith(".opus") || path.endsWith(".ogg") -> "audio/ogg"
            path.endsWith(".mp3") -> "audio/mp3"
            path.endsWith(".m4a") -> "audio/m4a"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".aac") -> "audio/aac"
            else -> "audio/ogg"
        }
    }
}
