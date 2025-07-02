package com.example.socialmediaproject.ui.searchusersandposts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.SearchPostAdapter
import com.example.socialmediaproject.adapter.SearchUserAdapter
import com.example.socialmediaproject.databinding.FragmentSearchUsersAndPostsBinding
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ConcatAdapter
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.dataclass.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialmediaproject.activity.MainActivity

class SearchUsersAndPostsFragment : Fragment() {
    private lateinit var viewModel: SearchUsersAndPostsViewModel
    private lateinit var binding: FragmentSearchUsersAndPostsBinding
    private var lastUsers: List<User>? = null
    private var lastPosts: List<PostViewModel>? = null
    private val userAdapter = SearchUserAdapter {
        val bundle = Bundle()
        bundle.putString("wall_user_id", it)
        findNavController().navigate(R.id.navigation_mainpage, bundle)
    }
    private val postAdapter = SearchPostAdapter(onDetailClicked = {
        val bundle = Bundle()
        bundle.putString("post_id", it)
        findNavController().navigate(R.id.navigation_postWithComment, bundle)
    },
    onImageClicked = {
        val bundle = Bundle()
        bundle.putString("IMAGE_URL", it)
        findNavController().navigate(R.id.viewingimagefragment, bundle)
    })
    private val concat = ConcatAdapter(userAdapter, postAdapter)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentSearchUsersAndPostsBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[SearchUsersAndPostsViewModel::class.java]
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = concat
        }
        viewModel.userlist.observe(viewLifecycleOwner) { users ->
            lastUsers=users
            userAdapter.submitList(users)
            updateEmptyState()
        }
        viewModel.postlist.observe(viewLifecycleOwner) { posts ->
            lastPosts=posts
            postAdapter.submitList(posts)
            updateEmptyState()
        }
        binding.searchViewFriends.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    lastUsers = null
                    lastPosts = null
                    binding.searchProgress.isVisible = true
                    lifecycleScope.launch {
                        viewModel.setSearchQuery(it)
                    }
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        viewModel.setSearchQuery("")
                    }
                }
                return false
            }
        })
        binding.filterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_all -> binding.rvSearchResults.adapter = concat
                R.id.chip_users -> binding.rvSearchResults.adapter = userAdapter
                R.id.chip_posts -> binding.rvSearchResults.adapter = postAdapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }

    private fun updateEmptyState() {
        if (lastUsers == null || lastPosts == null) return
        binding.searchProgress.isVisible = false
        val hasAny = lastUsers!!.isNotEmpty() || lastPosts!!.isNotEmpty()
        binding.emptyStateContainer.isVisible = !hasAny
        binding.rvSearchResults.isVisible = hasAny
    }
}