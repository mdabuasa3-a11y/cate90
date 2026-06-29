package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LmConnectViewModel
import com.example.data.database.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: LmConnectViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val friendsList by viewModel.friends.collectAsState()
    val blockedList by viewModel.blockedUsers.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0=Server, 1=Users, 2=Logs

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
                            text = "ADMIN COMMAND CENTER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "LM Connect Core System Control",
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
        ) {
            // Dashboard Selector Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF151C26), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Server Telemetry", "User Directory", "Audit Log").forEachIndexed { idx, label ->
                    Button(
                        onClick = { activeSubTab = idx },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSubTab == idx) Color(0xFF0066FF) else Color.Transparent,
                            contentColor = if (activeSubTab == idx) Color.White else Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (activeSubTab) {
                0 -> ServerTelemetryTab(viewModel)
                1 -> UserDirectoryTab(viewModel, friendsList, blockedList)
                2 -> AuditLogsTab()
            }
        }
    }
}

// ==========================================
// SUB-TAB 1: SERVER MONITORING & DIALS
// ==========================================
@Composable
fun ServerTelemetryTab(viewModel: LmConnectViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Indicators Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TelemetryGaugeCard(
                label = "CPU Load",
                value = "${viewModel.adminCpuUsage}%",
                color = if (viewModel.adminCpuUsage > 30) Color(0xFFEA4335) else Color(0xFF00FFCC),
                modifier = Modifier.weight(1f)
            )

            TelemetryGaugeCard(
                label = "RAM Capacity",
                value = "${String.format("%.1f", viewModel.adminRamUsage)} GB",
                color = Color(0xFF0066FF),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TelemetryGaugeCard(
                label = "Ingress Node",
                value = "${String.format("%.1f", viewModel.adminBandwidthIn)} MB/s",
                color = Color(0xFF00C2FF),
                modifier = Modifier.weight(1f)
            )

            TelemetryGaugeCard(
                label = "Egress Volume",
                value = "${String.format("%.1f", viewModel.adminBandwidthOut)} MB/s",
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        // Live Speed Graph (Interactive dynamic Canvas wave!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Network Throughput Over Time",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    drawLine(Color(0xFF1E293B), start = androidx.compose.ui.geometry.Offset(0f, height*0.25f), end = androidx.compose.ui.geometry.Offset(width, height*0.25f))
                    drawLine(Color(0xFF1E293B), start = androidx.compose.ui.geometry.Offset(0f, height*0.5f), end = androidx.compose.ui.geometry.Offset(width, height*0.5f))
                    drawLine(Color(0xFF1E293B), start = androidx.compose.ui.geometry.Offset(0f, height*0.75f), end = androidx.compose.ui.geometry.Offset(width, height*0.75f))

                    // Draw glowing graph line based on cpu fluctuation
                    val points = 15
                    val step = width / (points - 1)
                    val path = androidx.compose.ui.graphics.Path()

                    val random = java.util.Random(viewModel.adminCpuUsage.toLong())
                    path.moveTo(0f, height * 0.5f)
                    for (i in 1 until points) {
                        val py = height * 0.1f + random.nextFloat() * height * 0.8f
                        path.lineTo(i * step, py)
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF00C2FF),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Bandwidth Limit: 10 Gbps", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("System Uptime: ${viewModel.adminServerUptime}", fontSize = 11.sp, color = Color(0xFF00FFCC))
                }
            }
        }

        // Server stats metrics
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151C26)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Secure File Storage Stats", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Divider(color = Color(0xFF1E293B))

                listOf(
                    Triple("Active Encrypted Connections", "4,210 Nodes", Color(0xFF22C55E)),
                    Triple("Data Packets Verified", "154M packets/sec", Color(0xFF00C2FF)),
                    Triple("Peak Bandwidth Used", "852.4 MB/s Today", Color(0xFF00FFCC)),
                    Triple("Firewall Intrusions Blocked", "40 Attacks blocked", Color(0xFFEA4335))
                ).forEach { (label, value, tint) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tint)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryGaugeCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ==========================================
// SUB-TAB 2: USER DIRECTORY MANAGEMENT
// ==========================================
@Composable
fun UserDirectoryTab(
    viewModel: LmConnectViewModel,
    friends: List<UserEntity>,
    blocked: List<UserEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Connected Clients & Reports Monitor",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Unify list for display
            val allServerUsers = friends + blocked

            items(allServerUsers) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151C26)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                user.username.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (user.isBlocked) Color(0xFFEA4335) else Color(0xFF00FFCC)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Code: ${user.userCode}", fontSize = 12.sp, color = Color(0xFF94A3B8))

                            if (user.reportCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEA4335),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${user.reportCount} Reports Filed",
                                        fontSize = 11.sp,
                                        color = Color(0xFFEA4335),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Block/Unblock Button
                        Button(
                            onClick = { viewModel.blockUser(user.userCode, !user.isBlocked) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.isBlocked) Color(0xFF22C55E) else Color(0xFFEA4335).copy(alpha = 0.2f),
                                contentColor = if (user.isBlocked) Color.White else Color(0xFFEA4335)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (user.isBlocked) "Authorize" else "Suspend",
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

// ==========================================
// SUB-TAB 3: AUDIT LOGS
// ==========================================
@Composable
fun AuditLogsTab() {
    val auditLogs = listOf(
        "12:41:25 - [SERVER] Ingress tunnel established on port 443.",
        "12:35:12 - [SYSTEM] Backup of large shared files completed successfully.",
        "12:30:45 - [USER] Marcus Brody initiated secure QR peer-to-peer pairing.",
        "12:22:04 - [E2EE] Dynamic key rotation refreshed for Chat global channels.",
        "12:15:00 - [ADMIN] Authorization requested for file multiplier accelerator.",
        "12:00:52 - [SECURITY] Firewall packet inspector filtered 4 bad requests."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Central Security Audit Ledger", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151C26)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                auditLogs.forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "▶",
                            color = Color(0xFF00FFCC),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                        )
                        Text(
                            text = log,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
