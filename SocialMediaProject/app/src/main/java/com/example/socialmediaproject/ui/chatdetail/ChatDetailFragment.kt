package com.example.socialmediaproject.ui.chatdetail

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.MessageAdapter
import com.example.socialmediaproject.databinding.FragmentChatDetailBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.example.socialmediaproject.dataclass.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatDetailFragment : Fragment() {
    private lateinit var binding: FragmentChatDetailBinding
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var chatUser: ChatUser
    private val auth=FirebaseAuth.getInstance()

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
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = if (currentUserId < chatUser.id) "${currentUserId}_${chatUser.id}"
        else "${chatUser.id}_${currentUserId}"
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(requireContext())
        val adapter = MessageAdapter(currentUserId, chatUser.avatarUrl)
        binding.recyclerViewMessages.adapter = adapter
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        }
        viewModel.loadMessages(chatId, currentUserId)
        viewModel.startListeningMessages(chatId, currentUserId)
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val message = Message(
                    senderId = currentUserId,
                    receiverId = chatUser.id,
                    text = text,
                    timestamp = Timestamp.now()
                )
                viewModel.sendMessage(chatId, message)
                binding.etMessage.setText("")
            }
        }
        checkIfCanSendMessage(auth.currentUser?.uid?:"", chatUser.id)
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopListening()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    fun checkIfCanSendMessage(currentUserId: String, friendId: String) {
        FirebaseFirestore.getInstance().collection("Users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                val friends = doc["friends"] as? List<String> ?: emptyList()
                val canChat = friendId in friends
                if (!canChat) {
                    binding.etMessage.isEnabled = false
                    binding.btnSend.visibility = View.GONE
                    binding.etMessage.hint = "2 người không còn là bạn bè"
                }
            }
    }
}