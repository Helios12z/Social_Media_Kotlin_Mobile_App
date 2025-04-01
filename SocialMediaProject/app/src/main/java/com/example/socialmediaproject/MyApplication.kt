package com.example.socialmediaproject

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.Debug.logLevel=LogLevel.VERBOSE
        OneSignal.initWithContext(this, "e354e0b8-a22b-4662-8696-6d2431f7191c")
    }
}