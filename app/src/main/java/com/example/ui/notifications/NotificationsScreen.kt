package com.example.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.UiState
import com.example.data.model.AppNotification
import com.example.ui.NearMeViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMePrimaryBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSuccess
import com.example.ui.theme.NearMeSurfaceElevated
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary
import com.example.ui.theme.NearMeWarning

@Composable
fun NotificationsScreen(
    viewModel: NearMeViewModel
) {
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val giftsState by viewModel.receivedGiftsState.collectAsStateWithLifecycle()

    var showPrefsDialog by remember { mutableStateOf(false) }
    var notifyConnections by remember { mutableStateOf(true) }
    var notifyMessages by remember { mutableStateOf(true) }
    var notifyEvents by remember { mutableStateOf(true) }
    var notifySafety by remember { mutableStateOf(true) }

    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val gifts = (giftsState as? UiState.Success)?.data.orEmpty()
    val unreadCount = notifications.count { !it.read }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.headlineSmall,
                        color = NearMeTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (unreadCount > 0) "$unreadCount unread updates" else "All caught up",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unreadCount > 0) NearMeElectricBlue else NearMeTextSecondary
                    )
                }
                OutlinedButton(
                    onClick = { showPrefsDialog = true },
                    modifier = Modifier.testTag("notification_prefs_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Preferences")
                }
            }
        }

        if (gifts.isNotEmpty()) {
            item {
                Text("Virtual Gifts Received (${gifts.size})", style = MaterialTheme.typography.titleMedium, color = NearMeElectricBlue)
            }
            items(gifts.take(3), key = { "gift_${it.id}" }) { gift ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = NearMeWarning)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${gift.senderName} sent a ${gift.giftType}", style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                            if (gift.message.isNotBlank()) {
                                Text("\"${gift.message}\"", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        when (notificationsState) {
            is UiState.Loading -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NearMeElectricBlue)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.NotificationsNone,
                        title = "Unable to load notifications",
                        description = (notificationsState as UiState.Error).message
                    )
                }
            }
            is UiState.Success -> {
                if (notifications.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.NotificationsNone,
                            title = "No notifications yet",
                            description = "Connection requests, mutual matches, meetup updates, and safety reviews appear here in real time."
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onMarkRead = {
                                if (!item.read) viewModel.markNotificationRead(item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPrefsDialog) {
        AlertDialog(
            onDismissRequest = { showPrefsDialog = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Notification Preferences", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrefToggleRow("Connection Requests & Matches", notifyConnections) { notifyConnections = it }
                    PrefToggleRow("Direct Messages & Chatrooms", notifyMessages) { notifyMessages = it }
                    PrefToggleRow("Nearby Verified Events", notifyEvents) { notifyEvents = it }
                    PrefToggleRow("Verification & Security Alerts", notifySafety) { notifySafety = it }
                }
            },
            confirmButton = {
                Button(onClick = { showPrefsDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showPrefsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun NotificationCard(
    item: AppNotification,
    onMarkRead: () -> Unit
) {
    val icon = when (item.eventType) {
        "connection_request", "connection_accepted" -> Icons.Filled.PersonAdd
        "verification_submitted", "verification_verified" -> Icons.Filled.VerifiedUser
        "security" -> Icons.Filled.Security
        else -> Icons.Filled.Notifications
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().testTag("notification_card_${item.id}"),
        onClick = onMarkRead
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                color = if (!item.read) NearMePrimaryBlue.copy(alpha = 0.25f) else NearMeSurfaceElevated,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!item.read) NearMeElectricBlue else NearMeTextSecondary,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = NearMeTextMain,
                        fontWeight = if (!item.read) FontWeight.Bold else FontWeight.Medium
                    )
                    if (item.read) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Read", tint = NearMeSuccess, modifier = Modifier.size(16.dp))
                    } else {
                        Text("NEW", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(item.body, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                if (!item.read) {
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to mark as read", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                }
            }
        }
    }
}

@Composable
private fun PrefToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
