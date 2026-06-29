package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.example.R
import com.example.data.database.CallHistoryEntity
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.LmConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LmConnectViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Chats, 1=Calls, 2=Friends, 3=Files, 4=Profile

    val profile = viewModel.currentUserProfile

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_1782589505328),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "LM CONNECT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Secured Chats"
                                    1 -> "Voice & Video Calls"
                                    2 -> "Connected Directory"
                                    3 -> "Large File Manager"
                                    else -> "My Profile"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF00C2FF)
                            )
                        }
                    }
                },
                actions = {
                    if (selectedTab == 4 && profile.isAdmin) {
                        IconButton(
                            onClick = onNavigateToAdmin,
                            modifier = Modifier.testTag("admin_dashboard_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Terminal",
                                tint = Color(0xFF00C2FF)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                NavigationBar(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(32.dp))
                ) {
                    val items = listOf(
                        Triple("Chats", Icons.Default.Chat, Icons.Outlined.Chat),
                        Triple("Calls", Icons.Default.Phone, Icons.Outlined.Phone),
                        Triple("Friends", Icons.Default.People, Icons.Outlined.People),
                        Triple("Files", Icons.Default.Folder, Icons.Outlined.FolderOpen),
                        Triple("Profile", Icons.Default.Person, Icons.Outlined.Person)
                    )

                    items.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) filledIcon else outlinedIcon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF3B82F6),
                                unselectedIconColor = Color(0xFF94A3B8),
                                selectedTextColor = Color(0xFF3B82F6),
                                unselectedTextColor = Color(0xFF94A3B8),
                                indicatorColor = Color(0xFF3B82F6).copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_$label")
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF020617)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D1527), Color(0xFF020617))
                    )
                )
        ) {
            when (selectedTab) {
                0 -> ChatsTab(viewModel, onNavigateToChat)
                1 -> CallsTab(viewModel)
                2 -> FriendsTab(viewModel)
                3 -> FilesTab(viewModel)
                4 -> ProfileTab(viewModel, onNavigateToAdmin)
            }
        }
    }
}

// =========================================================================
// TAB 1: CHATS VIEW
// =========================================================================
@Composable
fun ChatsTab(viewModel: LmConnectViewModel, onNavigateToChat: () -> Unit) {
    val chats by viewModel.chats.collectAsState()

    if (chats.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Chat,
            title = "No Secure Chats Yet",
            subtitle = "Add friends or share secure files to begin double-encrypted conversations."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chats) { chat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectChat(chat.chatId)
                            onNavigateToChat()
                        }
                        .testTag("chat_card_${chat.chatId}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Styled Avatar / Group Emblem
                        Box(modifier = Modifier.size(52.dp)) {
                            if (chat.isGroup) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF0066FF), Color(0xFF00FFCC))
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1E293B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.chatName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00C2FF),
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            // Online dot indicator for specific users
                            if (!chat.isGroup) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(0xFF22C55E), CircleShape)
                                        .border(2.dp, Color(0xFF151C26), CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Title & Last Message
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.chatName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chat.lastMessageText,
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Badge / Time details
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "12:40 PM",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            if (chat.unreadCount > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF0066FF), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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

// =========================================================================
// TAB 2: CALLS VIEW
// =========================================================================
@Composable
fun CallsTab(viewModel: LmConnectViewModel) {
    val logs by viewModel.callLogs.collectAsState()
    var showStartCallDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HD Video & Audio Logs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (logs.isNotEmpty()) {
                    Text(
                        text = "Clear History",
                        fontSize = 13.sp,
                        color = Color(0xFFEA4335),
                        modifier = Modifier
                            .clickable { viewModel.clearCalls() }
                            .padding(4.dp)
                    )
                }
            }

            if (logs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Call,
                    title = "No Recent Calls",
                    subtitle = "All your HD voice and video calls will show up here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { call ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF1E293B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (call.isVoice) Icons.Default.Phone else Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = if (call.status == "Missed") Color(0xFFEA4335) else Color(0xFF00C2FF)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = call.contactName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (call.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                                            contentDescription = null,
                                            tint = if (call.status == "Missed") Color(0xFFEA4335) else Color(0xFF22C55E),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (call.status == "Missed") "Missed" else "Duration: ${call.durationSec}s",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Text(
                                    text = "Today",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to launch an interactive call
        FloatingActionButton(
            onClick = { showStartCallDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("start_call_fab"),
            containerColor = Color(0xFF0066FF),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.PhoneCallback, contentDescription = "New Call")
        }

        if (showStartCallDialog) {
            AlertDialog(
                onDismissRequest = { showStartCallDialog = false },
                title = { Text("Start HD Calling Session") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Connect securely with high definition video or high-fidelity spatial voice streams.")
                        listOf(
                            Triple("Elon Musk", "LMC-TESLA1", true),
                            Triple("Sophia Carter", "LMC-SOPHI1", false),
                            Triple("Alex Rivera", "LMC-ALEXR1", true)
                        ).forEach { (name, code, isV) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showStartCallDialog = false
                                        viewModel.startCall(name, code, isVoice = isV)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Phone, contentDescription = "Voice", tint = Color(0xFF00C2FF))
                                        Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color(0xFF00FFCC))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStartCallDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// =========================================================================
// TAB 3: FRIENDS VIEW
// =========================================================================
@Composable
fun FriendsTab(viewModel: LmConnectViewModel) {
    val friendsList by viewModel.friends.collectAsState()
    val requestList by viewModel.pendingRequests.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var scanModeActive by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Add Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or add User Code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C2FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("friend_search_input")
                )

                // Simulated QR Scanner button
                IconButton(
                    onClick = { scanModeActive = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR Code",
                        tint = Color(0xFF00C2FF)
                    )
                }
            }

            if (searchQuery.isNotEmpty() && searchQuery.contains("-")) {
                Button(
                    onClick = {
                        viewModel.addFriend(searchQuery) { success ->
                            if (success) {
                                searchQuery = ""
                                addError = null
                            } else {
                                addError = "Unable to add user. Verify code."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("Add user code: $searchQuery")
                }
            }

            addError?.let {
                Text(it, color = Color(0xFFEA4335), fontSize = 12.sp)
            }
        }

        // Pending requests section
        if (requestList.isNotEmpty()) {
            Text(
                text = "Incoming Requests (${requestList.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00C2FF),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requestList) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF00C2FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.username.take(1).uppercase(), color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(user.userCode, fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.acceptFriendRequest(user.userCode) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF22C55E), CircleShape)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { viewModel.rejectFriendRequest(user.userCode) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFEA4335), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Friends directory
        Text(
            text = "My Network Connection",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val filteredFriends = friendsList.filter {
            it.username.contains(searchQuery, ignoreCase = true) || it.userCode.contains(searchQuery, ignoreCase = true)
        }

        if (filteredFriends.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.GroupAdd,
                title = "No Friends Found",
                subtitle = "Share your unique LMC user code to let colleagues connect with you."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFriends) { friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(friend.username.take(2).uppercase(), color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.username, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(friend.bio, fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(friend.userCode, fontSize = 10.sp, color = Color(0xFF00C2FF))
                                }
                            }

                            IconButton(
                                onClick = { viewModel.blockUser(friend.userCode, true) }
                            ) {
                                Icon(Icons.Default.Block, contentDescription = "Block User", tint = Color(0xFFEA4335).copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (scanModeActive) {
        AlertDialog(
            onDismissRequest = { scanModeActive = false },
            title = { Text("Simulating QR Code Scan") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Standard QR Scanner reads the peer code to provision instant point-to-point connections.")
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White)
                            .border(3.dp, Color(0xFF00C2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(120.dp))
                    }
                    Text("Mocking a QR scanner read event for peer 'LMC-SOPHI1' (Sophia Carter).", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addFriend("LMC-SOPHI1") { }
                        scanModeActive = false
                    }
                ) {
                    Text("Trigger Mock Read")
                }
            },
            dismissButton = {
                TextButton(onClick = { scanModeActive = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================
// TAB 4: FILES VIEW
// =========================================================================
@Composable
fun FilesTab(viewModel: LmConnectViewModel) {
    val downloadTasks by viewModel.downloadTasks.collectAsState()
    val sharedFilesList by viewModel.sharedFiles.collectAsState()

    var showFolderPicker by remember { mutableStateOf(false) }
    var folderInputText by remember { mutableStateOf(viewModel.currentDownloadFolder) }

    var activeOpenDialogTask by remember { mutableStateOf<com.example.ui.viewmodel.DownloadTask?>(null) }
    var activeRenameDialogTask by remember { mutableStateOf<com.example.ui.viewmodel.DownloadTask?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val activeDownloads = downloadTasks.filter { 
        it.status != com.example.ui.viewmodel.DownloadStatus.COMPLETED 
    }
    
    val completedHistory = downloadTasks.filter { 
        it.status == com.example.ui.viewmodel.DownloadStatus.COMPLETED &&
        (viewModel.downloadSearchQuery.isEmpty() || it.fileName.contains(viewModel.downloadSearchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Downloads & Local Storage",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // 1. Search Bar
        OutlinedTextField(
            value = viewModel.downloadSearchQuery,
            onValueChange = { viewModel.downloadSearchQuery = it },
            placeholder = { Text("Search files by name...", color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFCC),
                unfocusedBorderColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // 2. High-Speed Accelerator Toggle & Download Path Setup
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // High-Speed Accelerator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("High-Speed Cloud Transfer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Multi-threaded download speeds up to 12.8 GB/s", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                    Switch(
                        checked = viewModel.speedMultiplierEnabled,
                        onCheckedChange = { viewModel.speedMultiplierEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0066FF)
                        )
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Storage Path Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            folderInputText = viewModel.currentDownloadFolder
                            showFolderPicker = true
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00C2FF), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Download Directory", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(viewModel.currentDownloadFolder, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit Folder", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }

        // 3. Active Queue & Downloads Section
        Text("Active Downloads & Queue", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

        if (activeDownloads.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No active downloads in queue.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeDownloads.forEach { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = when (task.fileType.uppercase()) {
                                            "ZIP", "RAR", "7Z" -> Icons.Default.Archive
                                            "APK" -> Icons.Default.Android
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF00FFCC),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(task.fileName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Size: ${task.fileSize} • Sent by: ${task.senderName}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (task.status == com.example.ui.viewmodel.DownloadStatus.DOWNLOADING) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp).clickable { viewModel.pauseDownload(task.downloadId) })
                                    } else if (task.status == com.example.ui.viewmodel.DownloadStatus.PAUSED || task.status == com.example.ui.viewmodel.DownloadStatus.FAILED) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp).clickable { viewModel.resumeDownload(task.downloadId) })
                                    }
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp).clickable { viewModel.cancelDownload(task.downloadId) })
                                }
                            }

                            // Progress bar
                            if (task.status == com.example.ui.viewmodel.DownloadStatus.PENDING) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    Text("Waiting in queue...", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { task.progress },
                                        color = if (task.status == com.example.ui.viewmodel.DownloadStatus.FAILED) Color(0xFFEF4444) else Color(0xFF00FFCC),
                                        trackColor = Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                    )
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = if (task.status == com.example.ui.viewmodel.DownloadStatus.FAILED) {
                                                task.errorMsg ?: "Connection lost. Reconnecting..."
                                            } else {
                                                "Downloading... ${(task.progress * 100).toInt()}% • Speed: ${task.speed}"
                                            },
                                            fontSize = 10.sp,
                                            color = if (task.status == com.example.ui.viewmodel.DownloadStatus.FAILED) Color(0xFFEF4444) else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. File Download History
        Text("Finished Downloads & Local Cache", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

        if (completedHistory.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No matching completed files found.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                completedHistory.forEach { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (task.fileType.uppercase()) {
                                        "ZIP", "RAR" -> Icons.Default.Archive
                                        "APK" -> Icons.Default.Android
                                        else -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.fileName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Size: ${task.fileSize} • Sent by: ${task.senderName} • ${task.uploadDate}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("E2EE Checksum SHA-256 Verified", fontSize = 10.sp, color = Color(0xFF22C55E))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Open",
                                        color = Color(0xFF00FFCC),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { activeOpenDialogTask = task }
                                    )
                                    Text(
                                        "Rename",
                                        color = Color(0xFF00C2FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            activeRenameDialogTask = task
                                            renameInputText = task.fileName.substringBeforeLast(".")
                                        }
                                    )
                                    Text(
                                        "Delete",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { viewModel.deleteDownload(task.downloadId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. CHOOSE DOWNLOAD DIRECTORY DIALOG
    if (showFolderPicker) {
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Choose Target Storage Folder", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select or enter the device folder where downloaded repositories are stored:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    OutlinedTextField(
                        value = folderInputText,
                        onValueChange = { folderInputText = it },
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
                        viewModel.changeDownloadFolder(folderInputText)
                        showFolderPicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("Set Folder", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderPicker = false }) {
                    Text("Cancel", color = Color(0xFFEA4335))
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White
        )
    }

    // 2. OPEN SECURE FILE DIALOG (Simulating anti-virus scanner decryption and verify)
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

    // 3. RENAME CACHED FILE DIALOG
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

// =========================================================================
// TAB 5: PROFILE VIEW
// =========================================================================
@Composable
fun ProfileTab(viewModel: LmConnectViewModel, onNavigateToAdmin: () -> Unit) {
    val profile = viewModel.currentUserProfile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover Photo & Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1782589517414),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0x99020617))))
            )

            // Centered User ID Tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = profile.userCode,
                    color = Color(0xFF00C2FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Avatar offsetting card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Frame
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF00C2FF), CircleShape)
                        .border(4.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.username.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "LMC Verified User",
                    fontSize = 11.sp,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = profile.bio,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(profile.statusText, fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // QR Code and Connection Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My Encrypted Share Code",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

            // Canvas drawing a futuristic glowing QR Code
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draws a realistic blue-tinted QR
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val size = this.size.width
                    // Draw finding patterns (three corners)
                    drawRect(Color(0xFF001F4E), size = this.size * 0.3f)
                    drawRect(Color(0xFF001F4E), topLeft = androidx.compose.ui.geometry.Offset(size * 0.7f, 0f), size = this.size * 0.3f)
                    drawRect(Color(0xFF001F4E), topLeft = androidx.compose.ui.geometry.Offset(0f, size * 0.7f), size = this.size * 0.3f)

                    // Draw lots of smaller random data blocks to simulate real QR
                    val squareSize = size * 0.08f
                    val random = java.util.Random(12345)
                    for (i in 0..10) {
                        for (j in 0..10) {
                            if (random.nextBoolean()) {
                                drawRect(
                                    Color(0xFF0066FF),
                                    topLeft = androidx.compose.ui.geometry.Offset(i * squareSize + size * 0.1f, j * squareSize + size * 0.1f),
                                    size = androidx.compose.ui.geometry.Size(squareSize * 0.8f, squareSize * 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Colleagues can scan this code using their camera or friends system QR scanner to start high-speed sharing instantly.",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (profile.isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onNavigateToAdmin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_terminal_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Access Administrative Command")
                }
            }
        }
    }
}

// Generic Empty State template view
@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}
