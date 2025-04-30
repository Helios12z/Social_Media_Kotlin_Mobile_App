package com.example.socialmediaproject.ui.friendlist

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
import com.example.socialmediaproject.databinding.FragmentFriendListBinding

class FriendListFragment : Fragment() {
    private lateinit var binding: FragmentFriendListBinding
    private lateinit var viewModel: FriendListViewModel
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= FragmentFriendListBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[FriendListViewModel::class.java]
        userId=arguments?.getString("user_id")?:""
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = FriendAdapter { friend ->
            val b = Bundle().apply { putString("wall_user_id", friend.id) }
            findNavController().navigate(R.id.navigation_mainpage, b)
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
            adapter.submitList(list)
        }
        viewModel.loadInitialFriends(userId)
    }
}