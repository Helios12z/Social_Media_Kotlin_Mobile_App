package com.example.socialmediaproject.ui.mainpage

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.FeedAdapter
import com.example.socialmediaproject.databinding.FragmentMainPageBinding
import com.example.socialmediaproject.dataclass.PostViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainPageFragment : Fragment(), FeedAdapter.OnPostInteractionListener {
    private val viewModel: MainPageViewModel by viewModels()
    private lateinit var binding: FragmentMainPageBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<PostViewModel>()
    private var wallUserId =  ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentMainPageBinding.inflate(inflater, container, false)
        wallUserId = arguments?.getString("wall_user_id") ?: ""
        viewModel.wallUserId=wallUserId
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        initViews(binding.root)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.loadPosts()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.visibility=View.VISIBLE
        db.collection("Users").document(wallUserId).get().addOnSuccessListener {
            result->if (result.exists()) {
                Glide.with(requireContext()).load(result.getString("avatarurl"))
                    .placeholder(R.drawable.avataricon)
                    .error(R.drawable.avataricon)
                    .into(binding.profileAvatar)
                binding.profileUsername.text = result.getString("name")
                if (result.getString("bio")=="") binding.profileBio.visibility=View.GONE
                else binding.profileBio.text = result.getString("bio")
                Glide.with(requireContext()).load(result.getString("wallurl"))
                    .placeholder(R.color.white)
                    .error(R.color.white)
                    .into(binding.wallImage)
                val currentUserId=auth.currentUser?.uid ?: ""
                if (currentUserId==wallUserId) {
                    binding.buttonAddFriend.visibility=View.GONE
                }
                else {
                    db.collection("Users").document(currentUserId).get().addOnSuccessListener {
                            newresult->if (newresult.exists()) {
                            val friends=newresult.get("friends") as? List<String>
                            if (friends?.contains(wallUserId) == true) {
                                binding.buttonAddFriend.visibility = View.GONE
                                binding.buttonUnfriend.visibility = View.VISIBLE
                                binding.buttonChat.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                val friendlist=result.get("friends") as? List<String> ?: emptyList()
                binding.profileFollowersCount.text=friendlist.size.toString()
                db.collection("Posts").whereEqualTo("userid", wallUserId).get().addOnSuccessListener { listitem ->
                    if (listitem != null) {
                        binding.profilePostsCount.text = listitem.size().toString()
                    }
                }
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    private fun initViews(view: View) {
        recyclerViewFeed = binding.recyclerViewPosts
        swipeRefreshLayout = binding.swipeRefreshLayout
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(requireContext(), postList, this)
        recyclerViewFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshFeed()
        }
    }

    private fun observeViewModel() {
        viewModel.postlist.observe(viewLifecycleOwner) { posts ->
            feedAdapter.updatePosts(posts)
        }
        viewModel.isloading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    override fun onLikeClicked(position: Int) {
        val post=viewModel.postlist.value?.get(position)?:return
        viewModel.toggleLike(post, position)
    }

    override fun onCommentClicked(position: Int) {
        Toast.makeText(
            requireContext(),
            "Mở bình luận cho bài viết: ${postList[position].id}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onShareClicked(position: Int) {
        Toast.makeText(
            requireContext(),
            "Chia sẻ bài viết: ${postList[position].id}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onUserClicked(position: Int) {
        val post=viewModel.postlist.value?.get(position)?:return
        val goToFragment=MainPageFragment()
        val bundle=Bundle()
        bundle.putString("wall_user_id", post.userId)
        goToFragment.arguments=bundle
        findNavController().navigate(R.id.action_homeFragment_to_mainPageFragment, bundle)
    }

    override fun onMoreOptionsClicked(position: Int, anchorView: View) {
        PopupMenu(requireContext(), anchorView).apply {
            //inflate(R.menu.post_options_menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    /*R.id.action_save -> {
                        Toast.makeText(requireContext(), "Đã lưu bài viết", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_report -> {
                        Toast.makeText(requireContext(), "Đã báo cáo bài viết", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_hide -> {
                        postList.removeAt(position)
                        feedAdapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "Đã ẩn bài viết", Toast.LENGTH_SHORT).show()
                        true
                    }*/
                    else -> false
                }
            }
            show()
        }
    }

    override fun onImageClicked(postPosition: Int, imagePosition: Int) {
        val post=viewModel.postlist.value?.get(postPosition)?:return
        val images=post?.imageUrls
        val imageurl=images?.get(imagePosition)
        if (imageurl!=null) {
            val bundle = bundleOf("IMAGE_URL" to imageurl)
            findNavController().navigate(R.id.viewingimagefragment, bundle)
        }
    }
}