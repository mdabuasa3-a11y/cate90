package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    viewModel: LmConnectViewModel,
    modifier: Modifier = Modifier
) {
    val call = viewModel.activeCall ?: return

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF020617)
        )
    )

    // Pulse animation for calling states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (call.isIncoming) {
            // INCOMING CALL REQUEST LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = "INCOMING ${if (call.isVoice) "HD VOICE" else "HD VIDEO"} CALL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C2FF),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Pulse avatar container
                    Box(
                        modifier = Modifier
                            .size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((120f * pulseScale).dp)
                                .background(Color(0xFF00C2FF).copy(alpha = 0.15f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = call.contactName.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 32.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = call.contactName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Peer: ${call.userCode} | Encrypted Channel",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Interactive Accept/Decline keys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Key
                    FloatingActionButton(
                        onClick = { viewModel.hangUpCall(completedNormally = false) },
                        containerColor = Color(0xFFEA4335),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("decline_call_btn")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline")
                    }

                    // Accept Key
                    FloatingActionButton(
                        onClick = { viewModel.acceptCall() },
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("accept_call_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept")
                    }
                }
            }
        } else {
            // ACTIVE ESTABLISHED STREAM LAYOUT
            if (call.isVoice) {
                // VOICE CALL VIEW WITH LIVE ANIMATED VISUAL WAVEFORMS
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Text(
                            text = "SECURE ENCRYPTED VOICE STREAM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Digital ticking timer
                        Text(
                            text = String.format("%02d:%02d", call.durationSec / 60, call.durationSec % 60),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((120f * pulseScale).dp)
                                    .background(Color(0xFF00FFCC).copy(alpha = 0.1f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(Color(0xFF151C26), CircleShape)
                                    .border(1.dp, Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = call.contactName.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC),
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = call.contactName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Interactive voice waves
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val bars = 30
                        val barWidth = width / (bars * 1.5f)

                        val random = java.util.Random(call.durationSec.toLong())
                        for (i in 0 until bars) {
                            val barHeight = (height * 0.2f + random.nextFloat() * height * 0.7f) * pulseScale
                            drawRoundRect(
                                color = Color(0xFF00FFCC).copy(alpha = 0.7f),
                                topLeft = androidx.compose.ui.geometry.Offset(i * barWidth * 1.5f, (height - barHeight) / 2),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }

                    // Action Controls Block
                    CallControlsBlock(
                        isMuted = call.isMuted,
                        isCameraOn = call.isCameraOn,
                        isVoice = true,
                        onMuteToggle = { viewModel.toggleMute() },
                        onCameraToggle = { },
                        onHangUp = { viewModel.hangUpCall() }
                    )
                }
            } else {
                // VIDEO CALL VIEW WITH BEAUTIFUL HIGH-TECH LAYERS
                Box(modifier = Modifier.fillMaxSize()) {
                    // Full bleed background simulating the remote participant's video stream
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF002244), Color(0xFF020617))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFF00FFCC).copy(alpha = 0.3f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Encrypted Camera Stream: ${call.contactName}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Picture-in-Picture window for local feed (if camera is on)
                    if (call.isCameraOn) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .size(width = 100.dp, height = 150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF00C2FF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00C2FF))
                                Text("Self", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Bottom controls overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Floating Badge indicating caller status
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xCC0B0E14)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF22C55E), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${call.contactName} | " + String.format("%02d:%02d", call.durationSec / 60, call.durationSec % 60),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Video Call buttons
                        CallControlsBlock(
                            isMuted = call.isMuted,
                            isCameraOn = call.isCameraOn,
                            isVoice = false,
                            onMuteToggle = { viewModel.toggleMute() },
                            onCameraToggle = { viewModel.toggleCamera() },
                            onHangUp = { viewModel.hangUpCall() }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CENTRAL CALL CONTROLS COMPONENT
// ==========================================
@Composable
fun CallControlsBlock(
    isMuted: Boolean,
    isCameraOn: Boolean,
    isVoice: Boolean,
    onMuteToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onHangUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151C26).copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        IconButton(
            onClick = onMuteToggle,
            modifier = Modifier
                .size(48.dp)
                .background(if (isMuted) Color(0xFFEA4335) else Color(0xFF1E293B), CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mute Toggle",
                tint = Color.White
            )
        }

        // Hang Up red button
        IconButton(
            onClick = onHangUp,
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFEA4335), CircleShape)
                .testTag("hangup_call_btn")
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Hang Up",
                tint = Color.White
            )
        }

        // Camera button (only meaningful for video calls)
        if (!isVoice) {
            IconButton(
                onClick = onCameraToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (!isCameraOn) Color(0xFFEA4335) else Color(0xFF1E293B), CircleShape)
            ) {
                Icon(
                    imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Camera Toggle",
                    tint = Color.White
                )
            }
        } else {
            // Speaker icon for audio call simulation
            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1E293B), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speaker",
                    tint = Color.White
                )
            }
        }
    }
}
