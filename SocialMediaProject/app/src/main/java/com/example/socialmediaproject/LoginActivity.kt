package com.example.socialmediaproject

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var signuptext: TextView
    private lateinit var intent: Intent
    private lateinit var loginbutton: Button
    private lateinit var firebaseauth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)
        title=findViewById(R.id.title)
        SetTitleGradientColor(title)
        signuptext=findViewById(R.id.signUpText)
        signuptext.setOnClickListener { OnSignUpClicked() }
        loginbutton=findViewById(R.id.loginButton)
        firebaseauth=FirebaseAuth.getInstance()
        loginbutton.setOnClickListener {
            val email=findViewById<TextInputEditText>(R.id.email)
            val password=findViewById<TextInputEditText>(R.id.password)
            if (email.text.toString().isNotEmpty() && password.text.toString().isNotEmpty())
            {
                firebaseauth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                    .addOnCompleteListener(this) {
                        task->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            else Toast.makeText(this, "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show()
                    }
            }
            else Toast.makeText(this, "Vui lòng nhập đầy đủ các trường!", Toast.LENGTH_SHORT).show()
        }
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