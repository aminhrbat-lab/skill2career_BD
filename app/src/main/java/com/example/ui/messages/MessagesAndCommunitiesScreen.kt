package com.example.ui.messages

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AiTaskState
import com.example.core.UiState
import com.example.data.model.Chatroom
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.PlatformSettings
import com.example.data.model.PublicProfile
import com.example.data.model.SocialEvent
import com.example.ui.NearMeViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeDanger
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMePrimaryBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSuccess
import com.example.ui.theme.NearMeSurfaceElevated
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary
import com.example.ui.theme.NearMeWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagesAndCommunitiesScreen(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    platformSettings: PlatformSettings,
    initialSubTab: Int = 0
) {
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var activeChatroom by remember { mutableStateOf<Chatroom?>(null) }

    if (activeConversation != null) {
        BackHandler { activeConversation = null }
        DirectChatDetailView(
            viewModel = viewModel,
            myProfile = myProfile,
            conversation = activeConversation!!,
            onBack = { activeConversation = null }
        )
        return
    }

    if (activeChatroom != null) {
        BackHandler { activeChatroom = null }
        ChatroomDetailView(
            viewModel = viewModel,
            myProfile = myProfile,
            room = activeChatroom!!,
            onBack = { activeChatroom = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = NearMeSecondaryBg,
            contentColor = NearMeElectricBlue
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("Direct Chats") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Chatrooms") })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("Circles & Events") })
        }

        when (selectedSubTab) {
            0 -> DirectChatsTab(
                viewModel = viewModel,
                myProfile = myProfile,
                onOpenConversation = { activeConversation = it }
            )
            1 -> ChatroomsTab(
                viewModel = viewModel,
                myProfile = myProfile,
                chatroomsEnabled = platformSettings.chatroomsEnabled,
                onOpenRoom = { activeChatroom = it }
            )
            else -> CirclesAndEventsTab(viewModel = viewModel, myProfile = myProfile)
        }
    }
}

@Composable
private fun DirectChatsTab(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    onOpenConversation: (Conversation) -> Unit
) {
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val connectionsState by viewModel.connectionsState.collectAsStateWithLifecycle()
    val profilesState by viewModel.discoverableProfilesState.collectAsStateWithLifecycle()

    var showChatPrivacy by remember { mutableStateOf(false) }

    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val connections = (connectionsState as? UiState.Success)?.data.orEmpty()
    val acceptedConnections = connections.filter { it.status == "accepted" }
    val pendingIncoming = connections.filter { it.receiverId == myProfile.userId && it.status == "pending" }
    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()

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
                Text("Private Messaging", style = MaterialTheme.typography.titleLarge, color = NearMeTextMain)
                OutlinedButton(onClick = { showChatPrivacy = true }) {
                    Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Privacy")
                }
            }
        }

        if (pendingIncoming.isNotEmpty()) {
            item {
                Text("Connection Requests (${pendingIncoming.size})", style = MaterialTheme.typography.titleSmall, color = NearMeElectricBlue)
            }
            items(pendingIncoming, key = { it.id }) { conn ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(conn.senderName, style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                    if (conn.introMessage.isNotBlank()) {
                        Text("\"${conn.introMessage}\"", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.respondToConnection(conn, "accepted") }) { Text("Accept & Chat") }
                        OutlinedButton(onClick = { viewModel.respondToConnection(conn, "declined") }) { Text("Decline") }
                    }
                }
            }
        }

        if (conversations.isEmpty() && acceptedConnections.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Chat,
                    title = "No private conversations yet",
                    description = "Connect with someone on Discover or Nearby Map first. Private messages are strictly restricted to mutual participants."
                )
            }
        } else {
            items(conversations, key = { it.id }) { conv ->
                val otherName = conv.participantNames.firstOrNull { it != myProfile.fullName }
                    ?: conv.participantNames.lastOrNull()
                    ?: "Connection"
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenConversation(conv) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(name = otherName, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(otherName, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Text(conv.lastMessage, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (acceptedConnections.isNotEmpty()) {
                item {
                    Text("Connected Friends (${acceptedConnections.size})", style = MaterialTheme.typography.titleSmall, color = NearMeElectricBlue)
                }
                items(acceptedConnections, key = { "conn_${it.id}" }) { conn ->
                    val otherUid = conn.participantUids.firstOrNull { it != myProfile.userId } ?: ""
                    val otherName = if (conn.senderId == myProfile.userId) conn.receiverName else conn.senderName
                    val targetProfile = allProfiles.firstOrNull { it.userId == otherUid } ?: PublicProfile(userId = otherUid, fullName = otherName)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.openOrStartConversation(myProfile.fullName, targetProfile) { convId ->
                                onOpenConversation(
                                    Conversation(
                                        id = convId,
                                        participantUids = listOf(myProfile.userId, otherUid),
                                        participantNames = listOf(myProfile.fullName, otherName)
                                    )
                                )
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(otherName, style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                            Text("Message", style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                        }
                    }
                }
            }
        }
    }

    if (showChatPrivacy) {
        var hideOnline by remember { mutableStateOf(myProfile.hideOnlineStatus) }
        var hideLastSeen by remember { mutableStateOf(myProfile.hideLastSeen) }
        var readReceipts by remember { mutableStateOf(myProfile.readReceiptsEnabled) }
        var typingInd by remember { mutableStateOf(myProfile.typingIndicatorEnabled) }
        AlertDialog(
            onDismissRequest = { showChatPrivacy = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Chat Privacy Controls", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchLine("Hide Online Status", hideOnline) { hideOnline = it }
                    SwitchLine("Hide Last Seen", hideLastSeen) { hideLastSeen = it }
                    SwitchLine("Read Receipts", readReceipts) { readReceipts = it }
                    SwitchLine("Typing Indicator", typingInd) { typingInd = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateChatPrivacy(hideOnline, hideLastSeen, readReceipts, typingInd)
                    showChatPrivacy = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showChatPrivacy = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DirectChatDetailView(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    conversation: Conversation,
    onBack: () -> Unit
) {
    val messages by viewModel.observeMessages(conversation.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val aiHelperState by viewModel.aiConversationHelperState.collectAsStateWithLifecycle()
    var messageInput by remember { mutableStateOf("") }
    var messageType by remember { mutableStateOf("text") }
    var selectedMessage by remember { mutableStateOf<DirectMessage?>(null) }

    val otherName = conversation.participantNames.firstOrNull { it != myProfile.fullName } ?: "Member"

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Chat Header
        Surface(color = NearMeSecondaryBg, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NearMeTextMain)
                }
                UserAvatar(name = otherName, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(otherName, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                    Text(
                        if (myProfile.readReceiptsEnabled) "End-to-participant private chat • Read receipts ON" else "Private chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = NearMeElectricBlue
                    )
                }
                IconButton(onClick = {
                    viewModel.runConversationHelper(myProfile.fullName, otherName, messageInput)
                }) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Safety & Icebreaker", tint = NearMeAccentPurple)
                }
            }
        }

        // AI Conversation Safety / Icebreaker Banner
        when (val st = aiHelperState) {
            is AiTaskState.Loading -> {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NearMeElectricBlue, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Gemini AI crafting friendly suggestion...", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                }
            }
            is AiTaskState.Success -> {
                Surface(
                    color = NearMeAccentPurple.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("AI Suggestion (tap to insert)", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                        Text(
                            st.data,
                            style = MaterialTheme.typography.bodySmall,
                            color = NearMeTextMain,
                            modifier = Modifier.clickable {
                                messageInput = st.data
                                viewModel.resetAiConversationHelper()
                            }
                        )
                    }
                }
            }
            is AiTaskState.Error -> {
                Text(st.message, color = NearMeDanger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            }
            AiTaskState.Idle -> Unit
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == myProfile.userId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine) NearMePrimaryBlue else NearMeSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { selectedMessage = msg }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (msg.messageType != "text") {
                                Text("[${msg.messageType.uppercase()}] ${msg.attachmentLabel}", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                            }
                            Text(
                                text = msg.text.ifBlank { msg.attachmentLabel },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.unsent) NearMeTextSecondary else NearMeTextMain
                            )
                            if (msg.reaction.isNotBlank()) {
                                Text(msg.reaction, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Message Type Chips & Composer
        Surface(color = NearMeSecondaryBg, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("text", "photo", "voice_note", "file").forEach { t ->
                        FilterChip(
                            selected = messageType == t,
                            onClick = { messageType = t },
                            label = { Text(t.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Write a private message...") },
                        modifier = Modifier.weight(1f).testTag("dm_input"),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val attach = if (messageType == "text") "" else "Shared $messageType"
                                viewModel.sendDirectMessage(conversation, myProfile.fullName, messageInput, messageType, attach)
                                messageInput = ""
                                messageType = "text"
                            }
                        },
                        modifier = Modifier.testTag("dm_send_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = NearMeElectricBlue)
                    }
                }
            }
        }
    }

    if (selectedMessage != null) {
        val m = selectedMessage!!
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            containerColor = NearMeSecondaryBg,
            title = { Text("Message Actions", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("React:", style = MaterialTheme.typography.labelLarge, color = NearMeTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("👍", "❤️", "🔥", "😂").forEach { emoji ->
                            OutlinedButton(onClick = {
                                viewModel.reactToDirectMessage(conversation.id, m.id, emoji)
                                selectedMessage = null
                            }) { Text(emoji) }
                        }
                    }
                    if (m.senderId == myProfile.userId && !m.unsent) {
                        OutlinedButton(
                            onClick = {
                                viewModel.unsendDirectMessage(conversation.id, m.id)
                                selectedMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Unsend Message")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteDirectMessage(conversation.id, m.id)
                                selectedMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = NearMeDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", color = NearMeDanger)
                        }
                    }
                    TextButton(onClick = {
                        viewModel.submitReport("message", m.id, "Inappropriate message: ${m.text.take(80)}")
                        selectedMessage = null
                    }) {
                        Text("Report Message", color = NearMeDanger)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedMessage = null }) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatroomsTab(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    chatroomsEnabled: Boolean,
    onOpenRoom: (Chatroom) -> Unit
) {
    val roomsState by viewModel.chatroomsState.collectAsStateWithLifecycle()
    val rooms = (roomsState as? UiState.Success)?.data.orEmpty()
    var showCreateRoom by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("all") }

    val visibleRooms = remember(rooms, filterType) {
        if (filterType == "all") rooms else rooms.filter { it.roomType == filterType }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Community Chatrooms", style = MaterialTheme.typography.titleLarge, color = NearMeTextMain)
                    Text("Public, Private, Password, Interest & Location rooms", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                }
                Button(
                    onClick = { showCreateRoom = true },
                    enabled = chatroomsEnabled,
                    modifier = Modifier.testTag("create_room_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create")
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all", "public", "interest", "location", "private", "password").forEach { t ->
                    FilterChip(
                        selected = filterType == t,
                        onClick = { filterType = t },
                        label = { Text(t.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                    )
                }
            }
        }

        if (visibleRooms.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Forum,
                    title = "No chatrooms yet",
                    description = "Create a local room like 'University Students', 'Coffee & Tech', or 'Travelling Lovers'!",
                    actionText = "Create Chatroom",
                    onAction = { showCreateRoom = true }
                )
            }
        } else {
            items(visibleRooms, key = { it.id }) { room ->
                val isMember = myProfile.userId in room.memberUids
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (isMember) onOpenRoom(room)
                        else if (room.joinOption == "anyone") {
                            viewModel.joinChatroom(room)
                            onOpenRoom(room)
                        }
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (room.roomType in listOf("private", "password")) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = NearMeWarning, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Column {
                                Text(room.name, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                                Text("${room.roomType.uppercase()} • ${room.category} • ${room.memberUids.size} members", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                            }
                        }
                        Button(onClick = {
                            if (!isMember) viewModel.joinChatroom(room)
                            onOpenRoom(room)
                        }) {
                            Text(if (isMember) "Open" else "Join")
                        }
                    }
                    if (room.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(room.description, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary, maxLines = 2)
                    }
                }
            }
        }
    }

    if (showCreateRoom) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var roomType by remember { mutableStateOf("public") }
        var category by remember { mutableStateOf("Social Community") }
        var passcode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateRoom = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Create Chatroom", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Room Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("public", "interest", "location", "private", "password").forEach { t ->
                            FilterChip(selected = roomType == t, onClick = { roomType = t }, label = { Text(t) })
                        }
                    }
                    if (roomType == "password") {
                        OutlinedTextField(value = passcode, onValueChange = { passcode = it }, label = { Text("Room Passcode") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createChatroom(
                            name = name,
                            description = desc,
                            roomType = roomType,
                            joinOption = if (roomType == "private") "invite" else "anyone",
                            passcode = passcode,
                            category = category,
                            cityLabel = myProfile.cityLabel
                        )
                        showCreateRoom = false
                    },
                    enabled = name.trim().length >= 2
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateRoom = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ChatroomDetailView(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    room: Chatroom,
    onBack: () -> Unit
) {
    val messages by viewModel.observeChatroomMessages(room).collectAsStateWithLifecycle(initialValue = emptyList())
    var input by remember { mutableStateOf("") }
    var msgType by remember { mutableStateOf("text") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = NearMeSecondaryBg, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NearMeTextMain)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(room.name, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                    Text("${room.memberUids.size} members • ${room.roomType}", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                }
                TextButton(onClick = {
                    viewModel.submitReport("chatroom", room.id, "Reported room ${room.name} for moderation review")
                }) {
                    Text("Report", color = NearMeDanger)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(msg.senderName, style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                    if (msg.messageType != "text") {
                        Text("[${msg.messageType.uppercase()}]", style = MaterialTheme.typography.labelSmall, color = NearMeAccentPurple)
                    }
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                }
            }
        }

        Surface(color = NearMeSecondaryBg, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("text", "photo", "poll").forEach { t ->
                        FilterChip(selected = msgType == t, onClick = { msgType = t }, label = { Text(t) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Message ${room.name}...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendChatroomMessage(room, myProfile.fullName, input, msgType)
                            input = ""
                            msgType = "text"
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = NearMeElectricBlue)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CirclesAndEventsTab(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile
) {
    val context = LocalContext.current
    val circlesState by viewModel.circlesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val circles = (circlesState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()

    var showCreateCircle by remember { mutableStateOf(false) }
    var showCreateEvent by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Social Circles & Groups", style = MaterialTheme.typography.titleLarge, color = NearMeTextMain)
                OutlinedButton(onClick = { showCreateCircle = true }) { Text("+ Circle") }
            }
        }

        if (circles.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Group,
                    title = "Create your social circles",
                    description = "Organize Close Friends, Family, Work, Study circles or Nearby Social Groups.",
                    actionText = "Create Circle",
                    onAction = { showCreateCircle = true }
                )
            }
        } else {
            items(circles, key = { it.id }) { circle ->
                val isMember = myProfile.userId in circle.memberUids
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(circle.name, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Text("${circle.circleType.replace("_", " ")} • ${circle.memberUids.size} members", style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
                        }
                        if (!isMember) {
                            Button(onClick = { viewModel.joinCircle(circle) }) { Text("Join") }
                        }
                    }
                    if (circle.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(circle.description, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Events & Meetups", style = MaterialTheme.typography.titleLarge, color = NearMeTextMain)
                Button(onClick = { showCreateEvent = true }) { Text("+ Event") }
            }
        }

        if (events.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Event,
                    title = "No upcoming meetups",
                    description = "Host a Coffee Meetup, University Gathering, or Travelling Group. Verified events appear on the Nearby Map!",
                    actionText = "Host Event",
                    onAction = { showCreateEvent = true }
                )
            }
        } else {
            items(events, key = { it.id }) { ev ->
                val joined = myProfile.userId in ev.attendeeUids
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Text("${ev.eventDateText} • ${ev.cityLabel}", style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                            Text(
                                if (ev.verified) "Verified Event (Visible on Map)" else "Pending Verification",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ev.verified) NearMeSuccess else NearMeWarning
                            )
                        }
                        Button(onClick = { viewModel.joinEvent(ev) }, enabled = !joined) {
                            Text(if (joined) "Joined" else "Join")
                        }
                    }
                    if (ev.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(ev.description, style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Meetup Safety Check-in: I'm attending \"${ev.title}\" (${ev.eventDateText}) in ${ev.cityLabel} via NearMe."
                            )
                        }
                        context.startActivity(Intent.createChooser(share, "Share Meetup Safety Details with Trusted Contact"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Safety Details with Trusted Contact")
                    }
                }
            }
        }
    }

    if (showCreateCircle) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("close_friends") }
        var desc by remember { mutableStateOf("") }
        var rules by remember { mutableStateOf("Be respectful and welcoming.") }
        AlertDialog(
            onDismissRequest = { showCreateCircle = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Create Social Circle", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Circle Name") }, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("close_friends", "family", "work", "study", "nearby_group", "custom").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.replace("_", " ")) })
                        }
                    }
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("Group Rules") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.createCircle(name, type, desc, rules); showCreateCircle = false }, enabled = name.trim().length >= 2) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateCircle = false }) { Text("Cancel") } }
        )
    }

    if (showCreateEvent) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("coffee") }
        var city by remember { mutableStateOf(myProfile.cityLabel.ifBlank { "Dhaka" }) }
        var dateText by remember { mutableStateOf("Saturday 5:00 PM") }
        AlertDialog(
            onDismissRequest = { showCreateEvent = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Create Meetup / Event", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("coffee", "university", "travel", "social", "community").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Approximate City / Venue Area") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Date & Time") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createEvent(myProfile.fullName, title, desc, type, city, myProfile.approxLat, myProfile.approxLng, dateText)
                    showCreateEvent = false
                }, enabled = title.trim().length >= 2) { Text("Create Event") }
            },
            dismissButton = { TextButton(onClick = { showCreateEvent = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
