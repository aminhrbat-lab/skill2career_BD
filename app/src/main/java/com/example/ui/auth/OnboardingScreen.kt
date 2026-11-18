package com.example.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AiTaskState
import com.example.core.LocationHelper
import com.example.data.model.PrivateUserInfo
import com.example.data.model.PublicProfile
import com.example.ui.NearMeViewModel
import com.example.ui.components.GlassCard
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
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: NearMeViewModel,
    initialEmail: String,
    initialDisplayName: String,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiBiosState by viewModel.aiBiosState.collectAsStateWithLifecycle()

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 9

    // Step 1: Basic Info + 18+ Verification
    var fullName by remember { mutableStateOf(initialDisplayName.ifBlank { "" }) }
    var username by remember {
        mutableStateOf(
            initialDisplayName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }.take(20)
        )
    }
    var birthYearText by remember { mutableStateOf("2000") }
    var mobileNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Prefer not to say") }
    var cityLabel by remember { mutableStateOf("Dhaka") }
    var stepError by remember { mutableStateOf<String?>(null) }

    // Step 2: Profile Picture & Avatar Color
    var selectedPhotoLabel by remember { mutableStateOf("") }
    var avatarColorHex by remember { mutableStateOf("#2563EB") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedPhotoLabel = "Profile Photo (${uri.lastPathSegment ?: "selected"})"
        }
    }

    // Step 3 & 4: Education & Profession
    var education by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    // Step 5: Interests & Hobbies
    val availableInterests = listOf(
        "Tech & AI", "Coffee", "Travel", "Photography", "Music", "Startups",
        "Fitness", "Books", "Gaming", "Design", "Foodie", "Movies", "Hiking", "Volunteering"
    )
    var selectedInterests by remember { mutableStateOf(listOf("Coffee", "Travel", "Tech & AI")) }
    var customInterest by remember { mutableStateOf("") }

    // Step 6: Relationship Preferences
    val intentionOptions = listOf("Friendship", "Dating", "Serious Relationship", "Networking", "Socializing")
    var selectedIntentions by remember { mutableStateOf(listOf("Friendship", "Networking")) }

    // Step 7: Location Permission (Explicit Opt-In)
    var locationSharingEnabled by remember { mutableStateOf(false) }
    var invisibleMode by remember { mutableStateOf(false) }
    var discoveryRadiusKm by remember { mutableFloatStateOf(10f) }
    var approxLat by remember { mutableStateOf<Double?>(null) }
    var approxLng by remember { mutableStateOf<Double?>(null) }
    var locationStatusMsg by remember { mutableStateOf("Location sharing is OFF by default.") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val coords = LocationHelper.fetchApproximateLocation(context)
                if (coords != null) {
                    approxLat = coords.first
                    approxLng = coords.second
                    locationSharingEnabled = true
                    invisibleMode = false
                    locationStatusMsg = "Approximate grid ready (~1.1 km precision)."
                } else {
                    locationSharingEnabled = true
                    locationStatusMsg = "Permission granted; approximate city grid enabled."
                }
            }
        } else {
            locationSharingEnabled = false
            locationStatusMsg = "Location permission declined. You can still browse manually."
        }
    }

    // Step 8: Privacy Settings
    var showAge by remember { mutableStateOf(true) }
    var showEducation by remember { mutableStateOf(true) }
    var showProfession by remember { mutableStateOf(true) }
    var showDistance by remember { mutableStateOf(true) }
    var hideOnlineStatus by remember { mutableStateOf(false) }
    var readReceiptsEnabled by remember { mutableStateOf(true) }

    // Step 9: Bio & Complete
    var bio by remember { mutableStateOf("") }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val parsedAge = remember(birthYearText) {
        val yr = birthYearText.toIntOrNull() ?: 0
        if (yr in 1900..currentYear) currentYear - yr else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearMeBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Top Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.labelLarge,
                    color = NearMeElectricBlue
                )
                Text(
                    text = stepTitle(currentStep),
                    style = MaterialTheme.typography.titleSmall,
                    color = NearMeTextMain
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { currentStep / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = NearMeElectricBlue,
                trackColor = NearMeSurfaceElevated
            )

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        Text("Create Your Adult Profile (18+)", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Underage registration is strictly blocked.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; stepError = null },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_fullname_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; stepError = null },
                            label = { Text("Username *") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_username_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = birthYearText,
                            onValueChange = { birthYearText = it.filter { c -> c.isDigit() }.take(4); stepError = null },
                            label = { Text("Birth Year (must be 18+) *") },
                            supportingText = {
                                Text(
                                    if (parsedAge >= 18) "Verified age: $parsedAge years"
                                    else "Minimum registration age is 18 years",
                                    color = if (parsedAge >= 18) NearMeSuccess else NearMeDanger
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_birthyear_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            label = { Text("Mobile Number (stored privately)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cityLabel,
                            onValueChange = { cityLabel = it },
                            label = { Text("City / Area") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Gender", style = MaterialTheme.typography.labelLarge, color = NearMeTextMain)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { g ->
                                FilterChip(
                                    selected = gender == g,
                                    onClick = { gender = g },
                                    label = { Text(g) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                                )
                            }
                        }
                    }

                    2 -> {
                        Text("Profile Picture & Look", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Pick a photo with the zero-permission Android Photo Picker or choose your signature aura.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                UserAvatar(name = fullName.ifBlank { "NM" }, colorHex = avatarColorHex, size = 84.dp)
                                if (selectedPhotoLabel.isNotBlank()) {
                                    Text(selectedPhotoLabel, style = MaterialTheme.typography.bodySmall, color = NearMeElectricBlue)
                                }
                                OutlinedButton(
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Select Photo")
                                }
                            }
                        }
                        Text("Avatar Aura Color", style = MaterialTheme.typography.labelLarge, color = NearMeTextMain)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("#2563EB", "#38BDF8", "#8B5CF6", "#10B981", "#F59E0B").forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (avatarColorHex == hex) 3.dp else 1.dp,
                                            color = if (avatarColorHex == hex) NearMeTextMain else NearMeBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { avatarColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarColorHex == hex) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        Text("Education", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Share your academic background (optional).", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        OutlinedTextField(
                            value = education,
                            onValueChange = { education = it },
                            label = { Text("Degree / Field of Study") },
                            placeholder = { Text("e.g., B.Sc. in Computer Science") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = institution,
                            onValueChange = { institution = it },
                            label = { Text("University / Institution") },
                            placeholder = { Text("e.g., Dhaka University") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    4 -> {
                        Text("Profession", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Let professionals and peers connect with you (optional).", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        OutlinedTextField(
                            value = profession,
                            onValueChange = { profession = it },
                            label = { Text("Role / Profession") },
                            placeholder = { Text("e.g., Software Engineer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company / Organization") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    5 -> {
                        Text("Interests & Hobbies", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Select topics so NearMe can suggest compatible people & chatrooms.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (availableInterests + selectedInterests).distinct().forEach { tag ->
                                val selected = tag in selectedInterests
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedInterests = if (selected) selectedInterests - tag else (selectedInterests + tag).take(15)
                                    },
                                    label = { Text(tag) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customInterest,
                                onValueChange = { customInterest = it },
                                label = { Text("Add custom interest") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                val clean = customInterest.trim()
                                if (clean.isNotBlank() && clean !in selectedInterests) {
                                    selectedInterests = (selectedInterests + clean).take(15)
                                    customInterest = ""
                                }
                            }) {
                                Text("Add")
                            }
                        }
                    }

                    6 -> {
                        Text("Relationship Preferences", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Select one or more intentions for your NearMe connections.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            intentionOptions.forEach { intent ->
                                val selected = intent in selectedIntentions
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedIntentions = if (selected) selectedIntentions - intent else (selectedIntentions + intent).take(5)
                                    },
                                    label = { Text(intent) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMeAccentPurple)
                                )
                            }
                        }
                    }

                    7 -> {
                        Text("Location Privacy & Radar", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("NearMe never tracks you automatically and never reveals exact coordinates publicly.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NearMeElectricBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text(locationStatusMsg, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                                }
                                Button(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            scope.launch {
                                                val coords = LocationHelper.fetchApproximateLocation(context)
                                                if (coords != null) {
                                                    approxLat = coords.first
                                                    approxLng = coords.second
                                                }
                                                locationSharingEnabled = true
                                                invisibleMode = false
                                                locationStatusMsg = "Approximate location enabled (~1.1 km grid)."
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (locationSharingEnabled) "Refresh Approximate Location" else "Enable Approximate Location")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Invisible Mode", style = MaterialTheme.typography.titleSmall, color = NearMeTextMain)
                                        Text("Hide from Nearby Map & discovery", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                                    }
                                    Switch(
                                        checked = invisibleMode,
                                        onCheckedChange = {
                                            invisibleMode = it
                                            if (it) locationSharingEnabled = false
                                        }
                                    )
                                }
                                Text("Discovery Radius: ${discoveryRadiusKm.toInt()} km", style = MaterialTheme.typography.labelLarge, color = NearMeElectricBlue)
                                Slider(
                                    value = discoveryRadiusKm,
                                    onValueChange = { discoveryRadiusKm = it },
                                    valueRange = 1f..50f
                                )
                            }
                        }
                    }

                    8 -> {
                        Text("Privacy Controls", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Choose exactly what registered members can see on your profile.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrivacySwitchRow("Show Age", showAge) { showAge = it }
                                PrivacySwitchRow("Show Education", showEducation) { showEducation = it }
                                PrivacySwitchRow("Show Profession", showProfession) { showProfession = it }
                                PrivacySwitchRow("Show Approximate Distance", showDistance) { showDistance = it }
                                PrivacySwitchRow("Hide Online Status", hideOnlineStatus) { hideOnlineStatus = it }
                                PrivacySwitchRow("Send Read Receipts", readReceiptsEnabled) { readReceiptsEnabled = it }
                            }
                        }
                    }

                    9 -> {
                        Text("Complete Profile & AI Bio Writer", style = MaterialTheme.typography.headlineSmall, color = NearMeTextMain)
                        Text("Let Gemini craft a standout NearMe bio from your profile, or write your own.", style = MaterialTheme.typography.bodySmall, color = NearMeTextSecondary)

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Your Bio") },
                            placeholder = { Text("Tell nearby people what excites you...") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_bio_input"),
                            minLines = 3,
                            maxLines = 5
                        )

                        Button(
                            onClick = {
                                viewModel.generateAiBios(
                                    fullName = fullName.ifBlank { "Member" },
                                    profession = profession,
                                    education = education,
                                    interests = selectedInterests,
                                    intentions = selectedIntentions,
                                    city = cityLabel
                                )
                            },
                            enabled = aiBiosState !is AiTaskState.Loading,
                            modifier = Modifier.fillMaxWidth().testTag("ai_bio_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NearMeAccentPurple)
                        ) {
                            if (aiBiosState is AiTaskState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NearMeTextMain, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Writing with Gemini AI...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generate AI Bios")
                            }
                        }

                        when (val state = aiBiosState) {
                            is AiTaskState.Success -> {
                                state.data.forEachIndexed { idx, suggestion ->
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { bio = suggestion }
                                    ) {
                                        Text("Option ${idx + 1} (Tap to use)", style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                                        Spacer(Modifier.height(4.dp))
                                        Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
                                    }
                                }
                            }
                            is AiTaskState.Error -> {
                                Text(state.message, color = NearMeDanger, style = MaterialTheme.typography.bodySmall)
                            }
                            else -> Unit
                        }
                    }
                }

                if (stepError != null) {
                    Surface(color = NearMeDanger.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                        Text(stepError!!, color = NearMeDanger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(onClick = { stepError = null; currentStep-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentStep in 2..8) {
                        TextButton(onClick = { stepError = null; currentStep++ }) {
                            Text("Skip", color = NearMeTextSecondary)
                        }
                    }
                    Button(
                        onClick = {
                            if (currentStep == 1) {
                                when {
                                    fullName.trim().isEmpty() -> stepError = "Please enter your full name."
                                    username.trim().length < 2 -> stepError = "Username must be at least 2 characters."
                                    parsedAge < 18 -> stepError = "Minimum age is 18 years. Underage registration is not allowed."
                                    else -> {
                                        stepError = null
                                        currentStep++
                                    }
                                }
                            } else if (currentStep < totalSteps) {
                                stepError = null
                                currentStep++
                            } else {
                                val finalProfile = PublicProfile(
                                    userId = viewModel.currentUserId,
                                    fullName = fullName.trim(),
                                    username = username.trim().lowercase().replace(" ", "_"),
                                    age = parsedAge.coerceIn(18, 120),
                                    gender = gender,
                                    bio = bio.ifBlank { "Exploring NearMe in ${cityLabel.ifBlank { "my city" }}!" },
                                    education = education.trim(),
                                    institution = institution.trim(),
                                    profession = profession.trim(),
                                    company = company.trim(),
                                    cityLabel = cityLabel.trim().ifBlank { "Nearby" },
                                    approxLat = approxLat,
                                    approxLng = approxLng,
                                    locationSharingEnabled = locationSharingEnabled && !invisibleMode,
                                    invisibleMode = invisibleMode,
                                    discoveryRadiusKm = discoveryRadiusKm.toDouble(),
                                    hobbies = selectedInterests.take(10),
                                    interests = selectedInterests,
                                    intentions = selectedIntentions.ifEmpty { listOf("Friendship") },
                                    avatarColorHex = avatarColorHex,
                                    photoLabels = if (selectedPhotoLabel.isNotBlank()) listOf(selectedPhotoLabel) else emptyList(),
                                    featuredLabels = if (selectedPhotoLabel.isNotBlank()) listOf(selectedPhotoLabel) else emptyList(),
                                    hideOnlineStatus = hideOnlineStatus,
                                    readReceiptsEnabled = readReceiptsEnabled,
                                    showAge = showAge,
                                    showEducation = showEducation,
                                    showProfession = showProfession,
                                    showDistance = showDistance,
                                    onboardingCompleted = true
                                )
                                val privInfo = PrivateUserInfo(
                                    userId = viewModel.currentUserId,
                                    email = initialEmail,
                                    mobileNumber = mobileNumber.trim(),
                                    dateOfBirth = "$birthYearText-01-01"
                                )
                                viewModel.saveProfile(finalProfile, privInfo, isNewProfile = true) {
                                    onCompleted()
                                }
                            }
                        },
                        modifier = Modifier.testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NearMePrimaryBlue)
                    ) {
                        Text(if (currentStep == totalSteps) "Enter NearMe" else "Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NearMeTextMain)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun stepTitle(step: Int): String = when (step) {
    1 -> "Basic Info"
    2 -> "Profile Picture"
    3 -> "Education"
    4 -> "Profession"
    5 -> "Interests"
    6 -> "Intentions"
    7 -> "Location"
    8 -> "Privacy"
    else -> "Complete Profile"
}
