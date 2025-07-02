package com.example.socialmediaproject.ui.searchchatuser

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
import com.example.socialmediaproject.adapter.FriendAdapter
import com.example.socialmediaproject.databinding.FragmentSearchChatUserBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.example.socialmediaproject.dataclass.Friend
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialmediaproject.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth

class SearchChatUserFragment : Fragment() {
    private lateinit var binding: FragmentSearchChatUserBinding
    private var fullUserList: List<Friend> = emptyList()
    private lateinit var  auth: FirebaseAuth
    private lateinit var  viewModel: SearchChatUserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentSearchChatUserBinding.inflate(inflater, container, false)
        auth=FirebaseAuth.getInstance()
        viewModel= ViewModelProvider(requireActivity())[SearchChatUserViewModel::class.java]
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter=FriendAdapter{
            chatUsers->val chatUser=ChatUser(
                id=chatUsers.id,
                username=chatUsers.displayName,
                avatarUrl=chatUsers.avatarUrl)
                val bundle=Bundle().apply {
                putParcelable("chatUser", chatUser)
            }
            findNavController().navigate(R.id.navigation_chatdetail, bundle)
        }
        binding.recyclerViewFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            this.adapter = adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(rv, dx, dy)
                    if (!rv.canScrollVertically(1) && !viewModel.isLoading) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
        viewModel.friends.observe(viewLifecycleOwner) { list ->
            fullUserList=list
            val query = binding.searchViewFriends.query?.toString().orEmpty()
            if (query.isEmpty()) {
                adapter.submitList(fullUserList)
            } else {
                adapter.submitList(filterList(query))
            }
        }
        binding.searchViewFriends.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText.orEmpty()
                adapter.submitList(filterList(q))
                return true
            }
        })
        viewModel.loadInitialFriends(auth.currentUser?.uid?:"")
    }

    private fun filterList(query: String): List<Friend> {
        if (query.isEmpty()) return fullUserList
        val terms = query.lowercase().split("\\s+".toRegex())
        return fullUserList.filter { f ->
            val nameCombined = "${f.displayName} ${f.fullName}".lowercase()
            terms.all { term -> nameCombined.contains(term) }
        }
    }
}