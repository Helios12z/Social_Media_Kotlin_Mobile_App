package com.example.socialmediaproject.activity

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
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private lateinit var logintext: TextView
    private lateinit var title: TextView
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var signupbutton: Button
    private lateinit var firebaseauth: FirebaseAuth
    private var db=FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signUpCard)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navBarHeight + 24
            )
            insets
        }
        if (savedInstanceState!=null)
        {
            binding.name.setText(savedInstanceState.getString("name"))
            binding.email.setText(savedInstanceState.getString("email"))
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
            val nicknameRegex = "^[a-zA-Z0-9_]+$".toRegex()
            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && confirmpassword.isNotEmpty())
            {
                if (password==confirmpassword)
                {
                    val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
                    if (!email.matches(emailPattern)) {
                        Toast.makeText(this, "Email không đúng định dạng!", Toast.LENGTH_SHORT).show()
                    }
                    else if (!name.matches(nicknameRegex)) {
                        Toast.makeText(this, "Nickname chỉ được chứa chữ cái, số và dấu gạch dưới!", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        val loading= LoadingDialogFragment()
                        loading.show(supportFragmentManager, "loading")
                        signupbutton.isEnabled=false
                        val usersRef = db.collection("Users")
                        usersRef.whereEqualTo("email", email).get()
                            .addOnSuccessListener { emailResult ->
                                if (!emailResult.isEmpty) {
                                    loading.dismiss()
                                    Toast.makeText(this, "Email đã tồn tại!", Toast.LENGTH_SHORT).show()
                                    signupbutton.isEnabled=true
                                }
                                else {
                                    firebaseauth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {
                                    task -> if (task.isSuccessful)
                                    {
                                        loading.dismiss()
                                        val userid=task.result.user?.uid
                                        if (userid!=null) AddNewUserToDb(binding.name.text.toString(), userid, binding.email.text.toString())
                                    }
                                    else  {
                                        loading.dismiss()
                                        Toast.makeText(this, "Đăng kí tài khoản thất bại!", Toast.LENGTH_SHORT).show()
                                        signupbutton.isEnabled=true
                                    }
                                }
                            }
                        }
                    }
                }
                else Toast.makeText(this, "Mật khẩu nhập lại không đúng!", Toast.LENGTH_SHORT).show()
            }
            else Toast.makeText(this, "Vui lòng nhập đầy đủ các trường!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("email", binding.email.text.toString())
        outState.putString("name", binding.name.text.toString())
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

    private fun AddNewUserToDb(name: String, userid: String, email: String)
    {
        val loading=LoadingDialogFragment()
        loading.show(supportFragmentManager, "loading")
        val user= hashMapOf(
            "userid" to userid,
            "name" to name,
            "email" to email,
            "role" to "user",
            "isfirsttime" to true
        )
        db.collection("Users").document(userid).set(user).addOnSuccessListener {
            loading.dismiss()
            Toast.makeText(this, "Đăng kí tài khoản thành công!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            loading.dismiss()
            Toast.makeText(this, "Đăng kí tài khoản thất bại!", Toast.LENGTH_SHORT).show()
            binding.signUpButton.isEnabled=true
        }
    }
}