package com.example.socialmediaproject.ui.accountdetail

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class AccountDetailViewModel : ViewModel() {
    val isAvatarUploading=MutableLiveData(false)
    val isWallUploading=MutableLiveData(false)
    val avataruri=MutableLiveData<Uri?>()
    val walluri=MutableLiveData<Uri?>()
    private val _isUploading = MediatorLiveData<Boolean>().apply {
        addSource(isAvatarUploading) { value = it == true || isWallUploading.value == true }
        addSource(isWallUploading) { value = it == true || isAvatarUploading.value == true }
    }
    val isUploading: LiveData<Boolean> = _isUploading
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"

    suspend fun uploadAvatar(context: Context, uri: Uri) {
        isAvatarUploading.postValue(true)
        val imageUrl = uploadImageToImgbb(context, uri)
        Log.d("IMAGE URL IMAGE URL IMAGE URL: ", imageUrl.toString())
        if (imageUrl != null) {
            avataruri.postValue(Uri.parse(imageUrl))
        }
        isAvatarUploading.postValue(false)
    }

    suspend fun uploadWall(context: Context, uri: Uri) {
        isWallUploading.postValue(true)
        val imageUrl = uploadImageToImgbb(context, uri)
        Log.d("IMAGE URL IMAGE URL IMAGE URL: ", imageUrl.toString())
        if (imageUrl != null) {
            walluri.postValue(Uri.parse(imageUrl))
        }
        isWallUploading.postValue(false)
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