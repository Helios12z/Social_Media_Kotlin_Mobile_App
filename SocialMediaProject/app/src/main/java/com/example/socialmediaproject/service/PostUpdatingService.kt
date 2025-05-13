package com.example.socialmediaproject.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.socialmediaproject.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class PostUpdatingService: Service() {
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"
    private val db = FirebaseFirestore.getInstance()
    private val uploadedImage = arrayListOf<String>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isUpdating=false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val postId = intent?.getStringExtra("post_id") ?: ""
        val postContent = intent?.getStringExtra("post_content") ?: ""
        val privacy = intent?.getStringExtra("privacy") ?: "Công khai"
        val imageUrls = intent?.getStringArrayListExtra("image_urls") ?: arrayListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_id",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Đang cập nhật bài viết")
            .setContentText("Vui lòng đợi...")
            .setSmallIcon(R.drawable.uploadicon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(3, notification)
        if (postId.isNotEmpty()) {
            updatePost(postId, postContent, privacy, imageUrls)
        }
        else {
            notifyStatus(NotificationService.ACTION.UPDATE, "Không tìm thấy bài viết để cập nhật!")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun updatePost(postId: String, content: String, privacy: String, imageUrls: List<String>) {
        db.collection("Posts").document(postId).get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val oldImageUrls = document.get("imageurl") as? List<String> ?: emptyList()
                val oldContent = document.getString("content") ?: ""
                val newImageUrls = imageUrls.filterNot { oldImageUrls.contains(it) }
                if (oldContent != content) {
                    getCategories { categories ->
                        if (categories.isEmpty()) {
                            notifyStatus(NotificationService.ACTION.UPDATE, "Lỗi trong quá trình phân tích!")
                            stopSelf()
                            return@getCategories
                        }
                        serviceScope.launch {
                            try {
                                val response = AIService.classifyPost(content, categories) ?: ""
                                val categoryList = response.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                if (newImageUrls.isNotEmpty()) {
                                    uploadAllImages(newImageUrls) {
                                        val updatedImageUrls = (imageUrls.intersect(oldImageUrls) + uploadedImage).toList()
                                        val data = hashMapOf<String, Any>(
                                            "content" to content,
                                            "privacy" to privacy,
                                            "imageurl" to updatedImageUrls,
                                            "category" to categoryList,
                                            "isUpdatedAt" to System.currentTimeMillis()
                                        )
                                        db.collection("Posts").document(postId)
                                            .update(data)
                                            .addOnSuccessListener {
                                                notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thành công!")
                                                stopSelf()
                                            }
                                            .addOnFailureListener {
                                                notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thất bại!")
                                                stopSelf()
                                            }
                                    }
                                }
                                else {
                                    val data = hashMapOf<String, Any>(
                                        "content" to content,
                                        "privacy" to privacy,
                                        "imageurl" to (oldImageUrls.intersect(imageUrls)).toList(),
                                        "category" to categoryList,
                                        "isUpdatedAt" to System.currentTimeMillis()
                                    )
                                    db.collection("Posts").document(postId)
                                        .update(data)
                                        .addOnSuccessListener {
                                            notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thành công!")
                                            stopSelf()
                                        }
                                        .addOnFailureListener {
                                            notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thất bại!")
                                            stopSelf()
                                        }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                notifyStatus(NotificationService.ACTION.UPDATE, "Lỗi khi xử lý bài đăng!")
                                stopSelf()
                            }
                        }
                    }
                }
                else {
                    if (newImageUrls.isNotEmpty()) {
                        uploadAllImages(newImageUrls) {
                            val updatedImageUrls = (oldImageUrls.intersect(imageUrls) + uploadedImage).toList()
                            val data = hashMapOf<String, Any>(
                                "content" to content,
                                "privacy" to privacy,
                                "imageurl" to updatedImageUrls,
                                "isUpdatedAt" to System.currentTimeMillis()
                            )
                            db.collection("Posts").document(postId)
                            .update(data)
                            .addOnSuccessListener {
                                notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thành công!")
                                stopSelf()
                            }
                            .addOnFailureListener {
                                notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thất bại!")
                                stopSelf()
                            }
                        }
                    }
                    else {
                        val data = hashMapOf<String, Any>(
                            "content" to content,
                            "privacy" to privacy,
                            "imageurl" to (oldImageUrls.intersect(imageUrls)).toList(),
                            "isUpdatedAt" to System.currentTimeMillis()
                        )
                        db.collection("Posts").document(postId)
                        .update(data)
                        .addOnSuccessListener {
                            notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thành công!")
                            stopSelf()
                        }
                        .addOnFailureListener {
                            notifyStatus(NotificationService.ACTION.UPDATE, "Cập nhật bài viết thất bại!")
                            stopSelf()
                        }
                    }
                }
            }
            else {
                notifyStatus(NotificationService.ACTION.UPDATE, "Không tìm thấy bài viết để cập nhật!")
                stopSelf()
            }
        }
        .addOnFailureListener {
            notifyStatus(NotificationService.ACTION.UPDATE, "Lỗi khi lấy bài viết!")
            stopSelf()
        }
    }

    private fun notifyStatus(action: NotificationService.ACTION, content: String) {
        val intent = Intent(this, NotificationService::class.java).apply {
            this.action = action.name
            putExtra("content", content)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun uploadAllImages(imageList: List<String>, callback: () -> Unit) {
        var uploadedCount = 0
        if (imageList.isEmpty()) {
            callback()
            return
        }
        for (uri in imageList) {
            if (uri.startsWith("http")) {
                uploadedCount++
                continue
            }
            uploadImageToImgbb(uri) { imageUrl ->
                if (imageUrl != null) {
                    uploadedImage.add(imageUrl)
                }
                uploadedCount++
                if (uploadedCount == imageList.size) {
                    callback()
                }
            }
        }
    }

    private fun uploadImageToImgbb(imageUri: String, callback: (String?) -> Unit) {
        Thread {
            try {
                val base64Image = uriToBase64(Uri.parse(imageUri)) ?: return@Thread callback(null)
                val client = OkHttpClient()
                val requestBody = FormBody.Builder()
                    .add("key", API_KEY)
                    .add("image", base64Image)
                    .build()
                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        callback(null)
                        return@use
                    }
                    val jsonResponse = JSONObject(response.body!!.string())
                    val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                    callback(imageUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getCategories(callback: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Categories").get().addOnSuccessListener { documents ->
            val interests = mutableListOf<String>()
            for (document in documents) {
                document.getString("name")?.let { interests.add(it) }
            }
            callback(interests)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }
}