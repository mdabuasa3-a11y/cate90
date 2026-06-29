package com.example.data.repository

import android.content.Context
import com.example.data.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class LmConnectRepository(
    private val context: Context,
    private val database: LmConnectDatabase = LmConnectDatabase.getDatabase(context)
) {
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val callDao = database.callDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTransfers = mutableMapOf<String, Job>()
    private val activeUploadTasks = mutableMapOf<String, com.google.firebase.storage.UploadTask>()

    // Expose flows directly from Room
    val friends: Flow<List<UserEntity>> = userDao.getFriends()
    val pendingRequests: Flow<List<UserEntity>> = userDao.getPendingRequests()
    val blockedUsers: Flow<List<UserEntity>> = userDao.getBlockedUsers()
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()
    val callHistory: Flow<List<CallHistoryEntity>> = callDao.getAllCalls()
    val sharedFiles: Flow<List<MessageEntity>> = messageDao.getSharedFiles()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    suspend fun getChatById(chatId: String): ChatEntity? = chatDao.getChatById(chatId)
    suspend fun getUserByCode(userCode: String): UserEntity? = userDao.getUserByCode(userCode)

    init {
        repositoryScope.launch {
            prepopulateIfEmpty()
        }
    }

    private suspend fun prepopulateIfEmpty() {
        val chats = allChats.first()
        if (chats.isEmpty()) {
            // 1. Create Mock Users / Friends
            val mockUsers = listOf(
                UserEntity(
                    userCode = "LMC-TESLA1",
                    username = "Elon Musk",
                    avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=150&q=80",
                    coverUrl = "https://images.unsplash.com/photo-1541185933-ef5d8ed016c2?auto=format&fit=crop&w=400&q=80",
                    bio = "Mars & Cars. 🚀 Sharing large rockets and larger files.",
                    statusText = "Occupied at Starbase",
                    onlineStatus = "Online",
                    isFriend = true,
                    hasPendingRequest = false,
                    isIncomingRequest = false,
                    isBlocked = false
                ),
                UserEntity(
                    userCode = "LMC-ALEXR1",
                    username = "Alex Rivera",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80",
                    coverUrl = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=400&q=80",
                    bio = "System Architect | ISO & Linux enthusiast.",
                    statusText = "Compiling kernels...",
                    onlineStatus = "Last seen 5m ago",
                    isFriend = true,
                    hasPendingRequest = false,
                    isIncomingRequest = false,
                    isBlocked = false
                ),
                UserEntity(
                    userCode = "LMC-SOPHI1",
                    username = "Sophia Carter",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=400&q=80",
                    bio = "Creative Director. Photography, cinematography, big render ZIP files.",
                    statusText = "On set 🎬",
                    onlineStatus = "Online",
                    isFriend = true,
                    hasPendingRequest = false,
                    isIncomingRequest = false,
                    isBlocked = false
                ),
                UserEntity(
                    userCode = "LMC-SARAH2",
                    username = "Sarah Jenkins",
                    avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=150&q=80",
                    coverUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=400&q=80",
                    bio = "UI/UX Designer. Pixel perfect layout artist.",
                    statusText = "In Figma, do not disturb",
                    onlineStatus = "Online",
                    isFriend = true,
                    hasPendingRequest = false,
                    isIncomingRequest = false,
                    isBlocked = false
                ),
                UserEntity(
                    userCode = "LMC-REQ999",
                    username = "Marcus Brody",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80",
                    coverUrl = "",
                    bio = "Wants to share CAD assets.",
                    statusText = "Hey there! I am using LM Connect",
                    onlineStatus = "Offline",
                    isFriend = false,
                    hasPendingRequest = true,
                    isIncomingRequest = true,
                    isBlocked = false
                ),
                UserEntity(
                    userCode = "LMC-BADBOY",
                    username = "Spam User",
                    avatarUrl = "",
                    coverUrl = "",
                    bio = "Click here to win a lottery",
                    statusText = "Blocked",
                    onlineStatus = "Offline",
                    isFriend = false,
                    hasPendingRequest = false,
                    isIncomingRequest = false,
                    isBlocked = true
                )
            )
            userDao.insertUsers(mockUsers)

            // 2. Create Active Chats
            val mockChats = listOf(
                ChatEntity(
                    chatId = "CHAT_GROUP_GLOBAL",
                    chatName = "LM Core Team & Devs",
                    isGroup = true,
                    lastMessageText = "Sarah Jenkins: I uploaded the final 50GB design PSD and asset ZIPs.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 15,
                    unreadCount = 2
                ),
                ChatEntity(
                    chatId = "CHAT_ELON",
                    chatName = "Elon Musk",
                    isGroup = false,
                    lastMessageText = "100GB file sharing is out of this world! 🚀",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60,
                    unreadCount = 0
                ),
                ChatEntity(
                    chatId = "CHAT_SOPHIA",
                    chatName = "Sophia Carter",
                    isGroup = false,
                    lastMessageText = "Sent file: Drone_Footage_4K.mov",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 120,
                    unreadCount = 0
                ),
                ChatEntity(
                    chatId = "CHAT_ALEX",
                    chatName = "Alex Rivera",
                    isGroup = false,
                    lastMessageText = "Send me that Linux ISO file whenever you're free, please.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 600,
                    unreadCount = 0
                )
            )
            chatDao.insertChats(mockChats)

            // 3. Populate Chat Messages
            val mockMessages = listOf(
                // Group Chat Messages
                MessageEntity(
                    messageId = "G1",
                    chatId = "CHAT_GROUP_GLOBAL",
                    senderCode = "LMC-ALEXR1",
                    senderName = "Alex Rivera",
                    text = "Hey team, welcome to the new high-speed sharing hub!",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),
                MessageEntity(
                    messageId = "G2",
                    chatId = "CHAT_GROUP_GLOBAL",
                    senderCode = "LMC-SARAH2",
                    senderName = "Sarah Jenkins",
                    text = "I've uploaded the full layout PSD and standard code package.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 100,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),
                MessageEntity(
                    messageId = "G3",
                    chatId = "CHAT_GROUP_GLOBAL",
                    senderCode = "LMC-SARAH2",
                    senderName = "Sarah Jenkins",
                    text = "Shared File: lm_connect_premium_assets.psd",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 95,
                    type = "FILE",
                    fileUrl = "simulated_url",
                    fileName = "lm_connect_premium_assets.psd",
                    fileType = "PSD",
                    fileSize = "22.5 GB",
                    transferProgress = 1.0f
                ),
                MessageEntity(
                    messageId = "G4",
                    chatId = "CHAT_GROUP_GLOBAL",
                    senderCode = "SYSTEM",
                    senderName = "LM Connect Server",
                    text = "High-speed transmission finalized for 'lm_connect_premium_assets.psd' (Avg: 850 MB/s).",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 94,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),

                // Elon Chat
                MessageEntity(
                    messageId = "E1",
                    chatId = "CHAT_ELON",
                    senderCode = "LMC-TESLA1",
                    senderName = "Elon Musk",
                    text = "Are you sending files securely?",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 180,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),
                MessageEntity(
                    messageId = "E2",
                    chatId = "MY_CODE", // current user code
                    senderCode = "MY_CODE",
                    senderName = "Me",
                    text = "Yes Elon! Standard LM Connect features complete end-to-end encryption with double-layer military standards.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 150,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),
                MessageEntity(
                    messageId = "E3",
                    chatId = "CHAT_ELON",
                    senderCode = "LMC-TESLA1",
                    senderName = "Elon Musk",
                    text = "100GB file sharing is out of this world! 🚀",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),

                // Sophia Chat
                MessageEntity(
                    messageId = "S1",
                    chatId = "CHAT_SOPHIA",
                    senderCode = "LMC-SOPHI1",
                    senderName = "Sophia Carter",
                    text = "Check out the raw footage drone edit. It is absolutely huge but LM Connect shared it instantly!",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 125,
                    type = "TEXT",
                    fileUrl = "",
                    fileName = "",
                    fileType = "NONE",
                    fileSize = ""
                ),
                MessageEntity(
                    messageId = "S2",
                    chatId = "CHAT_SOPHIA",
                    senderCode = "LMC-SOPHI1",
                    senderName = "Sophia Carter",
                    text = "Drone_Footage_4K.mov",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
                    type = "FILE",
                    fileUrl = "simulated_footage",
                    fileName = "Drone_Footage_4K.mov",
                    fileType = "MP4",
                    fileSize = "45.0 GB",
                    transferProgress = 1.0f
                )
            )
            mockMessages.forEach { messageDao.insertMessage(it) }

            // 4. Create Call Logs
            val mockCalls = listOf(
                CallHistoryEntity(
                    callId = "C1",
                    contactName = "Sophia Carter",
                    userCode = "LMC-SOPHI1",
                    isVoice = false,
                    isIncoming = true,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                    durationSec = 724,
                    status = "Completed"
                ),
                CallHistoryEntity(
                    callId = "C2",
                    contactName = "Elon Musk",
                    userCode = "LMC-TESLA1",
                    isVoice = true,
                    isIncoming = false,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 8,
                    durationSec = 0,
                    status = "Missed"
                ),
                CallHistoryEntity(
                    callId = "C3",
                    contactName = "Alex Rivera",
                    userCode = "LMC-ALEXR1",
                    isVoice = true,
                    isIncoming = true,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                    durationSec = 322,
                    status = "Completed"
                )
            )
            mockCalls.forEach { callDao.insertCall(it) }
        }
    }

    // ==========================================
    // Messaging API Actions
    // ==========================================

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderCode: String = "MY_CODE",
        senderName: String = "Me",
        type: String = "TEXT",
        replyToId: String? = null,
        replyToText: String? = null
    ): String {
        val msgId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId = msgId,
            chatId = chatId,
            senderCode = senderCode,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            fileUrl = "",
            fileName = "",
            fileType = "NONE",
            fileSize = "",
            replyToId = replyToId,
            replyToText = replyToText
        )
        messageDao.insertMessage(message)
        chatDao.updateLastMessage(chatId, "$senderName: $text", System.currentTimeMillis(), 0)

        // Simulate reply if private chat
        if (chatId != "CHAT_GROUP_GLOBAL" && senderCode == "MY_CODE") {
            simulateAutoReply(chatId)
        }

        return msgId
    }

    private fun simulateAutoReply(chatId: String) {
        repositoryScope.launch {
            delay(2000)
            val friendName = if (chatId == "CHAT_ELON") "Elon Musk" else "Sophia Carter"
            val friendCode = if (chatId == "CHAT_ELON") "LMC-TESLA1" else "LMC-SOPHI1"

            // Update user status to typing...
            val friend = userDao.getUserByCode(friendCode)
            if (friend != null) {
                userDao.updateUser(friend.copy(onlineStatus = "Typing..."))
                delay(1500)
                userDao.updateUser(friend.copy(onlineStatus = "Online"))
            }

            val replyText = when (chatId) {
                "CHAT_ELON" -> listOf(
                    "Excellent progress. We should schedule high-speed downloads for Mars colonists.",
                    "Fascinating. Let me send a bigger 100GB dataset payload now.",
                    "Great design! Clean, zero friction.",
                    "That is remarkable performance."
                ).random()
                else -> listOf(
                    "Awesome, that worked beautifully!",
                    "Did you receive the 50GB project zip file okay?",
                    "That's so cool. I'm finishing another video edit right now.",
                    "Wow! The speed is incredible."
                ).random()
            }

            val msgId = UUID.randomUUID().toString()
            val replyMessage = MessageEntity(
                messageId = msgId,
                chatId = chatId,
                senderCode = friendCode,
                senderName = friendName,
                text = replyText,
                timestamp = System.currentTimeMillis(),
                type = "TEXT",
                fileUrl = "",
                fileName = "",
                fileType = "NONE",
                fileSize = ""
            )
            messageDao.insertMessage(replyMessage)
            chatDao.updateLastMessage(chatId, "$friendName: $replyText", System.currentTimeMillis(), 1)
        }
    }

    // Edit, delete, react
    suspend fun editMessage(messageId: String, newText: String) {
        messageDao.editMessage(messageId, newText)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    suspend fun addReaction(messageId: String, emoji: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        val current = msg.reactions
        val updated = if (current.isEmpty()) {
            emoji
        } else if (current.contains(emoji)) {
            // Remove reaction
            current.split(",").filter { it != emoji }.joinToString(",")
        } else {
            "$current,$emoji"
        }
        messageDao.updateReactions(messageId, updated)
    }

    // ==========================================
    // File Transfer State Machine (Large Files)
    // ==========================================

    suspend fun startLargeFileShare(
        chatId: String,
        fileName: String,
        fileType: String,
        fileSize: String,
        senderCode: String = "MY_CODE",
        senderName: String = "Me"
    ): String {
        val msgId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId = msgId,
            chatId = chatId,
            senderCode = senderCode,
            senderName = senderName,
            text = "Shared file: $fileName ($fileSize)",
            timestamp = System.currentTimeMillis(),
            type = "FILE",
            fileUrl = "simulated_local_file_url",
            fileName = fileName,
            fileType = fileType,
            fileSize = fileSize,
            transferProgress = 0.0f,
            isTransferPaused = false
        )
        messageDao.insertMessage(message)
        chatDao.updateLastMessage(chatId, "$senderName uploaded $fileName", System.currentTimeMillis(), 0)

        // Launch simulated background progress
        launchFileTransferSimulation(msgId, chatId)

        return msgId
    }

    private fun getFirebaseStorage(): com.google.firebase.storage.FirebaseStorage? {
        return try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            com.google.firebase.storage.FirebaseStorage.getInstance(app)
        } catch (e: Throwable) {
            try {
                com.google.firebase.FirebaseApp.initializeApp(context)
                com.google.firebase.storage.FirebaseStorage.getInstance()
            } catch (ex: Throwable) {
                null
            }
        }
    }

    private fun launchFileTransferSimulation(messageId: String, chatId: String) {
        // Cancel existing if any
        activeTransfers[messageId]?.cancel()
        
        val storage = getFirebaseStorage()
        val currentMsg = runBlocking { messageDao.getMessageById(messageId) }

        if (storage != null && currentMsg != null && currentMsg.fileUrl != "simulated_local_file_url_completed") {
            try {
                val fileName = currentMsg.fileName
                val metaDataBytes = "Secure E2EE Large File Transfer Header Token. Target: $fileName, Size: ${currentMsg.fileSize}".toByteArray()
                val storageRef = storage.reference.child("chats/$chatId/$messageId/$fileName")

                val uploadTask = storageRef.putBytes(metaDataBytes)
                activeUploadTasks[messageId] = uploadTask

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = if (taskSnapshot.totalByteCount > 0) {
                        taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                    } else {
                        0.0f
                    }
                    repositoryScope.launch {
                        messageDao.updateTransferProgress(messageId, progress, uploadTask.isPaused)
                    }
                }.addOnSuccessListener {
                    activeUploadTasks.remove(messageId)
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        repositoryScope.launch {
                            val msg = messageDao.getMessageById(messageId)
                            if (msg != null) {
                                messageDao.insertMessage(msg.copy(
                                    transferProgress = 1.0f,
                                    readReceipt = "READ",
                                    fileUrl = uri.toString()
                                ))
                                chatDao.updateLastMessage(chatId, "${msg.senderName}: Sent file ${msg.fileName}", System.currentTimeMillis(), 0)
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    activeUploadTasks.remove(messageId)
                    // If failed/cancelled and not paused, fall back to simulated loop
                    repositoryScope.launch {
                        val msg = messageDao.getMessageById(messageId)
                        if (msg != null && !msg.isTransferPaused) {
                            launchFallbackSimulation(messageId, chatId)
                        }
                    }
                }
                return
            } catch (e: Throwable) {
                // Fall back to simulation
            }
        }

        launchFallbackSimulation(messageId, chatId)
    }

    private fun launchFallbackSimulation(messageId: String, chatId: String) {
        activeTransfers[messageId]?.cancel()
        val job = repositoryScope.launch {
            var currentProgress = 0.0f
            while (currentProgress < 1.0f) {
                delay(1000)

                val currentMsg = messageDao.getMessageById(messageId)
                if (currentMsg == null) {
                    break
                }

                if (currentMsg.isTransferPaused) {
                    continue
                }

                currentProgress = (currentMsg.transferProgress + 0.15f).coerceAtMost(1.0f)
                messageDao.updateTransferProgress(messageId, currentProgress, false)

                if (currentProgress >= 1.0f) {
                    messageDao.insertMessage(currentMsg.copy(
                        transferProgress = 1.0f,
                        readReceipt = "READ",
                        fileUrl = "simulated_local_file_url_completed"
                    ))
                    chatDao.updateLastMessage(chatId, "${currentMsg.senderName}: Sent file ${currentMsg.fileName}", System.currentTimeMillis(), 0)
                    break
                }
            }
            activeTransfers.remove(messageId)
        }
        activeTransfers[messageId] = job
    }

    suspend fun pauseFileTransfer(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        messageDao.updateTransferProgress(messageId, msg.transferProgress, true)
        activeUploadTasks[messageId]?.pause()
    }

    suspend fun resumeFileTransfer(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        messageDao.updateTransferProgress(messageId, msg.transferProgress, false)
        val uploadTask = activeUploadTasks[messageId]
        if (uploadTask != null) {
            uploadTask.resume()
        } else {
            val chatId = msg.chatId
            launchFileTransferSimulation(messageId, chatId)
        }
    }

    // ==========================================
    // Friend / QR Adding System
    // ==========================================

    suspend fun addFriendByCode(userCode: String): Boolean {
        // Find if user exists
        val uCode = userCode.uppercase().trim()
        val user = userDao.getUserByCode(uCode)
        return if (user != null) {
            userDao.updateFriendStatus(uCode, isFriend = true, hasPending = false, isIncoming = false)
            // Create chat for them
            chatDao.insertChat(
                ChatEntity(
                    chatId = "CHAT_${uCode.replace("-", "")}",
                    chatName = user.username,
                    isGroup = false,
                    lastMessageText = "You are now connected on LM Connect!",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = 0
                )
            )
            true
        } else {
            // Generate a random user to mock adding
            val randomNames = listOf("David Miller", "Emma Watson", "Tony Stark", "Diana Prince", "Bruce Wayne")
            val name = randomNames.random()
            val newUser = UserEntity(
                userCode = uCode,
                username = name,
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                coverUrl = "",
                bio = "Newly connected via QR Scanner.",
                statusText = "Hey, let's share some gigabytes!",
                onlineStatus = "Online",
                isFriend = true,
                hasPendingRequest = false,
                isIncomingRequest = false,
                isBlocked = false
            )
            userDao.insertUser(newUser)
            chatDao.insertChat(
                ChatEntity(
                    chatId = "CHAT_${uCode.replace("-", "")}",
                    chatName = name,
                    isGroup = false,
                    lastMessageText = "You are now connected on LM Connect!",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = 0
                )
            )
            true
        }
    }

    suspend fun handleFriendRequest(userCode: String, accept: Boolean) {
        if (accept) {
            userDao.updateFriendStatus(userCode, isFriend = true, hasPending = false, isIncoming = false)
            val user = userDao.getUserByCode(userCode) ?: return
            chatDao.insertChat(
                ChatEntity(
                    chatId = "CHAT_${userCode.replace("-", "")}",
                    chatName = user.username,
                    isGroup = false,
                    lastMessageText = "Friend request accepted! Say hello.",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = 0
                )
            )
        } else {
            userDao.updateFriendStatus(userCode, isFriend = false, hasPending = false, isIncoming = false)
        }
    }

    suspend fun blockUser(userCode: String, isBlocked: Boolean) {
        userDao.updateBlockStatus(userCode, isBlocked)
        if (isBlocked) {
            userDao.updateFriendStatus(userCode, isFriend = false, hasPending = false, isIncoming = false)
        }
    }

    suspend fun reportUser(userCode: String) {
        userDao.reportUser(userCode)
    }

    // ==========================================
    // Voice & Video Call Log Systems
    // ==========================================

    suspend fun registerCall(contactName: String, userCode: String, isVoice: Boolean, isIncoming: Boolean, status: String, durationSec: Int) {
        val call = CallHistoryEntity(
            callId = UUID.randomUUID().toString(),
            contactName = contactName,
            userCode = userCode,
            isVoice = isVoice,
            isIncoming = isIncoming,
            timestamp = System.currentTimeMillis(),
            durationSec = durationSec,
            status = status
        )
        callDao.insertCall(call)
    }

    suspend fun clearCallLogs() {
        callDao.clearCallHistory()
    }
}
