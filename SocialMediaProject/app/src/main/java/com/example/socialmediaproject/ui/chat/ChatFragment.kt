package com.example.socialmediaproject.ui.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.ChatUserAdapter
import com.example.socialmediaproject.databinding.FragmentChatBinding
import com.example.socialmediaproject.ui.chatdetail.ChatDetailFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialmediaproject.activity.MainActivity

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatUserAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentChatBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).hideNavigationWithBlur()
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
        val menuHost: MenuHost = binding.toolbar
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_chat, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_search -> {
                        findNavController().navigate(R.id.navigation_search_chat_user)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }
}