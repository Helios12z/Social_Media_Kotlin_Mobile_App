package com.example.socialmediaproject

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.socialmediaproject.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class MyApplication: Application(), Application.ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        OneSignal.Debug.logLevel=LogLevel.VERBOSE
        OneSignal.initWithContext(this, "e354e0b8-a22b-4662-8696-6d2431f7191c")
        registerActivityLifecycleCallbacks(this)
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