package com.example.socialmediaproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.databinding.ActivityAddPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val REQUEST_IMAGE_PICK = 100
class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var MediaAdapter: MediaAdapter
    private val imageList = mutableListOf<Uri>()
    private lateinit var rv_selected_media: RecyclerView
    private lateinit var privacyspinner: Spinner
    private lateinit var privacy: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            0
        )
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userid = auth.currentUser?.uid
        if (userid != null) {
            db.collection("Users").document(userid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("name")
                    binding.tvUsername.text = username
                }
            }
        }
        privacyspinner = binding.postprivacy
        val listprivacy = arrayListOf<String>("Công khai", "Riêng tư", "Bạn bè")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listprivacy)
        privacyspinner.adapter = adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        privacyspinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (parent != null) {
                    privacy = parent.getItemAtPosition(position).toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        MediaAdapter = MediaAdapter(imageList, ::removeImage)
        rv_selected_media = findViewById<RecyclerView>(R.id.rv_selected_media)
        rv_selected_media.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_selected_media.adapter = MediaAdapter
        binding.btnAddPhotos.setOnClickListener {
            openGallery()
        }
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnPost.setOnClickListener {
            binding.btnPost.isEnabled = false
            binding.btnPost.setBackgroundColor(Color.BLACK)
            binding.progressBar.visibility = View.VISIBLE
            if (imageList.isEmpty() && binding.etPostContent.text.isEmpty()) {
                Toast.makeText(this, "Không thể đăng một bài trống không!", Toast.LENGTH_SHORT).show()
                binding.btnPost.isEnabled = true
                binding.btnPost.setBackgroundColor(Color.parseColor("#FF6200EE"))
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }
            else
            {
                intent=Intent(this, NotificationService::class.java)
                intent.action=NotificationService.ACTION.START.toString()
                intent = Intent(this, NotificationService::class.java)
                intent.action = NotificationService.ACTION.START.toString()
                intent.putExtra("content", "Đang đăng bài...")
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) startForegroundService(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                else {
                    startService(intent)
                }
                val postintent = Intent(this, PostingService::class.java)
                postintent.putExtra("post_content", binding.etPostContent.text.toString())
                postintent.putExtra("privacy", privacy)
                postintent.putParcelableArrayListExtra("image_list", ArrayList(imageList))
                startService(postintent)
                val mainintent=Intent(this, MainActivity::class.java)
                startActivity(mainintent)
                finish()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
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
}