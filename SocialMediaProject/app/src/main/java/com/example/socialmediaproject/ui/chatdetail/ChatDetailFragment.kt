package com.example.socialmediaproject.ui.chatdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.MessageAdapter
import com.example.socialmediaproject.databinding.FragmentChatDetailBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.example.socialmediaproject.dataclass.Message
import com.example.socialmediaproject.service.AIService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ChatDetailFragment : Fragment() {
    private lateinit var binding: FragmentChatDetailBinding
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var chatUser: ChatUser
    private val auth=FirebaseAuth.getInstance()
    private var hasInitialLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        binding=FragmentChatDetailBinding.inflate(inflater, container, false)
        chatUser = arguments?.getParcelable("chatUser") ?: ChatUser()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        binding.tvChatUsername.text = chatUser.username
        Glide.with(requireContext())
            .load(chatUser.avatarUrl)
            .placeholder(R.drawable.avataricon)
            .into(binding.ivChatAvatar)
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = if (currentUserId < chatUser.id) "${currentUserId}_${chatUser.id}"
        else "${chatUser.id}_${currentUserId}"
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMessages.layoutManager = layoutManager
        val adapter = MessageAdapter(currentUserId, chatUser.avatarUrl) {
            message->showMessageOptionBottomSheet(message, chatId)
        }
        binding.recyclerViewMessages.adapter = adapter
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (!hasInitialLoaded) {
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                hasInitialLoaded=true
            }
        }
        viewModel.loadMessages(chatId, currentUserId)
        viewModel.startListeningMessages(chatId, currentUserId)
        binding.recyclerViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible == 0) {
                    viewModel.loadOlderMessages(chatId, currentUserId, requireContext())
                }
            }
        })
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val message = Message(
                    senderId = currentUserId,
                    receiverId = chatUser.id,
                    text = text,
                    timestamp = Timestamp.now()
                )
                viewModel.sendMessage(chatId, message)
                binding.etMessage.setText("")
            }
            if (text.startsWith("@VectorAI", true)) {
                val prompt = text.removePrefix("@VectorAI").trim()
                askVectorAI(chatId, prompt)
            }
        }
        checkIfCanSendMessage(auth.currentUser?.uid?:"", chatUser.id)
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopListening()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    fun checkIfCanSendMessage(currentUserId: String, friendId: String) {
        FirebaseFirestore.getInstance().collection("Users")
        .document(currentUserId)
        .get()
        .addOnSuccessListener { doc ->
            val friends = doc["friends"] as? List<String> ?: emptyList()
            val canChat = friendId in friends
            if (!canChat) {
                binding.btnSend.visibility = View.GONE
                binding.etMessage.apply {
                    isEnabled = false
                    hint = "2 người không còn là bạn bè"
                    gravity = Gravity.CENTER
                }
                binding.btnAttach.visibility=View.GONE
            }
        }
    }

    fun showMessageOptionBottomSheet(message: Message, chatId: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(view)
        val button1: Button=view.findViewById(R.id.button1)
        val button2: Button=view.findViewById(R.id.button2)
        button1.setText("Sao chép")
        button2.setText("Thu hồi")
        button1.setOnClickListener {
            bottomSheetDialog.dismiss()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Tin nhắn sao chép", message.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Đã sao chép tin nhắn", Toast.LENGTH_SHORT).show()
        }
        button2.setOnClickListener {
            bottomSheetDialog.dismiss()
            viewModel.removeMessage(chatId, message)
        }
        bottomSheetDialog.show()
    }

    private fun askVectorAI(chatId: String, userPrompt: String) {
        lifecycleScope.launch {
            try {
                val template = getVectorAIPromptTemplate(requireContext())
                val truePrompt = template.replace("{{user_input}}", userPrompt)
                val aiResponse = AIService.chatWithAI(truePrompt)
                Log.d("VectorAI", "AI Response: $aiResponse")
                val aiMessage = Message(
                    senderId = "Ordinary_VectorAI",
                    receiverId = auth.currentUser?.uid ?: "",
                    text = aiResponse,
                    timestamp = Timestamp.now(),
                    read = false
                )
                viewModel.sendMessage(chatId, aiMessage)
            } catch (e: Exception) {
                Log.e("VectorAI", "Error asking AI", e)
                val errorMessage = Message(
                    senderId = "Ordinary_VectorAI",
                    receiverId = auth.currentUser?.uid ?: "",
                    text = "Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu của bạn.",
                    timestamp = Timestamp.now(),
                    read = false
                )
                viewModel.sendMessage(chatId, errorMessage)
            }
        }
    }

    private fun getVectorAIPromptTemplate(context: Context): String {
        return context.assets.open("VectorAI_Data.txt")
            .bufferedReader()
            .use { it.readText() }
    }
}