package com.example.socialmediaproject.ui.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.FriendRecommendAdapter
import com.example.socialmediaproject.databinding.FragmentSearchBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.socialmediaproject.adapter.ReceivedFriendRequestAdapter
import com.example.socialmediaproject.adapter.SentRequestAdapter

class SearchFragment : Fragment() {
    private val viewModel: SearchViewModel by activityViewModels()
    private lateinit var binding: FragmentSearchBinding

    private val friendRecommendAdapter = FriendRecommendAdapter(
        onAddFriendClick = { friend -> viewModel.sendFriendRequest(friend) },
        onResendClick    = { id, cb -> viewModel.resendFriendRequest(id, cb) }
    )
    private val sentRequestAdapter = SentRequestAdapter(
        onResendClick = { id, cb -> viewModel.resendFriendRequest(id, cb) }
    )
    private val receivedFriendRequestAdapter = ReceivedFriendRequestAdapter(
        onRejectClick = { id, cb -> viewModel.rejectFriendRequest(id, cb) },
        onAcceptClick = { id, cb -> viewModel.acceptFriendRequest(id, cb) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerViewFriendRecommend.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendRecommendAdapter
        }
        binding.recyclerViewSentRequest.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sentRequestAdapter
        }
        binding.recyclerViewInvitation.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = receivedFriendRequestAdapter
        }
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.search_filter_options,
            android.R.layout.simple_spinner_item
        ).also { spinnerAdapter ->
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFilter.adapter = spinnerAdapter
        }
        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                when (position) {
                    0 -> showRecommendations()
                    1 -> showSentRequests()
                    2 -> showReceivedRequests()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchRecommendations()
            viewModel.fetchSentRequests()
            viewModel.fetchReceivedFriendRequests()
        }
        observeViewModel()
        viewModel.fetchRecommendations()
        viewModel.fetchSentRequests()
        viewModel.fetchReceivedFriendRequests()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recommendations.collect {
                        friendRecommendAdapter.submitList(it)
                        if (binding.spinnerFilter.selectedItemPosition == 0) {
                            updateEmptyState(it.isEmpty())
                        }
                    }
                }
                launch {
                    viewModel.sentRequests.collect {
                        sentRequestAdapter.submitList(it)
                        if (binding.spinnerFilter.selectedItemPosition == 1) {
                            updateEmptyState(it.isEmpty())
                        }
                    }
                }
                launch {
                    viewModel.receivedRequests.collect {
                        receivedFriendRequestAdapter.submitList(it)
                        if (binding.spinnerFilter.selectedItemPosition == 2) {
                            updateEmptyState(it.isEmpty())
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        if (!loading && binding.swipeRefreshLayout.isRefreshing) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collect { msg ->
                        msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.textViewEmpty.text = when (binding.spinnerFilter.selectedItemPosition) {
            0 -> "Không có gợi ý kết bạn nào"
            1 -> "Không có lời mời đã gửi"
            else -> "Không có lời mời nào"
        }
    }

    private fun showRecommendations() {
        binding.recyclerViewFriendRecommend.visibility = View.VISIBLE
        binding.recyclerViewSentRequest.visibility     = View.GONE
        binding.recyclerViewInvitation.visibility       = View.GONE
        updateEmptyState(friendRecommendAdapter.currentList.isEmpty())
    }

    private fun showSentRequests() {
        binding.recyclerViewFriendRecommend.visibility = View.GONE
        binding.recyclerViewSentRequest.visibility     = View.VISIBLE
        binding.recyclerViewInvitation.visibility       = View.GONE
        updateEmptyState(sentRequestAdapter.currentList.isEmpty())
    }

    private fun showReceivedRequests() {
        binding.recyclerViewFriendRecommend.visibility = View.GONE
        binding.recyclerViewSentRequest.visibility     = View.GONE
        binding.recyclerViewInvitation.visibility       = View.VISIBLE
        updateEmptyState(receivedFriendRequestAdapter.currentList.isEmpty())
    }
}