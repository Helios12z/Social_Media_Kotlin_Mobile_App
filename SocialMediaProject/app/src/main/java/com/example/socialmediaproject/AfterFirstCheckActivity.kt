package com.example.socialmediaproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.socialmediaproject.databinding.ActivityAfterFirstCheckBinding

class AfterFirstCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAfterFirstCheckBinding
    private lateinit var btnno: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityAfterFirstCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnNo.setOnClickListener {
            OnButtonNoClicked()
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