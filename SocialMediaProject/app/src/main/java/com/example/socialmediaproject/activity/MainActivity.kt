package com.example.socialmediaproject.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.socialmediaproject.service.PostingService
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ActivityMainBinding
import com.example.socialmediaproject.service.OneSignalHelper
import com.example.socialmediaproject.service.sendPushNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db= FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForPermissions()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId!!)
                    .update("fcmToken", token)
            }
        }
        auth= FirebaseAuth.getInstance()
        checkFriendRequestsOnLogin(this)
        checkMentionsOnLogin()
        val userid=auth.currentUser?.uid
        val usersref=db.collection("Users")
        usersref.whereEqualTo("userid", userid).get().addOnSuccessListener {
            documents->if (!documents.isEmpty) {
                val userdoc=documents.documents[0]
                val isfirsttime=userdoc.getBoolean("isfirsttime") ?: false
                if (isfirsttime) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        intent= Intent(this, FirstCheckActivity::class.java)
                        startActivity(intent)
                    }, 500)
                }
            }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home
            )
        )
        navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_plus -> {
                    if (!PostingService.isposting) {
                        if (!navController.popBackStack(R.id.navigation_post, false)) {
                            navController.navigate(R.id.navigation_post)
                        }
                    } else {
                        if (!navController.popBackStack(R.id.navigation_plus, false)) {
                            navController.navigate(R.id.navigation_plus)
                        }
                    }
                    true
                }
                R.id.navigation_home -> {
                    navController.popBackStack(navController.graph.startDestinationId, false)
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_account -> {
                    if (!navController.popBackStack(R.id.navigation_account, false)) {
                        navController.navigate(R.id.navigation_account)
                    }
                    true
                }
                R.id.navigation_search->{
                    if (!navController.popBackStack(R.id.navigation_search, false)) {
                        navController.navigate(R.id.navigation_search)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }

    fun checkFriendRequestsOnLogin(context: Context) {
        val currentUserId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshots = db.collection("friend_requests")
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("status", "pending")
                    .whereEqualTo("notified", false)
                    .get()
                    .await()
                val count = snapshots.size()
                if (count > 0) {
                    sendPushNotification(currentUserId, "Bạn có $count lời mời kết bạn mới!")
                    val batch = db.batch()
                    for (doc in snapshots.documents) {
                        batch.update(doc.reference, "notified", true)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {

            }
        }
    }

    fun checkMentionsOnLogin() {
        val currentUserId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("comments")
                    .whereArrayContains("mentionedUserIds", currentUserId)
                    .get()
                    .await()
                val unnotified = snapshot.documents.filter { doc ->
                    val notifiedList = doc.get("notifiedUserIds") as? List<*> ?: emptyList<Any>()
                    !notifiedList.contains(currentUserId)
                }
                val count = unnotified.size
                if (count > 0) {
                    OneSignalHelper.sendMentionNotification(
                        userId = currentUserId,
                        commentId = "",
                        message = "Bạn đã được nhắc đến trong $count bình luận"
                    )
                    val batch = db.batch()
                    for (doc in unnotified) {
                        batch.update(doc.reference, "notifiedUserIds", FieldValue.arrayUnion(currentUserId))
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}