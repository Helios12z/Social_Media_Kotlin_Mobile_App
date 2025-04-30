package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.FriendShareAdapter
import com.example.socialmediaproject.databinding.FragmentFriendShareDialogBinding
import com.example.socialmediaproject.dataclass.Friend
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FieldPath
import com.google.android.gms.tasks.Tasks

class FriendShareDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentFriendShareDialogBinding
    private var listener: OnFriendSelectedListener? = null
    private lateinit var adapter: FriendShareAdapter
    private val friends = mutableListOf<Friend>()
    private lateinit var rv: RecyclerView

    interface OnFriendSelectedListener {
        fun onFriendSelected(friend: Friend)
    }

    fun setOnFriendSelectedListener(l: OnFriendSelectedListener) {
        listener = l
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentFriendShareDialogBinding.inflate(inflater, container, false)
        rv=binding.recyclerViewFriends
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchFriends()
        rv.layoutManager = LinearLayoutManager(context)
        adapter = FriendShareAdapter(friends) { friend ->
            listener?.onFriendSelected(friend)
            dismiss()
        }
        rv.adapter = adapter
    }

    private fun fetchFriends() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db  = FirebaseFirestore.getInstance()
        db.collection("Users")
        .document(uid)
        .get()
        .addOnSuccessListener { userSnap ->
            @Suppress("UNCHECKED_CAST")
            val friendIds = (userSnap.get("friends") as? List<String>) ?: emptyList()
            if (friendIds.isEmpty()) return@addOnSuccessListener
            friends.clear()
            val chunks = friendIds.chunked(10)
            chunks.forEach { chunk ->
                db.collection("Users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        val f = Friend(
                            id = doc.id,
                            displayName = doc.getString("name") ?: "",
                            fullName = doc.getString("fullname")    ?: "",
                            avatarUrl = doc.getString("avatarurl")   ?: "",
                            isFriend = true,
                            mutualFriendCount = 0
                        )
                        friends.add(f)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance(): FriendShareDialogFragment = FriendShareDialogFragment()
    }
}