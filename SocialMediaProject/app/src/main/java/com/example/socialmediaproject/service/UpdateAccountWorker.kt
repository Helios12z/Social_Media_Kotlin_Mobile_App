package com.example.socialmediaproject.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class UpdateAccountWorker(context: Context, workerParams: WorkerParameters): CoroutineWorker(context, workerParams) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"

    override suspend fun doWork(): Result {
        auth=FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.failure()
        val context=applicationContext
        val userUpdate = mutableMapOf<String, Any>()
        inputData.getString("avatarUri")?.let { uriString ->
            if (uriString!="NO") {
                val avatarUrl = uploadImageToImgbb(context, Uri.parse(uriString))
                if (avatarUrl != null) {
                    userUpdate["avatarurl"] = avatarUrl
                }
            }
            else userUpdate["avatarurl"]=""
        }
        inputData.getString("wallUri")?.let { uriString ->
            if (uriString!="NO") {
                val wallUrl = uploadImageToImgbb(context, Uri.parse(uriString))
                if (wallUrl != null) {
                    userUpdate["wallurl"] = wallUrl
                }
            }
            else userUpdate["wallurl"]=""
        }
        inputData.getString("name")?.let { userUpdate["name"] = it }
        inputData.getString("fullname")?.let { userUpdate["fullname"] = it }
        inputData.getString("birthday")?.let { userUpdate["birthday"] = it }
        inputData.getString("address")?.let { userUpdate["address"] = it }
        inputData.getString("phone")?.let { userUpdate["phonenumber"] = it }
        inputData.getString("bio")?.let { userUpdate["bio"] = it }
        inputData.getString("gender")?.let { userUpdate["gender"] = it }
        return try {
            db.collection("Users").document(userId).set(userUpdate, SetOptions.merge()).await()
            sendNotification("Cập nhật tài khoản thành công!")
            Result.success()
        }
        catch (e: Exception) {
            sendNotification("Cập nhật tài khoản thất bại!")
            Result.failure()
        }
    }

    private fun sendNotification(content: String) {
        val intent = Intent(applicationContext, NotificationService::class.java).apply {
            action = NotificationService.ACTION.UPDATE.toString()
            putExtra("content", content)
        }
        applicationContext.startService(intent)
    }

    private suspend fun uploadImageToImgbb(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val base64Image = uriToBase64(context, imageUri) ?: return@withContext null
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
                    if (!response.isSuccessful) return@withContext null
                    val jsonResponse = JSONObject(response.body!!.string())
                    return@withContext jsonResponse.getJSONObject("data").getString("url")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
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