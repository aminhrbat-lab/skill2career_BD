package com.example.ui.admin

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AiTaskState
import com.example.core.UiState
import com.example.data.model.PlatformSettings
import com.example.data.model.PublicProfile
import com.example.ui.NearMeViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeDanger
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSuccess
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary
import com.example.ui.theme.NearMeWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCenterScreen(
    viewModel: NearMeViewModel,
    adminRoleLabel: String,
    platformSettings: PlatformSettings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val usersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val reportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val verificationsState by viewModel.adminVerificationsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.adminEventsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.adminAuditLogsState.collectAsStateWithLifecycle()
    val chatroomsState by viewModel.chatroomsState.collectAsStateWithLifecycle()
    val aiRiskState by viewModel.aiRiskState.collectAsStateWithLifecycle()

    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()
    val verifications = (verificationsState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()
    val chatrooms = (chatroomsState as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }
    var userSearch by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Admin Bar
        Surface(color = NearMeSecondaryBg, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to App", tint = NearMeTextMain)
                }
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = NearMeElectricBlue)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("NearMe Admin Control Center", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                    Text("Role: $adminRoleLabel • Private DMs strictly isolated", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = NearMeSecondaryBg,
            contentColor = NearMeElectricBlue,
            edgePadding = 12.dp
        ) {
            listOf("Analytics", "Users", "Moderation", "Verifications", "Events", "Settings & Audit").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            // 0: Analytics
            0 -> {
                val activeCount = users.count { it.accountStatus == "active" }
                val onlineCount = users.count { it.onlineStatus && !it.hideOnlineStatus }
                val verifiedCount = users.count { it.verificationStatus == "verified" }
                val openReports = reports.count { it.status == "open" }
                val pendingVerifs = verifications.count { it.status == "pending" }
                val verifiedEvents = events.count { it.verified }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Platform Health & Live Metrics", style = MaterialTheme.typography.titleLarge, color = NearMeTextMain)
                    }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricBox("Total Members", users.size.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Active Profiles", activeCount.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Online Now", onlineCount.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Verified Users", verifiedCount.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Open Reports", openReports.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Pending Verifs", pendingVerifs.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Chatrooms", chatrooms.size.toString(), Modifier.fillMaxWidth(0.47f))
                            MetricBox("Verified Events", "$verifiedEvents/${events.size}", Modifier.fillMaxWidth(0.47f))
                        }
                    }
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Security & Privacy Guarantees", style = MaterialTheme.typography.titleMedium, color = NearMeElectricBlue)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "• Admins cannot read private user direct messages (enforced in Firestore rules).\n" +
                                    "• Gemini AI moderation provides advisory risk insights only; all bans/suspensions require human action.\n" +
                                    "• Every administrative action is recorded in an immutable AdminAuditLog.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NearMeTextSecondary
                            )
                        }
                    }
                }
            }

            // 1: Users
            1 -> {
                val filteredUsers = remember(users, userSearch) {
                    if (userSearch.isBlank()) users
                    else users.filter {
                        it.fullName.contains(userSearch, ignoreCase = true) ||
                            it.username.contains(userSearch, ignoreCase = true) ||
                            it.cityLabel.contains(userSearch, ignoreCase = true)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = userSearch,
                            onValueChange = { userSearch = it },
                            label = { Text("Search users by name, username, city") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(filteredUsers, key = { it.userId }) { u ->
                        AdminUserRow(
                            user = u,
                            onStatusChange = { newStatus ->
                                viewModel.adminUpdateUserStatus(u, newStatus, "Admin status change ($adminRoleLabel)")
                            },
                            onAwardBadge = { badge ->
                                viewModel.adminAwardBadge(u, badge)
                            }
                        )
                    }
                }
            }

            // 2: Moderation Reports
            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (aiRiskState !is AiTaskState.Idle) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NearMeAccentPurple)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gemini AI Advisory Risk Insight", style = MaterialTheme.typography.titleSmall, color = NearMeElectricBlue)
                                }
                                Spacer(Modifier.height(6.dp))
                                when (val st = aiRiskState) {
                                    is AiTaskState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NearMeElectricBlue)
                                    is AiTaskState.Success -> {
                                        Text("Signal: ${st.data.riskLevel}", style = MaterialTheme.typography.labelLarge, color = NearMeWarning)
                                        Text(st.data.summary, style = MaterialTheme.typography.bodySmall, color = NearMeTextMain)
                                        Text("Suggested human review: ${st.data.recommendedAction}", style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                                    }
                                    is AiTaskState.Error -> Text(st.message, color = NearMeDanger, style = MaterialTheme.typography.bodySmall)
                                    AiTaskState.Idle -> Unit
                                }
                            }
                        }
                    }

                    if (reports.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Flag,
                                title = "No moderation reports",
                                description = "User-submitted safety reports appear here for AI-assisted triage and human moderation."
                            )
                        }
                    } else {
                        items(reports, key = { it.id }) { report ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("${report.targetType.uppercase()} • Status: ${report.status}", style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                                Text("Target: ${report.targetId}", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(report.reason, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                                if (report.aiRiskSummary.isNotBlank()) {
                                    Text("Review Note: ${report.aiRiskSummary}", style = MaterialTheme.typography.bodySmall, color = NearMeWarning)
                                }
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { viewModel.analyzeReportRiskWithAi(report) }) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("AI Risk Scan")
                                    }
                                    Button(onClick = {
                                        val note = (aiRiskState as? AiTaskState.Success)?.data?.summary ?: "Warning issued by human moderator"
                                        viewModel.adminResolveReport(report, "warning_issued", note, "Warning issued by $adminRoleLabel")
                                    }) { Text("Issue Warning") }
                                    OutlinedButton(onClick = {
                                        val note = (aiRiskState as? AiTaskState.Success)?.data?.summary ?: "Restricted by human moderator"
                                        viewModel.adminResolveReport(report, "restricted", note, "Restricted by $adminRoleLabel")
                                    }) { Text("Restrict") }
                                    OutlinedButton(onClick = {
                                        viewModel.adminResolveReport(report, "dismissed", "Dismissed after review", "Dismissed by $adminRoleLabel")
                                    }) { Text("Dismiss") }
                                }
                            }
                        }
                    }
                }
            }

            // 3: Verification Queue
            3 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (verifications.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.VerifiedUser,
                                title = "No verification requests",
                                description = "Selfie, Phone OTP, and restricted National ID requests appear here for officer approval."
                            )
                        }
                    } else {
                        items(verifications, key = { it.id }) { req ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("${req.fullName} (${req.method.uppercase()})", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                                Text("Status: ${req.status.uppercase()}", style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                                Text(req.documentSummary, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.adminReviewVerification(req, true, "Approved by $adminRoleLabel") }) {
                                        Text("Approve")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.adminReviewVerification(req, false, "Rejected by $adminRoleLabel") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NearMeDanger)
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4: Events Map Verification
            4 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (events.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.EventAvailable,
                                title = "No events to review",
                                description = "Community events must be verified before appearing on the public Nearby Map."
                            )
                        }
                    } else {
                        items(events, key = { it.id }) { ev ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(ev.title, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                                Text("${ev.organizerName} • ${ev.cityLabel} • ${ev.eventDateText}", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                                Text(
                                    if (ev.verified) "Verified on Public Map" else "Unverified (Hidden from Public Map)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ev.verified) NearMeSuccess else NearMeWarning
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { viewModel.adminVerifyEvent(ev, !ev.verified) }) {
                                    Text(if (ev.verified) "Unverify Event" else "Verify for Map")
                                }
                            }
                        }
                    }
                }
            }

            // 5: Platform Settings, Backup Export & Audit Logs
            else -> {
                var regEnabled by remember(platformSettings) { mutableStateOf(platformSettings.registrationEnabled) }
                var gpsEnabled by remember(platformSettings) { mutableStateOf(platformSettings.gpsDiscoveryEnabled) }
                var roomsEnabled by remember(platformSettings) { mutableStateOf(platformSettings.chatroomsEnabled) }
                var maintMode by remember(platformSettings) { mutableStateOf(platformSettings.maintenanceMode) }
                var dailyLimitText by remember(platformSettings) { mutableStateOf(platformSettings.dailyConnectionLimit.toString()) }
                var announcement by remember(platformSettings) { mutableStateOf(platformSettings.announcement) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Platform Feature Controls", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Spacer(Modifier.height(8.dp))
                            AdminSwitch("New User Registration", regEnabled) { regEnabled = it }
                            AdminSwitch("GPS Nearby Discovery", gpsEnabled) { gpsEnabled = it }
                            AdminSwitch("Community Chatrooms", roomsEnabled) { roomsEnabled = it }
                            AdminSwitch("Maintenance Mode (Non-Admins Locked)", maintMode) { maintMode = it }
                            AdminSwitch("Future Premium Subscriptions (Disabled at Launch)", false) { }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dailyLimitText,
                                onValueChange = { dailyLimitText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Daily Connection Request Limit") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = announcement,
                                onValueChange = { announcement = it },
                                label = { Text("Global Announcement Banner") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.savePlatformSettings(
                                        platformSettings.copy(
                                            registrationEnabled = regEnabled,
                                            gpsDiscoveryEnabled = gpsEnabled,
                                            chatroomsEnabled = roomsEnabled,
                                            maintenanceMode = maintMode,
                                            dailyConnectionLimit = (dailyLimitText.toIntOrNull() ?: 25).coerceIn(1, 500),
                                            announcement = announcement
                                        ),
                                        reason = "Saved via Admin Control Center ($adminRoleLabel)"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_platform_settings_button")
                            ) {
                                Text("Save Platform Settings")
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Database Backup & Recovery Strategy", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Production backup strategy pairs automated Cloud Firestore scheduled snapshots with on-demand admin JSON exports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NearMeTextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val json = viewModel.exportAdminBackup(users, reports, auditLogs, platformSettings)
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "NearMe Admin Backup Export")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Export Admin JSON Backup"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export Manual JSON Backup")
                            }
                        }
                    }

                    item {
                        Text("Immutable Admin Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, color = NearMeElectricBlue)
                    }
                    items(auditLogs.take(20), key = { it.id }) { log ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(log.action, style = MaterialTheme.typography.labelLarge, color = NearMeElectricBlue)
                            Text("Target: ${log.targetId} • ${log.previousState} -> ${log.newState}", style = MaterialTheme.typography.bodySmall, color = NearMeTextMain)
                            Text("Reason: ${log.reason}", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = NearMeElectricBlue, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserRow(
    user: PublicProfile,
    onStatusChange: (String) -> Unit,
    onAwardBadge: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("${user.fullName} (@${user.username})", style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
        Text(
            "${user.cityLabel.ifBlank { "No city" }} • Status: ${user.accountStatus.uppercase()} • Verif: ${user.verificationStatus}",
            style = MaterialTheme.typography.bodySmall,
            color = if (user.accountStatus == "active") NearMeElectricBlue else NearMeDanger
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (user.accountStatus != "active") {
                OutlinedButton(onClick = { onStatusChange("active") }) { Text("Restore Active") }
            }
            if (user.accountStatus != "suspended") {
                OutlinedButton(onClick = { onStatusChange("suspended") }) { Text("Suspend") }
            }
            if (user.accountStatus != "banned") {
                OutlinedButton(
                    onClick = { onStatusChange("banned") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NearMeDanger)
                ) { Text("Ban") }
            }
            OutlinedButton(onClick = { onAwardBadge("Community Builder") }) { Text("+ Builder Badge") }
        }
    }
}

@Composable
private fun AdminSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = NearMeTextMain, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
