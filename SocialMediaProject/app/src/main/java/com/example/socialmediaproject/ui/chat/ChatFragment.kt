package com.example.socialmediaproject.ui.chat

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.ChatUserAdapter
import com.example.socialmediaproject.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatUserAdapter
    private lateinit var recyclerView: RecyclerView
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentChatBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView=binding.recyclerViewFriends
        recyclerView.layoutManager= LinearLayoutManager(requireContext())
        viewModel.chatUsers.observe(viewLifecycleOwner) {
            users->adapter = ChatUserAdapter(users) { selectedUser ->
            // TODO: mở màn hình chat detail
        }
            recyclerView.adapter = adapter
        }
        viewModel.loadFriends(auth.currentUser?.uid ?: "")
    }
}