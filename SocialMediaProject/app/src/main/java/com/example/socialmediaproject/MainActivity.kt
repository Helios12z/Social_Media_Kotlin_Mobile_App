package com.example.socialmediaproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.socialmediaproject.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db= FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth= FirebaseAuth.getInstance()
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

        navView.setOnItemSelectedListener {
            item->when(item.itemId) {
                R.id.navigation_plus -> {
                    val intent=Intent(this, AddPostActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
}