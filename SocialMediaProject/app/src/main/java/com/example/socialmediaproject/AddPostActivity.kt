package com.example.socialmediaproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.databinding.ActivityAddPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

private const val REQUEST_IMAGE_PICK = 100
class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var MediaAdapter: MediaAdapter
    private val imageList = mutableListOf<Uri>()
    private lateinit var rv_selected_media: RecyclerView

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
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed()
    {
        val intent=Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveImageToInternalStorage(context: Context, file: Uri, filename: String): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(file) ?: return null
            val directory = File(context.filesDir, "images")
            if (!directory.exists()) directory.mkdirs()

            val imageFile = File(directory, filename)
            val outputStream = FileOutputStream(imageFile)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            return imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
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

        if (imageList.isEmpty()) {
            rv_selected_media.visibility = View.GONE
        }
    }
}