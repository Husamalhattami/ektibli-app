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
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class TranscriptionResult {
    data class Success(val text: String, val audioDurationSec: Int? = null) : TranscriptionResult()
    data class Error(val message: String) : TranscriptionResult()
}

class GeminiAudioTranscriber(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(75, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudioUri(
        uri: Uri,
        userCustomApiKey: String? = null
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val rawMimeType = contentResolver.getType(uri) ?: detectMimeType(uri)
            val effectiveMimeType = normalizeMimeType(rawMimeType, uri)

            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext TranscriptionResult.Error("تعذر فتح ملف الصوت المحدد من الذاكرة")

            val audioBytes = inputStream.use { it.readBytes() }

            // 1. Check if empty
            if (audioBytes.isEmpty()) {
                return@withContext TranscriptionResult.Error("الملف الصوتي فارغ (0 بايت) ولا يحتوي على بيانات صوتية.")
            }

            // 2. Check if exceeds max size (20MB)
            if (audioBytes.size > 20 * 1024 * 1024) {
                return@withContext TranscriptionResult.Error("حجم الملف الصوتي يتجاوز الحد الأقصى المسموح به (20 ميجابايت).")
            }

            val effectiveApiKey = when {
                !userCustomApiKey.isNullOrBlank() -> userCustomApiKey.trim()
                BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY.trim()
                else -> null
            }

            // 3. Check if API key is missing
            if (effectiveApiKey.isNullOrBlank()) {
                return@withContext TranscriptionResult.Error(
                    "مفتاح Gemini API غير مضاف.\nيرجى إدخال مفتاحك المجاني في شاشة (المظهر والإعدادات) أو تهيئته عبر Secrets Panel لتفعيل التفريغ الذكي."
                )
            }

            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Construct Gemini multimodal prompt
            val promptText = "أنت مساعد مخصص للأشخاص الصم وضعاف السمع. قم بتفريغ هذا التسجيل الصوتي بدقة متناهية إلى نص عربي فصيح وواضح، واكتب علامات الترقيم واسم المتحدث إذا أمكن. لا تضف أي مقدمة أو تعليقات خارجية، فقط النص المنطوق."

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Audio part
                val audioPart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mime_type", effectiveMimeType)
                        put("data", base64Audio)
                    }
                    put("inline_data", inlineData)
                }
                partsArray.put(audioPart)

                // Text prompt part
                val textPart = JSONObject().apply {
                    put("text", promptText)
                }
                partsArray.put(textPart)

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$effectiveApiKey"

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
                    when (response.code) {
                        400 -> "صيغة الملف غير مدعومة من محرك الذكاء الاصطناعي."
                        401, 403 -> "مفتاح Gemini API غير صالح أو غير مصرح له بالوصول."
                        429 -> "تم تجاوز حد الطلبات المسموح به لمفتاح الذكاء الاصطناعي، يرجى الانتظار دقيقة والمحاولة."
                        500, 503 -> "خادم الذكاء الاصطناعي يواجه ضغطاً مؤقتاً، يرجى إعادة المحاولة."
                        else -> "خطأ من خادم الذكاء الاصطناعي (رمز ${response.code})"
                    }
                }
                return@withContext TranscriptionResult.Error("فشل تحويل الصوت: $errorMsg")
            }

            val parsedJson = JSONObject(responseString)
            val candidates = parsedJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext TranscriptionResult.Error("لم يتمكن الذكاء الاصطناعي من استخراج أو تمييز كلام واضح في هذا التسجيل الصوتي.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return@withContext TranscriptionResult.Error("لم يتم العثور على نص منطوق داخل هذا التسجيل.")
            }

            val resultBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    resultBuilder.append(part.getString("text")).append("\n")
                }
            }

            val text = resultBuilder.toString().trim()
            if (text.isEmpty()) {
                TranscriptionResult.Error("لم يتمكن الذكاء الاصطناعي من استخراج أو تمييز كلام واضح في هذا التسجيل الصوتي.")
            } else {
                TranscriptionResult.Success(text)
            }
        } catch (e: UnknownHostException) {
            TranscriptionResult.Error("تعذر الاتصال بالإنترنت. يرجى التحقق من اتصال الهاتف بالشبكة والمحاولة مرة أخرى.")
        } catch (e: ConnectException) {
            TranscriptionResult.Error("تعذر الاتصال بخادم الذكاء الاصطناعي. يرجى التأكد من تشغيل الإنترنت.")
        } catch (e: SocketTimeoutException) {
            TranscriptionResult.Error("انتهت مهلة الاتصال بالخادم أثناء معالجة المقطع الصوتي. يُرجى التحقق من سرعة الإنترنت وإعادة المحاولة.")
        } catch (e: Exception) {
            TranscriptionResult.Error("حدث خطأ أثناء معالجة الصوت: ${e.localizedMessage ?: "خطأ غير متوقع"}")
        }
    }

    private fun normalizeMimeType(rawMime: String?, uri: Uri): String {
        val lower = (rawMime ?: "").lowercase()
        val path = (uri.path ?: "").lowercase()
        return when {
            lower.contains("ogg") || lower.contains("opus") || path.endsWith(".opus") || path.endsWith(".ogg") -> "audio/ogg"
            lower.contains("mp4") || lower.contains("m4a") || lower.contains("aac") || path.endsWith(".m4a") || path.endsWith(".aac") -> "audio/mp4"
            lower.contains("mpeg") || lower.contains("mp3") || path.endsWith(".mp3") -> "audio/mp3"
            lower.contains("wav") || path.endsWith(".wav") -> "audio/wav"
            lower.contains("3gpp") || lower.contains("3gp") || path.endsWith(".3gp") -> "audio/3gpp"
            lower.contains("amr") || path.endsWith(".amr") -> "audio/amr"
            else -> "audio/ogg"
        }
    }

    private fun detectMimeType(uri: Uri): String {
        val path = uri.path?.lowercase() ?: ""
        return when {
            path.endsWith(".opus") || path.endsWith(".ogg") -> "audio/ogg"
            path.endsWith(".mp3") -> "audio/mp3"
            path.endsWith(".m4a") || path.endsWith(".aac") -> "audio/mp4"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".3gp") -> "audio/3gpp"
            path.endsWith(".amr") -> "audio/amr"
            else -> "audio/ogg"
        }
    }
}
