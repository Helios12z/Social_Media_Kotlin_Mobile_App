package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.MediaAdapter
import com.example.socialmediaproject.service.NotificationService
import com.example.socialmediaproject.service.PostingService
import com.example.socialmediaproject.databinding.FragmentPostingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val REQUEST_IMAGE_PICK = 100
class PostingFragment : Fragment() {

    private lateinit var binding: FragmentPostingBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var mediaadapter: MediaAdapter
    private val imagelist= mutableListOf<Uri>()
    private lateinit var privacyspinner: Spinner
    private lateinit var rv_selected_media: RecyclerView
    private lateinit var privacy: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentPostingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userid = auth.currentUser?.uid
        if (userid != null) {
            val loading=LoadingDialogFragment()
            loading.show(parentFragmentManager, "loading")
            db.collection("Users").document(userid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    loading.dismiss()
                    val username = document.getString("name")
                    val userimageuri=document.getString("avatarurl")
                    if (userimageuri!=null) {
                        Glide.with(requireContext())
                            .load(userimageuri)
                            .placeholder(R.drawable.avataricon)
                            .error(R.drawable.avataricon)
                            .into(binding.imgProfile)
                    }
                    else binding.imgProfile.setImageResource(R.drawable.avataricon)
                    binding.tvUsername.text = username
                }
                else {
                    loading.dismiss()
                    Toast.makeText(requireContext(), "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                }
            }
            .addOnFailureListener {
                loading.dismiss()
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            }
        }
        privacyspinner = binding.postprivacy
        val listprivacy= mutableListOf<String>()
        val loading=LoadingDialogFragment()
        loading.show(parentFragmentManager, "loading")
        db.collection("Privacies").get().addOnSuccessListener {
            documents->
            loading.dismiss()
            if (documents!=null) {
                for (document in documents) listprivacy.add(document.getString("name")?:"")
            }
            val adapter=ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listprivacy)
            privacyspinner.adapter=adapter
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
        }
        .addOnFailureListener {
            loading.dismiss()
            Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        }
        mediaadapter= MediaAdapter(imagelist, ::removeImage)
        rv_selected_media = binding.rvSelectedMedia
        rv_selected_media.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv_selected_media.adapter = mediaadapter
        binding.btnAddPhotos.setOnClickListener {
            openGallery()
        }
        binding.btnPost.setOnClickListener {
            binding.btnPost.isEnabled = false
            val loading=LoadingDialogFragment()
            loading.show(parentFragmentManager, "loading")
            if (imagelist.isEmpty()) {
                Toast.makeText(requireContext(), "Không thể đăng một bài không có ảnh hoặc nội dung!", Toast.LENGTH_SHORT).show()
                binding.btnPost.isEnabled = true
                loading.dismiss()
                return@setOnClickListener
            }
            else
            {
                val intent = Intent(requireContext(), NotificationService::class.java).apply {
                    action = NotificationService.ACTION.START.toString()
                    putExtra("content", "Đang đăng bài...")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(intent)
                } else {
                    requireContext().startService(intent)
                }
                val postIntent = Intent(requireContext(), PostingService::class.java).apply {
                    putExtra("post_content", binding.etPostContent.text.toString())
                    putExtra("privacy", privacy)
                    putParcelableArrayListExtra("image_list", ArrayList(imagelist))
                }
                requireContext().startService(postIntent)
                loading.dismiss()
                parentFragmentManager.popBackStack()
            }
        }
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
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