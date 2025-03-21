package com.example.socialmediaproject

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.socialmediaproject.databinding.ActivityAddPostBinding

class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed()
    {
        val intent=Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}