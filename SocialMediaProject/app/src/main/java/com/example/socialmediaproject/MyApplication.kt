package com.example.socialmediaproject

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.example.socialmediaproject.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener

class MyApplication: Application(), Application.ActivityLifecycleCallbacks {
    private lateinit var auth: FirebaseAuth

    override fun onCreate() {
        super.onCreate()
        OneSignal.Debug.logLevel=LogLevel.VERBOSE
        OneSignal.initWithContext(this, "e354e0b8-a22b-4662-8696-6d2431f7191c")
        registerActivityLifecycleCallbacks(this)
        initNotificationClickHandler()
    }

    private fun initNotificationClickHandler() {
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(result: INotificationClickEvent) {
                val data = result.notification.additionalData
                val type = data?.optString("type")
                val context = this@MyApplication
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                when (type) {
                    "voice_call" -> {
                        val callerId = data.optString("callerId")
                        if (!callerId.isNullOrEmpty()) {
                            intent.putExtra("navigateTo", "chat")
                            intent.putExtra("senderId", callerId)
                        }
                    }

                    "add_friend" -> {
                        val senderId = data.optString("senderId")
                        if (!senderId.isNullOrEmpty()) {
                            if (senderId=="all") {
                                intent.putExtra("navigateTo", "notificationPage")
                            }
                            else {
                                intent.putExtra("navigateTo", "mainPage")
                                intent.putExtra("wall_user_id", senderId)
                            }
                        }
                    }

                    "mention" -> {
                        val commentId = data.optString("commentId")
                        val postId=data.optString("postId")
                        if (!commentId.isNullOrEmpty() && !postId.isNullOrEmpty()) {
                            if (postId=="all" && commentId=="all") {
                                intent.putExtra("navigateTo", "notificationPage")
                            }
                            else {
                                intent.putExtra("navigateTo", "postFullPage")
                                intent.putExtra("postId", postId)
                                intent.putExtra("commentId", commentId)
                            }
                        }
                    }
                }
                auth=FirebaseAuth.getInstance()
                val user=auth.currentUser
                if (user!=null) context.startActivity(intent)
                else NotificationNavigationCache.pendingIntent = intent
            }
        })
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            Constant.PresenceHelper.updateLastActive(uid)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is MainActivity) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            Constant.PresenceHelper.setOffline(uid)
        }
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }
}

object NotificationNavigationCache {
    var pendingIntent: Intent? = null
}