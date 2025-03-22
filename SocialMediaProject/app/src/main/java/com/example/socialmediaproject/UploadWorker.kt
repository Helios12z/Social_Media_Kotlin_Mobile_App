package com.example.socialmediaproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(context: Context, workerParams: WorkerParameters): CoroutineWorker(context, workerParams) {
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"
    private lateinit var db: FirebaseFirestore
    override suspend fun doWork(): Result {
        val userid=inputData.getString("userid") ?: return Result.failure()
        val content=inputData.getString("content") ?: return Result.failure()
        val imageuris=inputData.getStringArray("imageuris") ?: return Result.failure()
        val uploadedimages= mutableListOf<String>()
        for (uri in imageuris) {
            var imageurl=uploadImageToImgbb(Uri.parse(uri)) ?: return Result.retry()
            uploadedimages.add(imageurl)
        }
        try {
            for (uri in imageuris) {
                val imageUrl = withContext(Dispatchers.IO) { uploadImageToImgbb(Uri.parse(uri)) }
                if (imageUrl.isNullOrEmpty()) return Result.retry()
                uploadedimages.add(imageUrl)
            }

            val isSuccess = uploadPostToDatabase(userid, uploadedimages, content)
            if (isSuccess) showNotification("Đăng bài thành công!")
            else showNotification("Đăng bài thất bại!")
            return if (isSuccess) Result.success() else Result.failure()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun showNotification(content: String)
    {
        val notificationmanager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            val channel=NotificationChannel("Notifications", "Thông báo", NotificationManager.IMPORTANCE_HIGH)
            notificationmanager.createNotificationChannel(channel)
        }
        val notification=NotificationCompat.Builder(applicationContext, "Notifications")
            .setSmallIcon(R.drawable.uploadicon)
            .setContentTitle("Thông báo")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationmanager.notify(2, notification)
    }

    private suspend fun uploadPostToDatabase(userid: String, images: List<String>, content: String): Boolean
    {
        return try {
            val post = hashMapOf(
                "userid" to userid,
                "imageurl" to images,
                "content" to content,
                "timestamp" to System.currentTimeMillis()
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("Posts").add(post).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun uploadImageToImgbb(imageUri: Uri): String? {
        return try {
            val base64Image = uriToBase64(imageUri) ?: return null
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
                if (!response.isSuccessful) return null
                val jsonResponse = JSONObject(response.body!!.string())
                jsonResponse.getJSONObject("data").getString("url")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
                ?: return null
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
}