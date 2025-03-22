package com.example.socialmediaproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.databinding.ActivityAddPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val REQUEST_IMAGE_PICK = 100
class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var MediaAdapter: MediaAdapter
    private val imageList = mutableListOf<Uri>()
    private lateinit var rv_selected_media: RecyclerView
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"
    private val uploadedimage = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth=FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()
        val userid=auth.currentUser?.uid
        if (userid!=null) {
            db.collection("Users").document(userid).get().addOnSuccessListener {
                document->
                    if (document.exists()) {
                        val username=document.getString("name")
                        binding.tvUsername.text=username
                    }
            }
        }
        MediaAdapter = MediaAdapter(imageList, ::removeImage)
        rv_selected_media=findViewById<RecyclerView>(R.id.rv_selected_media)
        rv_selected_media.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_selected_media.adapter = MediaAdapter
        binding.btnAddPhotos.setOnClickListener {
            openGallery()
        }
        binding.btnBack.setOnClickListener {
            val intent=Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnPost.setOnClickListener {
            binding.btnPost.isEnabled = false
            binding.btnPost.setBackgroundColor(Color.BLACK)
            binding.progressBar.visibility = View.VISIBLE
            uploadAllImages() //upload post duoc goi o ben trong uploadallimages
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed()
    {
        val intent=Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            data?.let {
                if (it.clipData != null) {
                    val count = it.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = it.clipData!!.getItemAt(i).uri
                        imageList.add(imageUri)
                    }
                } else if (it.data != null) {
                    imageList.add(it.data!!)
                }
                MediaAdapter.notifyDataSetChanged()
                rv_selected_media.visibility = View.VISIBLE
            }
        }
    }

    private fun removeImage(position: Int) {
        imageList.removeAt(position)
        MediaAdapter.notifyItemRemoved(position)
        MediaAdapter.notifyItemRangeChanged(position, imageList.size)
    }

    private fun uploadAllImages() {
        var uploadedCount = 0
        if (imageList.isEmpty()) {
            UploadPost()
            return
        }

        for (uri in imageList) {
            uploadImageToImgbb(uri) { imageUrl ->
                if (imageUrl != null) {
                    uploadedimage.add(imageUrl)
                }
                uploadedCount++
                if (uploadedCount == imageList.size) {
                    UploadPost()
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
                        Log.e("UploadError", "Lỗi: ${response.message}")
                        callback(null)
                        return@use
                    }
                    val jsonResponse = JSONObject(response.body!!.string())
                    val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                    callback(imageUrl)
                    uploadedimage.add(imageUrl)
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

    private fun UploadPost()
    {
        val userid=auth.currentUser?.uid
        val post= hashMapOf(
            "userid" to userid,
            "imageurl" to uploadedimage,
            "content" to binding.etPostContent.text.toString(),
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("Posts").add(post).addOnSuccessListener {
            Toast.makeText(this, "Đăng bài thành công", Toast.LENGTH_SHORT).show()
            val intent=Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Đăng bài thất bại", Toast.LENGTH_SHORT).show()
        }
    }
}