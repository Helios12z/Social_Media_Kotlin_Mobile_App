package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel

class PostViewModel: ViewModel() {
    val postContent: String = ""
    val postPrivacy: String = ""
    val postImgUrl= mutableListOf<Uri>()


}