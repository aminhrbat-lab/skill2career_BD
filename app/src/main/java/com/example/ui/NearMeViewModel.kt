package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AiTaskState
import com.example.core.UiState
import com.example.data.model.ActivityLog
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRoleRecord
import com.example.data.model.AiRiskAnalysis
import com.example.data.model.AppNotification
import com.example.data.model.Chatroom
import com.example.data.model.ChatroomMessage
import com.example.data.model.CompatibilityResult
import com.example.data.model.Connection
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.ModerationReport
import com.example.data.model.PlatformSettings
import com.example.data.model.PostComment
import com.example.data.model.PrivateUserInfo
import com.example.data.model.PublicProfile
import com.example.data.model.SocialCircle
import com.example.data.model.SocialEvent
import com.example.data.model.SocialPost
import com.example.data.model.Story
import com.example.data.model.VerificationRequest
import com.example.data.model.VirtualGift
import com.example.data.repository.AdminRepository
import com.example.data.repository.AiRepository
import com.example.data.repository.AuthAndProfileRepository
import com.example.data.repository.CommunityRepository
import com.example.data.repository.SocialGraphRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "NearMeVM"
private const val STOP_TIMEOUT_MS = 5000L

class NearMeViewModel(
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    private val profileRepo: AuthAndProfileRepository,
    private val socialRepo: SocialGraphRepository,
    private val communityRepo: CommunityRepository,
    private val adminRepo: AdminRepository,
    private val aiRepo: AiRepository
) : ViewModel() {

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    private fun showSnack(msg: String) {
        _snackMessage.value = msg
    }

    private fun <T> Flow<T>.toUiStateFlow(
        label: String,
        initial: UiState<T> = UiState.Loading
    ): StateFlow<UiState<T>> {
        return this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = initial
            )
    }

    val myProfileState: StateFlow<UiState<PublicProfile?>> =
        profileRepo.observeMyProfile(currentUserId).toUiStateFlow("my profile")

    val privateInfoState: StateFlow<UiState<PrivateUserInfo?>> =
        profileRepo.observePrivateInfo(currentUserId).toUiStateFlow("private info")

    val activityLogsState: StateFlow<UiState<List<ActivityLog>>> =
        profileRepo.observeActivityLogs(currentUserId).toUiStateFlow("activity logs")

    val discoverableProfilesState: StateFlow<UiState<List<PublicProfile>>> =
        profileRepo.observeDiscoverableProfiles().toUiStateFlow("nearby profiles")

    val connectionsState: StateFlow<UiState<List<Connection>>> =
        socialRepo.observeMyConnections(currentUserId).toUiStateFlow("connections")

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        socialRepo.observeMyConversations(currentUserId).toUiStateFlow("conversations")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        socialRepo.observeMyNotifications(currentUserId).toUiStateFlow("notifications")

    val circlesState: StateFlow<UiState<List<SocialCircle>>> =
        socialRepo.observeMyCircles(currentUserId).toUiStateFlow("circles")

    val myVerificationsState: StateFlow<UiState<List<VerificationRequest>>> =
        socialRepo.observeMyVerifications(currentUserId).toUiStateFlow("verifications")

    val receivedGiftsState: StateFlow<UiState<List<VirtualGift>>> =
        socialRepo.observeReceivedGifts(currentUserId).toUiStateFlow("virtual gifts")

    val feedPostsState: StateFlow<UiState<List<SocialPost>>> =
        communityRepo.observeFeedPosts(currentUserId).toUiStateFlow("social feed")

    val storiesState: StateFlow<UiState<List<Story>>> =
        communityRepo.observeActiveStories(currentUserId).toUiStateFlow("stories")

    val chatroomsState: StateFlow<UiState<List<Chatroom>>> =
        communityRepo.observeChatrooms(currentUserId).toUiStateFlow("chatrooms")

    val eventsState: StateFlow<UiState<List<SocialEvent>>> =
        communityRepo.observeEvents(currentUserId).toUiStateFlow("events")

    val platformSettingsState: StateFlow<UiState<PlatformSettings>> =
        adminRepo.observePlatformSettings().toUiStateFlow("platform settings")

    val myAdminRoleState: StateFlow<UiState<AdminRoleRecord?>> =
        adminRepo.observeMyAdminRole(currentUserId).toUiStateFlow("admin role")

    val isSuperAdminEmail: Boolean
        get() = isEmailVerified && currentUserEmail.equals("aminhrbat@gmail.com", ignoreCase = true)

    // Admin streams (collected only inside AdminCenterScreen when authorized)
    val adminUsersState: StateFlow<UiState<List<PublicProfile>>> =
        adminRepo.observeAllUsersForAdmin().toUiStateFlow("admin users")

    val adminReportsState: StateFlow<UiState<List<ModerationReport>>> =
        adminRepo.observeAllReports().toUiStateFlow("admin reports")

    val adminVerificationsState: StateFlow<UiState<List<VerificationRequest>>> =
        adminRepo.observeAllVerifications().toUiStateFlow("admin verifications")

    val adminEventsState: StateFlow<UiState<List<SocialEvent>>> =
        adminRepo.observeAllEventsForAdmin().toUiStateFlow("admin events")

    val adminAuditLogsState: StateFlow<UiState<List<AdminAuditLog>>> =
        adminRepo.observeAuditLogs().toUiStateFlow("admin audit logs")

    // AI Task States
    private val _aiBiosState = MutableStateFlow<AiTaskState<List<String>>>(AiTaskState.Idle)
    val aiBiosState: StateFlow<AiTaskState<List<String>>> = _aiBiosState.asStateFlow()

    private val _aiCompatibilityState = MutableStateFlow<AiTaskState<CompatibilityResult>>(AiTaskState.Idle)
    val aiCompatibilityState: StateFlow<AiTaskState<CompatibilityResult>> = _aiCompatibilityState.asStateFlow()

    private val _aiConversationHelperState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val aiConversationHelperState: StateFlow<AiTaskState<String>> = _aiConversationHelperState.asStateFlow()

    private val _aiRiskState = MutableStateFlow<AiTaskState<AiRiskAnalysis>>(AiTaskState.Idle)
    val aiRiskState: StateFlow<AiTaskState<AiRiskAnalysis>> = _aiRiskState.asStateFlow()

    fun resetAiCompatibility() {
        _aiCompatibilityState.value = AiTaskState.Idle
    }

    fun resetAiConversationHelper() {
        _aiConversationHelperState.value = AiTaskState.Idle
    }

    fun resetAiRisk() {
        _aiRiskState.value = AiTaskState.Idle
    }

    // --- Profile & Onboarding ---
    fun saveProfile(
        profile: PublicProfile,
        privateInfo: PrivateUserInfo?,
        isNewProfile: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = profileRepo.saveInitialOrFullProfile(profile, privateInfo, isNewProfile)
            result.onSuccess {
                profileRepo.logActivity(
                    if (isNewProfile) "profile_created" else "profile_updated",
                    if (isNewProfile) "Completed NearMe registration & onboarding" else "Updated profile details"
                )
                showSnack(if (isNewProfile) "Welcome to NearMe!" else "Profile saved")
                onComplete()
            }.onFailure { err ->
                showSnack(err.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun updateLocationPrivacy(
        sharingEnabled: Boolean,
        invisibleMode: Boolean,
        radiusKm: Double,
        approxLat: Double?,
        approxLng: Double?,
        cityLabel: String?
    ) {
        viewModelScope.launch {
            profileRepo.updateLocationPrivacy(
                locationSharingEnabled = sharingEnabled,
                invisibleMode = invisibleMode,
                discoveryRadiusKm = radiusKm,
                approxLat = approxLat,
                approxLng = approxLng,
                cityLabel = cityLabel
            ).onSuccess {
                showSnack(
                    when {
                        invisibleMode -> "Invisible Mode enabled — hidden from map & discovery"
                        sharingEnabled -> "Approximate location sharing active (${radiusKm} km)"
                        else -> "Location sharing paused"
                    }
                )
            }.onFailure { err ->
                showSnack(err.localizedMessage ?: "Failed to update location settings")
            }
        }
    }

    fun updateChatPrivacy(
        hideOnlineStatus: Boolean,
        hideLastSeen: Boolean,
        readReceiptsEnabled: Boolean,
        typingIndicatorEnabled: Boolean
    ) {
        viewModelScope.launch {
            profileRepo.updateChatPrivacySettings(
                hideOnlineStatus, hideLastSeen, readReceiptsEnabled, typingIndicatorEnabled
            ).onSuccess {
                showSnack("Chat privacy updated")
            }.onFailure { err ->
                showSnack(err.localizedMessage ?: "Failed to update chat privacy")
            }
        }
    }

    fun logProfileView(viewedUserName: String) {
        viewModelScope.launch {
            profileRepo.logActivity("profile_view", "Viewed profile of $viewedUserName")
        }
    }

    fun deactivateAccount() {
        viewModelScope.launch {
            profileRepo.updateAccountStatusSelf("deactivated")
                .onSuccess { showSnack("Account deactivated. You can reactivate anytime.") }
                .onFailure { showSnack(it.localizedMessage ?: "Deactivation failed") }
        }
    }

    fun exportPrivacyJson(
        profile: PublicProfile?,
        privateInfo: PrivateUserInfo?,
        logs: List<ActivityLog>
    ): String {
        viewModelScope.launch {
            profileRepo.logActivity("data_export", "Exported personal data and Privacy Report")
        }
        return profileRepo.buildPrivacyExportJson(profile, privateInfo, logs)
    }

    // --- AI Features (Firebase AI Logic) ---
    fun generateAiBios(
        fullName: String,
        profession: String,
        education: String,
        interests: List<String>,
        intentions: List<String>,
        city: String
    ) {
        viewModelScope.launch {
            _aiBiosState.value = AiTaskState.Loading
            aiRepo.generateProfileBios(fullName, profession, education, interests, intentions, city)
                .onSuccess { _aiBiosState.value = AiTaskState.Success(it) }
                .onFailure { _aiBiosState.value = AiTaskState.Error(it.localizedMessage ?: "AI Bio generation failed") }
        }
    }

    fun computeAiCompatibility(me: PublicProfile, other: PublicProfile) {
        viewModelScope.launch {
            _aiCompatibilityState.value = AiTaskState.Loading
            aiRepo.computeCompatibility(me, other)
                .onSuccess { _aiCompatibilityState.value = AiTaskState.Success(it) }
                .onFailure { _aiCompatibilityState.value = AiTaskState.Error(it.localizedMessage ?: "Compatibility analysis failed") }
        }
    }

    fun runConversationHelper(myName: String, targetName: String, draft: String) {
        viewModelScope.launch {
            _aiConversationHelperState.value = AiTaskState.Loading
            aiRepo.suggestConversationStarter(myName, targetName, draft)
                .onSuccess { _aiConversationHelperState.value = AiTaskState.Success(it) }
                .onFailure { _aiConversationHelperState.value = AiTaskState.Error(it.localizedMessage ?: "AI assistant failed") }
        }
    }

    fun analyzeReportRiskWithAi(report: ModerationReport) {
        viewModelScope.launch {
            _aiRiskState.value = AiTaskState.Loading
            aiRepo.analyzeReportRisk(report.targetType, report.reason, "Target ID: ${report.targetId}")
                .onSuccess { _aiRiskState.value = AiTaskState.Success(it) }
                .onFailure { _aiRiskState.value = AiTaskState.Error(it.localizedMessage ?: "AI risk scan failed") }
        }
    }

    // --- Connections & Messaging ---
    fun sendConnectionRequest(
        sender: PublicProfile,
        receiver: PublicProfile,
        introMessage: String,
        compatibility: CompatibilityResult? = null
    ) {
        viewModelScope.launch {
            socialRepo.sendConnectionRequest(sender, receiver, introMessage, compatibility)
                .onSuccess { showSnack("Connection request sent to ${receiver.fullName}") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to send request") }
        }
    }

    fun respondToConnection(connection: Connection, newStatus: String) {
        viewModelScope.launch {
            socialRepo.respondToConnection(connection, newStatus)
                .onSuccess { showSnack("Connection $newStatus") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to update connection") }
        }
    }

    fun blockUser(myProfile: PublicProfile, target: PublicProfile) {
        viewModelScope.launch {
            socialRepo.blockUser(myProfile, target)
                .onSuccess { showSnack("User blocked. Your identity is kept private.") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to block user") }
        }
    }

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> =
        socialRepo.observeMessages(conversationId, currentUserId)

    fun sendDirectMessage(
        conversation: Conversation,
        senderName: String,
        text: String,
        messageType: String = "text",
        attachmentLabel: String = ""
    ) {
        viewModelScope.launch {
            socialRepo.sendDirectMessage(conversation, senderName, text, messageType, attachmentLabel)
                .onFailure { showSnack(it.localizedMessage ?: "Failed to send message") }
        }
    }

    fun unsendDirectMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            socialRepo.unsendDirectMessage(conversationId, messageId)
                .onSuccess { showSnack("Message unsent") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to unsend") }
        }
    }

    fun reactToDirectMessage(conversationId: String, messageId: String, reaction: String) {
        viewModelScope.launch {
            socialRepo.reactToDirectMessage(conversationId, messageId, reaction)
        }
    }

    fun deleteDirectMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            socialRepo.deleteDirectMessage(conversationId, messageId)
                .onSuccess { showSnack("Message deleted") }
        }
    }

    fun openOrStartConversation(myName: String, target: PublicProfile, onReady: (String) -> Unit) {
        viewModelScope.launch {
            socialRepo.ensureConversation(currentUserId, myName, target.userId, target.fullName)
                .onSuccess { convId -> onReady(convId) }
                .onFailure { showSnack(it.localizedMessage ?: "Unable to open chat") }
        }
    }

    // --- Social Feed, Stories, Chatrooms, Circles, Events ---
    fun createPost(
        authorName: String,
        content: String,
        mediaType: String,
        mediaLabel: String,
        cityLabel: String,
        visibility: String
    ) {
        viewModelScope.launch {
            communityRepo.createPost(authorName, content, mediaType, mediaLabel, cityLabel, visibility)
                .onSuccess { showSnack("Post published!") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to publish post") }
        }
    }

    fun toggleLikePost(post: SocialPost) {
        viewModelScope.launch {
            communityRepo.toggleLikePost(post)
        }
    }

    fun toggleSavePost(post: SocialPost) {
        viewModelScope.launch {
            communityRepo.toggleSavePost(post)
                .onSuccess { showSnack("Saved posts updated") }
        }
    }

    fun observePostComments(post: SocialPost): Flow<List<PostComment>> =
        communityRepo.observePostComments(post, currentUserId)

    fun addComment(post: SocialPost, authorName: String, text: String) {
        viewModelScope.launch {
            communityRepo.addComment(post, authorName, text)
                .onFailure { showSnack(it.localizedMessage ?: "Failed to add comment") }
        }
    }

    fun createStory(
        authorName: String,
        caption: String,
        accentColor: String,
        visibility: String,
        highlight: Boolean
    ) {
        viewModelScope.launch {
            communityRepo.createStory(authorName, caption, "text", accentColor, visibility, highlight)
                .onSuccess { showSnack("Story added (24h active)") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to post story") }
        }
    }

    fun observeChatroomMessages(room: Chatroom): Flow<List<ChatroomMessage>> =
        communityRepo.observeChatroomMessages(room, currentUserId)

    fun createChatroom(
        name: String,
        description: String,
        roomType: String,
        joinOption: String,
        passcode: String,
        category: String,
        cityLabel: String
    ) {
        viewModelScope.launch {
            communityRepo.createChatroom(name, description, roomType, joinOption, passcode, category, cityLabel)
                .onSuccess { showSnack("Chatroom \"$name\" created") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to create chatroom") }
        }
    }

    fun joinChatroom(room: Chatroom) {
        viewModelScope.launch {
            communityRepo.joinChatroom(room)
                .onSuccess { showSnack("Joined ${room.name}") }
                .onFailure { showSnack(it.localizedMessage ?: "Could not join room") }
        }
    }

    fun sendChatroomMessage(room: Chatroom, senderName: String, text: String, messageType: String = "text") {
        viewModelScope.launch {
            communityRepo.sendChatroomMessage(room, senderName, text, messageType)
                .onFailure { showSnack(it.localizedMessage ?: "Failed to send message") }
        }
    }

    fun createEvent(
        organizerName: String,
        title: String,
        description: String,
        eventType: String,
        cityLabel: String,
        approxLat: Double?,
        approxLng: Double?,
        eventDateText: String
    ) {
        viewModelScope.launch {
            communityRepo.createEvent(organizerName, title, description, eventType, cityLabel, approxLat, approxLng, eventDateText)
                .onSuccess { showSnack("Event created! Pending verification for public map.") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to create event") }
        }
    }

    fun joinEvent(event: SocialEvent) {
        viewModelScope.launch {
            communityRepo.joinEvent(event)
                .onSuccess { showSnack("Joined ${event.title}") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to join event") }
        }
    }

    fun createCircle(name: String, circleType: String, description: String, rulesText: String) {
        viewModelScope.launch {
            socialRepo.createCircle(name, circleType, description, rulesText)
                .onSuccess { showSnack("Circle \"$name\" created") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to create circle") }
        }
    }

    fun joinCircle(circle: SocialCircle) {
        viewModelScope.launch {
            socialRepo.joinCircle(circle)
                .onSuccess { showSnack("Joined ${circle.name}") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to join circle") }
        }
    }

    fun sendVirtualGift(senderName: String, receiverId: String, giftType: String, message: String) {
        viewModelScope.launch {
            socialRepo.sendVirtualGift(senderName, receiverId, giftType, message)
                .onSuccess { showSnack("Virtual $giftType gift sent!") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to send gift") }
        }
    }

    fun submitVerification(fullName: String, method: String, documentSummary: String) {
        viewModelScope.launch {
            socialRepo.submitVerification(fullName, method, documentSummary)
                .onSuccess { showSnack("Verification request submitted") }
                .onFailure { showSnack(it.localizedMessage ?: "Submission failed") }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            socialRepo.submitReport(targetType, targetId, reason)
                .onSuccess { showSnack("Report submitted for safety review") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to submit report") }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            socialRepo.markNotificationRead(notificationId)
        }
    }

    // --- Admin Actions ---
    fun savePlatformSettings(settings: PlatformSettings, reason: String) {
        viewModelScope.launch {
            adminRepo.savePlatformSettings(settings, reason)
                .onSuccess { showSnack("Platform settings updated & audited") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to save settings") }
        }
    }

    fun adminUpdateUserStatus(user: PublicProfile, newStatus: String, reason: String) {
        viewModelScope.launch {
            adminRepo.updateUserAccountStatus(user, newStatus, reason)
                .onSuccess { showSnack("User ${user.fullName} -> $newStatus") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to update user") }
        }
    }

    fun adminAwardBadge(user: PublicProfile, badge: String) {
        viewModelScope.launch {
            adminRepo.awardUserBadge(user, badge)
                .onSuccess { showSnack("Awarded $badge badge") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to award badge") }
        }
    }

    fun adminReviewVerification(request: VerificationRequest, approved: Boolean, note: String) {
        viewModelScope.launch {
            adminRepo.reviewVerification(request, approved, note)
                .onSuccess { showSnack(if (approved) "Verification approved" else "Verification rejected") }
                .onFailure { showSnack(it.localizedMessage ?: "Review failed") }
        }
    }

    fun adminResolveReport(report: ModerationReport, newStatus: String, aiSummary: String, reason: String) {
        viewModelScope.launch {
            adminRepo.resolveReport(report, newStatus, aiSummary, reason)
                .onSuccess { showSnack("Report resolved: $newStatus") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to resolve report") }
        }
    }

    fun adminVerifyEvent(event: SocialEvent, verified: Boolean) {
        viewModelScope.launch {
            adminRepo.verifyEvent(event, verified)
                .onSuccess { showSnack(if (verified) "Event verified for public map" else "Event unverified") }
                .onFailure { showSnack(it.localizedMessage ?: "Failed to update event") }
        }
    }

    fun exportAdminBackup(
        users: List<PublicProfile>,
        reports: List<ModerationReport>,
        logs: List<AdminAuditLog>,
        settings: PlatformSettings
    ): String = adminRepo.generateBackupExportJson(users, reports, logs, settings)
}
