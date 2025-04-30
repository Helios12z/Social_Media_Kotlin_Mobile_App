package com.example.socialmediaproject.ui.searchusersandposts

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentSearchUsersAndPostsBinding

class SearchUsersAndPostsFragment : Fragment() {
    private lateinit var viewModel: SearchUsersAndPostsViewModel
    private lateinit var binding: FragmentSearchUsersAndPostsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentSearchUsersAndPostsBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[SearchUsersAndPostsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}