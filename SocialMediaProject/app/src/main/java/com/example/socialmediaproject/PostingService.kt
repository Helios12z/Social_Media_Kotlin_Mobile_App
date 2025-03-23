package com.example.socialmediaproject

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException

class PostingService : Service() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"
    private val uploadedImage = arrayListOf<String>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        var isposting=false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isposting=true
        val postContent = intent?.getStringExtra("post_content") ?: ""
        val imageList = intent?.getParcelableArrayListExtra<Uri>("image_list") ?: arrayListOf()
        val privacy = intent?.getStringExtra("privacy") ?: "Công khai"
        startForeground(1, createNotification("Đang đăng bài"))
        uploadAllImages(imageList) {
            UploadPost(postContent, privacy)
        }
        return START_NOT_STICKY
    }

    private fun uploadAllImages(imageList: List<Uri>, callback: () -> Unit) {
        var uploadedCount = 0
        if (imageList.isEmpty()) {
            callback()
            return
        }

        for (uri in imageList) {
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

    private fun uploadImageToImgbb(imageUri: Uri, callback: (String?) -> Unit) {
        Thread {
            try {
                val base64Image = uriToBase64(imageUri) ?: return@Thread callback(null)
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

    private fun UploadPost(content: String, privacy: String) {
        getCategories { categories ->
            if (categories.isNotEmpty()) {
                serviceScope.launch {
                    try {
                        updateNotification("Đang phân tích nội dung...")
                        val response = AIService.classifyPost(content, categories) ?: ""
                        Log.d("AI_RESPONSE", response.ifEmpty { "Không có kết quả" })
                        val userid = auth.currentUser?.uid
                        val post = hashMapOf(
                            "userid" to userid,
                            "imageurl" to uploadedImage,
                            "content" to content,
                            "timestamp" to System.currentTimeMillis(),
                            "privacy" to privacy,
                            "category" to extractCategory(response)
                        )
                        val rawdata=extractCategory(response)?:""
                        val categorynamelist=rawdata.split(",").map{it.trim()}
                        for (categoryname in categorynamelist) {
                            db.collection("Categories").whereEqualTo("name", categoryname).get().addOnSuccessListener {
                                result->if (result.isEmpty) {
                                    serviceScope.launch {
                                        val listcategoryid= mutableListOf<String>()
                                        db.collection("Categories").get().addOnSuccessListener {
                                            documents->for (document in documents) {
                                                document.getString("categoryid")?.let { listcategoryid.add(it) }
                                        }
                                        }
                                        val categoryid=extractCategory(AIService.classifyItem(categoryname, listcategoryid))
                                        val category= hashMapOf(
                                            "name" to categoryname,
                                            "categoryid" to categoryid
                                        )
                                        db.collection("Categories").add(category).addOnSuccessListener {
                                            Log.d("UploadCategory", "Thêm danh mục thành công!")
                                        }
                                    }
                                }
                            }
                        }
                        db.collection("Posts").add(post)
                            .addOnSuccessListener {
                                updateNotification("Đăng bài thành công!")
                                isposting=false
                                stopSelf()
                            }
                            .addOnFailureListener {
                                updateNotification("Đăng bài thất bại!")
                                isposting=false
                                stopSelf()
                            }
                    } catch (e: Exception) {
                        Log.e("UploadPost", "Lỗi khi xử lý bài đăng", e)
                        updateNotification("Lỗi khi xử lý bài đăng!")
                        isposting=false
                        stopSelf()
                    }
                }
            } else {
                updateNotification("Lỗi trong quá trình phân tích!")
                isposting=false
                stopSelf()
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val channelId = "upload_post_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Đang đăng bài", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Thông báo")
            .setContentText(content)
            .setSmallIcon(R.drawable.uploadicon)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification)
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
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

    private fun extractCategory(jsonResponse: String): String? {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val candidates = jsonObject.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}