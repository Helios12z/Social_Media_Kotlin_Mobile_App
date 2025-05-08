package com.example.socialmediaproject.dataclass

data class User(val userid: String = "",
                val name: String = "",
                val avatarurl: String = "",
                val email: String = "",
                val friends: List<String> = listOf(),
                val fullName: String ="",
                val bio:String="")
