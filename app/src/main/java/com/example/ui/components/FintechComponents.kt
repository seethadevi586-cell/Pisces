package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    isPositive: Boolean? = null,
    icon: ImageVector? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                val displayIcon = icon ?: when (isPositive) {
                    true -> Icons.Outlined.TrendingUp
                    false -> Icons.Outlined.TrendingDown
                    null -> null
                }
                if (displayIcon != null) {
                    Icon(
                        imageVector = displayIcon,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val icon = when (status) {
        OrderStatus.FILLED -> Icons.Outlined.CheckCircle
        OrderStatus.PARTIALLY_FILLED -> Icons.Outlined.ChangeCircle
        OrderStatus.REJECTED, OrderStatus.FAILED -> Icons.Outlined.Cancel
        OrderStatus.SUBMITTING, OrderStatus.QUEUED -> Icons.Outlined.HourglassEmpty
        OrderStatus.CANCELLED -> Icons.Outlined.RemoveCircleOutline
        else -> Icons.Outlined.Info
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceSecondary)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PureWhite
        )
    }
}

@Composable
fun OptionTypeBadge(optionType: OptionType, side: OrderSide) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceSecondary)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${side.name} ${optionType.name}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PureWhite
        )
    }
}

@Composable
fun KillSwitchBanner(isActive: Boolean) {
    if (isActive) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfacePrimary)
                .border(1.dp, PureWhite)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = "Alert",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GLOBAL KILL SWITCH ENGAGED: COPY TRADING FROZEN",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
fun EmergencyStopBanner(isStopped: Boolean) {
    if (isStopped) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfacePrimary)
                .border(1.dp, PureWhite)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.PauseCircle,
                    contentDescription = "Emergency",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMERGENCY COPY FREEZE ENGAGED: NEW ORDERS BLOCKED",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
fun PiscesPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = PureWhite,
            contentColor = PureBlack,
            disabledContainerColor = SurfaceSecondary,
            disabledContentColor = TextDisabled
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = PureBlack,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Processing...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun PiscesSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfacePrimary,
            contentColor = PureWhite,
            disabledContainerColor = PureBlack,
            disabledContentColor = TextDisabled
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = PureWhite,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Processing...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun LoadingSkeletonCard(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 72.dp
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = PureWhite,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Syncing live data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onAction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ErrorStateView(
    title: String = "Unable to load data",
    message: String = "Network connection timed out. Tap retry to re-establish connection.",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            PiscesPrimaryButton(
                text = "Retry",
                onClick = onRetry,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
    }
}

@Composable
fun OfflineNoticeBanner(
    isOffline: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isOffline) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(SurfaceSecondary)
                .border(1.dp, SurfaceBorder)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WifiOff,
                        contentDescription = "Offline",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connection lost • Paper offline mode",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                }
                TextButton(onClick = onRetry) {
                    Text("Retry", color = PureWhite, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
