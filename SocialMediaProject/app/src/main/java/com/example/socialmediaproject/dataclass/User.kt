package com.example.socialmediaproject.dataclass

import com.google.firebase.database.PropertyName

data class User(val userid: String = "",
                val name: String = "",
                val avatarurl: String = "",
                val email: String = "",
                val friends: List<String> = listOf())
