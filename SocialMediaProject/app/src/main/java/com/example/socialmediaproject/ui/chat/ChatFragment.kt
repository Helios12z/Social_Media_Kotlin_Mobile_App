package com.example.socialmediaproject.ui.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.ChatUserAdapter
import com.example.socialmediaproject.databinding.FragmentChatBinding
import com.example.socialmediaproject.ui.chatdetail.ChatDetailFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        recyclerView=binding.recyclerViewFriends
        recyclerView.layoutManager= LinearLayoutManager(requireContext())
        viewModel.chatUsers.observe(viewLifecycleOwner) {
            users->adapter = ChatUserAdapter(users) { selectedUser ->
            val bundle = Bundle()
            bundle.putParcelable("chatUser", selectedUser)
            val chatDetailFragment = ChatDetailFragment()
            chatDetailFragment.arguments = bundle
            findNavController().navigate(R.id.action_chatFragment_to_chatDetailFragment, bundle)
        }
            recyclerView.adapter = adapter
        }
        viewModel.initializeFriends(auth.currentUser?.uid ?: "")
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