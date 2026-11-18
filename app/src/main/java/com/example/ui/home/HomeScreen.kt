package com.example.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.UiState
import com.example.data.model.Chatroom
import com.example.data.model.PlatformSettings
import com.example.data.model.PublicProfile
import com.example.data.model.SocialEvent
import com.example.data.model.SocialPost
import com.example.data.model.Story
import com.example.ui.NearMeViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.UserAvatar
import com.example.ui.components.computeDistanceText
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeBorder
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
fun HomeScreen(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    platformSettings: PlatformSettings,
    onNavigateDiscover: () -> Unit,
    onNavigateMap: () -> Unit,
    onNavigateCommunities: () -> Unit
) {
    val context = LocalContext.current
    val postsState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val profilesState by viewModel.discoverableProfilesState.collectAsStateWithLifecycle()
    val chatroomsState by viewModel.chatroomsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    var showPostDialog by remember { mutableStateOf(false) }
    var showStoryDialog by remember { mutableStateOf(false) }
    var activeStory by remember { mutableStateOf<Story?>(null) }
    var activeCommentPost by remember { mutableStateOf<SocialPost?>(null) }

    val posts = (postsState as? UiState.Success)?.data.orEmpty()
    val stories = (storiesState as? UiState.Success)?.data.orEmpty()
    val nearbyPeople = (profilesState as? UiState.Success)?.data.orEmpty()
        .filter { it.userId != myProfile.userId && !it.invisibleMode }
    val chatrooms = (chatroomsState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Announcement Banner if set
        if (platformSettings.announcement.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NearMePrimaryBlue.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().border(1.dp, NearMeElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = platformSettings.announcement,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NearMeElectricBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Hero Welcome Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, NearMeBorder, RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nearme_hero_1794946706626),
                    contentDescription = "NearMe community radar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(NearMeSecondaryBg.copy(alpha = 0.92f), Color.Transparent)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Hey ${myProfile.fullName.substringBefore(" ")}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = NearMeTextMain
                    )
                    Text(
                        text = "Explore ${nearbyPeople.size} active members & ${chatrooms.size} communities",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearMeElectricBlue
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showPostDialog = true },
                            modifier = Modifier.height(36.dp).testTag("create_post_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NearMePrimaryBlue)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Post", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onNavigateMap,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nearby Map", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Stories Strip (24h + Highlights)
        item {
            SectionHeader(
                title = "Stories & Highlights",
                subtitle = "24-hour moments & Close Friends",
                actionLabel = "+ Add Story",
                onAction = { showStoryDialog = true }
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showStoryDialog = true }.testTag("add_story_chip")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NearMeSurfaceElevated)
                                .border(2.dp, NearMeElectricBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add story", tint = NearMeElectricBlue)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Your Story", style = MaterialTheme.typography.labelSmall, color = NearMeTextSecondary)
                    }
                }
                items(stories, key = { it.id }) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp).clickable { activeStory = story }
                    ) {
                        UserAvatar(
                            name = story.authorName,
                            colorHex = story.accentColor,
                            size = 64.dp,
                            isVerified = story.highlight
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = story.authorName.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = NearMeTextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Nearby People & Suggested Connections
        item {
            SectionHeader(
                title = "Nearby & Suggested",
                subtitle = "Registered adults matching your interests",
                actionLabel = "Discover All",
                onAction = onNavigateDiscover
            )
            Spacer(Modifier.height(8.dp))
            if (nearbyPeople.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.LocationOn,
                    title = "No nearby members visible yet",
                    description = "As other members complete onboarding or enable location sharing, they will appear here.",
                    actionText = "Open Discovery",
                    onAction = onNavigateDiscover
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(nearbyPeople.take(10), key = { it.userId }) { person ->
                        GlassCard(
                            modifier = Modifier.width(176.dp),
                            onClick = onNavigateDiscover
                        ) {
                            UserAvatar(
                                name = person.fullName,
                                colorHex = person.avatarColorHex,
                                size = 48.dp,
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
                            Text(
                                text = computeDistanceText(myProfile, person),
                                style = MaterialTheme.typography.bodySmall,
                                color = NearMeElectricBlue,
                                maxLines = 1
                            )
                            if (person.interests.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = person.interests.take(2).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NearMeTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Popular Chatrooms & Trending Events Quick Strip
        item {
            SectionHeader(
                title = "Communities & Meetups",
                subtitle = "${chatrooms.size} chatrooms • ${events.size} events",
                actionLabel = "Explore",
                onAction = onNavigateCommunities
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(chatrooms.take(5), key = { "room_${it.id}" }) { room ->
                    ChatroomQuickCard(room = room, onClick = onNavigateCommunities)
                }
                items(events.take(5), key = { "event_${it.id}" }) { event ->
                    EventQuickCard(event = event, onClick = onNavigateCommunities)
                }
            }
        }

        // Social News Feed
        item {
            SectionHeader(
                title = "Social News Feed",
                subtitle = "Latest posts from NearMe members",
                actionLabel = "New Post",
                onAction = { showPostDialog = true }
            )
        }

        if (posts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Forum,
                    title = "Start the conversation",
                    description = "Share a text update, photo note, or local meetup idea with your NearMe community.",
                    actionText = "Create First Post",
                    onAction = { showPostDialog = true }
                )
            }
        } else {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    currentUserId = myProfile.userId,
                    onLike = { viewModel.toggleLikePost(post) },
                    onSave = { viewModel.toggleSavePost(post) },
                    onComment = { activeCommentPost = post },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${post.authorName} on NearMe: \"${post.content}\"")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share NearMe Post"))
                    }
                )
            }
        }
    }

    if (showPostDialog) {
        CreatePostDialog(
            defaultCity = myProfile.cityLabel,
            onDismiss = { showPostDialog = false },
            onSubmit = { content, mediaType, mediaLabel, city, visibility ->
                viewModel.createPost(myProfile.fullName, content, mediaType, mediaLabel, city, visibility)
                showPostDialog = false
            }
        )
    }

    if (showStoryDialog) {
        CreateStoryDialog(
            onDismiss = { showStoryDialog = false },
            onSubmit = { caption, accent, visibility, highlight ->
                viewModel.createStory(myProfile.fullName, caption, accent, visibility, highlight)
                showStoryDialog = false
            }
        )
    }

    if (activeStory != null) {
        val story = activeStory!!
        AlertDialog(
            onDismissRequest = { activeStory = null },
            containerColor = NearMeSecondaryBg,
            title = { Text(story.authorName, color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = NearMePrimaryBlue.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = story.caption,
                            style = MaterialTheme.typography.titleMedium,
                            color = NearMeTextMain,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                    Text(
                        text = if (story.highlight) "Saved Highlight • ${story.visibility}" else "Expires in 24h • ${story.visibility}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NearMeElectricBlue
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeStory = null }) { Text("Close") }
            }
        )
    }

    if (activeCommentPost != null) {
        PostCommentsDialog(
            viewModel = viewModel,
            post = activeCommentPost!!,
            myName = myProfile.fullName,
            onDismiss = { activeCommentPost = null }
        )
    }
}

@Composable
private fun ChatroomQuickCard(room: Chatroom, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.width(200.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Forum, contentDescription = null, tint = NearMeElectricBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(room.roomType.uppercase(), style = MaterialTheme.typography.labelSmall, color = NearMeElectricBlue)
        }
        Spacer(Modifier.height(6.dp))
        Text(room.name, style = MaterialTheme.typography.titleSmall, color = NearMeTextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${room.memberUids.size} members • ${room.category}", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary, maxLines = 1)
    }
}

@Composable
private fun EventQuickCard(event: SocialEvent, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.width(210.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Event, contentDescription = null, tint = NearMeAccentPurple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (event.verified) "VERIFIED EVENT" else "UPCOMING", style = MaterialTheme.typography.labelSmall, color = if (event.verified) NearMeSuccess else NearMeWarning)
        }
        Spacer(Modifier.height(6.dp))
        Text(event.title, style = MaterialTheme.typography.titleSmall, color = NearMeTextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${event.eventDateText} • ${event.cityLabel}", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary, maxLines = 1)
    }
}

@Composable
private fun PostCard(
    post: SocialPost,
    currentUserId: String,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    val isLiked = currentUserId in post.likedByUids
    val isSaved = currentUserId in post.savedByUids

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = post.authorName, size = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(post.authorName, style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                Text(
                    text = buildString {
                        append(post.visibility.replaceFirstChar { it.uppercase() })
                        if (post.cityLabel.isNotBlank()) append(" • ${post.cityLabel}")
                        if (post.mediaType != "text") append(" • ${post.mediaType}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = NearMeTextSecondary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(post.content, style = MaterialTheme.typography.bodyLarge, color = NearMeTextMain)
        if (post.mediaLabel.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = NearMeSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = NearMeElectricBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(post.mediaLabel, style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like post",
                        tint = if (isLiked) Color(0xFFF43F5E) else NearMeTextSecondary
                    )
                }
                Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium, color = NearMeTextSecondary)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onComment) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Comments", tint = NearMeTextSecondary)
                }
                Text("${post.commentsCount}", style = MaterialTheme.typography.labelMedium, color = NearMeTextSecondary)
            }
            Row {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save post",
                        tint = if (isSaved) NearMeElectricBlue else NearMeTextSecondary
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Share post", tint = NearMeTextSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatePostDialog(
    defaultCity: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("text") }
    var mediaLabel by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(defaultCity) }
    var visibility by remember { mutableStateOf("public") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            mediaType = "photo"
            mediaLabel = "Attached photo (${uri.lastPathSegment ?: "media"})"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NearMeSecondaryBg,
        title = { Text("Create Social Post", color = NearMeTextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's happening nearby?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("post_content_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("text", "photo", "video", "location").forEach { t ->
                        FilterChip(
                            selected = mediaType == t,
                            onClick = { mediaType = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                        )
                    }
                }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (mediaLabel.isBlank()) "Attach Photo / Video" else mediaLabel)
                }
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Approximate City / Spot") },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("public", "connections", "private").forEach { vis ->
                        FilterChip(
                            selected = visibility == vis,
                            onClick = { visibility = vis },
                            label = { Text(vis.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMeAccentPurple)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onSubmit(content, mediaType, mediaLabel, city, visibility) },
                enabled = content.isNotBlank()
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Boolean) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("public") }
    var highlight by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NearMeSecondaryBg,
        title = { Text("Share 24h Story", color = NearMeTextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Story message or mood") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("public", "close_friends").forEach { vis ->
                        FilterChip(
                            selected = visibility == vis,
                            onClick = { visibility = vis },
                            label = { Text(if (vis == "public") "Public" else "Close Friends") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save as Highlight", color = NearMeTextMain, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = highlight, onCheckedChange = { highlight = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (caption.isNotBlank()) onSubmit(caption, "#2563EB", visibility, highlight) },
                enabled = caption.isNotBlank()
            ) {
                Text("Share Story")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PostCommentsDialog(
    viewModel: NearMeViewModel,
    post: SocialPost,
    myName: String,
    onDismiss: () -> Unit
) {
    val comments by viewModel.observePostComments(post).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NearMeSecondaryBg,
        title = { Text("Comments (${comments.size})", color = NearMeTextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (comments.isEmpty()) {
                    Text("No comments yet. Be the first!", color = NearMeTextSecondary, style = MaterialTheme.typography.bodySmall)
                } else {
                    comments.take(8).forEach { c ->
                        Surface(color = NearMeSurfaceElevated, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(c.authorName, style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                                Text(c.text, style = MaterialTheme.typography.bodySmall, color = NearMeTextMain)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a reply...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(post, myName, commentText)
                            commentText = ""
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send comment", tint = NearMeElectricBlue)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
