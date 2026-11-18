package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.LocationHelper
import com.example.data.model.PublicProfile
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeBorder
import com.example.ui.theme.NearMeDanger
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMePrimaryBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSuccess
import com.example.ui.theme.NearMeSurfaceElevated
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NearMeSecondaryBg.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, NearMeBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun UserAvatar(
    name: String,
    colorHex: String = "#2563EB",
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    isVerified: Boolean = false
) {
    val parsedColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            NearMePrimaryBlue
        }
    }
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "NM" }
    }

    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(parsedColor, NearMeAccentPurple)
                    )
                )
                .border(1.5.dp, NearMeElectricBlue.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = NearMeTextMain,
                fontWeight = FontWeight.Bold
            )
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(NearMeSuccess)
                    .border(2.dp, NearMeSecondaryBg, CircleShape)
            )
        }
        if (isVerified) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verified member",
                tint = NearMeElectricBlue,
                modifier = Modifier
                    .size(size * 0.34f)
                    .align(Alignment.TopEnd)
                    .background(NearMeSecondaryBg, CircleShape)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NearMeTextMain
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NearMeTextSecondary
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = NearMeElectricBlue)
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NearMeSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NearMeElectricBlue, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = NearMeTextSecondary
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = NearMePrimaryBlue)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

fun computeDistanceText(me: PublicProfile?, other: PublicProfile): String {
    if (!other.showDistance || !other.locationSharingEnabled || other.invisibleMode) {
        return other.cityLabel.ifBlank { "Distance private" }
    }
    if (me?.approxLat != null && me.approxLng != null && other.approxLat != null && other.approxLng != null) {
        val meters = LocationHelper.distanceMeters(me.approxLat, me.approxLng, other.approxLat, other.approxLng)
        return LocationHelper.formatApproximateDistance(meters)
    }
    return other.cityLabel.ifBlank { "Nearby" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailDialog(
    me: PublicProfile?,
    target: PublicProfile,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onAnalyzeCompatibility: () -> Unit,
    onSendGift: (String, String) -> Unit,
    onBlock: () -> Unit,
    onReport: (String) -> Unit
) {
    var introText by remember { mutableStateOf("Hey ${target.fullName.substringBefore(" ")}, great to meet someone nearby!") }
    var showReportBox by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var showGiftPicker by remember { mutableStateOf(false) }
    var selectedGift by remember { mutableStateOf("star") }
    var giftMessage by remember { mutableStateOf("Sending good vibes!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NearMeSecondaryBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = target.fullName,
                    colorHex = target.avatarColorHex,
                    size = 52.dp,
                    isOnline = target.onlineStatus && !target.hideOnlineStatus,
                    isVerified = target.verificationStatus == "verified"
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (target.showAge) "${target.fullName}, ${target.age}" else target.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        color = NearMeTextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${target.username} • ${target.gender}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearMeTextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NearMeElectricBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(computeDistanceText(me, target), style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                }
                if (target.bio.isNotBlank()) {
                    Text(target.bio, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                }
                if (target.showProfession && target.profession.isNotBlank()) {
                    Text(
                        "Work: ${target.profession}${if (target.company.isNotBlank()) " at ${target.company}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearMeTextSecondary
                    )
                }
                if (target.showEducation && target.education.isNotBlank()) {
                    Text(
                        "Education: ${target.education}${if (target.institution.isNotBlank()) " • ${target.institution}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearMeTextSecondary
                    )
                }
                if (target.interests.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        target.interests.take(6).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = NearMeSurfaceElevated
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NearMeElectricBlue
                                )
                            }
                        }
                    }
                }
                if (target.badges.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NearMeSuccess, modifier = Modifier.size(16.dp))
                        Text(target.badges.joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = NearMeSuccess)
                    }
                }

                OutlinedTextField(
                    value = introText,
                    onValueChange = { introText = it },
                    label = { Text("Intro message (with request)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAnalyzeCompatibility,
                        modifier = Modifier.weight(1f).testTag("ai_compat_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Match", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showGiftPicker = !showGiftPicker },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gift", maxLines = 1)
                    }
                }

                if (showGiftPicker) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("flower", "heart", "star", "spark").forEach { g ->
                                FilterChip(
                                    selected = selectedGift == g,
                                    onClick = { selectedGift = g },
                                    label = { Text(g.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = giftMessage,
                            onValueChange = { giftMessage = it },
                            label = { Text("Gift note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSendGift(selectedGift, giftMessage)
                                showGiftPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send Free Virtual Gift")
                        }
                    }
                }

                if (showReportBox) {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason for report") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (reportReason.isNotBlank()) {
                                onReport(reportReason)
                                showReportBox = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NearMeDanger)
                    ) {
                        Text("Submit Safety Report")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showReportBox = !showReportBox }) {
                        Icon(Icons.Filled.Flag, contentDescription = null, tint = NearMeDanger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Report", color = NearMeDanger)
                    }
                    TextButton(onClick = onBlock) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = NearMeTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Block", color = NearMeTextSecondary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(introText) },
                modifier = Modifier.testTag("send_connect_button")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NearMeTextSecondary)
            }
        }
    )
}
