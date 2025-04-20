package com.example.socialmediaproject.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmediaproject.databinding.ActivityAfterFirstCheckBinding

class AfterFirstCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAfterFirstCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityAfterFirstCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnNo.setOnClickListener {
            OnButtonNoClicked()
        }
        binding.btnYes.setOnClickListener {
            val intent= Intent(this, AccountCompleteActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    private fun OnButtonNoClicked()
    {
        finish()
    }
}