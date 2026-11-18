package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.CompatibilityResult
import com.example.data.model.Connection
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.ModerationReport
import com.example.data.model.PublicProfile
import com.example.data.model.SocialCircle
import com.example.data.model.VerificationRequest
import com.example.data.model.VirtualGift
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialGraphRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observeMyConnections(userId: String): Flow<List<Connection>> =
        db.collection("connections")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Connection::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeMyConversations(userId: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Conversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeMessages(conversationId: String, userId: String): Flow<List<DirectMessage>> =
        db.collection("conversations").document(conversationId)
            .collection("messages")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    DirectMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeMyNotifications(userId: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    AppNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeMyCircles(userId: String): Flow<List<SocialCircle>> {
        val myCirclesFlow = db.collection("circles")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { it.toObjects(SocialCircle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val nearbyGroupsFlow = db.collection("circles")
            .whereEqualTo("circleType", "nearby_group")
            .snapshots()
            .map { it.toObjects(SocialCircle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        return combine(myCirclesFlow, nearbyGroupsFlow) { mine, nearby ->
            (mine + nearby).distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeMyVerifications(userId: String): Flow<List<VerificationRequest>> =
        db.collection("verifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    VerificationRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeReceivedGifts(userId: String): Flow<List<VirtualGift>> =
        db.collection("virtual_gifts")
            .whereEqualTo("receiverId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    VirtualGift::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    private fun deterministicPairId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    suspend fun sendConnectionRequest(
        sender: PublicProfile,
        receiver: PublicProfile,
        introMessage: String,
        compatibility: CompatibilityResult?
    ): Result<Unit> {
        val pairId = deterministicPairId(sender.userId, receiver.userId)
        val docPath = "connections/$pairId"
        return try {
            val uid = requireUserId()
            require(uid == sender.userId) { "Authenticated user mismatch." }
            require(sender.userId != receiver.userId) { "Cannot connect with yourself." }
            val payload = mapOf(
                "senderId" to uid,
                "receiverId" to receiver.userId,
                "participantUids" to listOf(uid, receiver.userId),
                "senderName" to sender.fullName.ifBlank { "NearMe Member" }.take(100),
                "receiverName" to receiver.fullName.ifBlank { "NearMe Member" }.take(100),
                "introMessage" to introMessage.trim().take(300),
                "status" to "pending",
                "interestScore" to (compatibility?.interestScore ?: 80).coerceIn(0, 100),
                "lifestyleScore" to (compatibility?.lifestyleScore ?: 78).coerceIn(0, 100),
                "educationScore" to (compatibility?.educationScore ?: 75).coerceIn(0, 100),
                "overallScore" to (compatibility?.overallScore ?: 78).coerceIn(0, 100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("connections").document(pairId).set(payload).await()
            createSelfNotification(
                title = "Connection Request Sent",
                body = "Your intro was sent to ${receiver.fullName}.",
                eventType = "connection_request"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun respondToConnection(connection: Connection, newStatus: String): Result<Unit> {
        val docPath = "connections/${connection.id}"
        return try {
            val uid = requireUserId()
            require(connection.id.isNotBlank()) { "Connection ID is missing." }
            require(newStatus in listOf("accepted", "declined", "blocked")) { "Invalid status." }
            val updates = mutableMapOf<String, Any?>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == "blocked") {
                updates["blockedBy"] = uid
            }
            db.collection("connections").document(connection.id)
                .update(updates.filterValues { it != null })
                .await()

            if (newStatus == "accepted") {
                ensureConversation(connection.senderId, connection.senderName, connection.receiverId, connection.receiverName)
                createSelfNotification(
                    title = "New Mutual Connection",
                    body = "You connected with ${if (connection.senderId == uid) connection.receiverName else connection.senderName}!",
                    eventType = "connection_accepted"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun blockUser(myProfile: PublicProfile, target: PublicProfile): Result<Unit> {
        val pairId = deterministicPairId(myProfile.userId, target.userId)
        val docPath = "connections/$pairId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("connections").document(pairId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "status" to "blocked",
                        "blockedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                // Create as pending then immediately block or store block in pending-to-blocked flow?
                // Rules require create status == 'pending', so create pending first then update to blocked.
                ref.set(
                    mapOf(
                        "senderId" to uid,
                        "receiverId" to target.userId,
                        "participantUids" to listOf(uid, target.userId),
                        "senderName" to myProfile.fullName.ifBlank { "Member" }.take(100),
                        "receiverName" to target.fullName.ifBlank { "Member" }.take(100),
                        "introMessage" to "",
                        "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                ref.update(
                    mapOf(
                        "status" to "blocked",
                        "blockedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureConversation(
        uidA: String,
        nameA: String,
        uidB: String,
        nameB: String
    ): Result<String> {
        val convId = deterministicPairId(uidA, uidB)
        val docPath = "conversations/$convId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("conversations").document(convId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "participantUids" to listOf(uidA, uidB),
                        "participantNames" to listOf(nameA.ifBlank { "Member" }, nameB.ifBlank { "Member" }),
                        "lastMessage" to "Connected on NearMe! Say hello.",
                        "lastSenderId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        conversation: Conversation,
        senderName: String,
        text: String,
        messageType: String = "text",
        attachmentLabel: String = ""
    ): Result<Unit> {
        val docPath = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            require(text.isNotBlank() || attachmentLabel.isNotBlank()) { "Message cannot be empty." }
            val msgRef = db.collection("conversations").document(conversation.id)
                .collection("messages").document()
            val cleanText = text.trim().take(2000)
            msgRef.set(
                mapOf(
                    "conversationId" to conversation.id,
                    "senderId" to uid,
                    "senderName" to senderName.ifBlank { "Member" }.take(100),
                    "participantUids" to conversation.participantUids,
                    "text" to cleanText,
                    "messageType" to messageType,
                    "attachmentLabel" to attachmentLabel.take(200),
                    "reaction" to "",
                    "unsent" to false,
                    "readByUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val preview = if (cleanText.isNotBlank()) cleanText else "[$messageType] $attachmentLabel"
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "lastMessage" to preview.take(500),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unsendDirectMessage(conversationId: String, messageId: String): Result<Unit> {
        val docPath = "conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "text" to "Message unsent",
                        "unsent" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reactToDirectMessage(
        conversationId: String,
        messageId: String,
        reaction: String
    ): Result<Unit> {
        val docPath = "conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "reaction" to reaction.take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDirectMessage(conversationId: String, messageId: String): Result<Unit> {
        val docPath = "conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendVirtualGift(
        senderName: String,
        receiverId: String,
        giftType: String,
        message: String
    ): Result<Unit> {
        val docPath = "virtual_gifts"
        return try {
            val uid = requireUserId()
            require(uid != receiverId) { "Cannot send a gift to yourself." }
            db.collection("virtual_gifts").document().set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.ifBlank { "Member" }.take(100),
                    "receiverId" to receiverId,
                    "giftType" to giftType,
                    "message" to message.trim().take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createCircle(
        name: String,
        circleType: String,
        description: String,
        rulesText: String
    ): Result<Unit> {
        val docPath = "circles"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Circle name must be at least 2 characters." }
            db.collection("circles").document().set(
                mapOf(
                    "ownerId" to uid,
                    "name" to name.trim().take(80),
                    "circleType" to circleType,
                    "description" to description.trim().take(400),
                    "rulesText" to rulesText.trim().take(600),
                    "memberUids" to listOf(uid),
                    "moderatorUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinCircle(circle: SocialCircle): Result<Unit> {
        val docPath = "circles/${circle.id}"
        return try {
            val uid = requireUserId()
            if (uid in circle.memberUids) return Result.success(Unit)
            db.collection("circles").document(circle.id).update(
                mapOf(
                    "memberUids" to (circle.memberUids + uid).distinct().take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitVerification(
        fullName: String,
        method: String,
        documentSummary: String
    ): Result<Unit> {
        val docPath = "verifications"
        return try {
            val uid = requireUserId()
            db.collection("verifications").document().set(
                mapOf(
                    "userId" to uid,
                    "fullName" to fullName.trim().take(100),
                    "method" to method,
                    "documentSummary" to documentSummary.trim().take(300),
                    "status" to "pending",
                    "reviewerId" to "",
                    "reviewNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "verificationStatus" to "pending",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            createSelfNotification(
                title = "Verification Submitted",
                body = "Your $method verification request is now pending officer review.",
                eventType = "verification_submitted"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String
    ): Result<Unit> {
        val docPath = "reports"
        return try {
            val uid = requireUserId()
            require(reason.trim().length >= 2) { "Please provide a reason for your report." }
            db.collection("reports").document().set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to reason.trim().take(500),
                    "aiRiskSummary" to "",
                    "status" to "open",
                    "reviewedBy" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createSelfNotification(
        title: String,
        body: String,
        eventType: String
    ): Result<Unit> {
        val docPath = "notifications"
        return try {
            val uid = requireUserId()
            db.collection("notifications").document().set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(120),
                    "body" to body.take(400),
                    "eventType" to eventType.take(60),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val docPath = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId).update(
                mapOf(
                    "read" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
