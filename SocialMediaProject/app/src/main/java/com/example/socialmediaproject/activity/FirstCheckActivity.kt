package com.example.socialmediaproject.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ActivityFirstCheckBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirstCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstCheckBinding
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnContinue: Button
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val selectedInterests = mutableSetOf<String>()
    private val interests = mutableListOf<String>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_first_check)
        binding= ActivityFirstCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        GetListOfInterests {
            setupChips()
        }
        binding.btnContinue.isEnabled=false
        binding.btnContinue.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        chipGroup = binding.chipGroup
        btnContinue = binding.btnContinue
        auth= FirebaseAuth.getInstance()
        btnContinue.setOnClickListener {
            val userid=auth.currentUser?.uid
            if (userid != null)
            {
                val loading=LoadingDialogFragment()
                loading.show(supportFragmentManager, "loading")
                val userref=db.collection("Users").document(userid)
                userref.update("interests", selectedInterests.toList()).addOnSuccessListener {
                    userref.update("isfirsttime", false).addOnSuccessListener {
                        loading.dismiss()
                        val intent=Intent(this, AfterFirstCheckActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        loading.dismiss()
                        Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    loading.dismiss()
                    Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    private fun GetListOfInterests(callback: ()->Unit)
    {
        val loading=LoadingDialogFragment()
        loading.show(supportFragmentManager, "loading")
        db.collection("Categories").get().addOnSuccessListener {
            documents->interests.clear()
            loading.dismiss()
            for (document in documents)
            {
                val name=document.getString("name")
                if (name!=null) interests.add(name)
            }
            callback()
        }
        .addOnFailureListener {
            loading.dismiss()
            Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChips() {
        interests.forEach { interest ->
            val chip = Chip(this).apply {
                text = interest
                isCheckable = true
                setTextColor(Color.WHITE)
                setChipBackgroundColorResource(R.color.chip_selector)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedInterests.add(interest)
                    else selectedInterests.remove(interest)
                    updateContinueButton()
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateContinueButton() {
        val isEnabled = selectedInterests.size >= 2
        btnContinue.isEnabled = isEnabled
        val color = if (isEnabled) {
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.GRAY)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        btnContinue.setBackgroundColor(color)
    }
}