package com.example.socialmediaproject

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var signuptext: TextView
    private lateinit var intent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)
        title=findViewById(R.id.title)
        SetTitleGradientColor(title)
        signuptext=findViewById(R.id.signUpText)
        signuptext.setOnClickListener { OnSignUpClicked() }
    }

    private fun SetTitleGradientColor(title: TextView)
    {
        val paint = title.paint
        val width = title.paint.measureText(title.text.toString())
        val shader = LinearGradient(
            0f, 0f, width, 0f,
            intArrayOf(0xFF000000.toInt(), 0xFF800080.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
    }

    private fun OnSignUpClicked()
    {
        intent=Intent(this,SignUpActivity::class.java)
        startActivity(intent)
    }
}