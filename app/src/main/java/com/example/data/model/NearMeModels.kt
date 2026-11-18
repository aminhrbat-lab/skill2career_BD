package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val age: Int = 18,
    val gender: String = "",
    val bio: String = "",
    val aboutMe: String = "",
    val education: String = "",
    val institution: String = "",
    val profession: String = "",
    val company: String = "",
    val cityLabel: String = "",
    val approxLat: Double? = null,
    val approxLng: Double? = null,
    val locationSharingEnabled: Boolean = false,
    val invisibleMode: Boolean = false,
    val discoveryRadiusKm: Double = 10.0,
    val languages: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val relationshipStatus: String = "Single",
    val intentions: List<String> = emptyList(),
    val profileTheme: String = "Electric Navy",
    val avatarColorHex: String = "#2563EB",
    val photoLabels: List<String> = emptyList(),
    val featuredLabels: List<String> = emptyList(),
    val verificationStatus: String = "unverified",
    val accountStatus: String = "active",
    val badges: List<String> = emptyList(),
    val onlineStatus: Boolean = true,
    val hideOnlineStatus: Boolean = false,
    val hideLastSeen: Boolean = false,
    val readReceiptsEnabled: Boolean = true,
    val typingIndicatorEnabled: Boolean = true,
    val showAge: Boolean = true,
    val showEducation: Boolean = true,
    val showProfession: Boolean = true,
    val showDistance: Boolean = true,
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateUserInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val dateOfBirth: String = "",
    val permanentAddress: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventType: String = "",
    val description: String = "",
    val deviceInfo: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Connection(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val participantUids: List<String> = emptyList(),
    val senderName: String = "",
    val receiverName: String = "",
    val introMessage: String = "",
    val status: String = "pending",
    val blockedBy: String = "",
    val interestScore: Int = 0,
    val lifestyleScore: Int = 0,
    val educationScore: Int = 0,
    val overallScore: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantUids: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = "text",
    val attachmentLabel: String = "",
    val reaction: String = "",
    val unsent: Boolean = false,
    val readByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Chatroom(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val name: String = "",
    val description: String = "",
    val roomType: String = "public",
    val joinOption: String = "anyone",
    val passcodeHash: String = "",
    val category: String = "General",
    val cityLabel: String = "",
    val memberUids: List<String> = emptyList(),
    val moderatorUids: List<String> = emptyList(),
    val blockedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatroomMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val publicRoom: Boolean = true,
    val allowedUids: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = "text",
    val safetyFlag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val mediaType: String = "text",
    val mediaLabel: String = "",
    val cityLabel: String = "",
    val visibility: String = "public",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val likedByUids: List<String> = emptyList(),
    val savedByUids: List<String> = emptyList(),
    val safetyStatus: String = "clean",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postAuthorId: String = "",
    val postVisibility: String = "public",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val caption: String = "",
    val mediaType: String = "text",
    val accentColor: String = "#2563EB",
    val visibility: String = "public",
    val highlight: Boolean = false,
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialEvent(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val title: String = "",
    val description: String = "",
    val eventType: String = "coffee",
    val cityLabel: String = "",
    val approxLat: Double? = null,
    val approxLng: Double? = null,
    val eventDateText: String = "",
    val verified: Boolean = false,
    val attendeeUids: List<String> = emptyList(),
    val pendingUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialCircle(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val circleType: String = "close_friends",
    val description: String = "",
    val rulesText: String = "",
    val memberUids: List<String> = emptyList(),
    val moderatorUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val eventType: String = "general",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VerificationRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val method: String = "selfie",
    val documentSummary: String = "",
    val status: String = "pending",
    val reviewerId: String = "",
    val reviewNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "user",
    val targetId: String = "",
    val reason: String = "",
    val aiRiskSummary: String = "",
    val status: String = "open",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VirtualGift(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val giftType: String = "star",
    val message: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformSettings(
    @DocumentId val id: String = "",
    val registrationEnabled: Boolean = true,
    val gpsDiscoveryEnabled: Boolean = true,
    val chatroomsEnabled: Boolean = true,
    val photoUploadLimit: Int = 20,
    val dailyConnectionLimit: Int = 25,
    val premiumEnabled: Boolean = false,
    val adsEnabled: Boolean = false,
    val minRegistrationAge: Int = 18,
    val announcement: String = "",
    val maintenanceMode: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRoleRecord(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val email: String = "",
    val role: String = "ADMIN",
    val assignedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val action: String = "",
    val targetId: String = "",
    val previousState: String = "",
    val newState: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CompatibilityResult(
    val targetUserId: String,
    val interestScore: Int,
    val lifestyleScore: Int,
    val educationScore: Int,
    val overallScore: Int,
    val summary: String,
    val icebreaker: String
)

data class AiRiskAnalysis(
    val riskLevel: String,
    val summary: String,
    val recommendedAction: String
)
