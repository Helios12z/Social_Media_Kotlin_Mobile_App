package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.MediaAdapter
import com.example.socialmediaproject.databinding.FragmentEditPostBinding
import com.example.socialmediaproject.service.PostUpdatingService
import com.example.socialmediaproject.service.PostingService
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REQUEST_IMAGE_PICK = 100
class EditPostFragment : Fragment() {
    private lateinit var binding: FragmentEditPostBinding
    private lateinit var viewModel: PostViewModel
    private lateinit var postId: String
    private val db=FirebaseFirestore.getInstance()
    private lateinit var privacySpinner: Spinner
    private lateinit var privacy: String
    private lateinit var mediaadapter: MediaAdapter
    private var imagelist= mutableListOf<Uri>()
    private lateinit var rv_selected_media: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentEditPostBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[PostViewModel::class.java]
        postId=arguments?.getString("postId")?:""
        rv_selected_media=binding.rvPostMedia
        privacySpinner = binding.postprivacy
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listPrivacy= mutableListOf<String>()
        mediaadapter= MediaAdapter(imagelist, ::removeImage)
        rv_selected_media.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv_selected_media.adapter = mediaadapter
        binding.btnAddPhotos.setOnClickListener {
            openGallery()
        }
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnSave.setOnClickListener {
            if (binding.etPostContent.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Không thể lưu một bài trống không!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(requireContext(), PostUpdatingService::class.java).apply {

            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
            parentFragmentManager.popBackStack()
        }
        db.collection("Privacies").get().addOnSuccessListener {
            documents->if (documents!=null && !documents.isEmpty) {
                for (document in documents) listPrivacy.add(document.getString("name") ?: "")
                val adapter= ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listPrivacy)
                privacySpinner.adapter=adapter
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                privacySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                if (postId!="") loadPostInfo(postId)
            }
        }
    }

    private fun loadPostInfo(postId: String) {
        db.collection("Posts").document(postId).get().addOnSuccessListener {
            result->if (result.exists()) {
                binding.etPostContent.setText(result.getString("content"))
                val imageUrls=result.get("imageurl") as? List<String> ?: emptyList()
                if (imageUrls.isNotEmpty()) {
                    binding.rvPostMedia.visibility=View.VISIBLE
                    val urlUris = imageUrls.map { Uri.parse(it) }
                    imagelist.clear()
                    imagelist.addAll(urlUris)
                    mediaadapter.notifyDataSetChanged()
                }
                else {
                    binding.rvPostMedia.visibility=View.GONE
                }
                db.collection("Users").document(result.getString("userid")?:"").get().addOnSuccessListener {
                    doc->if (doc.exists()) {
                        val avatarUrl=doc.getString("avatarurl")?:""
                        Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.avataricon)
                            .error(R.drawable.avataricon)
                            .into(binding.imgProfile)
                        binding.tvUsername.setText(doc.getString("name")?:"")
                    }
                }
                .addOnFailureListener {
                    return@addOnFailureListener
                }
                val timestamp=result.getLong("timestamp")?:0
                binding.tvPostDate.setText("Đã đăng vào: ${getTimeAgo(timestamp)}")
                val updateTimestamp=result.getLong("isUpdatedAt")?:0
                if (updateTimestamp.toInt()!=0) {
                    binding.tvPostUpdateDate.visibility=View.VISIBLE
                    binding.tvPostUpdateDate.setText("Cập nhật vào: ${getTimeAgo(updateTimestamp)}")
                }
                val oldPrivacy = result.getString("privacy") ?: ""
                if (oldPrivacy.isNotEmpty()) {
                    val spinnerAdapter = privacySpinner.adapter as ArrayAdapter<String>
                    val pos = spinnerAdapter.getPosition(oldPrivacy)
                    if (pos >= 0) {
                        privacySpinner.setSelection(pos)
                        privacy = oldPrivacy
                    }
                }
            }
            else {
                Toast.makeText(requireContext(), "Không tìm thấy bài đăng", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi Internet", Toast.LENGTH_SHORT).show()
        }
    }

    fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - timestamp
        val seconds = timeDifference / 1000
        return when {
            seconds < 60 -> "Vừa xong"
            seconds < 3600 -> "${seconds / 60} phút trước"
            seconds < 86400 -> "${seconds / 3600} giờ trước"
            seconds < 604800 -> "${seconds / 86400} ngày trước"
            else -> {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    private fun removeImage(position: Int) {
        imagelist.removeAt(position)
        mediaadapter.notifyItemRemoved(position)
        mediaadapter.notifyItemRangeChanged(position, imagelist.size)
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
                        imagelist.add(imageUri)
                    }
                } else if (it.data != null) {
                    imagelist.add(it.data!!)
                }
                mediaadapter.notifyDataSetChanged()
                rv_selected_media.visibility = View.VISIBLE
            }
        }
    }
}