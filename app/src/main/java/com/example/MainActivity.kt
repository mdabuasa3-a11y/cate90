package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LmConnectViewModel

enum class AppScreen {
    AUTH,
    MAIN,
    CHAT,
    SETTINGS,
    ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LmConnectViewModel = viewModel()
            val profile = viewModel.currentUserProfile

            MyApplicationTheme(darkTheme = profile.isDarkTheme) {
                // Determine screen transition states
                var currentScreen by remember { mutableStateOf(AppScreen.AUTH) }

                // Synchronize login status state with screen routing
                LaunchedEffect(viewModel.isLoggedIn) {
                    currentScreen = if (viewModel.isLoggedIn) AppScreen.MAIN else AppScreen.AUTH
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val isWide = maxWidth >= 720.dp

                        // Main Screen Routing
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transitions"
                        ) { screen ->
                            when (screen) {
                                AppScreen.AUTH -> {
                                    AuthScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = AppScreen.MAIN },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                AppScreen.ADMIN -> {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = AppScreen.MAIN },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                AppScreen.MAIN, AppScreen.CHAT -> {
                                    if (isWide) {
                                        // Side-by-Side Dual Pane Layout
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            // Left Pane: Navigation + Main lists
                                            MainScreen(
                                                viewModel = viewModel,
                                                onNavigateToChat = {
                                                    // Managed inline on wide layouts, but sync route for consistency
                                                    currentScreen = AppScreen.CHAT
                                                },
                                                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                                                onNavigateToAdmin = { currentScreen = AppScreen.ADMIN },
                                                modifier = Modifier
                                                    .width(360.dp)
                                                    .fillMaxHeight()
                                            )

                                            // Divider
                                            VerticalDivider(color = Color.White.copy(alpha = 0.1f))

                                            // Right Pane: Active Chat Room or Placeholder
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF0B0E14))
                                            ) {
                                                if (viewModel.activeChatId != null) {
                                                    ChatRoomScreen(
                                                        viewModel = viewModel,
                                                        onBack = {
                                                            viewModel.selectChat(null)
                                                            currentScreen = AppScreen.MAIN
                                                        },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    // Empty detail placeholder
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(32.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Chat,
                                                            contentDescription = "Secured Chat Icon",
                                                            tint = Color(0xFF00FFCC).copy(alpha = 0.3f),
                                                            modifier = Modifier.size(72.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(24.dp))
                                                        Text(
                                                            text = "Select a Secured Tunnel",
                                                            color = Color.White,
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Text(
                                                            text = "Pick an active private connection or group channel from the left directory to begin double-encrypted communication, high-speed file transfers, and secure chats.",
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 13.sp,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.widthIn(max = 360.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Standard Single Pane Mobile Layout
                                        if (screen == AppScreen.MAIN) {
                                            MainScreen(
                                                viewModel = viewModel,
                                                onNavigateToChat = { currentScreen = AppScreen.CHAT },
                                                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                                                onNavigateToAdmin = { currentScreen = AppScreen.ADMIN },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            ChatRoomScreen(
                                                viewModel = viewModel,
                                                onBack = {
                                                    viewModel.selectChat(null)
                                                    currentScreen = AppScreen.MAIN
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // IN-APP NOTIFICATIONS PANEL OVERLAY
                        val notifications by viewModel.inAppNotifications.collectAsState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(notifications) { notif ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + slideInVertically { -it },
                                        exit = fadeOut() + slideOutVertically { -it }
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                when (notif.type) {
                                                    com.example.ui.viewmodel.NotificationType.SUCCESS -> Color(0xFF22C55E)
                                                    com.example.ui.viewmodel.NotificationType.WARNING -> Color(0xFFF59E0B)
                                                    com.example.ui.viewmodel.NotificationType.ERROR -> Color(0xFFEF4444)
                                                    else -> Color(0xFF00C2FF)
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when (notif.type) {
                                                        com.example.ui.viewmodel.NotificationType.SUCCESS -> Icons.Default.CheckCircle
                                                        com.example.ui.viewmodel.NotificationType.WARNING -> Icons.Default.Warning
                                                        com.example.ui.viewmodel.NotificationType.ERROR -> Icons.Default.Error
                                                        else -> Icons.Default.Info
                                                    },
                                                    contentDescription = null,
                                                    tint = when (notif.type) {
                                                        com.example.ui.viewmodel.NotificationType.SUCCESS -> Color(0xFF22C55E)
                                                        com.example.ui.viewmodel.NotificationType.WARNING -> Color(0xFFF59E0B)
                                                        com.example.ui.viewmodel.NotificationType.ERROR -> Color(0xFFEF4444)
                                                        else -> Color(0xFF00C2FF)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = notif.title,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = notif.message,
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CALL OVERLAY SYSTEM: Overlays everything else if call is active!
                        if (viewModel.activeCall != null) {
                            CallScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
