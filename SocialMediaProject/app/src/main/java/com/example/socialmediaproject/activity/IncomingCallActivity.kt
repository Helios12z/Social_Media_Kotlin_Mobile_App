package com.example.socialmediaproject.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.socialmediaproject.NotificationNavigationCache
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ActivityIncomingBinding
import com.google.firebase.firestore.FirebaseFirestore

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var callerId: String
    private lateinit var roomId: String
    private lateinit var binding: ActivityIncomingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        callerId = intent.getStringExtra("callerId") ?: ""
        roomId = intent.getStringExtra("roomId") ?: ""

        db.collection("Users").document(callerId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.txtCallerName.text = doc.getString("name")
                Glide.with(this)
                    .load(doc.getString("avatarurl"))
                    .placeholder(R.drawable.avataricon)
                    .error(R.drawable.avataricon)
                    .into(binding.imgCallerAvatar)
            }
        }

        db.collection("calls").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val status = snapshot?.getString("status") ?: return@addSnapshotListener
                if (status == "ended" || status == "rejected" || status == "declined") {
                    finish()
                }
            }

        binding.btnAccept.setOnClickListener {
            db.collection("calls").document(roomId).get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val callType = snapshot.getString("type") ?: "voice"

                    db.collection("calls").document(roomId).update("status", "accepted")

                    NotificationNavigationCache.pendingIntent = intent.apply {
                        putExtra("navigateTo", if (callType == "video") "video_calling" else "calling")
                        putExtra("user_id", callerId)
                        putExtra("room_id", roomId)
                    }

                    val mainIntent = Intent(this, MainActivity::class.java)
                    mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(mainIntent)
                    finish()
                } else {
                    Toast.makeText(this, "Cuộc gọi không hợp lệ", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnDecline.setOnClickListener {
            db.collection("calls").document(roomId).update("status", "declined")
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.incomingCallLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}