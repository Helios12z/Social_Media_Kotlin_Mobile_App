package com.example.socialmediaproject.ui.voicecall

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentVoiceCallBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class VoiceCallFragment : Fragment() {

    private lateinit var binding: FragmentVoiceCallBinding
    private lateinit var viewModel: VoiceCallViewModel
    private lateinit var db: FirebaseFirestore
    private lateinit var chatUserId: String
    private lateinit var roomId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentVoiceCallBinding.inflate(layoutInflater, container, false)
        viewModel= ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
        db= FirebaseFirestore.getInstance()
        chatUserId=arguments?.getString("user_id")?:""
        if (!chatUserId.isNullOrEmpty())
        {
            db.collection("Users").document(chatUserId).get().addOnSuccessListener {
                result->if (result.exists()) {
                    Glide.with(requireContext())
                        .load(result.getString("avatarurl"))
                        .error(R.drawable.avataricon)
                        .placeholder(R.drawable.avataricon)
                        .into(binding.callingUserAvatar)
                    binding.callingUserName.text=result.getString("name")
                }
                else requireActivity().onBackPressed()
            }
        }
        roomId = arguments?.getString("room_id") ?: ""
        viewModel.roomId = roomId
        viewModel.initializePeerConnectionFactory(requireContext())
        viewModel.initPeerConnection()
        viewModel.listenForOffer()
        viewModel.listenForCallerCandidates()
        db.collection("calls").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val status = snapshot?.getString("status") ?: return@addSnapshotListener

                when (status) {
                    "accepted" -> {
                        viewModel.isCaller = true
                        viewModel.startCall()
                        viewModel.listenForAnswer()
                    }
                    "declined" -> {
                        Toast.makeText(requireContext(), "Cuộc gọi bị từ chối", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed()
                    }
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }
}