package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PiscesPrimaryButton
import com.example.ui.components.PiscesSecondaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun SplashScreen(viewModel: TradingViewModel) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .clickable { viewModel.dismissSplash() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SurfacePrimary, RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TrendingUp,
                    contentDescription = "PISCES",
                    tint = PureWhite,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PISCES",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                ),
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "INDIAN OPTIONS TRADE MIRRORING",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "INITIALIZING PAPER SANDBOX...",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = PureWhite,
                modifier = Modifier.alpha(pulseAlpha)
            )
        }
    }
}

@Composable
fun LoginScreen(viewModel: TradingViewModel) {
    val loginInput by viewModel.loginInput.collectAsState()
    val otpInput by viewModel.otpInput.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PISCES",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "INSTITUTIONAL TRADE MIRRORING",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (!otpSent) "Sign In / Onboard" else "Verify 6-Digit OTP",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )

                    Text(
                        text = if (!otpSent) {
                            "Enter your registered mobile number or email for passwordless authentication."
                        } else {
                            "A secure code was sent to $loginInput. Enter to proceed."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    if (!otpSent) {
                        OutlinedTextField(
                            value = loginInput,
                            onValueChange = { viewModel.setLoginInput(it) },
                            label = { Text("Mobile or Email") },
                            leadingIcon = {
                                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = PureWhite)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("login_input_field")
                        )

                        PiscesPrimaryButton(
                            text = if (isBusy) "Sending Code..." else "Request Login OTP",
                            onClick = { viewModel.sendOtp() },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("send_otp_button")
                        )
                    } else {
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { viewModel.setOtpInput(it) },
                            label = { Text("Enter OTP (e.g. 123456)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = PureWhite)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("otp_input_field")
                        )

                        PiscesPrimaryButton(
                            text = if (isBusy) "Verifying..." else "Verify & Sign In",
                            onClick = { viewModel.verifyOtp() },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("verify_otp_button")
                        )

                        PiscesSecondaryButton(
                            text = "Change Mobile / Email",
                            onClick = { viewModel.setLoginInput("") }
                        )
                    }

                    if (loginError != null) {
                        Text(
                            text = "Error: $loginError",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite
                        )
                    }

                    HorizontalDivider(color = SurfaceBorderSubtle)

                    // Quick sandbox access button
                    PiscesSecondaryButton(
                        text = "Instant Sandbox Bypass (Demo Trader)",
                        onClick = { viewModel.verifyOtp() },
                        icon = Icons.Outlined.Shield,
                        modifier = Modifier.testTag("demo_login_button")
                    )
                }
            }
        }
    }
}
