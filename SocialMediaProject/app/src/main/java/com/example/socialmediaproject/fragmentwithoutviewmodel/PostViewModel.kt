package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel

class PostViewModel: ViewModel() {
    var postContent: String = ""
    var postPrivacy: String = ""
    var hasStashedSave: Boolean = false
    var stashedPostId: String = ""

    fun saveStashedChange(postId: String, content: String, privacy: String) {
        postContent=content
        postPrivacy=privacy
        stashedPostId=postId
        hasStashedSave=true
    }
}