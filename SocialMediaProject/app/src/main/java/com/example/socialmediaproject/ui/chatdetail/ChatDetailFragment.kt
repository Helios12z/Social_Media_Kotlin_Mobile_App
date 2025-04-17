package com.example.socialmediaproject.ui.chatdetail

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentChatBinding
import com.example.socialmediaproject.databinding.FragmentChatDetailBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatDetailFragment : Fragment() {
    private lateinit var binding: FragmentChatDetailBinding
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var chatUser: ChatUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        binding=FragmentChatDetailBinding.inflate(inflater, container, false)
        chatUser = arguments?.getParcelable("chatUser") ?: ChatUser()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        binding.tvChatUsername.text = chatUser.username
        Glide.with(requireContext())
            .load(chatUser.avatarUrl)
            .placeholder(R.drawable.avataricon)
            .into(binding.ivChatAvatar)
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }
}