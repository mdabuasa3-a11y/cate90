package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.LmConnectViewModel

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    VERIFY_EMAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: LmConnectViewModel,
    modifier: Modifier = Modifier
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Input States
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var keepMeSignedIn by remember { mutableStateOf(true) }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Reset error/success messages when switching modes
    LaunchedEffect(authMode) {
        viewModel.authError = null
        viewModel.authSuccessMessage = null
    }

    // Handle verification routing on successful register or unverified login
    LaunchedEffect(viewModel.isEmailVerified, viewModel.isLoggedIn) {
        if (!viewModel.isEmailVerified && viewModel.authSuccessMessage != null && viewModel.authSuccessMessage!!.contains("Verification")) {
            authMode = AuthMode.VERIFY_EMAIL
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B132B), // Cosmic Slate Deep Night Blue
            Color(0xFF010610)  // Pitch Black Space
        )
    )

    val neonBlueColor = Color(0xFF00C2FF)
    val accentBlueColor = Color(0xFF0066FF)

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // APP LOGO & BRAND SECTION
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.5.dp, neonBlueColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1782589505328),
                        contentDescription = "LM Connect Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.welcome_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.welcome_subtitle),
                    fontSize = 13.sp,
                    color = neonBlueColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // DYNAMIC AUTHENTICATION CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header for current Mode
                        Text(
                            text = when (authMode) {
                                AuthMode.LOGIN -> "Account Sign In"
                                AuthMode.REGISTER -> "Account Registration"
                                AuthMode.FORGOT_PASSWORD -> "Reset Password"
                                AuthMode.VERIFY_EMAIL -> "Email Verification"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ERROR/SUCCESS ALERTS
                        viewModel.authError?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF451010))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error icon",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = err,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        viewModel.authSuccessMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF103A20))
                                    .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success icon",
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = Color(0xFF86EFAC),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // SWITCH CONTENT BY AUTH MODE
                        when (authMode) {
                            AuthMode.LOGIN -> {
                                // EMAIL INPUT
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(stringResource(id = R.string.email_label)) },
                                    placeholder = { Text(stringResource(id = R.string.email_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = neonBlueColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // PASSWORD INPUT
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(stringResource(id = R.string.password_label)) },
                                    placeholder = { Text(stringResource(id = R.string.password_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = neonBlueColor) },
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // KEEP ME SIGNED IN & FORGOT PWD
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { keepMeSignedIn = !keepMeSignedIn }
                                    ) {
                                        Checkbox(
                                            checked = keepMeSignedIn,
                                            onCheckedChange = { keepMeSignedIn = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = accentBlueColor,
                                                uncheckedColor = Color.White.copy(alpha = 0.4f),
                                                checkmarkColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("keep_me_signed_in_checkbox")
                                        )
                                        Text(
                                            text = stringResource(id = R.string.keep_me_signed_in),
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = stringResource(id = R.string.forgot_password_link),
                                        color = neonBlueColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { authMode = AuthMode.FORGOT_PASSWORD }
                                            .testTag("forgot_password_button")
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // SIGN IN BUTTON
                                Button(
                                    onClick = { viewModel.firebaseSignIn(email.trim(), password, keepMeSignedIn) },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentBlueColor,
                                        contentColor = Color.White,
                                        disabledContainerColor = accentBlueColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(stringResource(id = R.string.sign_in_button), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // GOOGLE SIGN IN BUTTON
                                Button(
                                    onClick = { viewModel.firebaseGoogleSignIn(email.trim().ifEmpty { "leftleo291@gmail.com" }, "leftleo291") },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("google_signin_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1E293B)
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color(0xFFEA4335), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(stringResource(id = R.string.continue_with_google), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // TOGGLE TO REGISTER
                                Text(
                                    text = stringResource(id = R.string.dont_have_account),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable { authMode = AuthMode.REGISTER }
                                        .testTag("create_account_button")
                                )
                            }

                            AuthMode.REGISTER -> {
                                // FULL NAME INPUT
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text(stringResource(id = R.string.full_name_label)) },
                                    placeholder = { Text(stringResource(id = R.string.full_name_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = neonBlueColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("full_name_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // USERNAME INPUT
                                OutlinedTextField(
                                    value = usernameInput,
                                    onValueChange = { usernameInput = it },
                                    label = { Text(stringResource(id = R.string.username_label)) },
                                    placeholder = { Text(stringResource(id = R.string.username_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = neonBlueColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("username_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // EMAIL INPUT
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(stringResource(id = R.string.email_label)) },
                                    placeholder = { Text(stringResource(id = R.string.email_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = neonBlueColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // PASSWORD INPUT
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(stringResource(id = R.string.password_label)) },
                                    placeholder = { Text(stringResource(id = R.string.password_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = neonBlueColor) },
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // CONFIRM PASSWORD INPUT
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text(stringResource(id = R.string.confirm_password_label)) },
                                    placeholder = { Text(stringResource(id = R.string.confirm_password_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = neonBlueColor) },
                                    trailingIcon = {
                                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                            Icon(
                                                imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_password_input")
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // CREATE ACCOUNT BUTTON
                                Button(
                                    onClick = {
                                        if (password != confirmPassword) {
                                            viewModel.authError = "Passwords do not match."
                                        } else {
                                            viewModel.firebaseSignUp(email.trim(), password, fullName.trim(), usernameInput.trim())
                                        }
                                    },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentBlueColor,
                                        contentColor = Color.White,
                                        disabledContainerColor = accentBlueColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(stringResource(id = R.string.create_account_button), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // GOOGLE SIGN UP BUTTON
                                Button(
                                    onClick = { viewModel.firebaseGoogleSignIn(email.trim().ifEmpty { "leftleo291@gmail.com" }, fullName.trim().ifEmpty { "leftleo291" }) },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("google_signin_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1E293B)
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color(0xFFEA4335), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Google Sign Up", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // TOGGLE TO LOGIN
                                Text(
                                    text = stringResource(id = R.string.already_have_account),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable { authMode = AuthMode.LOGIN }
                                        .testTag("back_to_login_button")
                                )
                            }

                            AuthMode.FORGOT_PASSWORD -> {
                                Text(
                                    text = stringResource(id = R.string.reset_password_instruction),
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // EMAIL INPUT
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(stringResource(id = R.string.email_label)) },
                                    placeholder = { Text(stringResource(id = R.string.email_placeholder)) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = neonBlueColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = neonBlueColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = neonBlueColor,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input")
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // SEND RESET LINK BUTTON
                                Button(
                                    onClick = { viewModel.firebaseResetPassword(email.trim()) },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentBlueColor,
                                        contentColor = Color.White,
                                        disabledContainerColor = accentBlueColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(stringResource(id = R.string.send_reset_link), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // BACK TO LOGIN
                                Text(
                                    text = stringResource(id = R.string.back_to_login),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable { authMode = AuthMode.LOGIN }
                                        .testTag("back_to_login_button")
                                )
                            }

                            AuthMode.VERIFY_EMAIL -> {
                                Text(
                                    text = stringResource(id = R.string.verification_instruction),
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = email.ifEmpty { "your.email@gmail.com" },
                                        color = neonBlueColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // CHECK / VERIFY NOW BUTTON
                                Button(
                                    onClick = { viewModel.firebaseCheckVerification(email.trim()) },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("verify_email_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentBlueColor,
                                        contentColor = Color.White,
                                        disabledContainerColor = accentBlueColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(stringResource(id = R.string.verify_now_button), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // RESEND VERIFICATION EMAIL
                                OutlinedButton(
                                    onClick = { viewModel.firebaseResendVerificationEmail(email.trim()) },
                                    enabled = !viewModel.isAuthLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("resend_email_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text(stringResource(id = R.string.resend_verification), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // BACK TO LOGIN / LOGOUT
                                Text(
                                    text = stringResource(id = R.string.logout_and_back),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.logout()
                                            authMode = AuthMode.LOGIN
                                        }
                                        .testTag("back_to_login_button")
                                )
                            }
                        }
                    }
                }

                // TERMS & PRIVACY LINKS
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.terms_and_privacy),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.terms_link),
                            fontSize = 11.sp,
                            color = neonBlueColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { /* Terms action */ }
                        )
                        Text(
                            text = "|",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = stringResource(id = R.string.privacy_link),
                            fontSize = 11.sp,
                            color = neonBlueColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { /* Privacy action */ }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
