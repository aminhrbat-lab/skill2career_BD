package com.example.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AiTaskState
import com.example.core.LocationHelper
import com.example.core.UiState
import com.example.data.model.CompatibilityResult
import com.example.data.model.PlatformSettings
import com.example.data.model.PublicProfile
import com.example.ui.NearMeViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.ProfileDetailDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.computeDistanceText
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeDanger
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMePrimaryBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSuccess
import com.example.ui.theme.NearMeSurfaceElevated
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary

private enum class DiscoverMode { SWIPE, GRID, LIST }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    platformSettings: PlatformSettings
) {
    val profilesState by viewModel.discoverableProfilesState.collectAsStateWithLifecycle()
    val connectionsState by viewModel.connectionsState.collectAsStateWithLifecycle()
    val aiCompatState by viewModel.aiCompatibilityState.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(DiscoverMode.SWIPE) }
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var genderFilter by remember { mutableStateOf("All") }
    var maxDistanceKm by remember { mutableStateOf(25.0) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var onlineOnly by remember { mutableStateOf(false) }
    var swipeIndex by remember { mutableIntStateOf(0) }
    var selectedProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var showCompatModalFor by remember { mutableStateOf<PublicProfile?>(null) }

    val connections = (connectionsState as? UiState.Success)?.data.orEmpty()
    val blockedIds = remember(connections) {
        connections.filter { it.status == "blocked" }
            .flatMap { it.participantUids }
            .toSet()
    }
    val pendingSentCount = remember(connections, myProfile.userId) {
        connections.count { it.senderId == myProfile.userId && it.status == "pending" }
    }

    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()
    val filteredProfiles = remember(
        allProfiles, myProfile, blockedIds, searchQuery, genderFilter, maxDistanceKm, verifiedOnly, onlineOnly
    ) {
        allProfiles.filter { p ->
            if (p.userId == myProfile.userId || p.invisibleMode || p.userId in blockedIds) return@filter false
            if (genderFilter != "All" && !p.gender.equals(genderFilter, ignoreCase = true)) return@filter false
            if (verifiedOnly && p.verificationStatus != "verified") return@filter false
            if (onlineOnly && (!p.onlineStatus || p.hideOnlineStatus)) return@filter false
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val match = p.fullName.lowercase().contains(q) ||
                    p.profession.lowercase().contains(q) ||
                    p.education.lowercase().contains(q) ||
                    p.cityLabel.lowercase().contains(q) ||
                    p.interests.any { it.lowercase().contains(q) }
                if (!match) return@filter false
            }
            if (myProfile.approxLat != null && myProfile.approxLng != null &&
                p.approxLat != null && p.approxLng != null && p.locationSharingEnabled
            ) {
                val distKm = LocationHelper.distanceMeters(myProfile.approxLat, myProfile.approxLng, p.approxLat, p.approxLng) / 1000.0
                if (distKm > maxDistanceKm) return@filter false
            }
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Discover People", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                Text(
                    "Daily limit: $pendingSentCount / ${platformSettings.dailyConnectionLimit} requests",
                    style = MaterialTheme.typography.labelSmall,
                    color = NearMeElectricBlue
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mode = DiscoverMode.SWIPE }) {
                    Icon(Icons.Filled.Swipe, contentDescription = "Swipe view", tint = if (mode == DiscoverMode.SWIPE) NearMeElectricBlue else NearMeTextSecondary)
                }
                IconButton(onClick = { mode = DiscoverMode.GRID }) {
                    Icon(Icons.Filled.GridView, contentDescription = "Grid view", tint = if (mode == DiscoverMode.GRID) NearMeElectricBlue else NearMeTextSecondary)
                }
                IconButton(onClick = { mode = DiscoverMode.LIST }) {
                    Icon(Icons.Filled.ViewList, contentDescription = "List view", tint = if (mode == DiscoverMode.LIST) NearMeElectricBlue else NearMeTextSecondary)
                }
                IconButton(onClick = { showFilters = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filters", tint = NearMeElectricBlue)
                }
            }
        }

        // Quick Distance Chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0.1 to "100m", 0.5 to "500m", 1.0 to "1km", 5.0 to "5km", 10.0 to "10km", 25.0 to "25km").forEach { (km, label) ->
                FilterChip(
                    selected = maxDistanceKm == km,
                    onClick = { maxDistanceKm = km },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                )
            }
        }

        if (filteredProfiles.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Filled.People,
                title = "No matching profiles right now",
                description = "Try widening your distance radius or clearing filters. Pending connection requests: ${connections.count { it.receiverId == myProfile.userId && it.status == "pending" }}.",
                actionText = "Reset Filters",
                onAction = {
                    genderFilter = "All"
                    maxDistanceKm = 25.0
                    verifiedOnly = false
                    onlineOnly = false
                    searchQuery = ""
                }
            )
            // Show incoming pending connection requests below
            val incomingPending = connections.filter { it.receiverId == myProfile.userId && it.status == "pending" }
            if (incomingPending.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Incoming Requests (${incomingPending.size})", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                    }
                    items(incomingPending, key = { it.id }) { conn ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(conn.senderName, style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                            if (conn.introMessage.isNotBlank()) {
                                Text("\"${conn.introMessage}\"", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.respondToConnection(conn, "accepted") }) { Text("Accept") }
                                OutlinedButton(onClick = { viewModel.respondToConnection(conn, "declined") }) { Text("Decline") }
                            }
                        }
                    }
                }
            }
        } else {
            when (mode) {
                DiscoverMode.SWIPE -> {
                    val safeIndex = swipeIndex.coerceIn(0, filteredProfiles.lastIndex)
                    val person = filteredProfiles[safeIndex]
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        onClick = {
                            viewModel.logProfileView(person.fullName)
                            selectedProfile = person
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(NearMePrimaryBlue.copy(alpha = 0.35f), NearMeAccentPurple.copy(alpha = 0.35f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserAvatar(
                                        name = person.fullName,
                                        colorHex = person.avatarColorHex,
                                        size = 86.dp,
                                        isOnline = person.onlineStatus && !person.hideOnlineStatus,
                                        isVerified = person.verificationStatus == "verified"
                                    )
                                }
                                Text(
                                    text = if (person.showAge) "${person.fullName}, ${person.age}" else person.fullName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = NearMeTextMain
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NearMeElectricBlue, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(computeDistanceText(myProfile, person), style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                                }
                                if (person.showProfession && person.profession.isNotBlank()) {
                                    Text("${person.profession}${if (person.company.isNotBlank()) " @ ${person.company}" else ""}", style = MaterialTheme.typography.bodyMedium, color = NearMeTextSecondary)
                                }
                                if (person.bio.isNotBlank()) {
                                    Text(person.bio, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    person.interests.take(5).forEach { tag ->
                                        Surface(shape = RoundedCornerShape(50), color = NearMeSurfaceElevated) {
                                            Text(tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { swipeIndex = (safeIndex + 1) % filteredProfiles.size },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pass")
                                }
                                OutlinedButton(
                                    onClick = {
                                        showCompatModalFor = person
                                        viewModel.computeAiCompatibility(myProfile, person)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("AI Score")
                                }
                                Button(
                                    onClick = {
                                        if (pendingSentCount >= platformSettings.dailyConnectionLimit) {
                                            selectedProfile = person
                                        } else {
                                            viewModel.sendConnectionRequest(
                                                myProfile,
                                                person,
                                                "Hi ${person.fullName.substringBefore(" ")}, I'd love to connect on NearMe!",
                                                null
                                            )
                                            swipeIndex = (safeIndex + 1) % filteredProfiles.size
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NearMePrimaryBlue)
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Connect")
                                }
                            }
                        }
                    }
                }

                DiscoverMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredProfiles, key = { it.userId }) { person ->
                            GlassCard(
                                onClick = {
                                    viewModel.logProfileView(person.fullName)
                                    selectedProfile = person
                                }
                            ) {
                                UserAvatar(
                                    name = person.fullName,
                                    colorHex = person.avatarColorHex,
                                    size = 52.dp,
                                    isOnline = person.onlineStatus && !person.hideOnlineStatus,
                                    isVerified = person.verificationStatus == "verified"
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (person.showAge) "${person.fullName}, ${person.age}" else person.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NearMeTextMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(computeDistanceText(myProfile, person), style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                                if (person.interests.isNotEmpty()) {
                                    Text(person.interests.take(2).joinToString(", "), style = MaterialTheme.typography.labelSmall, color = NearMeTextSecondary, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                DiscoverMode.LIST -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredProfiles, key = { it.userId }) { person ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    viewModel.logProfileView(person.fullName)
                                    selectedProfile = person
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(
                                        name = person.fullName,
                                        colorHex = person.avatarColorHex,
                                        size = 52.dp,
                                        isOnline = person.onlineStatus && !person.hideOnlineStatus,
                                        isVerified = person.verificationStatus == "verified"
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (person.showAge) "${person.fullName}, ${person.age}" else person.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = NearMeTextMain
                                        )
                                        Text(
                                            text = "${computeDistanceText(myProfile, person)} • ${person.profession.ifBlank { person.intentions.joinToString(", ") }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NearMeTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(onClick = {
                                        showCompatModalFor = person
                                        viewModel.computeAiCompatibility(myProfile, person)
                                    }) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Match", tint = NearMeAccentPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Advanced Discovery Filters", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search name, interest, education, profession") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Gender", style = MaterialTheme.typography.labelLarge, color = NearMeTextMain)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Male", "Female", "Non-binary").forEach { g ->
                            FilterChip(selected = genderFilter == g, onClick = { genderFilter = g }, label = { Text(g) })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Verified Only", color = NearMeTextMain)
                        Switch(checked = verifiedOnly, onCheckedChange = { verifiedOnly = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Online Now", color = NearMeTextMain)
                        Switch(checked = onlineOnly, onCheckedChange = { onlineOnly = it })
                    }
                }
            },
            confirmButton = { Button(onClick = { showFilters = false }) { Text("Apply") } }
        )
    }

    if (selectedProfile != null) {
        val target = selectedProfile!!
        ProfileDetailDialog(
            me = myProfile,
            target = target,
            onDismiss = { selectedProfile = null },
            onConnect = { intro ->
                viewModel.sendConnectionRequest(myProfile, target, intro, null)
                selectedProfile = null
            },
            onAnalyzeCompatibility = {
                showCompatModalFor = target
                viewModel.computeAiCompatibility(myProfile, target)
            },
            onSendGift = { giftType, note ->
                viewModel.sendVirtualGift(myProfile.fullName, target.userId, giftType, note)
            },
            onBlock = {
                viewModel.blockUser(myProfile, target)
                selectedProfile = null
            },
            onReport = { reason ->
                viewModel.submitReport("user", target.userId, reason)
                selectedProfile = null
            }
        )
    }

    if (showCompatModalFor != null) {
        val target = showCompatModalFor!!
        AlertDialog(
            onDismissRequest = {
                showCompatModalFor = null
                viewModel.resetAiCompatibility()
            },
            containerColor = NearMeSecondaryBg,
            title = { Text("AI Compatibility: ${target.fullName}", color = NearMeTextMain) },
            text = {
                when (val st = aiCompatState) {
                    is AiTaskState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NearMeElectricBlue)
                            Text("Analyzing shared interests & intentions with Gemini...", color = NearMeTextSecondary)
                        }
                    }
                    is AiTaskState.Success -> {
                        val res: CompatibilityResult = st.data
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Overall Compatibility: ${res.overallScore}%", style = MaterialTheme.typography.titleMedium, color = NearMeElectricBlue, fontWeight = FontWeight.Bold)
                            ScoreBar("Interest Compatibility", res.interestScore)
                            ScoreBar("Lifestyle Compatibility", res.lifestyleScore)
                            ScoreBar("Education & Career", res.educationScore)
                            Text(res.summary, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                            Surface(color = NearMeSurfaceElevated, shape = RoundedCornerShape(12.dp)) {
                                Text("Icebreaker: \"${res.icebreaker}\"", style = MaterialTheme.typography.bodySmall, color = NearMeSuccess, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Text(st.message, color = NearMeDanger, style = MaterialTheme.typography.bodySmall)
                    }
                    AiTaskState.Idle -> Unit
                }
            },
            confirmButton = {
                val res = (aiCompatState as? AiTaskState.Success)?.data
                Button(onClick = {
                    viewModel.sendConnectionRequest(
                        myProfile,
                        target,
                        res?.icebreaker ?: "Hi ${target.fullName.substringBefore(" ")}, let's connect!",
                        res
                    )
                    showCompatModalFor = null
                    viewModel.resetAiCompatibility()
                }) {
                    Text("Connect with Icebreaker")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCompatModalFor = null
                    viewModel.resetAiCompatibility()
                }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
            Text("$score%", style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
        }
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = NearMeElectricBlue,
            trackColor = NearMeSurfaceElevated
        )
    }
}
