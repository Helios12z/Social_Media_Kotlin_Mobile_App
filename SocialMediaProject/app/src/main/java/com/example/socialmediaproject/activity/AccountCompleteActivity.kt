package com.example.socialmediaproject.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmediaproject.databinding.ActivityAccountCompleteBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.Date
import java.util.Locale

private const val REQUEST_AVATAR_PICK = 1000
private const val REQUEST_WALL_CAPTURE = 1001
class AccountCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountCompleteBinding
    private lateinit var tilbirthday: TextInputLayout
    private lateinit var tiladdress: TextInputLayout
    private lateinit var tilphone: TextInputLayout
    private lateinit var tilbio: TextInputLayout
    private lateinit var imgavatar: ShapeableImageView
    private lateinit var imgcoverphoto: ImageView
    private lateinit var genderspinner: Spinner
    private lateinit var avataruri: Uri
    private lateinit var walluri: Uri
    private lateinit var savebtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var gender:String
    private var cropType: Int=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityAccountCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tilbirthday=binding.tilBirthday
        tiladdress=binding.tilAddress
        tilphone=binding.tilPhone
        tilbio=binding.tilBio
        imgavatar=binding.imgAvatar
        imgcoverphoto=binding.imgCoverPhoto
        savebtn=binding.btnSaveProfile
        db=FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        val genderlist= mutableListOf<String>()
        db.collection("Genders").get().addOnSuccessListener {
            documents->if (documents!=null) {
                for (document in documents) {
                    genderlist.add(document.getString("name")?:"")
                }
                val adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderlist)
                genderspinner=binding.spinnerGender
                genderspinner.adapter=adapter
                genderspinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        gender=genderlist[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }
            }
        }
        setupBirthdayPicker()
        imgavatar.setOnClickListener {
            openGalleryForAvatar()
        }
        imgcoverphoto.setOnClickListener {
            openGalleryForWall()
        }
        var birthday:String
        var address:String
        var phone:String
        var bio:String
        savebtn.setOnClickListener {
            if (tilbirthday.editText?.text.isNullOrEmpty())
            {
                Toast.makeText(this, "Vui lòng nhập đầy ngày sinh nhật!", Toast.LENGTH_SHORT).show()
            }
            else {
                if (tiladdress.editText?.text.isNullOrEmpty()) address=""
                else address=tiladdress.editText?.text.toString()
                if (tilphone.editText?.text.isNullOrEmpty()) phone=""
                else phone=tilphone.editText?.text.toString()
                if (tilbio.editText?.text.isNullOrEmpty()) bio=""
                else bio=tilbio.editText?.text.toString()
                birthday=tilbirthday.editText?.text.toString()
                val userid=auth.currentUser?.uid
                if (userid!=null) {
                    val userupdate= hashMapOf(
                        "address" to address,
                        "phonenumber" to phone,
                        "bio" to bio,
                        "birthday" to birthday,
                        "gender" to gender,
                    )
                    db.collection("Users").document(userid).set(userupdate, SetOptions.merge()).addOnSuccessListener {
                        Toast.makeText(this, "Cập nhật tài khoản hoàn tất!", Toast.LENGTH_SHORT).show()
                        val intent=Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun setupBirthdayPicker() {
        tilbirthday.editText?.setOnClickListener {
            val datepicker=MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh nhật")
                .build()
            datepicker.addOnPositiveButtonClickListener {
                selection->val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tilbirthday.editText?.setText(sdf.format(Date(selection)))
            }
            datepicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun openGalleryForAvatar() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_AVATAR_PICK)
    }

    private fun openGalleryForWall() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_WALL_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val imgUri = data.data
            when (requestCode) {
                REQUEST_AVATAR_PICK -> startCrop(imgUri?:Uri.EMPTY, true)
                REQUEST_WALL_CAPTURE -> startCrop(imgUri?:Uri.EMPTY, false)
                UCrop.REQUEST_CROP -> handleCropResult(data)
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val builder=AlertDialog.Builder(this)
        builder.setTitle("Xác nhận")
        builder.setMessage("Bạn có muốn thoát và bỏ qua quá trình hoàn tất tài khoản không?")
        builder.setPositiveButton("Có") { dialog, which ->
            finish()
        }
        builder.setNegativeButton("Không") {
                dialog, which ->
        }
        builder.show()
    }

    private fun startCrop(sourceUri: Uri, isAvatar: Boolean) {
        cropType = if (isAvatar) REQUEST_AVATAR_PICK else REQUEST_WALL_CAPTURE
        val fileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(cacheDir, fileName)
        val destinationUri = Uri.fromFile(destinationFile)
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(if (isAvatar) 1f else 16f, if (isAvatar) 1f else 9f)
            .withMaxResultSize(1080, 1080)
        uCrop.start(this)
    }

    private fun handleCropResult(data: Intent) {
        val resultUri = UCrop.getOutput(data)
        Log.d("UCrop", "Cropped Image URI: $resultUri")
        if (resultUri != null) {
            if (cropType == REQUEST_AVATAR_PICK) {
                avataruri = resultUri
                imgavatar.setImageURI(avataruri)
            } else {
                walluri = resultUri
                imgcoverphoto.setImageURI(walluri)
            }
        }
    }
}