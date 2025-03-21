package com.example.socialmediaproject

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmediaproject.databinding.ActivityFirstCheckBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
        chipGroup = binding.chipGroup
        btnContinue = binding.btnContinue
        auth= FirebaseAuth.getInstance()
        btnContinue.setOnClickListener {
            val userid=auth.currentUser?.uid
            if (userid != null)
            {
                val userref=db.collection("Users").document(userid)
                userref.update("isfirsttime", false)
                userref.update("interests", selectedInterests.toList())
                val intent=Intent(this, AfterFirstCheckActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    private fun GetListOfInterests(callback: ()->Unit)
    {
        db.collection("Categories").get().addOnSuccessListener {
            documents->interests.clear()
            for (document in documents)
            {
                val name=document.getString("name")
                if (name!=null) interests.add(name)
            }
            callback()
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
        btnContinue.isEnabled = selectedInterests.size >= 2
        btnContinue.setBackgroundColor(
            if (selectedInterests.size >= 2) Color.parseColor("#4F46E5") else Color.GRAY
        )
    }
}