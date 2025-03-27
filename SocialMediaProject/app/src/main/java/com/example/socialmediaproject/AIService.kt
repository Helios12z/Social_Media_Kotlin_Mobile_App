package com.example.socialmediaproject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

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
}