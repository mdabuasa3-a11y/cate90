package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// Entities
// ==========================================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userCode: String,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String,
    val bio: String,
    val statusText: String,
    val onlineStatus: String, // "Online", "Offline", "Last seen 2h ago", "Typing..."
    val isFriend: Boolean,
    val hasPendingRequest: Boolean,
    val isIncomingRequest: Boolean,
    val isBlocked: Boolean,
    val reportCount: Int = 0
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val chatName: String,
    val isGroup: Boolean,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val unreadCount: Int
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderCode: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val type: String, // "TEXT", "VOICE", "FILE", "IMAGE", "STICKER", "GIF"
    val fileUrl: String,
    val fileName: String,
    val fileType: String, // "APK", "ZIP", "PSD", "PDF", "MP3", "MP4", "AI", "ISO", "NONE"
    val fileSize: String,
    val transferProgress: Float = 1.0f, // 0.0 to 1.0
    val isTransferPaused: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val reactions: String = "", // Comma-separated list of emojis, e.g. "👍,❤️"
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val readReceipt: String = "SENT" // "SENT", "DELIVERED", "READ"
)

@Entity(tableName = "calls")
data class CallHistoryEntity(
    @PrimaryKey val callId: String,
    val contactName: String,
    val userCode: String,
    val isVoice: Boolean, // false = video
    val isIncoming: Boolean,
    val timestamp: Long,
    val durationSec: Int,
    val status: String // "Missed", "Completed", "Declined"
)

// ==========================================
// DAOs
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isFriend = 1 AND isBlocked = 0")
    fun getFriends(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE hasPendingRequest = 1 AND isBlocked = 0")
    fun getPendingRequests(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isBlocked = 1")
    fun getBlockedUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userCode = :userCode LIMIT 1")
    suspend fun getUserByCode(userCode: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isFriend = :isFriend, hasPendingRequest = :hasPending, isIncomingRequest = :isIncoming WHERE userCode = :userCode")
    suspend fun updateFriendStatus(userCode: String, isFriend: Boolean, hasPending: Boolean, isIncoming: Boolean)

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE userCode = :userCode")
    suspend fun updateBlockStatus(userCode: String, isBlocked: Boolean)

    @Query("UPDATE users SET reportCount = reportCount + 1 WHERE userCode = :userCode")
    suspend fun reportUser(userCode: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("UPDATE chats SET lastMessageText = :msg, lastMessageTime = :time, unreadCount = :unread WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, msg: String, time: Long, unread: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun clearUnread(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :msgId LIMIT 1")
    suspend fun getMessageById(msgId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE type = 'FILE' OR type = 'IMAGE' OR type = 'VIDEO'")
    fun getSharedFiles(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE messageId = :messageId")
    suspend fun editMessage(messageId: String, newText: String)

    @Query("UPDATE messages SET isDeleted = 1, text = 'Message deleted' WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("UPDATE messages SET reactions = :reactions WHERE messageId = :messageId")
    suspend fun updateReactions(messageId: String, reactions: String)

    @Query("UPDATE messages SET transferProgress = :progress, isTransferPaused = :isPaused WHERE messageId = :messageId")
    suspend fun updateTransferProgress(messageId: String, progress: Float, isPaused: Boolean)
}

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallHistoryEntity)

    @Query("DELETE FROM calls WHERE callId = :callId")
    suspend fun deleteCall(callId: String)

    @Query("DELETE FROM calls")
    suspend fun clearCallHistory()
}

// ==========================================
// Database
// ==========================================

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        CallHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LmConnectDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: LmConnectDatabase? = null

        fun getDatabase(context: android.content.Context): LmConnectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LmConnectDatabase::class.java,
                    "lm_connect_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
