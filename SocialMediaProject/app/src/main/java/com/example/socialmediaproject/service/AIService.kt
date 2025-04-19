package com.example.socialmediaproject.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

object AIService {
    private const val API_KEY = "AIzaSyBf0xyHSQW2A4Y2Tf6d-0R0GD_8XRz0WcE"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY"
    suspend fun classifyPost(content: String, categories: List<String>): String? {
        val json = """
            {
                "contents": [
                    { "role": "user", "parts": [{ "text": "Hãy phân loại nội dung sau vào danh mục phù hợp trong danh sách, nếu nội dung thuộc nhiều danh mục, trình bày các danh mục mà nội dung đó thuộc về, cách nhau bởi dấu phẩy, chỉ cần đưa ra đáp án, không trả lời gì thêm, ví dụ: Học tập, Giải trí, Hài, Phàn nàn. Nếu không có danh mục phù hợp thì có thể tự tạo danh mục mới. Cố gắng xếp bài viết vào càng nhiều danh mục càng tốt, nhưng ưu tiên nhất vẫn là tính đúng đắn: ${categories.joinToString(", ")}\n$content" }] }
                ],
                "generationConfig": {
                    "temperature": 1,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 100,
                    "responseMimeType": "text/plain"
                }
            }
        """.trimIndent()

        val requestBody = RequestBody.create("application/json".toMediaType(), json)
        val request = Request.Builder().url(API_URL).post(requestBody).build()

        return withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }
    }

    suspend fun classifyItem(name: String, categories: List<String>): String {
        val json = """
    {
        "contents": [
            { 
                "role": "user", 
                "parts": [{ 
                    "text": "Dựa vào danh sách CategoryId cho trước, hãy phân loại mục sau vào CategoryId phù hợp. Chỉ được sử dụng những CategoryID có sẵn. Câu trả lời chứa duy nhất categoryId, ví dụ nếu mục đưa ra là Đánh giá thì câu trả lời nên đưa ra là news. Danh sách CategoryId: ${categories.joinToString(", ")}\n$name" 
                }] 
            }
        ],
        "generationConfig": {
            "temperature": 1,
            "topK": 40,
            "topP": 0.95,
            "maxOutputTokens": 50,
            "responseMimeType": "text/plain"
        }
    }
    """.trimIndent()

        val requestBody = RequestBody.create("application/json".toMediaType(), json)
        val request = Request.Builder().url(API_URL).post(requestBody).build()

        return withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use "unknown"
                response.body?.string() ?: "unknown"
            }
        }
    }

    suspend fun chatWithAI(prompt: String): String {
        val json = """
        {
            "contents": [
                {
                    "role": "user",
                    "parts": [{ "text": "$prompt" }]
                }
            ],
            "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.9,
                "maxOutputTokens": 500,
                "responseMimeType": "text/plain"
            }
        }
    """.trimIndent()
        val requestBody = RequestBody.create("application/json".toMediaType(), json)
        val request = Request.Builder().url(API_URL).post(requestBody).build()
        return withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Xin lỗi, tôi gặp lỗi!"
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