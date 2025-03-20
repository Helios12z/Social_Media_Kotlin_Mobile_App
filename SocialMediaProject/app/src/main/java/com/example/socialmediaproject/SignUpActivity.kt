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
import com.example.socialmediaproject.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var logintext: TextView
    private lateinit var intent: Intent
    private lateinit var title: TextView
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var signupbutton: Button
    private lateinit var firebaseauth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        logintext=binding.loginText
        logintext.setOnClickListener { OnLoginTextClikec() }
        title=binding.title
        SetTitleGradientColor(title)
        signupbutton=binding.signUpButton
        firebaseauth=FirebaseAuth.getInstance()
        signupbutton.setOnClickListener {
            val email=binding.email.text.toString()
            val password=binding.password.text.toString()
            val confirmpassword=binding.confirmPassword.text.toString()
            val name=binding.name.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && confirmpassword.isNotEmpty())
                if (password==confirmpassword)
                    firebaseauth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {
                        task -> if (task.isSuccessful)
                        {
                            Toast.makeText(this, "Đăng kí tài khoản thành công!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        else
                            Toast.makeText(this, "Đăng kí tài khoản thất bại!", Toast.LENGTH_SHORT).show()
                    }
                else Toast.makeText(this, "Mật khẩu nhập lại không đúng!", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, "Vui lòng nhập đầy đủ các trường!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun OnLoginTextClikec()
    {
        finish();
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
}