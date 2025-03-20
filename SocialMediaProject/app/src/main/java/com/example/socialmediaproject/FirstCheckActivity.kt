package com.example.socialmediaproject

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.example.socialmediaproject.databinding.ActivityFirstCheckBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FirstCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstCheckBinding
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnContinue: Button
    private val selectedInterests = mutableSetOf<String>()
    private val interests = listOf(
        "Daily life", "Comedy", "Entertainment", "Animals", "Food", "Drama",
        "Beauty & Style", "Learning", "Fitness & Gym", "Auto", "Family",
        "Health & Care", "DIY", "Daily life hacks", "Arts & Crafts",
        "Dance", "Outdoors", "Sports"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_first_check)
        binding= ActivityFirstCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        chipGroup = binding.chipGroup
        btnContinue = binding.btnContinue
        setupChips()
    }

    private fun setupChips() {
        interests.forEach { interest ->
            val chip = Chip(this).apply {
                text = interest
                isCheckable = true
                setTextColor(Color.WHITE)
                setChipBackgroundColorResource(R.color.chip_selector)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedInterests.add(interest)
                    else selectedInterests.remove(interest)
                    updateContinueButton()
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateContinueButton() {
        btnContinue.isEnabled = selectedInterests.size >= 3
        btnContinue.setBackgroundColor(
            if (selectedInterests.size >= 3) Color.parseColor("#4F46E5") else Color.GRAY
        )
    }
}