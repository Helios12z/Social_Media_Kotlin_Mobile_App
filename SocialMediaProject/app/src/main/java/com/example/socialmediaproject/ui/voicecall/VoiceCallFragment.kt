package com.example.socialmediaproject.ui.voicecall

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentVoiceCallBinding
import com.example.socialmediaproject.dataclass.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
        viewModel.isCaller = arguments?.getBoolean("isCaller") == true
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
                        if (!viewModel.isCaller) {
                            viewModel.listenForOffer()
                            viewModel.listenForCallerCandidates()
                        } else {
                            viewModel.startCall()
                            viewModel.listenForAnswer()
                            viewModel.listenForCalleeCandidates()
                        }
                        binding.callTimer.visibility = View.VISIBLE
                        binding.callTimer.base = SystemClock.elapsedRealtime()
                        binding.callTimer.start()
                    }
                    "declined" -> {
                        Toast.makeText(requireContext(), "Cuộc gọi bị từ chối", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    "ended" -> {
                        Toast.makeText(requireContext(), "Cuộc gọi đã kết thúc", Toast.LENGTH_SHORT).show()
                        viewModel.endCall()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            }
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnEndCall.setOnClickListener {
            binding.callTimer.stop()

            val durationMillis = SystemClock.elapsedRealtime() - binding.callTimer.base
            val durationText = formatDuration(durationMillis)

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val otherUserId = chatUserId

            val senderId: String
            val receiverId: String

            if (viewModel.isCaller) {
                senderId = currentUserId
                receiverId = otherUserId
            } else {
                senderId = otherUserId
                receiverId = currentUserId
            }

            val chatId = if (senderId < receiverId) {
                "${senderId}_${receiverId}"
            } else {
                "${receiverId}_${senderId}"
            }

            val message = Message(
                senderId = senderId,
                receiverId = receiverId,
                text = "Cuộc gọi thoại ($durationText)",
                timestamp = Timestamp.now(),
                link = false
            )
            sendMessage(chatId, message)
            db.collection("calls").document(roomId).update("status", "ended").addOnSuccessListener {
                viewModel.endCall()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Cuộc gọi không thể kết thúc", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendMessage(chatId: String, message: Message) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.set(mapOf("exists" to true), SetOptions.merge())
        val docRef=db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()
        val messageWithId = message.copy(id = docRef.id)
        docRef.set(messageWithId)
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE

        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}