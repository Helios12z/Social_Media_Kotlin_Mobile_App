package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentEditPostBinding

class EditPostFragment : Fragment() {
    private lateinit var binding: FragmentEditPostBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }
}