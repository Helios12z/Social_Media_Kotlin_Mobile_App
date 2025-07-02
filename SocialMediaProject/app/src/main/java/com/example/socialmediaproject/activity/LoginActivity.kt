package com.example.socialmediaproject.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.NotificationNavigationCache
import com.example.socialmediaproject.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal

class LoginActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var signuptext: TextView
    private lateinit var intent: Intent
    private lateinit var loginbutton: Button
    private lateinit var firebaseauth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rememberme: CheckBox
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var forgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        if (sharedPreferences.getBoolean("rememberMe", false)) {
            val savedEmail = sharedPreferences.getString("email", null)
            val savedPassword = sharedPreferences.getString("password", null)
            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                autoLogin(savedEmail, savedPassword)
            }
        }
        setContentView(R.layout.login)
        title=findViewById(R.id.title)
        signuptext=findViewById(R.id.signUpText)
        signuptext.setOnClickListener { onSignUpClicked() }
        loginbutton=findViewById(R.id.loginButton)
        forgotPassword=findViewById(R.id.forgotPassword)
        rememberme=findViewById(R.id.rememberMe)
        firebaseauth=FirebaseAuth.getInstance()
        loginbutton.setOnClickListener {
            val email=findViewById<TextInputEditText>(R.id.email)
            val password=findViewById<TextInputEditText>(R.id.password)
            if (email.text.toString().isNotEmpty() && password.text.toString().isNotEmpty())
            {
                loginbutton.isEnabled=false
                val loading=LoadingDialogFragment()
                loading.show(supportFragmentManager, "loading")
                db=FirebaseFirestore.getInstance()
                firebaseauth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener {
                    task-> if (task.isSuccessful) {
                    val userId = firebaseauth.currentUser?.uid ?: ""
                    db.collection("Users").document(userId).get()
                        .addOnSuccessListener { doc ->
                            val userEmailAuth = firebaseauth.currentUser?.email
                            val emailInFirestore = doc.getString("email")

                            if (userEmailAuth != null && emailInFirestore != null && userEmailAuth != emailInFirestore) {
                                db.collection("Users").document(userId)
                                    .update("email", userEmailAuth)
                                    .addOnSuccessListener {
                                        //TODO: think something to do here
                                    }
                                    .addOnFailureListener {
                                        //TODO: think something to do here
                                    }
                            }

                            val isBanned = doc.getBoolean("isBanned") ?: false
                            if (isBanned) {
                                if (!loading.isStateSaved) loading.dismiss()
                                else loading.dismissAllowingStateLoss()

                                AlertDialog.Builder(this)
                                    .setTitle("Tài khoản bị cấm")
                                    .setMessage("Tài khoản của bạn đã bị cấm bởi quản trị viên.")
                                    .setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                        firebaseauth.signOut()
                                    }
                                    .setCancelable(false)
                                    .show()
                                loginbutton.isEnabled=true
                            }
                            else {
                                if (!loading.isStateSaved) {
                                    loading.dismiss()
                                } else {
                                    loading.dismissAllowingStateLoss()
                                }
                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                if (rememberme.isChecked) {
                                    sharedPreferences.edit()
                                        .putBoolean("rememberMe", true)
                                        .putString("email", email.text.toString())
                                        .putString("password", password.text.toString())
                                        .apply()
                                } else {
                                    sharedPreferences.edit().clear().apply()
                                }
                                OneSignal.login(firebaseauth.currentUser?.uid?:"")
                                navigateToMainActivity()
                            }
                        }
                        .addOnFailureListener {
                            if (!loading.isStateSaved) {
                                loading.dismiss()
                            } else {
                                loading.dismissAllowingStateLoss()
                            }
                            Toast.makeText(this, "Tên đăng nhập/mật khẩu không chính xác hoặc không có internet!", Toast.LENGTH_SHORT).show()
                            loginbutton.isEnabled=true
                        }
                    }
                    else {
                        if (!loading.isStateSaved) {
                            loading.dismiss()
                        } else {
                            loading.dismissAllowingStateLoss()
                        }
                        Toast.makeText(this, "Tên đăng nhập/mật khẩu không chính xác hoặc không có internet!", Toast.LENGTH_SHORT).show()
                        loginbutton.isEnabled=true
                    }
                }
                .addOnFailureListener {
                    if (!loading.isStateSaved) {
                        loading.dismiss()
                    } else {
                        loading.dismissAllowingStateLoss()
                    }
                    Toast.makeText(this, "Tên đăng nhập/mật khẩu không chính xác hoặc không có internet!", Toast.LENGTH_SHORT).show()
                    loginbutton.isEnabled=true
                }
            }
            else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ các trường!", Toast.LENGTH_SHORT).show()
                loginbutton.isEnabled=true
            }
        }
        forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun onSignUpClicked()
    {
        intent=Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun autoLogin(email: String, password: String) {
        val loading = LoadingDialogFragment()
        loading.show(supportFragmentManager, "loading")
        firebaseauth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        firebaseauth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firebaseauth.currentUser?.uid ?: ""
                    db.collection("Users").document(userId).get()
                        .addOnSuccessListener { doc ->
                            val isBanned = doc.getBoolean("isBanned") ?: false
                            if (isBanned) {
                                if (!loading.isStateSaved) loading.dismiss()
                                else loading.dismissAllowingStateLoss()

                                AlertDialog.Builder(this)
                                    .setTitle("Tài khoản bị cấm")
                                    .setMessage("Tài khoản của bạn đã bị cấm bởi quản trị viên.")
                                    .setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                        sharedPreferences.edit().clear().apply()
                                        firebaseauth.signOut()
                                    }
                                    .setCancelable(false)
                                    .show()
                            } else {
                                if (!loading.isStateSaved) loading.dismiss()
                                else loading.dismissAllowingStateLoss()

                                OneSignal.login(userId)
                                navigateToMainActivity()
                            }
                        }
                        .addOnFailureListener {
                            if (!loading.isStateSaved) loading.dismiss()
                            else loading.dismissAllowingStateLoss()

                            sharedPreferences.edit().clear().apply()
                            Toast.makeText(this, "Lỗi khi xác thực người dùng!", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    if (!loading.isStateSaved) loading.dismiss()
                    else loading.dismissAllowingStateLoss()

                    sharedPreferences.edit().clear().apply()
                    Toast.makeText(this, "Tên đăng nhập/mật khẩu không chính xác hoặc không có internet!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        val pendingIntent = NotificationNavigationCache.pendingIntent
        if (pendingIntent != null) {
            pendingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(pendingIntent)
            NotificationNavigationCache.pendingIntent = null
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }
}