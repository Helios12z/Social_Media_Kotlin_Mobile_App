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
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchUsersAndPostsFragment : Fragment() {
    private lateinit var viewModel: SearchUsersAndPostsViewModel
    private lateinit var binding: FragmentSearchUsersAndPostsBinding
    private val userAdapter = SearchUserAdapter {
        val bundle = Bundle()
        bundle.putString("wall_user_id", it)
        findNavController().navigate(R.id.navigation_mainpage, bundle)
    }
    private val postAdapter = SearchPostAdapter {
        val bundle = Bundle()
        bundle.putString("post_id", it)
        findNavController().navigate(R.id.navigation_postWithComment, bundle)
    }
    private val concat = ConcatAdapter(userAdapter, postAdapter)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentSearchUsersAndPostsBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[SearchUsersAndPostsViewModel::class.java]
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = concat
        }
        viewModel.userlist.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
            toggleEmptyState()
        }
        viewModel.postlist.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            toggleEmptyState()
        }
        binding.searchViewFriends.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    lifecycleScope.launch {
                        viewModel.setSearchQuery(it)
                    }
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.refresh()
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

    fun toggleEmptyState() {
        val hasAny = userAdapter.itemCount + postAdapter.itemCount > 0
        binding.emptyStateContainer.isVisible = !hasAny
        binding.rvSearchResults.isVisible = hasAny
        binding.searchProgress.isVisible = false
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