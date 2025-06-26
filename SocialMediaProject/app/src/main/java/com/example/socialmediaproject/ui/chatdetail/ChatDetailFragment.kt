package com.example.socialmediaproject.ui.chatdetail

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.example.socialmediaproject.Constant
import com.example.socialmediaproject.R
import com.example.socialmediaproject.activity.IncomingCallActivity
import com.example.socialmediaproject.adapter.MessageAdapter
import com.example.socialmediaproject.databinding.FragmentChatDetailBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.example.socialmediaproject.dataclass.Message
import com.example.socialmediaproject.service.AIService
import com.example.socialmediaproject.service.OneSignalHelper
import com.example.socialmediaproject.service.UploadChatImgeWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val REQUEST_IMAGE_PICK = 100
class ChatDetailFragment : Fragment() {
    private lateinit var binding: FragmentChatDetailBinding
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var chatUser: ChatUser
    private val auth=FirebaseAuth.getInstance()
    private val db=FirebaseFirestore.getInstance()
    private var hasInitialLoaded = false
    private var isLink: Boolean=false
    private var selectedImageUri: Uri? = null
    private var incomingCallListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        binding=FragmentChatDetailBinding.inflate(inflater, container, false)
        chatUser = arguments?.getParcelable("chatUser") ?: ChatUser()
        askForPermission()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        if (chatUser.id!=Constant.ChatConstants.VECTOR_AI_ID) {
            binding.ivChatAvatar.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("wall_user_id", chatUser.id)
                findNavController().navigate(R.id.navigation_mainpage, bundle)
            }
            binding.tvChatUsername.text = chatUser.username
            Glide.with(requireContext())
                .load(chatUser.avatarUrl)
                .placeholder(R.drawable.avataricon)
                .into(binding.ivChatAvatar)
        }
        else {
            binding.ivChatAvatar.setImageResource(R.drawable.vectorai)
            binding.tvChatUsername.text = "VectorAI"
        }
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = if (currentUserId < chatUser.id) "${currentUserId}_${chatUser.id}"
        else "${chatUser.id}_${currentUserId}"
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMessages.layoutManager = layoutManager
        val adapter = MessageAdapter(currentUserId, chatUser.avatarUrl,
        onMessageLongClick = {
            message->showMessageOptionBottomSheet(message, chatId)
        },
        onLinkClick = {
            postId, commentId, messageContent->if (postId!="") {
                val bundle=Bundle()
                bundle.putString("post_id", postId)
                bundle.putString("comment_id", commentId)
                findNavController().navigate(R.id.navigation_postWithComment, bundle)
            }
            else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(messageContent))
                startActivity(intent)
            }
        },
        onPictureClick = {
            imageUrl->if (imageUrl!="") {
                val bundle=Bundle()
                bundle.putString("IMAGE_URL", imageUrl)
                findNavController().navigate(R.id.viewingimagefragment, bundle)
            }
        })
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
            selectedImageUri?.let { uri ->
                val workData = workDataOf(
                    "imageUrl" to uri.toString(),
                    "id" to chatId,
                    "senderId" to currentUserId,
                    "receiverId" to chatUser.id,
                )
                val uploadWork = OneTimeWorkRequestBuilder<UploadChatImgeWorker>()
                    .setInputData(workData)
                    .build()
                WorkManager.getInstance(requireContext()).enqueue(uploadWork)
                cancelImagePreview()
                selectedImageUri=null
                return@setOnClickListener
            }
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            if (chatUser.id==Constant.ChatConstants.VECTOR_AI_ID) {
                chatWithVectorAI(chatId, text)
                binding.etMessage.setText("")
            }
            else {
                val message = Message(
                    senderId = currentUserId,
                    receiverId = chatUser.id,
                    text = text,
                    timestamp = Timestamp.now(),
                    link = isLink
                )
                viewModel.sendMessage(chatId, message)
                binding.etMessage.setText("")
                if (text.startsWith("@VectorAI", true)) {
                    val prompt = text.removePrefix("@VectorAI").trim()
                    askVectorAI(chatId, prompt)
                }
            }
        }
        binding.btnAttach.setOnClickListener {
            val popup=PopupMenu(requireContext(), binding.btnAttach)
            val menuInflater: MenuInflater = popup.menuInflater
            menuInflater.inflate(R.menu.attachment_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.btnAttachLink -> {
                        isLink=true
                        binding.etMessage.setBackgroundResource(R.drawable.border_yellow)
                        binding.etMessage.setTextColor(resources.getColor(android.R.color.holo_orange_light))
                        binding.etMessage.hint = "Chỉ nhập Url Link vào đây..."
                        binding.btnCancelLink.visibility=View.VISIBLE
                        binding.tvLinkLabel.visibility=View.VISIBLE
                        true
                    }
                    R.id.btnAttachImage -> {
                        openGallery()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        binding.btnCancelLink.setOnClickListener {
            isLink=false
            binding.btnCancelLink.visibility = View.GONE
            binding.tvLinkLabel.visibility=View.GONE
            binding.etMessage.setBackgroundResource(R.drawable.rounded_edittext)
            binding.etMessage.setTextColor(resources.getColor(R.color.text_color))
            binding.etMessage.hint = "Nhập tin nhắn..."
        }
        binding.btnCancelImage.setOnClickListener {
            cancelImagePreview()
        }
        if (chatUser.id!=Constant.ChatConstants.VECTOR_AI_ID) checkIfCanSendMessage(auth.currentUser?.uid?:"", chatUser.id)
        else
        {
            binding.btnAttach.visibility=View.GONE
            binding.btnVoiceCall.visibility=View.GONE
        }
        listenUserActivity(chatUser.id)
        binding.btnVoiceCall.setOnClickListener {
            val callerId=auth.currentUser?.uid
            val roomId = "${callerId}_${chatUser.id}_${System.currentTimeMillis()}"
            val callData = hashMapOf(
                "callerId" to callerId,
                "receiverId" to chatUser.id,
                "status" to "calling",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("calls")
                .document(roomId)
                .set(callData).addOnSuccessListener {
                    db.collection("Users").document(auth.currentUser?.uid?:"").get().addOnSuccessListener {
                        result->if (result.exists()) {
                            val currentUserName=result.getString("name")

                            OneSignalHelper.sendCallNotification(
                                userId = chatUser.id,
                                message = "Cuộc gọi đến từ ${currentUserName}",
                                callerId = callerId ?: "",
                                roomId = roomId
                            )

                            val bundle = Bundle().apply {
                                putString("user_id", chatUser.id)
                                putString("room_id", roomId)
                                putBoolean("isCaller", true)
                            }
                            findNavController().navigate(R.id.navigation_calling, bundle)
                        }
                        else {
                            Toast.makeText(requireContext(), "Có lỗi khi thực hiện cuộc gọi", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Có lỗi khi thực hiện cuộc gọi", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Không thể tạo cuộc gọi", Toast.LENGTH_SHORT).show()
                }
        }
        listenForIncomingCall(chatUser.id)
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
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
        incomingCallListener?.remove()
    }

    fun checkIfCanSendMessage(currentUserId: String, friendId: String) {
        val db = FirebaseFirestore.getInstance()
        val meRef = db.collection("Users").document(currentUserId)
        val youRef = db.collection("Users").document(friendId)
        meRef.get().addOnSuccessListener { meDoc ->
            val friends = meDoc["friends"] as? List<String> ?: emptyList()
            meRef.collection("BlockedUsers").document(friendId)
            .get().addOnSuccessListener { blockByMe ->
                youRef.collection("BlockedUsers").document(currentUserId)
                .get().addOnSuccessListener { blockByYou ->
                    val isFriend = friendId in friends
                    val iBlockedYou = blockByMe.exists()
                    val youBlockedMe = blockByYou.exists()
                    val canChat = isFriend && !iBlockedYou && !youBlockedMe
                    if (!canChat) {
                        binding.btnSend.visibility = View.GONE
                        binding.btnAttach.visibility = View.GONE
                        binding.etMessage.apply {
                            isEnabled = false
                            gravity = Gravity.CENTER
                            hint = when {
                                iBlockedYou    -> "Bạn đã chặn người này"
                                youBlockedMe   -> "Bạn đã bị chặn"
                                else           -> "2 người không còn là bạn bè"
                            }
                        }
                    }
                }
            }
        }
    }

    fun showMessageOptionBottomSheet(message: Message, chatId: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val currentUserId=auth.currentUser?.uid?:""
        if (message.senderId==currentUserId) {
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
        }
        else {
            bottomSheetDialog.setContentView(view)
            val button1: Button=view.findViewById(R.id.button1)
            val button2: Button=view.findViewById(R.id.button2)
            button1.setText("Sao chép")
            button2.setText("Hủy")
            button1.setOnClickListener {
                bottomSheetDialog.dismiss()
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Tin nhắn sao chép", message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Đã sao chép tin nhắn", Toast.LENGTH_SHORT).show()
            }
            button2.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
        }
        bottomSheetDialog.show()
    }

    private fun askVectorAI(chatId: String, userPrompt: String) {
        lifecycleScope.launch {
            try {
                val template = getVectorAIPromptTemplate(requireContext())
                val truePrompt = template.replace("{{user_input}}", userPrompt)
                val aiResponse = AIService.chatWithAI(truePrompt)
                val aiMessage = Message(
                    senderId = "Ordinary_VectorAI",
                    receiverId = auth.currentUser?.uid ?: "",
                    text = aiResponse,
                    timestamp = Timestamp.now(),
                    read = false
                )
                viewModel.sendMessage(chatId, aiMessage)
            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun chatWithVectorAI(chatId: String, userText: String) {
        val userMessage = Message(
            senderId   = auth.currentUser?.uid ?: "",
            receiverId = Constant.ChatConstants.VECTOR_AI_ID,
            text       = userText,
            timestamp  = Timestamp.now(),
            read       = false
        )
        viewModel.sendMessage(chatId, userMessage)
        lifecycleScope.launch {
            try {
                val template   = getVectorAIPromptTemplate(requireContext())
                val truePrompt = template.replace("{{user_input}}", userText)
                val aiResponse = AIService.chatWithAI(truePrompt)
                val aiMessage = Message(
                    senderId   = Constant.ChatConstants.VECTOR_AI_ID,
                    receiverId = auth.currentUser?.uid ?: "",
                    text       = aiResponse,
                    timestamp  = Timestamp.now(),
                    read       = false
                )
                viewModel.sendMessage(chatId, aiMessage)
            } catch (e: Exception) {
                val errorMessage = Message(
                    senderId   = Constant.ChatConstants.VECTOR_AI_ID,
                    receiverId = auth.currentUser?.uid ?: "",
                    text       = "Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu của bạn.",
                    timestamp  = Timestamp.now(),
                    read       = false
                )
                viewModel.sendMessage(chatId, errorMessage)
            }
        }
    }

    private fun listenUserActivity(userId: String) {
        if (chatUser.id==Constant.ChatConstants.VECTOR_AI_ID) {
            binding.activeStatus.visibility=View.GONE
        }
        else {
            val db = FirebaseFirestore.getInstance()
            db.collection("Users")
            .document(userId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                val ts = snap.getTimestamp("lastActive") ?: return@addSnapshotListener
                val last = ts.toDate().time
                val now  = System.currentTimeMillis()
                val diff = now - last
                val statusText = when {
                    diff < 60_000 -> {
                        "Đang hoạt động"
                    }
                    diff < 3_600_000 -> {
                        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                        "Hoạt động $mins phút trước"
                    }
                    diff < 86_400_000 -> {
                        val hours = TimeUnit.MILLISECONDS.toHours(diff)
                        "Hoạt động $hours giờ trước"
                    }
                    else -> {
                        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        "Hoạt động vào ${fmt.format(Date(last))}"
                    }
                }
                binding.activeStatus.text = statusText
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                showImagePreview(imageUri)
            }
        }
    }

    private fun showImagePreview(imageUri: Uri) {
        binding.previewImageCard.visibility = View.VISIBLE
        binding.btnCancelImage.visibility = View.VISIBLE
        binding.etMessage.visibility = View.GONE
        Glide.with(requireContext())
            .load(imageUri)
            .centerCrop()
            .into(binding.ivPreviewImage)
        selectedImageUri = imageUri
    }

    private fun cancelImagePreview() {
        binding.previewImageCard.visibility = View.GONE
        binding.btnCancelImage.visibility = View.GONE
        binding.etMessage.visibility = View.VISIBLE
        selectedImageUri = null
    }

    private fun askForPermission() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        ActivityCompat.requestPermissions(requireActivity(), permissions, 1)
    }

    private fun listenForIncomingCall(fromUserId: String) {
        val myUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        incomingCallListener = FirebaseFirestore.getInstance()
            .collection("calls")
            .whereEqualTo("receiverId", myUserId)
            .whereEqualTo("callerId", fromUserId)
            .whereEqualTo("status", "calling")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val callDoc = snapshot.documents.first()
                val roomId = callDoc.id
                val intent = Intent(requireContext(), IncomingCallActivity::class.java).apply {
                    putExtra("callerId", fromUserId)
                    putExtra("roomId", roomId)
                }
                startActivity(intent)
            }
    }
}