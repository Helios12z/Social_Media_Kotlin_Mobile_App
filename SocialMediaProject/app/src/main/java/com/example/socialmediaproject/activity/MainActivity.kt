package com.example.socialmediaproject.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.socialmediaproject.NotificationNavigationCache
import com.example.socialmediaproject.service.PostingService
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ActivityMainBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.example.socialmediaproject.service.OneSignalHelper
import com.example.socialmediaproject.ui.notification.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db= FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationViewModel: NotificationViewModel
    private var bannedListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth= FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
            finish()
            return
        }
        val userid=auth.currentUser?.uid
        observeUserBannedStatus(userid ?: return)
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
        notificationViewModel=ViewModelProvider(this)[NotificationViewModel::class.java]
        notificationViewModel.fetchNotifications()
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        notificationViewModel.notificationsLiveData.observe(this) { notifications ->
            val unreadCount = notifications.count { !it.read }
            val badge = navView.getOrCreateBadge(R.id.navigation_notification)
            if (unreadCount > 0) {
                badge.isVisible = true
                badge.number = unreadCount
            } else {
                badge.clearNumber()
                badge.isVisible = false
            }
        }
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
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
                    if (!navController.popBackStack(R.id.navigation_home, false)) {
                        navController.navigate(R.id.navigation_home)
                    }
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
                R.id.navigation_notification->{
                    if (!navController.popBackStack(R.id.navigation_notification, false)) {
                        navController.navigate(R.id.navigation_notification)
                    }
                    true
                }
                else -> false
            }
        }
        handleIntent(intent)
        askForPermissions()
        checkFriendRequestsOnLogin(this)
        checkMentionsOnLogin()
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
                    OneSignalHelper.sendAddFriendNotification(currentUserId,
                        "Bạn có $count lời mời kết bạn mới!",
                        "all")
                    val batch = db.batch()
                    for (doc in snapshots.documents) {
                        batch.update(doc.reference, "notified", true)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    OneSignalHelper.sendMentionNotification(currentUserId,
                        "Bạn có $count lượt nhắc trong bình luận mới!",
                        "all",
                        "all")
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

    override fun onDestroy() {
        super.onDestroy()
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        if (!sharedPreferences.getBoolean("rememberMe", false)) {
            val auth=FirebaseAuth.getInstance()
            OneSignal.logout()
            sharedPreferences.edit().clear().apply()
            auth.signOut()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    //handle when the users click push notifications
    private fun handleIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigateTo")
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        when (navigateTo) {
            "chat" -> {
                val senderId = intent.getStringExtra("senderId")
                if (senderId != null) {
                    openChatDetailWithUser(senderId)
                }
            }

            "incoming_call" -> {
                val callerId = intent.getStringExtra("callerId")
                val roomId = intent.getStringExtra("roomId")
                val callIntent = Intent(this, IncomingCallActivity::class.java).apply {
                    putExtra("callerId", callerId)
                    putExtra("roomId", roomId)
                }
                startActivity(callIntent)
            }

            "calling" -> {
                val userId = intent.getStringExtra("user_id")
                val roomId = intent.getStringExtra("room_id")
                val bundle = Bundle().apply {
                    putString("user_id", userId)
                    putString("room_id", roomId)
                }
                navController.navigate(R.id.navigation_calling, bundle)
            }

            "notificationPage" -> {
                navController.navigate(R.id.navigation_notification)
            }

            "postFullPage" -> {
                val postId=intent.getStringExtra("postId")
                val commentId=intent.getStringExtra("commentId")
                val bundle=Bundle().apply {
                    putString("post_id", postId)
                    putString("comment_id", commentId)
                }
                navController.navigate(R.id.navigation_postWithComment, bundle)
            }

            "mainPage" -> {
                val wallUserId=intent.getStringExtra("wall_user_id")
                val bundle=Bundle().apply {
                    putString("wall_user_id", wallUserId)
                }
                navController.navigate(R.id.navigation_mainpage, bundle)
            }
        }
    }

    private fun openChatDetailWithUser(senderId: String) {
        db.collection("Users").document(senderId).get().addOnSuccessListener {
            result->if (result.exists()) {
                val selectedUser=ChatUser(
                    id=senderId,
                    username=result.getString("name")?:"",
                    avatarUrl=result.getString("avatarurl")
                )
                val bundle = Bundle().apply {
                    putParcelable("chatUser", selectedUser)
                }
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                navController.navigate(R.id.navigation_chatdetail, bundle)
            }
            else Toast.makeText(this, "Người gọi không tồn tại", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        NotificationNavigationCache.pendingIntent?.let { pendingIntent ->
            handleIntent(pendingIntent)
            NotificationNavigationCache.pendingIntent = null
        }
    }

    private fun observeUserBannedStatus(userId: String) {
        bannedListener = db.collection("Users").document(userId)
            .addSnapshotListener { docSnapshot, _ ->
                if (docSnapshot != null && docSnapshot.exists()) {
                    val isBanned = docSnapshot.getBoolean("isBanned") ?: false
                    if (isBanned) {
                        Toast.makeText(this, "Tài khoản của bạn đã bị cấm bởi quản trị viên.", Toast.LENGTH_LONG).show()

                        OneSignal.logout()
                        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                        sharedPreferences.edit().clear().apply()
                        FirebaseAuth.getInstance().signOut()

                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
    }
}