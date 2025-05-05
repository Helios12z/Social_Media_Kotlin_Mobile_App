package com.example.socialmediaproject.service

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class UploadChatImgeWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"
    private val db=FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val data = inputData
        val uriString = data.getString("imageUrl") ?: return Result.failure()
        val chatId = data.getString("id") ?: return Result.failure()
        val senderId = data.getString("senderId") ?: return Result.failure()
        val receiverId = data.getString("receiverId") ?: return Result.failure()
        val context = applicationContext
        val imageUrl = uploadImageToImgbb(context, Uri.parse(uriString)) ?: return Result.retry()
        val message = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "text" to "Đã gửi một ảnh",
            "imageUrl" to imageUrl,
            "timestamp" to Timestamp.now(),
            "picture" to true,
            "read" to false
        )
        try {
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .await()
            return Result.success()
        } catch(e: Exception) {
            return Result.retry()
        }
    }

    private suspend fun uploadImageToImgbb(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            val base64 = uriToBase64(context, uri) ?: return@withContext null
            val client = OkHttpClient()
            val body = FormBody.Builder()
                .add("key", API_KEY)
                .add("image", base64)
                .build()
            val req = Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(body)
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val data = JSONObject(res.body!!.string())
                    .getJSONObject("data")
                data.getString("url")
            }
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        } catch(e: IOException) { null }
    }
}