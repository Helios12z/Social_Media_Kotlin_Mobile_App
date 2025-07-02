package com.example.socialmediaproject.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: TextInputEditText
    private lateinit var sendButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()
        emailInput = findViewById(R.id.etEmail)
        sendButton = findViewById(R.id.btnSendResetLink)

        sendButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loading = LoadingDialogFragment()
            loading.show(supportFragmentManager, "loading")

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    loading.dismiss()
                    if (it.isSuccessful) {
                        AlertDialog.Builder(this)
                            .setTitle("Email đã được gửi")
                            .setMessage("Vui lòng kiểm tra email để đặt lại mật khẩu.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        Toast.makeText(this, "Không thể gửi email: ${it.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    loading.dismiss()
                    Toast.makeText(this, "Lỗi gửi email: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}