package com.example.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.LocationHelper
import com.example.core.UiState
import com.example.data.model.PlatformSettings
import com.example.data.model.PublicProfile
import com.example.data.model.SocialEvent
import com.example.ui.NearMeViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.ProfileDetailDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.computeDistanceText
import com.example.ui.theme.NearMeAccentPurple
import com.example.ui.theme.NearMeBackground
import com.example.ui.theme.NearMeBorder
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMePrimaryBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSuccess
import com.example.ui.theme.NearMeSurfaceElevated
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary
import com.example.ui.theme.NearMeWarning
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NearbyMapScreen(
    viewModel: NearMeViewModel,
    myProfile: PublicProfile,
    platformSettings: PlatformSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profilesState by viewModel.discoverableProfilesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    var radiusKm by remember(myProfile.discoveryRadiusKm) { mutableFloatStateOf(myProfile.discoveryRadiusKm.toFloat().coerceIn(0.1f, 25f)) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var selectedUser by remember { mutableStateOf<PublicProfile?>(null) }
    var selectedEvent by remember { mutableStateOf<SocialEvent?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val coords = LocationHelper.fetchApproximateLocation(context)
                viewModel.updateLocationPrivacy(
                    sharingEnabled = true,
                    invisibleMode = false,
                    radiusKm = radiusKm.toDouble(),
                    approxLat = coords?.first ?: myProfile.approxLat,
                    approxLng = coords?.second ?: myProfile.approxLng,
                    cityLabel = myProfile.cityLabel
                )
            }
        }
    }

    val allUsers = (profilesState as? UiState.Success)?.data.orEmpty()
    // STRICT PRIVACY: Only show users who explicitly enabled location sharing and are NOT in invisible mode
    val visibleMapUsers = remember(allUsers, myProfile, radiusKm) {
        allUsers.filter { u ->
            u.userId != myProfile.userId &&
                u.locationSharingEnabled &&
                !u.invisibleMode &&
                if (myProfile.approxLat != null && myProfile.approxLng != null && u.approxLat != null && u.approxLng != null) {
                    LocationHelper.distanceMeters(myProfile.approxLat, myProfile.approxLng, u.approxLat, u.approxLng) / 1000.0 <= radiusKm
                } else {
                    true
                }
        }
    }

    // STRICT EVENT POLICY: Only verified events appear on public discovery map
    val verifiedEvents = ((eventsState as? UiState.Success)?.data.orEmpty()).filter { it.verified }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Privacy & Location Control Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nearby Discovery Map", style = MaterialTheme.typography.titleLarge, color = NearMeTextMain)
                    Text(
                        text = when {
                            !platformSettings.gpsDiscoveryEnabled -> "GPS discovery paused by Admin"
                            myProfile.invisibleMode -> "Invisible Mode ON — you are hidden"
                            myProfile.locationSharingEnabled -> "Approximate grid active • ${visibleMapUsers.size} nearby"
                            else -> "Location sharing paused • Tap Enable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (myProfile.invisibleMode) NearMeWarning else NearMeElectricBlue
                    )
                }
                OutlinedButton(
                    onClick = { showInviteDialog = true },
                    modifier = Modifier.testTag("invite_nearme_button")
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Invite")
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (myProfile.locationSharingEnabled && !myProfile.invisibleMode) {
                            // Pause location sharing
                            viewModel.updateLocationPrivacy(
                                sharingEnabled = false,
                                invisibleMode = false,
                                radiusKm = radiusKm.toDouble(),
                                approxLat = null,
                                approxLng = null,
                                cityLabel = null
                            )
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                scope.launch {
                                    val coords = LocationHelper.fetchApproximateLocation(context)
                                    viewModel.updateLocationPrivacy(
                                        sharingEnabled = true,
                                        invisibleMode = false,
                                        radiusKm = radiusKm.toDouble(),
                                        approxLat = coords?.first ?: myProfile.approxLat,
                                        approxLng = coords?.second ?: myProfile.approxLng,
                                        cityLabel = myProfile.cityLabel
                                    )
                                }
                            } else {
                                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("toggle_location_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (myProfile.locationSharingEnabled && !myProfile.invisibleMode) NearMeSurfaceElevated else NearMePrimaryBlue
                    )
                ) {
                    Icon(
                        if (myProfile.locationSharingEnabled && !myProfile.invisibleMode) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (myProfile.locationSharingEnabled && !myProfile.invisibleMode) "Pause Location" else "Share Approx")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NearMeSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = NearMeWarning, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Invisible", style = MaterialTheme.typography.labelMedium, color = NearMeTextMain)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = myProfile.invisibleMode,
                        onCheckedChange = { inv ->
                            viewModel.updateLocationPrivacy(
                                sharingEnabled = if (inv) false else myProfile.locationSharingEnabled,
                                invisibleMode = inv,
                                radiusKm = radiusKm.toDouble(),
                                approxLat = null,
                                approxLng = null,
                                cityLabel = null
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.1f to "100m", 0.5f to "500m", 1f to "1km", 5f to "5km", 10f to "10km", 25f to "25km").forEach { (km, label) ->
                    FilterChip(
                        selected = radiusKm == km,
                        onClick = {
                            radiusKm = km
                            viewModel.updateLocationPrivacy(
                                sharingEnabled = myProfile.locationSharingEnabled,
                                invisibleMode = myProfile.invisibleMode,
                                radiusKm = km.toDouble(),
                                approxLat = null,
                                approxLng = null,
                                cityLabel = null
                            )
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NearMePrimaryBlue)
                    )
                }
            }
        }

        // Interactive Radar Map Canvas (Pan + Zoom)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF132448), NearMeBackground)
                    )
                )
                .border(1.dp, NearMeBorder, RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.7f, 2.5f)
                        panOffset = Offset(
                            x = (panOffset.x + pan.x).coerceIn(-300f, 300f),
                            y = (panOffset.y + pan.y).coerceIn(-300f, 300f)
                        )
                    }
                }
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val center = Offset(widthPx / 2f + panOffset.x, heightPx / 2f + panOffset.y)
            val baseRadius = (minOf(widthPx, heightPx) * 0.40f) * zoomScale

            Canvas(modifier = Modifier.fillMaxSize()) {
                // OSM-style street/grid lines
                val gridStep = 56f * zoomScale
                var gx = (center.x % gridStep)
                while (gx < size.width) {
                    drawLine(NearMeBorder.copy(alpha = 0.55f), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                    gx += gridStep
                }
                var gy = (center.y % gridStep)
                while (gy < size.height) {
                    drawLine(NearMeBorder.copy(alpha = 0.55f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                    gy += gridStep
                }

                // Concentric Radar Rings
                listOf(0.33f, 0.66f, 1.0f).forEach { factor ->
                    drawCircle(
                        color = NearMeElectricBlue.copy(alpha = 0.25f),
                        radius = baseRadius * factor,
                        center = center,
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                    )
                }
                // Crosshairs
                drawLine(NearMePrimaryBlue.copy(alpha = 0.25f), Offset(center.x - baseRadius, center.y), Offset(center.x + baseRadius, center.y), 1.5f)
                drawLine(NearMePrimaryBlue.copy(alpha = 0.25f), Offset(center.x, center.y - baseRadius), Offset(center.x, center.y + baseRadius), 1.5f)
            }

            // Center Me Marker
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((center.x - 24.dp.toPx()).roundToInt(), (center.y - 24.dp.toPx()).roundToInt())
                    }
            ) {
                UserAvatar(
                    name = myProfile.fullName,
                    colorHex = myProfile.avatarColorHex,
                    size = 48.dp,
                    isOnline = !myProfile.invisibleMode,
                    isVerified = myProfile.verificationStatus == "verified"
                )
            }

            // Nearby User Pins positioned deterministically around center by approximate delta or hash
            visibleMapUsers.forEachIndexed { index, user ->
                val angle = (index * 2.1f) + (user.userId.hashCode() % 360) * 0.0174f
                val distFactor = if (myProfile.approxLat != null && myProfile.approxLng != null && user.approxLat != null && user.approxLng != null) {
                    val km = LocationHelper.distanceMeters(myProfile.approxLat, myProfile.approxLng, user.approxLat, user.approxLng) / 1000.0
                    (km / radiusKm.coerceAtLeast(0.5f)).toFloat().coerceIn(0.22f, 0.92f)
                } else {
                    (0.32f + ((index % 5) * 0.14f)).coerceAtMost(0.9f)
                }
                val px = center.x + cos(angle) * baseRadius * distFactor
                val py = center.y + sin(angle) * baseRadius * distFactor

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset {
                            IntOffset((px - 28.dp.toPx()).roundToInt(), (py - 28.dp.toPx()).roundToInt())
                        }
                        .clickable {
                            viewModel.logProfileView(user.fullName)
                            selectedUser = user
                        }
                ) {
                    UserAvatar(
                        name = user.fullName,
                        colorHex = user.avatarColorHex,
                        size = 42.dp,
                        isOnline = user.onlineStatus && !user.hideOnlineStatus,
                        isVerified = user.verificationStatus == "verified"
                    )
                    Surface(
                        color = NearMeSecondaryBg.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = user.fullName.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = NearMeTextMain,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Verified Event Pins
            verifiedEvents.forEachIndexed { index, ev ->
                val angle = 0.8f + index * 1.7f
                val px = center.x + cos(angle) * baseRadius * 0.65f
                val py = center.y + sin(angle) * baseRadius * 0.65f
                Surface(
                    color = NearMeAccentPurple,
                    shape = CircleShape,
                    modifier = Modifier
                        .offset { IntOffset((px - 18.dp.toPx()).roundToInt(), (py - 18.dp.toPx()).roundToInt()) }
                        .size(36.dp)
                        .border(2.dp, NearMeTextMain, CircleShape)
                        .clickable { selectedEvent = ev }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Event, contentDescription = ev.title, tint = NearMeTextMain, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Map Legend Overlay
            Surface(
                color = NearMeSecondaryBg.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Radius: ${if (radiusKm < 1f) "${(radiusKm * 1000).toInt()}m" else "${radiusKm.toInt()}km"} • ${visibleMapUsers.size} people • ${verifiedEvents.size} verified events",
                    style = MaterialTheme.typography.labelSmall,
                    color = NearMeElectricBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (selectedUser != null) {
        val u = selectedUser!!
        ProfileDetailDialog(
            me = myProfile,
            target = u,
            onDismiss = { selectedUser = null },
            onConnect = { intro ->
                viewModel.sendConnectionRequest(myProfile, u, intro, null)
                selectedUser = null
            },
            onAnalyzeCompatibility = {
                viewModel.computeAiCompatibility(myProfile, u)
                selectedUser = null
            },
            onSendGift = { g, msg ->
                viewModel.sendVirtualGift(myProfile.fullName, u.userId, g, msg)
            },
            onBlock = {
                viewModel.blockUser(myProfile, u)
                selectedUser = null
            },
            onReport = { r ->
                viewModel.submitReport("user", u.userId, r)
                selectedUser = null
            }
        )
    }

    if (selectedEvent != null) {
        val ev = selectedEvent!!
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            containerColor = NearMeSecondaryBg,
            title = { Text(ev.title, color = NearMeTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Verified Meetup • ${ev.eventType}", color = NearMeSuccess, style = MaterialTheme.typography.labelMedium)
                    Text("${ev.eventDateText} • ${ev.cityLabel}", color = NearMeElectricBlue, style = MaterialTheme.typography.bodySmall)
                    Text(ev.description, color = NearMeTextMain, style = MaterialTheme.typography.bodyMedium)
                    Text("Attendees: ${ev.attendeeUids.size}", color = NearMeTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.joinEvent(ev); selectedEvent = null }) { Text("Join Event") }
            },
            dismissButton = {
                TextButton(onClick = { selectedEvent = null }) { Text("Close") }
            }
        )
    }

    if (showInviteDialog) {
        val inviteLink = "https://nearme.app/invite/${myProfile.username}"
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = NearMeSecondaryBg,
            title = { Text("Invite People to NearMe", color = NearMeTextMain) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Deterministic QR-style pattern card representing user invite link
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NearMeTextMain)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "NearMe Invite QR", tint = NearMeBackground, modifier = Modifier.size(96.dp))
                    }
                    Text(inviteLink, style = MaterialTheme.typography.labelMedium, color = NearMeElectricBlue)
                    Text(
                        "Invitations are only sent when you explicitly tap Share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NearMeTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join me on NearMe — Discover People & Explore Nearby! $inviteLink")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Invite via"))
                    showInviteDialog = false
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Close") }
            }
        )
    }
}
