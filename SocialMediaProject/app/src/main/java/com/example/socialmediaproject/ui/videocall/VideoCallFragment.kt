package com.example.socialmediaproject.ui.videocall

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentVideoCallBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class VideoCallFragment : Fragment() {

    private lateinit var binding: FragmentVideoCallBinding
    private lateinit var viewModel: VideoCallViewModel
    private lateinit var db: FirebaseFirestore
    private lateinit var chatUserId: String
    private lateinit var roomId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentVideoCallBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[VideoCallViewModel::class.java]
        hideNavbar()
        db = FirebaseFirestore.getInstance()
        chatUserId = arguments?.getString("user_id") ?: ""
        roomId = arguments?.getString("room_id") ?: ""
        viewModel.roomId = roomId

        viewModel.initFactoryAndTracks(requireContext(), binding.localVideoView, binding.remoteVideoView)
        viewModel.initPeerConnection()
        viewModel.isCaller = arguments?.getBoolean("isCaller") ?: false

        listenFirestoreStatus()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideNavbar()
        binding.btnEndVideoCall.setOnClickListener {
            db.collection("calls").document(roomId).update("status", "ended")
            viewModel.endCall()
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavbar()
        binding.localVideoView.setZOrderMediaOverlay(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showNavbar()
        viewModel.endCall()
    }

    private fun hideNavbar() {
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    private fun showNavbar() {
        val bottomnavbar = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility = View.VISIBLE
    }

    private fun listenFirestoreStatus() {
        db.collection("calls").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val status = snapshot?.getString("status") ?: return@addSnapshotListener
                when (status) {
                    "accepted" -> {
                        if (viewModel.isCaller) {
                            viewModel.startCall()
                            viewModel.listenForAnswer()
                            viewModel.listenForCalleeCandidates()
                        } else {
                            viewModel.listenForOffer()
                            viewModel.listenForCallerCandidates()
                        }
                    }
                    "declined", "ended" -> {
                        Toast.makeText(requireContext(), "Cuộc gọi kết thúc", Toast.LENGTH_SHORT).show()
                        viewModel.endCall()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            }
    }
}