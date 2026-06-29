package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LmConnectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LmConnectViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val profile = viewModel.currentUserProfile

    var isBackingUp by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PREFERENCES & SETTINGS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Configure your secure chat environment",
                            fontSize = 11.sp,
                            color = Color(0xFF00C2FF)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF020617),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(gradientBrush)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: UI Theme & Aesthetics
            Text("Aesthetics & Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151C26)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF00C2FF))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Theme", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0066FF)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguagePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00C2FF))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Language Selection", color = Color.White, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.selectedLanguage, color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // Section 2: Privacy & Security (E2EE controls)
            Text("Privacy & Security Protocols", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151C26)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Two-Step Verification", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.isTwoStepEnabled,
                            onCheckedChange = { viewModel.toggleTwoStep() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF22C55E))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Hide Online Status", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.hideOnlineStatus,
                            onCheckedChange = { viewModel.toggleHideOnline() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF22C55E))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Hide Last Seen", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.hideLastSeen,
                            onCheckedChange = { viewModel.toggleHideLastSeen() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF22C55E))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Hide Read Receipts", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.hideReadReceipts,
                            onCheckedChange = { viewModel.toggleHideReadReceipts() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF22C55E))
                        )
                    }
                }
            }

            // Section 3: Data & Storage
            Text("Data & Auto Download Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151C26)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF00FFCC))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Message Notifications", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.isNotificationEnabled,
                            onCheckedChange = { viewModel.toggleNotifications() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF0066FF))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Percent, contentDescription = null, tint = Color(0xFF00FFCC))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Data Saver Mode", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.isDataSaverEnabled,
                            onCheckedChange = { viewModel.toggleDataSaver() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF0066FF))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF00FFCC))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Auto Download Assets", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = profile.isAutoDownloadEnabled,
                            onCheckedChange = { viewModel.toggleAutoDownload() },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF0066FF))
                        )
                    }
                }
            }

            // Section 4: Backup & Restore
            Text("Backup & Restore Recovery", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Encrypted Cloud Backup", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Safeguard your messages and files with AES-256 standard private backups on our ultra-speed cloud vault.", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    if (isBackingUp) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { backupProgress },
                                color = Color(0xFF00FFCC),
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                            )
                            Text("Serializing database... ${(backupProgress * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF00FFCC))
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBackingUp = true
                                    backupProgress = 0f
                                    while (backupProgress < 1.0f) {
                                        delay(300)
                                        backupProgress += 0.1f
                                    }
                                    isBackingUp = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back Up Now")
                        }
                    }
                }
            }

            // Section 5: Account Session
            Text("Session Security", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF451010).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Secure Session Logout", fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5), fontSize = 14.sp)
                    Text("This will safely sign you out of your current device session and clear local access cache.", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out of LM Connect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select Interface Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "Spanish", "Arabic", "German", "Russian").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeLanguage(lang)
                                    showLanguagePicker = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang, color = Color.White)
                            if (profile.selectedLanguage == lang) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00C2FF))
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
