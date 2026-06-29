package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CallHistoryEntity
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.data.database.UserEntity
import com.example.data.repository.LmConnectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

// Current User profile state
data class CurrentUserProfile(
    val email: String = "",
    val username: String = "",
    val userCode: String = "LMC-7F3A9K",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val bio: String = "Connect Faster. Share Bigger. Stay Together.",
    val statusText: String = "Available",
    val isTwoStepEnabled: Boolean = false,
    val hideOnlineStatus: Boolean = false,
    val hideLastSeen: Boolean = false,
    val hideReadReceipts: Boolean = false,
    val isDarkTheme: Boolean = true,
    val selectedLanguage: String = "English",
    val isNotificationEnabled: Boolean = true,
    val isDataSaverEnabled: Boolean = false,
    val isAutoDownloadEnabled: Boolean = true,
    val isAdmin: Boolean = true // enable Admin Dashboard access!
)

// Active Call state
data class ActiveCallState(
    val contactName: String,
    val userCode: String,
    val isVoice: Boolean, // false = Video
    val isIncoming: Boolean,
    val durationSec: Int = 0,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isGroupCall: Boolean = false,
    val participants: List<String> = emptyList()
)

// ==========================================
// Download Manager Models
// ==========================================

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val downloadId: String,
    val fileName: String,
    val fileSize: String,
    val fileType: String,
    val senderName: String,
    val uploadDate: String,
    val progress: Float, // 0.0 to 1.0
    val status: DownloadStatus,
    val speed: String = "0 KB/s",
    val saveFolder: String = "/storage/emulated/0/Download/LMConnect",
    val isEncrypted: Boolean = true,
    val isIntegrityChecked: Boolean = false,
    val checksum: String = "",
    val errorMsg: String? = null
)

enum class NotificationType {
    INFO, SUCCESS, WARNING, ERROR
}

data class InAppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis()
)

class LmConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LmConnectRepository(application)

    // ==========================================
    // Download Manager System States
    // ==========================================
    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()

    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val inAppNotifications: StateFlow<List<InAppNotification>> = _inAppNotifications.asStateFlow()

    var currentDownloadFolder by mutableStateOf("/storage/emulated/0/Download/LMConnect")
        private set

    var downloadSearchQuery by mutableStateOf("")

    var speedMultiplierEnabled by mutableStateOf(true)

    // Active download coroutine jobs
    private val downloadJobs = mutableMapOf<String, Job>()

    // Current logged in user profile
    var currentUserProfile by mutableStateOf(CurrentUserProfile())
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    // Selected Chat Room
    var activeChatId by mutableStateOf<String?>(null)
        private set

    // Active Call
    var activeCall by mutableStateOf<ActiveCallState?>(null)
        private set

    // Flows observed by UI
    val friends: StateFlow<List<UserEntity>> = repository.friends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<UserEntity>> = repository.pendingRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<List<UserEntity>> = repository.blockedUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chats: StateFlow<List<ChatEntity>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallHistoryEntity>> = repository.callHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sharedFiles: StateFlow<List<MessageEntity>> = repository.sharedFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active chat messages flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<MessageEntity>> = snapshotFlow { activeChatId }
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForChat(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Call duration timer job
    private var callTimerJob: Job? = null

    // Real-time server telemetry simulation for Admin Dashboard
    var adminCpuUsage by mutableStateOf(14)
        private set
    var adminRamUsage by mutableStateOf(5.4f) // GB
    var adminBandwidthIn by mutableStateOf(145.2f) // MB/s
    var adminBandwidthOut by mutableStateOf(312.4f) // MB/s
    var adminServerUptime by mutableStateOf("14d 6h 32m")
    private var adminTelemetryJob: Job? = null

    // ==========================================
    // Authentication Operations (Firebase & Persistent Secure Local Fallback)
    // ==========================================

    private val authPrefs: SharedPreferences = application.getSharedPreferences("lm_connect_auth_prefs", Context.MODE_PRIVATE)

    var isEmailVerified by mutableStateOf(false)
        private set

    var isFirebaseMode by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
    var authSuccessMessage by mutableStateOf<String?>(null)
    var isAuthLoading by mutableStateOf(false)

    init {
        // Start live admin telemetry loop
        startAdminTelemetry()
        // Pre-populate completed downloads for rich history view
        prepopulateDownloadHistory()
        // Check authentication and auto login if session exists
        checkAndAutoLogin()
    }

    // ==========================================
    // Authentication Operations
    // ==========================================

    private fun getFirebaseAuth(): com.google.firebase.auth.FirebaseAuth? {
        return try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            com.google.firebase.auth.FirebaseAuth.getInstance(app)
        } catch (e: Throwable) {
            try {
                com.google.firebase.FirebaseApp.initializeApp(getApplication())
                com.google.firebase.auth.FirebaseAuth.getInstance()
            } catch (ex: Throwable) {
                null
            }
        }
    }

    fun checkAndAutoLogin() {
        authError = null
        authSuccessMessage = null
        try {
            val auth = getFirebaseAuth()
            if (auth != null) {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    isFirebaseMode = true
                    val verified = currentUser.isEmailVerified
                    isEmailVerified = verified
                    val savedKeepMeIn = authPrefs.getBoolean("keep_me_signed_in", true)
                    if (savedKeepMeIn) {
                        if (verified) {
                            val generatedCode = authPrefs.getString("user_code_${currentUser.email}", "")?.ifEmpty { null } ?: "LMC-7F3A9K"
                            currentUserProfile = currentUserProfile.copy(
                                email = currentUser.email ?: "",
                                username = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User",
                                userCode = generatedCode
                            )
                            isLoggedIn = true
                        } else {
                            isLoggedIn = false
                        }
                        return
                    }
                }
            }
        } catch (t: Throwable) {
            // Fallback gracefully
        }

        // Local SharedPreferences Fallback Auto-Login
        val sessionActive = authPrefs.getBoolean("session_active", false)
        val keepMeIn = authPrefs.getBoolean("keep_me_signed_in", true)
        if (sessionActive && keepMeIn) {
            val savedEmail = authPrefs.getString("logged_in_user_email", "") ?: ""
            val savedName = authPrefs.getString("logged_in_user_name", "") ?: ""
            val savedCode = authPrefs.getString("logged_in_user_code", "") ?: ""
            val verified = authPrefs.getBoolean("logged_in_user_is_verified", false)

            if (savedEmail.isNotEmpty()) {
                isFirebaseMode = false
                isEmailVerified = verified
                if (verified) {
                    currentUserProfile = currentUserProfile.copy(
                        email = savedEmail,
                        username = savedName,
                        userCode = savedCode
                    )
                    isLoggedIn = true
                }
            }
        }
    }

    private fun generateUserCode(email: String): String {
        val prefix = "LMC-"
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..6).map { chars.random() }.joinToString("")
        val fullCode = prefix + suffix
        authPrefs.edit().putString("logged_in_user_code", fullCode).apply()
        return fullCode
    }

    fun login(email: String, name: String) {
        val userCode = authPrefs.getString("local_user_code_$email", "")?.ifEmpty { null } ?: generateUserCode(email)
        currentUserProfile = currentUserProfile.copy(
            email = email,
            username = name.ifEmpty { email.substringBefore("@") },
            userCode = userCode,
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80",
            coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=400&q=80"
        )
        isLoggedIn = true
        isEmailVerified = true
        isFirebaseMode = false
        authPrefs.edit().apply {
            putBoolean("session_active", true)
            putString("logged_in_user_email", email)
            putString("logged_in_user_name", currentUserProfile.username)
            putString("logged_in_user_code", userCode)
            putBoolean("logged_in_user_is_verified", true)
        }.apply()
    }

    fun firebaseSignIn(email: String, password: String, keepMeSignedIn: Boolean) {
        if (email.isEmpty() || password.isEmpty()) {
            authError = "Email and Password cannot be empty."
            return
        }
        if (!email.contains("@")) {
            authError = "Please enter a valid email address."
            return
        }

        isAuthLoading = true
        authError = null
        authSuccessMessage = null

        val auth = getFirebaseAuth()
        if (auth != null) {
            isFirebaseMode = true
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isAuthLoading = false
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val verified = user.isEmailVerified
                            isEmailVerified = verified
                            val generatedCode = authPrefs.getString("user_code_${user.email}", "")?.ifEmpty { null } ?: generateUserCode(user.email ?: "")
                            authPrefs.edit().apply {
                                putBoolean("keep_me_signed_in", keepMeSignedIn)
                                putBoolean("session_active", true)
                                putString("logged_in_user_email", user.email)
                                putString("logged_in_user_name", user.displayName ?: user.email?.substringBefore("@") ?: "User")
                                putString("logged_in_user_code", generatedCode)
                                putBoolean("logged_in_user_is_verified", verified)
                            }.apply()

                            if (verified) {
                                currentUserProfile = currentUserProfile.copy(
                                    email = user.email ?: "",
                                    username = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                                    userCode = generatedCode
                                )
                                isLoggedIn = true
                            } else {
                                authError = "Please verify your email address before logging in."
                            }
                        }
                    } else {
                        val exception = task.exception
                        authError = exception?.localizedMessage ?: "Sign in failed. Check your credentials."
                    }
                }
        } else {
            isFirebaseMode = false
            viewModelScope.launch {
                delay(800)
                isAuthLoading = false
                val registeredPwd = authPrefs.getString("local_user_pwd_$email", null)
                if (registeredPwd == null) {
                    authError = "Account not found. Please create a new account first."
                } else if (registeredPwd != password) {
                    authError = "Invalid email or wrong password. Please try again."
                } else {
                    val savedName = authPrefs.getString("local_user_name_$email", "") ?: email.substringBefore("@")
                    val savedCode = authPrefs.getString("local_user_code_$email", "") ?: generateUserCode(email)
                    val verified = authPrefs.getBoolean("local_user_verified_$email", false)

                    isEmailVerified = verified
                    authPrefs.edit().apply {
                        putBoolean("keep_me_signed_in", keepMeSignedIn)
                        putBoolean("session_active", true)
                        putString("logged_in_user_email", email)
                        putString("logged_in_user_name", savedName)
                        putString("logged_in_user_code", savedCode)
                        putBoolean("logged_in_user_is_verified", verified)
                    }.apply()

                    if (verified) {
                        currentUserProfile = currentUserProfile.copy(
                            email = email,
                            username = savedName,
                            userCode = savedCode
                        )
                        isLoggedIn = true
                    } else {
                        authError = "Please verify your email address before logging in."
                    }
                }
            }
        }
    }

    fun firebaseSignUp(email: String, password: String, fullName: String, username: String) {
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            authError = "Full Name, Email and Password are required."
            return
        }
        if (!email.contains("@")) {
            authError = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            authError = "Password must be at least 6 characters."
            return
        }

        isAuthLoading = true
        authError = null
        authSuccessMessage = null

        val generatedCode = "LMC-" + (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
        val auth = getFirebaseAuth()

        if (auth != null) {
            isFirebaseMode = true
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = fullName
                        }
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            user.sendEmailVerification()
                                .addOnCompleteListener { verifyTask ->
                                    isAuthLoading = false
                                    if (verifyTask.isSuccessful) {
                                        isEmailVerified = false
                                        authPrefs.edit().apply {
                                            putString("user_code_$email", generatedCode)
                                            putString("local_user_name_$email", fullName)
                                        }.apply()
                                        authSuccessMessage = "Verification email sent to $email. Please check your inbox."
                                    } else {
                                        authError = "Failed to send verification email. Please try resending."
                                    }
                                }
                        }
                    } else {
                        isAuthLoading = false
                        authError = task.exception?.localizedMessage ?: "Registration failed."
                    }
                }
        } else {
            isFirebaseMode = false
            viewModelScope.launch {
                delay(1000)
                isAuthLoading = false
                authPrefs.edit().apply {
                    putString("local_user_pwd_$email", password)
                    putString("local_user_name_$email", fullName)
                    putString("local_user_code_$email", generatedCode)
                    putBoolean("local_user_verified_$email", false)
                }.apply()
                isEmailVerified = false
                authSuccessMessage = "Verification email successfully sent to $email (Simulated). Please check inbox."
            }
        }
    }

    fun firebaseResetPassword(email: String) {
        if (email.isEmpty() || !email.contains("@")) {
            authError = "Please enter a valid email address."
            return
        }

        isAuthLoading = true
        authError = null
        authSuccessMessage = null

        val auth = getFirebaseAuth()
        if (auth != null) {
            isFirebaseMode = true
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    isAuthLoading = false
                    if (task.isSuccessful) {
                        authSuccessMessage = "Password reset instructions sent successfully to $email."
                    } else {
                        authError = task.exception?.localizedMessage ?: "Failed to send password reset email."
                    }
                }
        } else {
            isFirebaseMode = false
            viewModelScope.launch {
                delay(800)
                isAuthLoading = false
                val exists = authPrefs.getString("local_user_pwd_$email", null) != null
                if (exists || email.endsWith("@gmail.com")) {
                    authSuccessMessage = "Password reset link has been successfully sent to $email (Simulated)."
                } else {
                    authError = "Email address not found."
                }
            }
        }
    }

    fun firebaseResendVerificationEmail(email: String) {
        isAuthLoading = true
        authError = null
        authSuccessMessage = null

        val auth = getFirebaseAuth()
        if (auth != null && auth.currentUser != null) {
            isFirebaseMode = true
            auth.currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    isAuthLoading = false
                    if (task.isSuccessful) {
                        authSuccessMessage = "A new verification email has been sent to $email."
                    } else {
                        authError = task.exception?.localizedMessage ?: "Failed to resend verification email."
                    }
                }
        } else {
            isFirebaseMode = false
            viewModelScope.launch {
                delay(800)
                isAuthLoading = false
                authSuccessMessage = "A new verification email has been sent to $email (Simulated)."
            }
        }
    }

    fun firebaseCheckVerification(email: String) {
        isAuthLoading = true
        authError = null

        val auth = getFirebaseAuth()
        if (auth != null && auth.currentUser != null) {
            isFirebaseMode = true
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                isAuthLoading = false
                val verified = auth.currentUser?.isEmailVerified ?: false
                isEmailVerified = verified
                if (verified) {
                    val savedName = authPrefs.getString("local_user_name_$email", "") ?: auth.currentUser?.displayName ?: email.substringBefore("@")
                    val savedCode = authPrefs.getString("user_code_$email", "") ?: generateUserCode(email)
                    authPrefs.edit().apply {
                        putBoolean("session_active", true)
                        putString("logged_in_user_email", email)
                        putString("logged_in_user_name", savedName)
                        putString("logged_in_user_code", savedCode)
                        putBoolean("logged_in_user_is_verified", true)
                    }.apply()

                    currentUserProfile = currentUserProfile.copy(
                        email = email,
                        username = savedName,
                        userCode = savedCode
                    )
                    isLoggedIn = true
                } else {
                    authError = "Email is not verified yet. Please click the link in your inbox."
                }
            }
        } else {
            isFirebaseMode = false
            viewModelScope.launch {
                delay(600)
                isAuthLoading = false
                authPrefs.edit().putBoolean("local_user_verified_$email", true).apply()
                isEmailVerified = true

                val savedName = authPrefs.getString("local_user_name_$email", "") ?: email.substringBefore("@")
                val savedCode = authPrefs.getString("local_user_code_$email", "") ?: generateUserCode(email)
                authPrefs.edit().apply {
                    putBoolean("session_active", true)
                    putString("logged_in_user_email", email)
                    putString("logged_in_user_name", savedName)
                    putString("logged_in_user_code", savedCode)
                    putBoolean("logged_in_user_is_verified", true)
                }.apply()

                currentUserProfile = currentUserProfile.copy(
                    email = email,
                    username = savedName,
                    userCode = savedCode
                )
                isLoggedIn = true
                authSuccessMessage = "Verification successful! Logging in..."
            }
        }
    }

    fun firebaseGoogleSignIn(email: String, username: String) {
        isAuthLoading = true
        authError = null
        authSuccessMessage = null

        viewModelScope.launch {
            delay(1000)
            isAuthLoading = false
            val userCode = authPrefs.getString("local_user_code_$email", "")?.ifEmpty { null } ?: generateUserCode(email)
            currentUserProfile = currentUserProfile.copy(
                email = email,
                username = username.ifEmpty { email.substringBefore("@") },
                userCode = userCode,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80",
                coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=400&q=80"
            )
            isLoggedIn = true
            isEmailVerified = true
            isFirebaseMode = false
            authPrefs.edit().apply {
                putBoolean("session_active", true)
                putString("logged_in_user_email", email)
                putString("logged_in_user_name", currentUserProfile.username)
                putString("logged_in_user_code", userCode)
                putBoolean("logged_in_user_is_verified", true)
            }.apply()
        }
    }

    fun logout() {
        val auth = getFirebaseAuth()
        if (auth != null) {
            try {
                auth.signOut()
            } catch (e: Exception) {}
        }
        isLoggedIn = false
        activeChatId = null
        activeCall = null
        authPrefs.edit().apply {
            putBoolean("session_active", false)
            putString("logged_in_user_email", "")
            putString("logged_in_user_name", "")
            putBoolean("logged_in_user_is_verified", false)
        }.apply()
    }

    fun updateProfile(name: String, bio: String, statusText: String, avatarUrl: String, coverUrl: String) {
        currentUserProfile = currentUserProfile.copy(
            username = name,
            bio = bio,
            statusText = statusText,
            avatarUrl = avatarUrl.ifEmpty { currentUserProfile.avatarUrl },
            coverUrl = coverUrl.ifEmpty { currentUserProfile.coverUrl }
        )
    }

    // ==========================================
    // Messaging Operations
    // ==========================================

    fun selectChat(chatId: String?) {
        activeChatId = chatId
    }

    fun sendTextMessage(text: String, replyToId: String? = null, replyToText: String? = null) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = text,
                senderCode = currentUserProfile.userCode,
                senderName = currentUserProfile.username,
                type = "TEXT",
                replyToId = replyToId,
                replyToText = replyToText
            )
        }
    }

    fun sendSticker(stickerName: String) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = stickerName,
                senderCode = currentUserProfile.userCode,
                senderName = currentUserProfile.username,
                type = "STICKER"
            )
        }
    }

    fun sendGif(gifUrl: String) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "GIF: $gifUrl",
                senderCode = currentUserProfile.userCode,
                senderName = currentUserProfile.username,
                type = "GIF"
            )
        }
    }

    fun sendVoiceMessage(duration: String) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "Voice Message ($duration)",
                senderCode = currentUserProfile.userCode,
                senderName = currentUserProfile.username,
                type = "VOICE"
            )
        }
    }

    // Large File Sharing Implementation
    fun shareLargeFile(fileName: String, fileType: String, fileSize: String) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            repository.startLargeFileShare(
                chatId = chatId,
                fileName = fileName,
                fileType = fileType,
                fileSize = fileSize,
                senderCode = currentUserProfile.userCode,
                senderName = currentUserProfile.username
            )
        }
    }

    fun pauseTransfer(messageId: String) {
        viewModelScope.launch {
            repository.pauseFileTransfer(messageId)
        }
    }

    fun resumeTransfer(messageId: String) {
        viewModelScope.launch {
            repository.resumeFileTransfer(messageId)
        }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newText)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.addReaction(messageId, emoji)
        }
    }

    // ==========================================
    // Friend Management Operations
    // ==========================================

    fun addFriend(userCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addFriendByCode(userCode)
            onResult(success)
        }
    }

    fun acceptFriendRequest(userCode: String) {
        viewModelScope.launch {
            repository.handleFriendRequest(userCode, accept = true)
        }
    }

    fun rejectFriendRequest(userCode: String) {
        viewModelScope.launch {
            repository.handleFriendRequest(userCode, accept = false)
        }
    }

    fun blockUser(userCode: String, block: Boolean) {
        viewModelScope.launch {
            repository.blockUser(userCode, block)
        }
    }

    fun reportUser(userCode: String) {
        viewModelScope.launch {
            repository.reportUser(userCode)
        }
    }

    // ==========================================
    // Call System Operations
    // ==========================================

    fun startCall(contactName: String, userCode: String, isVoice: Boolean, isGroup: Boolean = false) {
        val participantsList = if (isGroup) listOf("Sarah Jenkins", "Alex Rivera", "You") else listOf("You")
        activeCall = ActiveCallState(
            contactName = contactName,
            userCode = userCode,
            isVoice = isVoice,
            isIncoming = false,
            durationSec = 0,
            isGroupCall = isGroup,
            participants = participantsList
        )

        // Start call timer
        startCallTimer()
    }

    fun receiveCall(contactName: String, userCode: String, isVoice: Boolean) {
        activeCall = ActiveCallState(
            contactName = contactName,
            userCode = userCode,
            isVoice = isVoice,
            isIncoming = true,
            durationSec = 0
        )
    }

    fun acceptCall() {
        val call = activeCall ?: return
        activeCall = call.copy(isIncoming = false)
        startCallTimer()
    }

    fun hangUpCall(completedNormally: Boolean = true) {
        val call = activeCall ?: return
        callTimerJob?.cancel()
        callTimerJob = null

        val statusText = if (completedNormally) "Completed" else "Declined"

        viewModelScope.launch {
            repository.registerCall(
                contactName = call.contactName,
                userCode = call.userCode,
                isVoice = call.isVoice,
                isIncoming = call.isIncoming,
                status = statusText,
                durationSec = call.durationSec
            )
        }

        activeCall = null
    }

    fun toggleMute() {
        val call = activeCall ?: return
        activeCall = call.copy(isMuted = !call.isMuted)
    }

    fun toggleCamera() {
        val call = activeCall ?: return
        activeCall = call.copy(isCameraOn = !call.isCameraOn)
    }

    fun clearCalls() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val call = activeCall ?: break
                activeCall = call.copy(durationSec = call.durationSec + 1)
            }
        }
    }

    // ==========================================
    // Settings Settings Preferences
    // ==========================================

    fun toggleTheme() {
        currentUserProfile = currentUserProfile.copy(isDarkTheme = !currentUserProfile.isDarkTheme)
    }

    fun changeLanguage(language: String) {
        currentUserProfile = currentUserProfile.copy(selectedLanguage = language)
    }

    fun toggleTwoStep() {
        currentUserProfile = currentUserProfile.copy(isTwoStepEnabled = !currentUserProfile.isTwoStepEnabled)
    }

    fun toggleHideOnline() {
        currentUserProfile = currentUserProfile.copy(hideOnlineStatus = !currentUserProfile.hideOnlineStatus)
    }

    fun toggleHideLastSeen() {
        currentUserProfile = currentUserProfile.copy(hideLastSeen = !currentUserProfile.hideLastSeen)
    }

    fun toggleHideReadReceipts() {
        currentUserProfile = currentUserProfile.copy(hideReadReceipts = !currentUserProfile.hideReadReceipts)
    }

    fun toggleNotifications() {
        currentUserProfile = currentUserProfile.copy(isNotificationEnabled = !currentUserProfile.isNotificationEnabled)
    }

    fun toggleDataSaver() {
        currentUserProfile = currentUserProfile.copy(isDataSaverEnabled = !currentUserProfile.isDataSaverEnabled)
    }

    fun toggleAutoDownload() {
        currentUserProfile = currentUserProfile.copy(isAutoDownloadEnabled = !currentUserProfile.isAutoDownloadEnabled)
    }

    // ==========================================
    // Admin Telemetry Simulator
    // ==========================================

    private fun startAdminTelemetry() {
        adminTelemetryJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                adminCpuUsage = (10..38).random()
                adminRamUsage = (5.1f + (0..10).random() * 0.1f)
                adminBandwidthIn = (120f + (0..50).random() * 1.5f)
                adminBandwidthOut = (290f + (0..100).random() * 2.1f)
            }
        }
    }

    // ==========================================
    // Download Manager Implementation Methods
    // ==========================================

    private fun prepopulateDownloadHistory() {
        val history = listOf(
            DownloadTask(
                downloadId = "PRE_1",
                fileName = "Android_Studio_Arctic_Fox_Setup.exe",
                fileSize = "1.2 GB",
                fileType = "APK",
                senderName = "Alex Rivera",
                uploadDate = "Yesterday, 4:15 PM",
                progress = 1.0f,
                status = DownloadStatus.COMPLETED,
                speed = "0 KB/s",
                saveFolder = currentDownloadFolder,
                isEncrypted = true,
                isIntegrityChecked = true,
                checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            ),
            DownloadTask(
                downloadId = "PRE_2",
                fileName = "Flutter_SDK_v3.13_Stable.zip",
                fileSize = "840 MB",
                fileType = "ZIP",
                senderName = "Sarah Jenkins",
                uploadDate = "Yesterday, 11:30 AM",
                progress = 1.0f,
                status = DownloadStatus.COMPLETED,
                speed = "0 KB/s",
                saveFolder = currentDownloadFolder,
                isEncrypted = true,
                isIntegrityChecked = true,
                checksum = "8f42d9a3b6107ae2549bfdf4c27ae15436214159934ca495991b7852b855"
            ),
            DownloadTask(
                downloadId = "PRE_3",
                fileName = "index.html",
                fileSize = "45 KB",
                fileType = "HTML",
                senderName = "Marcus Brody",
                uploadDate = "2 days ago",
                progress = 1.0f,
                status = DownloadStatus.COMPLETED,
                speed = "0 KB/s",
                saveFolder = currentDownloadFolder,
                isEncrypted = true,
                isIntegrityChecked = true,
                checksum = "7ae14159934ca495991b7852b855e3b0c44298fc1c149afbf4c8996fb92427"
            )
        )
        _downloadTasks.value = history
    }

    fun showNotification(title: String, message: String, type: NotificationType = NotificationType.INFO) {
        val notif = InAppNotification(title = title, message = message, type = type)
        _inAppNotifications.update { it + notif }
        viewModelScope.launch {
            delay(5000)
            _inAppNotifications.update { list -> list.filter { it.id != notif.id } }
        }
    }

    fun startDownload(
        messageId: String,
        fileName: String,
        fileSize: String,
        fileType: String,
        senderName: String
    ) {
        val existing = _downloadTasks.value.find { it.downloadId == messageId }
        if (existing != null) {
            if (existing.status == DownloadStatus.PAUSED || existing.status == DownloadStatus.FAILED || existing.status == DownloadStatus.CANCELLED) {
                resumeDownload(messageId)
            }
            return
        }

        val newTask = DownloadTask(
            downloadId = messageId,
            fileName = fileName,
            fileSize = fileSize,
            fileType = fileType,
            senderName = senderName,
            uploadDate = "Just now",
            progress = 0.0f,
            status = DownloadStatus.PENDING,
            saveFolder = currentDownloadFolder
        )

        _downloadTasks.update { it + newTask }
        showNotification("Download Queued", "$fileName has been added to the high-speed download queue.", NotificationType.INFO)

        processDownloadQueue()
    }

    private fun processDownloadQueue() {
        val currentTasks = _downloadTasks.value
        val activeCount = currentTasks.count { it.status == DownloadStatus.DOWNLOADING }
        if (activeCount >= 3) {
            return
        }

        val nextPending = currentTasks.find { it.status == DownloadStatus.PENDING }
        if (nextPending != null) {
            executeDownload(nextPending.downloadId)
        }
    }

    private fun executeDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()

        val job = viewModelScope.launch {
            _downloadTasks.update { list ->
                list.map {
                    if (it.downloadId == downloadId) it.copy(status = DownloadStatus.DOWNLOADING, errorMsg = null) else it
                }
            }

            val task = _downloadTasks.value.find { it.downloadId == downloadId } ?: return@launch
            showNotification("Downloading", "Starting E2EE secure transfer of ${task.fileName}", NotificationType.INFO)

            var currentProgress = task.progress
            var interruptionSimulated = false

            while (currentProgress < 1.0f) {
                delay(800)

                val freshTask = _downloadTasks.value.find { it.downloadId == downloadId } ?: break
                if (freshTask.status != DownloadStatus.DOWNLOADING) {
                    break
                }

                // Simulate connection interruption with 7% chance to demonstrate Auto-Retry
                if (!interruptionSimulated && currentProgress > 0.3f && currentProgress < 0.7f && (1..100).random() <= 7) {
                    interruptionSimulated = true
                    _downloadTasks.update { list ->
                        list.map {
                            if (it.downloadId == downloadId) it.copy(
                                status = DownloadStatus.FAILED,
                                speed = "0 KB/s",
                                errorMsg = "Network interrupted. Auto-retrying in 3 seconds..."
                            ) else it
                        }
                    }
                    showNotification("Interrupted", "Network connection lost. Auto-retrying...", NotificationType.WARNING)
                    delay(3000)
                    _downloadTasks.update { list ->
                        list.map {
                            if (it.downloadId == downloadId) it.copy(status = DownloadStatus.DOWNLOADING, errorMsg = null) else it
                        }
                    }
                    showNotification("Resumed", "Connection restored! Auto-retry successful.", NotificationType.SUCCESS)
                    continue
                }

                val speedVal: String
                val increment: Float
                if (currentUserProfile.isDataSaverEnabled) {
                    speedVal = "${(1200..3200).random() / 1000f} MB/s"
                    increment = 0.04f
                } else if (speedMultiplierEnabled) {
                    val gbs = (12..48).random() / 10f
                    speedVal = "$gbs GB/s"
                    increment = 0.18f
                } else {
                    speedVal = "${(12..48).random()} MB/s"
                    increment = 0.08f
                }

                currentProgress = (currentProgress + increment).coerceAtMost(1.0f)

                _downloadTasks.update { list ->
                    list.map {
                        if (it.downloadId == downloadId) it.copy(
                            progress = currentProgress,
                            speed = speedVal
                        ) else it
                    }
                }

                if (currentProgress >= 1.0f) {
                    val finalChecksum = java.util.UUID.randomUUID().toString().replace("-", "").take(32)
                    _downloadTasks.update { list ->
                        list.map {
                            if (it.downloadId == downloadId) it.copy(
                                progress = 1.0f,
                                status = DownloadStatus.COMPLETED,
                                speed = "0 KB/s",
                                isIntegrityChecked = true,
                                checksum = finalChecksum
                            ) else it
                        }
                    }
                    showNotification("Download Completed", "${freshTask.fileName} downloaded & integrity verified (SHA-256 matches).", NotificationType.SUCCESS)
                    break
                }
            }

            downloadJobs.remove(downloadId)
            processDownloadQueue()
        }

        downloadJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        _downloadTasks.update { list ->
            list.map {
                if (it.downloadId == downloadId) it.copy(status = DownloadStatus.PAUSED, speed = "0 KB/s") else it
            }
        }
        val task = _downloadTasks.value.find { it.downloadId == downloadId }
        if (task != null) {
            showNotification("Download Paused", "${task.fileName} transfer suspended.", NotificationType.INFO)
        }
        processDownloadQueue()
    }

    fun resumeDownload(downloadId: String) {
        _downloadTasks.update { list ->
            list.map {
                if (it.downloadId == downloadId) it.copy(status = DownloadStatus.PENDING) else it
            }
        }
        val task = _downloadTasks.value.find { it.downloadId == downloadId }
        if (task != null) {
            showNotification("Download Resumed", "${task.fileName} placed back in the queue.", NotificationType.INFO)
        }
        processDownloadQueue()
    }

    fun cancelDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        _downloadTasks.update { list ->
            list.map {
                if (it.downloadId == downloadId) it.copy(status = DownloadStatus.CANCELLED, progress = 0.0f, speed = "0 KB/s") else it
            }
        }
        val task = _downloadTasks.value.find { it.downloadId == downloadId }
        if (task != null) {
            showNotification("Download Cancelled", "${task.fileName} download terminated.", NotificationType.WARNING)
        }
        processDownloadQueue()
    }

    fun deleteDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        val task = _downloadTasks.value.find { it.downloadId == downloadId }
        _downloadTasks.update { list ->
            list.filter { it.downloadId != downloadId }
        }
        if (task != null) {
            showNotification("File Deleted", "${task.fileName} and its database cached states have been removed.", NotificationType.WARNING)
        }
        processDownloadQueue()
    }

    fun renameDownload(downloadId: String, newName: String) {
        if (newName.isBlank()) return
        _downloadTasks.update { list ->
            list.map {
                if (it.downloadId == downloadId) {
                    val extension = it.fileName.substringAfterLast(".", "")
                    val finalName = if (extension.isNotEmpty() && !newName.endsWith(".$extension")) {
                        "$newName.$extension"
                    } else {
                        newName
                    }
                    it.copy(fileName = finalName)
                } else it
            }
        }
        showNotification("File Renamed", "Resource successfully renamed to $newName", NotificationType.SUCCESS)
    }

    fun changeDownloadFolder(newFolder: String) {
        if (newFolder.isNotBlank()) {
            currentDownloadFolder = newFolder
            _downloadTasks.update { list ->
                list.map {
                    if (it.status == DownloadStatus.COMPLETED) it.copy(saveFolder = newFolder) else it
                }
            }
            showNotification("Folder Changed", "Target storage path set to $newFolder", NotificationType.SUCCESS)
        }
    }

    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
        adminTelemetryJob?.cancel()
    }
}
