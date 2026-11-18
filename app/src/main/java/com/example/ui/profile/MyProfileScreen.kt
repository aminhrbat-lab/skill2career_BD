package com.example.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AiTaskState
import com.example.core.UiState
import com.example.data.model.PlatformSettings
import com.example.data.model.PrivateUserInfo
import com.example.data.model.PublicProfile
import com.example.ui.NearMeViewModel
import com.example.ui.auth.signOutUser
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.UserAvatar
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeBackground
import com.example.ui.theme.NearMeBorder
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
fun MyProfileScreen(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    platformSettings: PlatformSettings,
    hasAdminAccess: Boolean,
    onOpenAdminCenter: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val privateInfoState by viewModel.privateInfoState.collectAsStateWithLifecycle()
    val logsState by viewModel.activityLogsState.collectAsStateWithLifecycle()
    val verificationsState by viewModel.myVerificationsState.collectAsStateWithLifecycle()
    val giftsState by viewModel.receivedGiftsState.collectAsStateWithLifecycle()
    val aiBiosState by viewModel.aiBiosState.collectAsStateWithLifecycle()

    val privateInfo = (privateInfoState as? UiState.Success)?.data
    val logs = (logsState as? UiState.Success)?.data.orEmpty()
    val verifications = (verificationsState as? UiState.Success)?.data.orEmpty()
    val gifts = (giftsState as? UiState.Success)?.data.orEmpty()

    var showEditDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showDeactivateConfirm by remember { mutableStateOf(false) }

    // Android Photo Picker (zero storage permissions needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val maxAllowed = platformSettings.photoUploadLimit.coerceIn(1, 20)
            if (myProfile.photoLabels.size < maxAllowed) {
                val label = "Photo #${myProfile.photoLabels.size + 1} (${uri.lastPathSegment?.take(24) ?: "Moment"})"
                val updatedPhotos = myProfile.photoLabels + label
                val updatedFeatured = if (myProfile.featuredLabels.size < 3) {
                    myProfile.featuredLabels + label
                } else {
                    myProfile.featuredLabels
                }
                viewModel.saveProfile(
                    profile = myProfile.copy(
                        photoLabels = updatedPhotos,
                        featuredLabels = updatedFeatured
                    ),
                    privateInfo = privateInfo,
                    isNewProfile = false
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cover + Avatar Header Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, NearMeBorder, RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_community_cover_1794946707035),
                        contentDescription = "Profile cover artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, NearMeBackground.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Surface(
                        color = NearMeSecondaryBg.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Theme: ${myProfile.profileTheme}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NearMeElectricBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        name = myProfile.fullName,
                        colorHex = myProfile.avatarColorHex,
                        size = 64.dp,
                        isOnline = myProfile.onlineStatus && !myProfile.hideOnlineStatus,
                        isVerified = myProfile.verificationStatus == "verified"
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${myProfile.fullName}, ${myProfile.age}",
                            style = MaterialTheme.typography.titleLarge,
                            color = NearMeTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${myProfile.username} • ${myProfile.cityLabel.ifBlank { "Nearby" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NearMeElectricBlue
                        )
                        Text(
                            text = "Verification: ${myProfile.verificationStatus.uppercase()} • Status: ${myProfile.accountStatus.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (myProfile.verificationStatus == "verified") NearMeSuccess else NearMeWarning
                        )
                    }
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                if (myProfile.bio.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(myProfile.bio, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (myProfile.profession.isNotBlank()) {
                        Text(
                            "Work: ${myProfile.profession}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NearMeTextSecondary
                        )
                    }
                    if (myProfile.education.isNotBlank()) {
                        Text(
                            "Edu: ${myProfile.education}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NearMeTextSecondary
                        )
                    }
                }

                if (myProfile.badges.isNotEmpty() || myProfile.interests.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        myProfile.badges.forEach { badge ->
                            Surface(color = NearMeSuccess.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = NearMeSuccess, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(badge, style = MaterialTheme.typography.labelSmall, color = NearMeSuccess)
                                }
                            }
                        }
                        myProfile.interests.forEach { interest ->
                            Surface(color = NearMeSurfaceElevated, shape = RoundedCornerShape(50)) {
                                Text(
                                    interest,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NearMeElectricBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Photo Gallery (up to platform limit max 20, 3 featured)
        item {
            SectionHeader(
                title = "Photo Gallery (${myProfile.photoLabels.size}/${platformSettings.photoUploadLimit})",
                subtitle = "Zero-permission Photo Picker • Up to 3 featured photos",
                actionLabel = "+ Add Photo",
                onAction = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                if (myProfile.photoLabels.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = NearMeElectricBlue)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Add photos from your gallery using Android's private Photo Picker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NearMeTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        myProfile.photoLabels.forEach { photo ->
                            val isFeatured = photo in myProfile.featuredLabels
                            FilterChip(
                                selected = isFeatured,
                                onClick = {
                                    val updated = if (isFeatured) {
                                        myProfile.featuredLabels - photo
                                    } else if (myProfile.featuredLabels.size < 3) {
                                        myProfile.featuredLabels + photo
                                    } else {
                                        myProfile.featuredLabels
                                    }
                                    viewModel.saveProfile(
                                        profile = myProfile.copy(featuredLabels = updated),
                                        privateInfo = privateInfo,
                                        isNewProfile = false
                                    )
                                },
                                label = { Text(if (isFeatured) "★ $photo" else photo) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                            )
                        }
                    }
                }
            }
        }

        // Verification & Virtual Gifts
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = NearMeElectricBlue)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Identity Verification", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Text(
                                "Selfie, Phone OTP, or Restricted National ID (${verifications.size} submitted)",
                                style = MaterialTheme.typography.bodySmall,
                                color = NearMeTextSecondary
                            )
                        }
                    }
                    Button(
                        onClick = { showVerifyDialog = true },
                        modifier = Modifier.testTag("verify_identity_button")
                    ) {
                        Text(if (myProfile.verificationStatus == "verified") "Verified" else "Verify")
                    }
                }
                if (gifts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Received ${gifts.size} Virtual Gift(s) from community members",
                        style = MaterialTheme.typography.labelMedium,
                        color = NearMeWarning
                    )
                }
            }
        }

        // Safety, Privacy & Data Export Center
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = NearMeSuccess)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Safety, Privacy & Data Rights", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                        Text(
                            "Activity history (${logs.size} events), Privacy Report & GDPR-style JSON export.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NearMeTextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showSafetyDialog = true },
                        modifier = Modifier.weight(1f).testTag("privacy_report_button")
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Privacy Report")
                    }
                    OutlinedButton(
                        onClick = {
                            val json = viewModel.exportPrivacyJson(myProfile, privateInfo, logs)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "NearMe Personal Data Export")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Personal Data"))
                        },
                        modifier = Modifier.weight(1f).testTag("export_data_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export JSON")
                    }
                }
            }
        }

        // Admin Control Center Entry (only visible to verified Super Admin or assigned Admins)
        if (hasAdminAccess) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("admin_center_card"),
                    onClick = onOpenAdminCenter
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NearMePrimaryBlue, NearMeAccentPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = NearMeTextMain)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Admin Control Center", style = MaterialTheme.typography.titleMedium, color = NearMeTextMain)
                            Text(
                                "Role-authorized analytics, moderation, verification queue & system settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NearMeElectricBlue
                            )
                        }
                        Button(onClick = onOpenAdminCenter, modifier = Modifier.testTag("open_admin_button")) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Account Actions: Deactivate & Sign Out
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDeactivateConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NearMeWarning)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Deactivate")
                }
                Button(
                    onClick = { signOutUser(credentialManager, scope) },
                    modifier = Modifier.weight(1f).testTag("sign_out_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NearMeDanger)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }
        }
    }

    // Edit Profile Dialog with AI Bio Assistant + Privacy Toggles
    if (showEditDialog) {
        var fullName by remember { mutableStateOf(myProfile.fullName) }
        var bio by remember { mutableStateOf(myProfile.bio) }
        var profession by remember { mutableStateOf(myProfile.profession) }
        var education by remember { mutableStateOf(myProfile.education) }
        var cityLabel by remember { mutableStateOf(myProfile.cityLabel) }
        var theme by remember { mutableStateOf(myProfile.profileTheme) }
        var showAge by remember { mutableStateOf(myProfile.showAge) }
        var showEdu by remember { mutableStateOf(myProfile.showEducation) }
        var showProf by remember { mutableStateOf(myProfile.showProfession) }
        var showDist by remember { mutableStateOf(myProfile.showDistance) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Edit Profile", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    OutlinedButton(
                        onClick = {
                            viewModel.generateAiBios(
                                fullName = fullName,
                                profession = profession,
                                education = education,
                                interests = myProfile.interests,
                                intentions = myProfile.intentions,
                                city = cityLabel
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Suggest Bio")
                    }
                    if (aiBiosState is AiTaskState.Success) {
                        val suggestions = (aiBiosState as AiTaskState.Success<List<String>>).data
                        suggestions.take(2).forEach { s ->
                            Surface(
                                color = NearMeSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { bio = s }
                            ) {
                                Text(s, style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                    OutlinedTextField(value = profession, onValueChange = { profession = it }, label = { Text("Profession") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = education, onValueChange = { education = it }, label = { Text("Education") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cityLabel, onValueChange = { cityLabel = it }, label = { Text("City / Area") }, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Electric Navy", "Neon Sky", "Violet Pulse").forEach { t ->
                            FilterChip(selected = theme == t, onClick = { theme = t }, label = { Text(t) })
                        }
                    }
                    ProfileSwitch("Show Age", showAge) { showAge = it }
                    ProfileSwitch("Show Education", showEdu) { showEdu = it }
                    ProfileSwitch("Show Profession", showProf) { showProf = it }
                    ProfileSwitch("Show Approx Distance", showDist) { showDist = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveProfile(
                            profile = myProfile.copy(
                                fullName = fullName.ifBlank { myProfile.fullName },
                                bio = bio,
                                profession = profession,
                                education = education,
                                cityLabel = cityLabel,
                                profileTheme = theme,
                                showAge = showAge,
                                showEducation = showEdu,
                                showProfession = showProf,
                                showDistance = showDist
                            ),
                            privateInfo = privateInfo,
                            isNewProfile = false
                        )
                        showEditDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Verification Dialog (Selfie, Phone OTP, Restricted National ID)
    if (showVerifyDialog) {
        var method by remember { mutableStateOf("selfie") }
        var docSummary by remember { mutableStateOf("Live selfie posture verified — requesting blue badge") }
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Submit Verification", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "National ID details are strictly restricted to Verification Officers and never shown publicly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearMeTextSecondary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("selfie", "phone_otp", "nid").forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.uppercase()) })
                        }
                    }
                    OutlinedTextField(
                        value = docSummary,
                        onValueChange = { docSummary = it },
                        label = { Text("Verification Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.submitVerification(myProfile.fullName, method, docSummary)
                    showVerifyDialog = false
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") } }
        )
    }

    // Privacy Report & Activity Logs Dialog
    if (showSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyDialog = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Privacy & Security Activity", color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account Email: ${privateInfo?.email ?: "Private"}", style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                    Text("Location Sharing: ${if (myProfile.invisibleMode) "Invisible Mode" else if (myProfile.locationSharingEnabled) "Approximate (${myProfile.discoveryRadiusKm} km)" else "Paused"}", style = MaterialTheme.typography.bodySmall, color = NearMeTextMain)
                    Text("Recent Activity & Login History:", style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                    if (logs.isEmpty()) {
                        Text("No activity logs recorded yet.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                    } else {
                        logs.take(5).forEach { log ->
                            Text("• [${log.eventType}] ${log.description}", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSafetyDialog = false }) { Text("Close") } }
        )
    }

    // Deactivate Account Confirmation
    if (showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = { showDeactivateConfirm = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Deactivate Account?", color = NearMeTextMain) },
            text = {
                Text(
                    "Your profile will be hidden from Discover, Nearby Map, and feeds until reactivated. Your data remains safe.",
                    color = NearMeTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deactivateAccount()
                        showDeactivateConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NearMeDanger)
                ) { Text("Confirm Deactivate") }
            },
            dismissButton = { TextButton(onClick = { showDeactivateConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = NearMeTextMain)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
