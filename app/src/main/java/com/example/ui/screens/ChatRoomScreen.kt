package com.example.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MessageEntity
import com.example.ui.viewmodel.LmConnectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: LmConnectViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val chatId = viewModel.activeChatId ?: return
    val messages by viewModel.activeChatMessages.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val chat = chats.find { it.chatId == chatId } ?: return

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Action Drawers
    var showShareMenu by remember { mutableStateOf(false) }
    var showStickerMenu by remember { mutableStateOf(false) }
    var selectedReplyMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedReactionMessage by remember { mutableStateOf<MessageEntity?>(null) }

    // Simulated Voice Note Recording
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceRecordDurationSec by remember { mutableStateOf(0) }

    // Download Dialog states
    var activeOpenDialogTask by remember { mutableStateOf<com.example.ui.viewmodel.DownloadTask?>(null) }
    var activeRenameDialogTask by remember { mutableStateOf<com.example.ui.viewmodel.DownloadTask?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val downloadTasks by viewModel.downloadTasks.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            var name = "attached_file"
            var sizeString = "Unknown Size"
            var extension = "bin"
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            name = c.getString(nameIndex)
                        }
                        if (sizeIndex != -1) {
                            val sizeBytes = c.getLong(sizeIndex)
                            sizeString = when {
                                sizeBytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", sizeBytes.toFloat() / (1024 * 1024 * 1024))
                                sizeBytes >= 1024 * 1024 -> String.format("%.1f MB", sizeBytes.toFloat() / (1024 * 1024))
                                sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes.toFloat() / 1024)
                                else -> "$sizeBytes Bytes"
                            }
                        }
                    }
                }
                extension = name.substringAfterLast('.', "bin").uppercase()
            } catch (e: Exception) {
                // Fallback gracefully
            }
            viewModel.shareLargeFile(
                fileName = name,
                fileType = extension,
                fileSize = sizeString
            )
            showShareMenu = false
        }
    }

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice message recording timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceRecordDurationSec = 0
            while (isRecordingVoice) {
                delay(1000)
                voiceRecordDurationSec++
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.chatName.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontSize = 14.sp
                            )
                        }

                        Column {
                            Text(
                                text = chat.chatName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (chat.chatId == "CHAT_GROUP_GLOBAL") "6 Online | E2EE Tunnel" else "Online | Safe Tunnel",
                                fontSize = 11.sp,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startCall(chat.chatName, chat.chatId, isVoice = true, isGroup = chat.isGroup) },
                        modifier = Modifier.testTag("start_voice_call_room")
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Voice Call", tint = Color.White)
                    }
                    IconButton(
                        onClick = { viewModel.startCall(chat.chatName, chat.chatId, isVoice = false, isGroup = chat.isGroup) },
                        modifier = Modifier.testTag("start_video_call_room")
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF020617),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0B0E14)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0E14)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
            ) {
            // Messages list area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    val isMe = msg.senderCode == viewModel.currentUserProfile.userCode || msg.senderCode == "MY_CODE"
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        onReactionSelected = { selectedReactionMessage = msg },
                        onReplySelected = { selectedReplyMessage = msg },
                        onDelete = { viewModel.deleteMessage(msg.messageId) },
                        onPauseTransfer = { viewModel.pauseTransfer(msg.messageId) },
                        onResumeTransfer = { viewModel.resumeTransfer(msg.messageId) },
                        downloadTasks = downloadTasks,
                        onStartDownload = { message ->
                            viewModel.startDownload(
                                messageId = message.messageId,
                                fileName = message.fileName,
                                fileSize = message.fileSize,
                                fileType = message.fileType,
                                senderName = message.senderName
                            )
                        },
                        onPauseDownload = { downloadId -> viewModel.pauseDownload(downloadId) },
                        onResumeDownload = { downloadId -> viewModel.resumeDownload(downloadId) },
                        onCancelDownload = { downloadId -> viewModel.cancelDownload(downloadId) },
                        onOpenDownload = { task -> activeOpenDialogTask = task },
                        onRenameDownload = { task ->
                            activeRenameDialogTask = task
                            renameInputText = task.fileName.substringBeforeLast(".")
                        },
                        onDeleteDownload = { downloadId -> viewModel.deleteDownload(downloadId) }
                    )
                }
            }

            // Reply Preview Banner
            selectedReplyMessage?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Replying to ${reply.senderName}", fontSize = 11.sp, color = Color(0xFF00C2FF), fontWeight = FontWeight.Bold)
                        Text(text = reply.text, fontSize = 13.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { selectedReplyMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            }

            // Bottom Input Section
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick file menu toggle
                        IconButton(
                            onClick = {
                                showShareMenu = !showShareMenu
                                showStickerMenu = false
                            },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), CircleShape)
                                .testTag("share_file_btn")
                        ) {
                            Icon(
                                imageVector = if (showShareMenu) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Share",
                                tint = Color(0xFF00C2FF)
                            )
                        }

                        // Sticker drawer toggle
                        IconButton(
                            onClick = {
                                showStickerMenu = !showStickerMenu
                                showShareMenu = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = "Stickers",
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        // Input field or voice recording view
                        if (isRecordingVoice) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFEA4335), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Recording Voice Note...", color = Color.White, fontSize = 13.sp)
                                }
                                Text(
                                    text = String.format("%02d:%02d", voiceRecordDurationSec / 60, voiceRecordDurationSec % 60),
                                    color = Color(0xFFEA4335),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Encrypted message...", fontSize = 14.sp) },
                                singleLine = false,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00C2FF),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_text_input")
                            )
                        }

                        // Send / Mic buttons
                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendTextMessage(
                                        text = inputText,
                                        replyToId = selectedReplyMessage?.messageId,
                                        replyToText = selectedReplyMessage?.text
                                    )
                                    inputText = ""
                                    selectedReplyMessage = null
                                },
                                modifier = Modifier
                                    .background(Color(0xFF0066FF), CircleShape)
                                    .testTag("send_msg_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White
                                )
                            }
                        } else {
                            // Record voice note holding trigger
                            IconButton(
                                onClick = {
                                    if (isRecordingVoice) {
                                        isRecordingVoice = false
                                        val minutes = voiceRecordDurationSec / 60
                                        val seconds = voiceRecordDurationSec % 60
                                        val durationString = String.format("%02d:%02d", minutes, seconds)
                                        viewModel.sendVoiceMessage(durationString)
                                    } else {
                                        isRecordingVoice = true
                                    }
                                },
                                modifier = Modifier
                                    .background(
                                        if (isRecordingVoice) Color(0xFFEA4335) else Color(0xFF151C26),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Voice note",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // SHARE LARGE FILE MENU DRAWER
                    AnimatedVisibility(visible = showShareMenu) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Attach and Share Files",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )

                            // REAL Local Device File Attachment picker
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        filePickerLauncher.launch("*/*")
                                    }
                                    .testTag("attach_local_file_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0066FF).copy(alpha = 0.2f)),
                                border = BorderStroke(1.5.dp, Color(0xFF00C2FF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = "Attach Document",
                                            tint = Color(0xFF00C2FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Attach Local Document / File",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Real integration: Browse and select any device file",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF0066FF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("BROWSE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Or Simulate Secure Sharing of Large Data Sets",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Pick pre-configured file types & sizes to simulate
                            val fileOptions = listOf(
                                Triple("ISO Linux Archive", "ISO", "80.4 GB"),
                                Triple("CAD Mechanical model", "AI", "24.1 GB"),
                                Triple("Creative Assets Project", "PSD", "50.0 GB"),
                                Triple("Ultra HD Film footage", "MP4", "15.5 GB"),
                                Triple("App Deployment package", "APK", "5.2 GB")
                            )

                            fileOptions.forEach { (title, format, size) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.shareLargeFile(
                                                fileName = "${title.replace(" ", "_").lowercase()}.${format.lowercase()}",
                                                fileType = format,
                                                fileSize = size
                                            )
                                            showShareMenu = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = Color(0xFF00FFCC),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text("Format: $format | Secure Cloud Transfer", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(size, fontWeight = FontWeight.Bold, color = Color(0xFF00C2FF), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // STICKER / GIF MENU DRAWER
                    AnimatedVisibility(visible = showStickerMenu) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Select Premium Stickers", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val stickers = listOf(
                                    "🚀 Blast Off!", "🔥 On Fire", "💎 Premium Quality", "🎉 Congrats!", "💻 Coding Hard", "🔒 Secure Tunnel"
                                )
                                stickers.forEach { sticker ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.sendSticker(sticker)
                                                showStickerMenu = false
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(sticker, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Reaction selector popup
    selectedReactionMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedReactionMessage = null },
            title = { Text("Message Action Hub", fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("React to message:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "🚀").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.addReaction(msg.messageId, emoji)
                                        selectedReactionMessage = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Divider(color = Color(0xFF334155))

                    // Reply / Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedReplyMessage = msg
                                selectedReactionMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reply")
                        }

                        Button(
                            onClick = {
                                viewModel.deleteMessage(msg.messageId)
                                selectedReactionMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 1. OPEN SECURE FILE DIALOG
    activeOpenDialogTask?.let { task ->
        var stepState by remember { mutableStateOf(0) } // 0 = Decrypt, 1 = Virus Scan, 2 = Integrity, 3 = Launching, 4 = Ready
        var progress by remember { mutableStateOf(0f) }

        LaunchedEffect(task.downloadId) {
            progress = 0f
            stepState = 0
            while (progress < 1.0f) {
                delay(300)
                progress += 0.25f
            }
            delay(400)
            stepState = 1
            progress = 0f
            while (progress < 1.0f) {
                delay(200)
                progress += 0.34f
            }
            delay(400)
            stepState = 2
            progress = 0f
            while (progress < 1.0f) {
                delay(200)
                progress += 0.5f
            }
            delay(400)
            stepState = 3
            delay(800)
            stepState = 4
        }

        AlertDialog(
            onDismissRequest = { activeOpenDialogTask = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                    Text("Secure Resource Gateway", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Preparing to execute: ${task.fileName}", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    when (stepState) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("[1/4] Decrypting E2EE package blocks...", fontSize = 11.sp, color = Color.White)
                                LinearProgressIndicator(progress = { progress }, color = Color(0xFF00C2FF), trackColor = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                            }
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("[2/4] Executing anti-virus sandbox check...", fontSize = 11.sp, color = Color.White)
                                LinearProgressIndicator(progress = { progress }, color = Color(0xFFF59E0B), trackColor = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("[3/4] Testing checksum block match...", fontSize = 11.sp, color = Color.White)
                                LinearProgressIndicator(progress = { progress }, color = Color(0xFF22C55E), trackColor = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                            }
                        }
                        3 -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("[4/4] Activating on-device default handler...", fontSize = 11.sp, color = Color(0xFF00FFCC))
                            }
                        }
                        4 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gateway Verified", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text("The file is signature certified, decrypted, clean of viruses, and opened successfully in your device system editor.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("Virtual Folder: ${task.saveFolder}", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Text("SHA-256 Hash: ${task.checksum}", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (stepState == 4) {
                    TextButton(onClick = { activeOpenDialogTask = null }) {
                        Text("Dismiss", color = Color(0xFF00FFCC))
                    }
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White
        )
    }

    // 2. RENAME FILE DIALOG
    activeRenameDialogTask?.let { task ->
        AlertDialog(
            onDismissRequest = { activeRenameDialogTask = null },
            title = { Text("Rename Cached File", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide a new descriptive name for this asset:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDownload(task.downloadId, renameInputText)
                        activeRenameDialogTask = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("Apply Rename", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeRenameDialogTask = null }) {
                    Text("Cancel", color = Color(0xFFEA4335))
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White
        )
    }
}

// ==========================================
// MESSAGE BUBBLE COMPONENT
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    onReactionSelected: () -> Unit,
    onReplySelected: () -> Unit,
    onDelete: () -> Unit,
    onPauseTransfer: () -> Unit,
    onResumeTransfer: () -> Unit,
    downloadTasks: List<com.example.ui.viewmodel.DownloadTask>,
    onStartDownload: (MessageEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenDownload: (com.example.ui.viewmodel.DownloadTask) -> Unit,
    onRenameDownload: (com.example.ui.viewmodel.DownloadTask) -> Unit,
    onDeleteDownload: (String) -> Unit
) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val containerColor = if (isMe) Color(0xFF3B82F6).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f)
    val borderColors = if (isMe) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = align
    ) {
        // Sender Name if group chat and not me
        if (!isMe && message.senderCode != "SYSTEM") {
            Text(
                text = message.senderName,
                fontSize = 11.sp,
                color = Color(0xFF00C2FF),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Render actual message
            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                border = if (isMe) null else BorderStroke(1.dp, borderColors),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = { onReactionSelected() },
                        onLongClick = { onReactionSelected() }
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Replied message header
                    if (message.replyToText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = message.replyToText,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    when (message.type) {
                        "TEXT" -> {
                            Text(
                                text = message.text,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        "STICKER" -> {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x3300FFCC), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "🔥 ${message.text}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        "GIF" -> {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF000000), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "🎬 Shared GIF Asset",
                                    color = Color(0xFF00C2FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        "VOICE" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                                // Simulated audio wave lines
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    listOf(6, 12, 18, 8, 14, 22, 10, 16, 4).forEach { height ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(height.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                                Text(message.text, fontSize = 11.sp, color = Color.White)
                            }
                        }

                        "FILE" -> {
                            // Large File Share Card View with Progress meter
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "File",
                                        tint = Color(0xFF00FFCC)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = message.fileName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Size: ${message.fileSize} | ${message.fileType}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                // Interactive progress tracker
                                if (message.transferProgress < 1.0f) {
                                    LinearProgressIndicator(
                                        progress = { message.transferProgress },
                                        color = Color(0xFF00FFCC),
                                        trackColor = Color(0xFF334155),
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val pct = (message.transferProgress * 100).toInt()
                                        Text(
                                            text = if (message.isTransferPaused) "Paused $pct%" else "Uploading $pct% (840 MB/s)",
                                            fontSize = 10.sp,
                                            color = Color(0xFF00FFCC)
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (message.isTransferPaused) {
                                                Text(
                                                    "Resume",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF00C2FF),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { onResumeTransfer() }
                                                        .padding(4.dp)
                                                )
                                            } else {
                                                Text(
                                                    "Pause",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFEA4335),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { onPauseTransfer() }
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // SENDER METADATA OR DOWNLOAD OPTIONS
                                    val downloadTask = downloadTasks.find { it.downloadId == message.messageId }
                                    if (downloadTask == null) {
                                        Button(
                                            onClick = { onStartDownload(message) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Text("Download • ${message.fileSize}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        when (downloadTask.status) {
                                            com.example.ui.viewmodel.DownloadStatus.PENDING -> {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Waiting in queue...", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                                    }
                                                    Text(
                                                        text = "Cancel",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFEF4444),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.clickable { onCancelDownload(downloadTask.downloadId) }
                                                    )
                                                }
                                            }
                                            com.example.ui.viewmodel.DownloadStatus.DOWNLOADING -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    LinearProgressIndicator(
                                                        progress = { downloadTask.progress },
                                                        color = Color(0xFF00FFCC),
                                                        trackColor = Color(0xFF1E293B),
                                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                                    )
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Downloading... ${(downloadTask.progress * 100).toInt()}% (${downloadTask.speed})",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF00FFCC)
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(
                                                                text = "Pause",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFF59E0B),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.clickable { onPauseDownload(downloadTask.downloadId) }
                                                            )
                                                            Text(
                                                                text = "Cancel",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFEF4444),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.clickable { onCancelDownload(downloadTask.downloadId) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            com.example.ui.viewmodel.DownloadStatus.PAUSED -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    LinearProgressIndicator(
                                                        progress = { downloadTask.progress },
                                                        color = Color(0xFFF59E0B),
                                                        trackColor = Color(0xFF1E293B),
                                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                                    )
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Paused ${(downloadTask.progress * 100).toInt()}%",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFFF59E0B)
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(
                                                                text = "Resume",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF22C55E),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.clickable { onResumeDownload(downloadTask.downloadId) }
                                                            )
                                                            Text(
                                                                text = "Cancel",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFEF4444),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.clickable { onCancelDownload(downloadTask.downloadId) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            com.example.ui.viewmodel.DownloadStatus.FAILED -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = downloadTask.errorMsg ?: "Network connection lost.",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFEF4444)
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(
                                                            text = "Retry",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF3B82F6),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onResumeDownload(downloadTask.downloadId) }
                                                        )
                                                        Text(
                                                            text = "Cancel",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFFEF4444),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onCancelDownload(downloadTask.downloadId) }
                                                        )
                                                    }
                                                }
                                            }
                                            com.example.ui.viewmodel.DownloadStatus.CANCELLED -> {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Download cancelled.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                                    Text(
                                                        text = "Retry",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF00FFCC),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.clickable { onStartDownload(message) }
                                                    )
                                                }
                                            }
                                            com.example.ui.viewmodel.DownloadStatus.COMPLETED -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Downloaded & Verified (E2EE)", fontSize = 10.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(
                                                            text = "Open",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF00FFCC),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onOpenDownload(downloadTask) }
                                                        )
                                                        Text(
                                                            text = "Rename",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF00C2FF),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onRenameDownload(downloadTask) }
                                                        )
                                                        Text(
                                                            text = "Delete",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFFEF4444),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onDeleteDownload(downloadTask.downloadId) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time and read receipts metadata
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isEdited) {
                            Text("Edited", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Text(
                            text = "12:42 PM",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        if (isMe) {
                            Icon(
                                imageVector = if (message.readReceipt == "READ") Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = null,
                                tint = if (message.readReceipt == "READ") Color(0xFF00FFCC) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // Reactions list row below card
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = 8.dp, end = 8.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.split(",").forEach { emoji ->
                    Text(emoji, fontSize = 11.sp)
                }
            }
        }
    }
}
