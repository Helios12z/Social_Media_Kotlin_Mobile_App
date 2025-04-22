package com.example.socialmediaproject.ui.accountdetail

import android.app.Activity
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.net.toUri
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentAccountDetailBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.Date
import java.util.Locale

class AccountDetailFragment : Fragment() {
    private val viewModel: AccountDetailViewModel by viewModels()
    private lateinit var binding: FragmentAccountDetailBinding
    private val REQUEST_AVATAR_PICK = 1000
    private val REQUEST_WALL_CAPTURE = 1001
    private var cropType: Int=0
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var gender: String=""
    private var tmp1: Uri=Uri.EMPTY
    private var tmp2: Uri=Uri.EMPTY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentAccountDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBirthdayPicker()
        db=FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        val genderlist= mutableListOf<String>()
        val userid=auth.currentUser?.uid ?: ""
        val loading=LoadingDialogFragment()
        loading.show(parentFragmentManager, "loading")
        db.collection("Users").document(userid).get().addOnSuccessListener {
            result->if (result!=null) {
                binding.tilNickname.editText?.setText(result.getString("name"))
                binding.tilFullName.editText?.setText(result.getString("fullname"))
                binding.tilBirthday.editText?.setText(result.getString("birthday"))
                binding.tilBio.editText?.setText(result.getString("bio"))
                binding.tilPhone.editText?.setText(result.getString("phonenumber"))
                binding.tilAddress.editText?.setText(result.getString("address"))
                gender=result.getString("gender")?:""
                db.collection("Genders").get().addOnSuccessListener {
                    documents->if (documents!=null) {
                        loading.dismiss()
                        for (document in documents) {
                            genderlist.add(document.getString("name")?:"")
                        }
                        val adapter=ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderlist)
                        val genderAutoComplete = binding.spinnerGender
                        genderAutoComplete.setAdapter(adapter)
                        genderAutoComplete.setOnItemClickListener { _, _, position, _ ->
                            gender = genderlist[position]
                        }
                        val index = genderlist.indexOf(gender)
                        if (index != -1) {
                            binding.spinnerGender.setText(genderlist[index], false)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    loading.dismiss()
                }
                val avatarurl = result.getString("avatarurl")
                if (avatarurl != null) {
                    if (avatarurl=="") binding.imgAvatar.setImageResource(R.drawable.avataricon)
                    else {
                        Glide.with(requireContext())
                            .load(avatarurl)
                            .placeholder(R.drawable.avataricon)
                            .error(R.drawable.avataricon)
                            .into(binding.imgAvatar)
                    }
                }
                val wallurl = result.getString("wallurl")
                if (wallurl != null) {
                    if (wallurl=="") binding.imgCoverPhoto.setImageResource(R.color.background_color)
                    else {
                        Glide.with(requireContext())
                            .load(wallurl)
                            .placeholder(R.color.background_color)
                            .error(R.color.background_color)
                            .into(binding.imgCoverPhoto)
                    }
                }
            }
            else {
                Toast.makeText(requireContext(), "Tài khoản không còn tồn tại", Toast.LENGTH_SHORT).show()
                loading.dismiss()
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            loading.dismiss()
        }
        binding.imgAvatar.setOnClickListener {
            openGalleryForAvatar()
        }
        binding.imgCoverPhoto.setOnClickListener {
            openGalleryForWall()
        }
        binding.deleteavatarbutton.setOnClickListener {
            binding.imgAvatar.setImageResource(R.drawable.avataricon)
            tmp1="NO".toUri()
        }
        binding.deletewallbutton.setOnClickListener {
            binding.imgCoverPhoto.setImageResource(R.color.background_color)
            tmp2="NO".toUri()
        }
        binding.btnSaveProfile.setOnClickListener {
            if (binding.tilNickname.editText?.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Nickname không được bỏ trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else {
                if (tmp1== Uri.EMPTY && tmp2== Uri.EMPTY
                    && binding.tilNickname.editText?.text.isNullOrEmpty()
                    && binding.tilBirthday.editText?.text.isNullOrEmpty()
                    && binding.tilBio.editText?.text.isNullOrEmpty()
                    && binding.tilPhone.editText?.text.isNullOrEmpty()
                    && binding.tilAddress.editText?.text.isNullOrEmpty()
                    && gender.isEmpty()) {
                    Toast.makeText(requireContext(), "Không đủ điều kiện cập nhật!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else {
                    binding.btnSaveProfile.isEnabled = false
                    binding.progressBar.visibility=View.VISIBLE
                    val data= workDataOf(
                        "name" to binding.tilNickname.editText?.text.toString(),
                        "fullname" to binding.tilFullName.editText?.text.toString(),
                        "avatarUri" to (if (tmp1 == Uri.EMPTY) null else tmp1.toString()),
                        "wallUri" to (if (tmp2 == Uri.EMPTY) null else tmp2.toString()),
                        "birthday" to binding.tilBirthday.editText?.text.toString(),
                        "address" to binding.tilAddress.editText?.text.toString(),
                        "phone" to binding.tilPhone.editText?.text.toString(),
                        "bio" to binding.tilBio.editText?.text.toString(),
                        "gender" to gender
                    )
                    viewModel.startUploadWorker(data, requireContext())
                    viewModel.workStatus.observe(viewLifecycleOwner) {
                            isUploading->binding.progressBar.visibility = if (isUploading) View.VISIBLE else View.GONE
                        binding.btnSaveProfile.isEnabled = !isUploading
                    }
                }
            }
        }
    }

    private fun openGalleryForAvatar() {
        cropType=REQUEST_AVATAR_PICK
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_AVATAR_PICK)
    }

    private fun openGalleryForWall() {
        cropType=REQUEST_WALL_CAPTURE
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_WALL_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val imgUri = data.data
            when (requestCode) {
                REQUEST_AVATAR_PICK -> startCrop(imgUri!!, true)
                REQUEST_WALL_CAPTURE -> startCrop(imgUri!!, false)
                UCrop.REQUEST_CROP -> handleCropResult(data)
            }
        }
    }

    private fun startCrop(sourceUri: Uri, isAvatar: Boolean) {
        val fileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(requireContext().cacheDir, fileName)
        val destinationUri = Uri.fromFile(destinationFile)
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(if (isAvatar) 1f else 16f, if (isAvatar) 1f else 9f)
            .withMaxResultSize(1080, 1080)
        uCrop.start(requireContext(), this)
    }

    private fun setupBirthdayPicker() {
        binding.tilBirthday.editText?.setOnClickListener {
            val datepicker= MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh nhật")
                .build()
            datepicker.addOnPositiveButtonClickListener {
                    selection->val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tilBirthday.editText?.setText(sdf.format(Date(selection)))
            }
            datepicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun handleCropResult(data: Intent) {
        val resultUri = UCrop.getOutput(data)
        if (resultUri != null) {
            if (cropType == REQUEST_AVATAR_PICK) {
                Glide.with(requireContext())
                    .load(resultUri)
                    .placeholder(R.drawable.avataricon)
                    .error(R.drawable.avataricon)
                    .into(binding.imgAvatar)
                tmp1=resultUri
            } else {
                Glide.with(requireContext())
                    .load(resultUri)
                    .placeholder(R.drawable.loginbackground)
                    .error(R.drawable.loginbackground)
                    .into(binding.imgCoverPhoto)
                tmp2=resultUri
            }
        }
    }
}