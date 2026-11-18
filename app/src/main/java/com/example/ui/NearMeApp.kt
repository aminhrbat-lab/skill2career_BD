package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.UiState
import com.example.data.model.PlatformSettings
import com.example.data.repository.AdminRepository
import com.example.data.repository.AiRepository
import com.example.data.repository.AuthAndProfileRepository
import com.example.data.repository.CommunityRepository
import com.example.data.repository.SocialGraphRepository
import com.example.ui.admin.AdminCenterScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.OnboardingScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.GlassCard
import com.example.ui.discover.DiscoverScreen
import com.example.ui.home.HomeScreen
import com.example.ui.map.NearbyMapScreen
import com.example.ui.messages.MessagesAndCommunitiesScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.profile.MyProfileScreen
import com.example.ui.theme.NearMeBackground
import com.example.ui.theme.NearMeDanger
import com.example.ui.theme.NearMeElectricBlue
import com.example.ui.theme.NearMePrimaryBlue
import com.example.ui.theme.NearMeSecondaryBg
import com.example.ui.theme.NearMeSurfaceElevated
import com.example.ui.theme.NearMeTextMain
import com.example.ui.theme.NearMeTextSecondary
import com.example.ui.theme.NearMeWarning
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private data class NavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val mainTabs = listOf(
    NavTab("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavTab("Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
    NavTab("Nearby", Icons.Filled.Map, Icons.Outlined.Map, "nav_map"),
    NavTab("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_messages"),
    NavTab("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_notifications"),
    NavTab("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun NearMeApp() {
    val authUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = authUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
        return
    }

    key(user.uid) {
        val context = LocalContext.current
        val factory = remember(user.uid, context) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NearMeViewModel(
                        currentUserId = user.uid,
                        currentUserEmail = user.email.orEmpty(),
                        isEmailVerified = user.isEmailVerified,
                        profileRepo = AuthAndProfileRepository(context),
                        socialRepo = SocialGraphRepository(context),
                        communityRepo = CommunityRepository(context),
                        adminRepo = AdminRepository(context),
                        aiRepo = AiRepository()
                    ) as T
                }
            }
        }
        val nearMeVm: NearMeViewModel = viewModel(key = user.uid, factory = factory)
        AuthenticatedNearMeShell(viewModel = nearMeVm, firebaseUser = user)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedNearMeShell(
    viewModel: NearMeViewModel,
    firebaseUser: FirebaseUser
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val platformState by viewModel.platformSettingsState.collectAsStateWithLifecycle()
    val adminRoleState by viewModel.myAdminRoleState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        val msg = snackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    val platformSettings = (platformState as? UiState.Success)?.data ?: PlatformSettings()
    val adminRoleRecord = (adminRoleState as? UiState.Success)?.data
    val hasAdminAccess = viewModel.isSuperAdminEmail || adminRoleRecord != null
    val adminRoleLabel = when {
        viewModel.isSuperAdminEmail -> "SUPER_ADMIN"
        adminRoleRecord != null -> adminRoleRecord.role
        else -> "MEMBER"
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var messagesSubTab by remember { mutableIntStateOf(0) }
    var inAdminCenter by remember { mutableStateOf(false) }

    val unreadAlerts = (notificationsState as? UiState.Success)?.data?.count { !it.read } ?: 0

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(NearMeBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NearMeElectricBlue)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading your NearMe profile...", color = NearMeTextSecondary)
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().background(NearMeBackground).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Connection Issue", style = MaterialTheme.typography.titleLarge, color = NearMeDanger)
                    Spacer(Modifier.height(8.dp))
                    Text(pState.message, style = MaterialTheme.typography.bodyMedium, color = NearMeTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { signOutUser(credentialManager, scope) }) {
                        Text("Sign Out")
                    }
                }
            }
        }

        is UiState.Success -> {
            val myProfile = pState.data

            if (inAdminCenter && hasAdminAccess) {
                Scaffold(
                    containerColor = NearMeBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        AdminCenterScreen(
                            viewModel = viewModel,
                            adminRoleLabel = adminRoleLabel,
                            platformSettings = platformSettings,
                            onBack = { inAdminCenter = false }
                        )
                    }
                }
                return
            }

            if (myProfile == null || !myProfile.onboardingCompleted) {
                if (!platformSettings.registrationEnabled && myProfile == null && !hasAdminAccess) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(NearMeBackground).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Registration Paused", style = MaterialTheme.typography.titleLarge, color = NearMeWarning)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "New account onboarding is temporarily paused by NearMe administrators.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NearMeTextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { signOutUser(credentialManager, scope) }) {
                                Text("Sign Out")
                            }
                        }
                    }
                } else {
                    OnboardingScreen(
                        viewModel = viewModel,
                        initialEmail = firebaseUser.email.orEmpty(),
                        initialDisplayName = firebaseUser.displayName.orEmpty(),
                        onCompleted = { selectedTab = 0 }
                    )
                }
                return
            }

            if (myProfile.accountStatus in listOf("suspended", "banned")) {
                Box(
                    modifier = Modifier.fillMaxSize().background(NearMeBackground).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Account ${myProfile.accountStatus.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.titleLarge,
                            color = NearMeDanger
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your NearMe account is currently ${myProfile.accountStatus}. Contact NearMe Support if you believe this was in error.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NearMeTextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { signOutUser(credentialManager, scope) }) {
                            Text("Sign Out")
                        }
                    }
                }
                return
            }

            if (platformSettings.maintenanceMode && !hasAdminAccess) {
                Box(
                    modifier = Modifier.fillMaxSize().background(NearMeBackground).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Scheduled Maintenance", style = MaterialTheme.typography.titleLarge, color = NearMeWarning)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            platformSettings.announcement.ifBlank { "NearMe is undergoing brief maintenance and will be right back." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = NearMeTextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { signOutUser(credentialManager, scope) }) {
                            Text("Sign Out")
                        }
                    }
                }
                return
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(NearMeBackground)) {
                val useNavigationRail = maxWidth >= 600.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = NearMeSecondaryBg,
                            contentColor = NearMeTextMain,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.height(16.dp))
                            mainTabs.forEachIndexed { idx, tab ->
                                val selected = selectedTab == idx
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTab = idx },
                                    modifier = Modifier.testTag(tab.testTag),
                                    icon = {
                                        if (idx == 4 && unreadAlerts > 0) {
                                            BadgedBox(badge = { Badge { Text("$unreadAlerts") } }) {
                                                Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NearMeElectricBlue,
                                        selectedTextColor = NearMeElectricBlue,
                                        indicatorColor = NearMeSurfaceElevated
                                    )
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = NearMeBackground,
                        contentWindowInsets = WindowInsets.navigationBars,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (selectedTab) {
                                                0 -> "NearMe"
                                                1 -> "Discover"
                                                2 -> "Nearby Map"
                                                3 -> "Messages & Communities"
                                                4 -> "Notifications"
                                                else -> "My Profile"
                                            },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = NearMeTextMain
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = NearMeSecondaryBg
                                ),
                                actions = {
                                    if (myProfile.invisibleMode) {
                                        Surface(
                                            color = NearMeSurfaceElevated,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Text(
                                                "INVISIBLE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NearMeWarning,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = NearMeSecondaryBg,
                                    tonalElevation = 4.dp
                                ) {
                                    mainTabs.forEachIndexed { idx, tab ->
                                        val selected = selectedTab == idx
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { selectedTab = idx },
                                            modifier = Modifier.testTag(tab.testTag),
                                            icon = {
                                                if (idx == 4 && unreadAlerts > 0) {
                                                    BadgedBox(badge = { Badge { Text("$unreadAlerts") } }) {
                                                        Icon(
                                                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                            contentDescription = tab.label,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            },
                                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = NearMeElectricBlue,
                                                selectedTextColor = NearMeElectricBlue,
                                                indicatorColor = NearMePrimaryBlue.copy(alpha = 0.25f),
                                                unselectedIconColor = NearMeTextSecondary,
                                                unselectedTextColor = NearMeTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (platformSettings.announcement.isNotBlank()) {
                                Surface(
                                    color = NearMePrimaryBlue.copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = platformSettings.announcement,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NearMeElectricBlue,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        myProfile = myProfile,
                                        platformSettings = platformSettings,
                                        onNavigateDiscover = { selectedTab = 1 },
                                        onNavigateMap = { selectedTab = 2 },
                                        onNavigateCommunities = {
                                            messagesSubTab = 1
                                            selectedTab = 3
                                        }
                                    )
                                    1 -> DiscoverScreen(
                                        viewModel = viewModel,
                                        myProfile = myProfile,
                                        platformSettings = platformSettings
                                    )
                                    2 -> NearbyMapScreen(
                                        viewModel = viewModel,
                                        myProfile = myProfile,
                                        platformSettings = platformSettings
                                    )
                                    3 -> MessagesAndCommunitiesScreen(
                                        viewModel = viewModel,
                                        myProfile = myProfile,
                                        platformSettings = platformSettings,
                                        initialSubTab = messagesSubTab
                                    )
                                    4 -> NotificationsScreen(viewModel = viewModel)
                                    else -> MyProfileScreen(
                                        viewModel = viewModel,
                                        myProfile = myProfile,
                                        platformSettings = platformSettings,
                                        hasAdminAccess = hasAdminAccess,
                                        onOpenAdminCenter = { inAdminCenter = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
