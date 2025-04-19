package com.example.socialmediaproject.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

object AIService {
    private const val API_KEY = "AIzaSyBf0xyHSQW2A4Y2Tf6d-0R0GD_8XRz0WcE"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY"

    suspend fun classifyPost(content: String, categories: List<String>): String? {
        val prompt = buildString {
            append("Hãy phân loại nội dung sau vào danh mục phù hợp trong danh sách. ")
            append("Nếu nội dung thuộc nhiều danh mục, trình bày các danh mục mà nội dung đó thuộc về, cách nhau bởi dấu phẩy. ")
            append("Chỉ cần đưa ra đáp án, không trả lời gì thêm. ")
            append("Ví dụ: Học tập, Giải trí, Hài, Phàn nàn. ")
            append("Nếu không có danh mục phù hợp thì có thể tự tạo danh mục mới. ")
            append("Cố gắng xếp bài viết vào càng nhiều danh mục càng tốt, nhưng ưu tiên nhất vẫn là tính đúng đắn: ")
            append(categories.joinToString(", "))
            append("\n")
            append(content)
        }
        val contentJson = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(contentJson))
            put("generationConfig", JSONObject().apply {
                put("temperature", 1)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 100)
                put("responseMimeType", "text/plain")
            })
        }
        val requestBody = RequestBody.create("application/json".toMediaType(), requestJson.toString())
        val request = Request.Builder().url(API_URL).post(requestBody).build()
        return withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }
                try {
                    val body = response.body?.string() ?: return@use null
                    val jsonObject = JSONObject(body)
                    val candidates = jsonObject.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@use parts.getJSONObject(0).getString("text")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@use null
            }
        }
    }

    suspend fun chatWithAI(prompt: String): String {
        val contentJson = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(contentJson))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topK", 40)
                put("topP", 0.9)
                put("maxOutputTokens", 500)
                put("responseMimeType", "text/plain")
            })
        }
        val requestBody = RequestBody.create("application/json".toMediaType(), requestJson.toString())
        val request = Request.Builder().url(API_URL).post(requestBody).build()
        return withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Xin lỗi, tôi gặp lỗi!"
                }
                val body = response.body?.string() ?: return@withContext "Không nhận được phản hồi"
                try {
                    val jsonObject = JSONObject(body)
                    val candidates = jsonObject.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                    return@withContext "Xin lỗi, tôi không hiểu câu hỏi."
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext "Xin lỗi, tôi gặp lỗi khi xử lý phản hồi!"
                }
            }
        }
    }
}