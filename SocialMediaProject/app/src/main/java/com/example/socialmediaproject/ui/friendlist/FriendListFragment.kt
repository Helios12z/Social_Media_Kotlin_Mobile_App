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
import com.example.socialmediaproject.dataclass.Friend
import com.google.android.material.bottomnavigation.BottomNavigationView

class FriendListFragment : Fragment() {
    private lateinit var binding: FragmentFriendListBinding
    private lateinit var viewModel: FriendListViewModel
    private lateinit var userId: String
    private var fullFriendsList: List<Friend> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= FragmentFriendListBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[FriendListViewModel::class.java]
        userId=arguments?.getString("user_id")?:""
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
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
            fullFriendsList=list
            val query = binding.searchViewFriends.query?.toString().orEmpty()
            if (query.isEmpty()) {
                adapter.submitList(fullFriendsList)
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
        viewModel.loadInitialFriends(userId)
    }

    private fun filterList(query: String): List<Friend> {
        if (query.isEmpty()) return fullFriendsList
        val terms = query.lowercase().split("\\s+".toRegex())
        return fullFriendsList.filter { f ->
            val nameCombined = "${f.displayName} ${f.fullName}".lowercase()
            terms.all { term -> nameCombined.contains(term) }
        }
    }
}