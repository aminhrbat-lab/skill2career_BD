package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.Chatroom
import com.example.data.model.ChatroomMessage
import com.example.data.model.PostComment
import com.example.data.model.SocialEvent
import com.example.data.model.SocialPost
import com.example.data.model.Story
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

class CommunityRepository(
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

    fun observeFeedPosts(userId: String): Flow<List<SocialPost>> {
        val publicFlow = db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { it.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val myPostsFlow = db.collection("posts")
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { it.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return combine(publicFlow, myPostsFlow) { pub, mine ->
            (pub + mine).distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observePostComments(post: SocialPost, userId: String): Flow<List<PostComment>> {
        val query = if (post.visibility == "public") {
            db.collection("posts").document(post.id)
                .collection("comments")
                .whereEqualTo("postVisibility", "public")
        } else {
            db.collection("posts").document(post.id)
                .collection("comments")
                .whereEqualTo("postAuthorId", userId)
        }
        return query.snapshots().map { snap ->
            snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeActiveStories(userId: String): Flow<List<Story>> {
        val publicFlow = db.collection("stories")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { it.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val myFlow = db.collection("stories")
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { it.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return combine(publicFlow, myFlow) { pub, mine ->
            val now = System.currentTimeMillis()
            (pub + mine).distinctBy { it.id }
                .filter { it.highlight || it.expiresAtMillis > now }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeChatrooms(userId: String): Flow<List<Chatroom>> {
        val publicRoomsFlow = db.collection("chatrooms")
            .whereIn("roomType", listOf("public", "interest", "location"))
            .snapshots()
            .map { it.toObjects(Chatroom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val joinedRoomsFlow = db.collection("chatrooms")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { it.toObjects(Chatroom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return combine(publicRoomsFlow, joinedRoomsFlow) { pub, joined ->
            (pub + joined).distinctBy { it.id }
                .sortedByDescending { it.memberUids.size }
        }
    }

    fun observeChatroomMessages(room: Chatroom, userId: String): Flow<List<ChatroomMessage>> {
        val isPublic = room.roomType in listOf("public", "interest", "location")
        val query = if (isPublic) {
            db.collection("chatrooms").document(room.id)
                .collection("messages")
                .whereEqualTo("publicRoom", true)
        } else {
            db.collection("chatrooms").document(room.id)
                .collection("messages")
                .whereArrayContains("allowedUids", userId)
        }
        return query.snapshots().map { snap ->
            snap.toObjects(ChatroomMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeEvents(userId: String): Flow<List<SocialEvent>> {
        val verifiedFlow = db.collection("events")
            .whereEqualTo("verified", true)
            .snapshots()
            .map { it.toObjects(SocialEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val myOrganizedFlow = db.collection("events")
            .whereEqualTo("organizerId", userId)
            .snapshots()
            .map { it.toObjects(SocialEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val joinedFlow = db.collection("events")
            .whereArrayContains("attendeeUids", userId)
            .snapshots()
            .map { it.toObjects(SocialEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return combine(verifiedFlow, myOrganizedFlow, joinedFlow) { verified, mine, joined ->
            (verified + mine + joined).distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun createPost(
        authorName: String,
        content: String,
        mediaType: String,
        mediaLabel: String,
        cityLabel: String,
        visibility: String
    ): Result<Unit> {
        val docPath = "posts"
        return try {
            val uid = requireUserId()
            require(content.trim().isNotEmpty()) { "Post content cannot be empty." }
            db.collection("posts").document().set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.ifBlank { "Member" }.take(100),
                    "content" to content.trim().take(2000),
                    "mediaType" to mediaType,
                    "mediaLabel" to mediaLabel.take(200),
                    "cityLabel" to cityLabel.take(80),
                    "visibility" to visibility,
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "likedByUids" to emptyList<String>(),
                    "savedByUids" to emptyList<String>(),
                    "safetyStatus" to "clean",
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

    suspend fun toggleLikePost(post: SocialPost): Result<Unit> {
        val docPath = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val alreadyLiked = uid in post.likedByUids
            val newLikes = if (alreadyLiked) post.likedByUids - uid else (post.likedByUids + uid).distinct().take(500)
            db.collection("posts").document(post.id).update(
                mapOf(
                    "likedByUids" to newLikes,
                    "likesCount" to newLikes.size,
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

    suspend fun toggleSavePost(post: SocialPost): Result<Unit> {
        val docPath = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val alreadySaved = uid in post.savedByUids
            val newSaved = if (alreadySaved) post.savedByUids - uid else (post.savedByUids + uid).distinct().take(500)
            db.collection("posts").document(post.id).update(
                mapOf(
                    "savedByUids" to newSaved,
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

    suspend fun addComment(post: SocialPost, authorName: String, text: String): Result<Unit> {
        val docPath = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            require(text.trim().isNotEmpty()) { "Comment cannot be empty." }
            db.collection("posts").document(post.id).collection("comments").document().set(
                mapOf(
                    "postId" to post.id,
                    "postAuthorId" to post.authorId,
                    "postVisibility" to post.visibility,
                    "authorId" to uid,
                    "authorName" to authorName.ifBlank { "Member" }.take(100),
                    "text" to text.trim().take(600),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to (post.commentsCount + 1),
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

    suspend fun createStory(
        authorName: String,
        caption: String,
        mediaType: String,
        accentColor: String,
        visibility: String,
        highlight: Boolean
    ): Result<Unit> {
        val docPath = "stories"
        return try {
            val uid = requireUserId()
            require(caption.trim().isNotEmpty()) { "Story caption cannot be empty." }
            val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            db.collection("stories").document().set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.ifBlank { "Member" }.take(100),
                    "caption" to caption.trim().take(300),
                    "mediaType" to mediaType,
                    "accentColor" to accentColor,
                    "visibility" to visibility,
                    "highlight" to highlight,
                    "expiresAtMillis" to expiresAt,
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

    suspend fun createChatroom(
        name: String,
        description: String,
        roomType: String,
        joinOption: String,
        passcode: String,
        category: String,
        cityLabel: String
    ): Result<Unit> {
        val docPath = "chatrooms"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Chatroom name must be at least 2 characters." }
            db.collection("chatrooms").document().set(
                mapOf(
                    "creatorId" to uid,
                    "name" to name.trim().take(80),
                    "description" to description.trim().take(400),
                    "roomType" to roomType,
                    "joinOption" to joinOption,
                    "passcodeHash" to passcode.trim().take(128),
                    "category" to category.ifBlank { "General" }.take(60),
                    "cityLabel" to cityLabel.take(80),
                    "memberUids" to listOf(uid),
                    "moderatorUids" to listOf(uid),
                    "blockedUids" to emptyList<String>(),
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

    suspend fun joinChatroom(room: Chatroom): Result<Unit> {
        val docPath = "chatrooms/${room.id}"
        return try {
            val uid = requireUserId()
            if (uid in room.blockedUids) {
                return Result.failure(IllegalStateException("You are blocked from this chatroom."))
            }
            if (uid in room.memberUids) return Result.success(Unit)
            db.collection("chatrooms").document(room.id).update(
                mapOf(
                    "memberUids" to (room.memberUids + uid).distinct().take(200),
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

    suspend fun sendChatroomMessage(
        room: Chatroom,
        senderName: String,
        text: String,
        messageType: String = "text"
    ): Result<Unit> {
        val docPath = "chatrooms/${room.id}/messages"
        return try {
            val uid = requireUserId()
            require(text.trim().isNotEmpty()) { "Message cannot be empty." }
            val isPublic = room.roomType in listOf("public", "interest", "location")
            db.collection("chatrooms").document(room.id).collection("messages").document().set(
                mapOf(
                    "roomId" to room.id,
                    "senderId" to uid,
                    "senderName" to senderName.ifBlank { "Member" }.take(100),
                    "publicRoom" to isPublic,
                    "allowedUids" to room.memberUids.take(200),
                    "text" to text.trim().take(1000),
                    "messageType" to messageType,
                    "safetyFlag" to "",
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

    suspend fun createEvent(
        organizerName: String,
        title: String,
        description: String,
        eventType: String,
        cityLabel: String,
        approxLat: Double?,
        approxLng: Double?,
        eventDateText: String
    ): Result<Unit> {
        val docPath = "events"
        return try {
            val uid = requireUserId()
            require(title.trim().length >= 2) { "Event title must be at least 2 characters." }
            val payload = mutableMapOf<String, Any?>(
                "organizerId" to uid,
                "organizerName" to organizerName.ifBlank { "Member" }.take(100),
                "title" to title.trim().take(120),
                "description" to description.trim().take(800),
                "eventType" to eventType,
                "cityLabel" to cityLabel.ifBlank { "Nearby" }.take(100),
                "approxLat" to approxLat,
                "approxLng" to approxLng,
                "eventDateText" to eventDateText.ifBlank { "Upcoming Weekend" }.take(80),
                "verified" to false,
                "attendeeUids" to listOf(uid),
                "pendingUids" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("events").document().set(payload.filterValues { it != null }).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinEvent(event: SocialEvent): Result<Unit> {
        val docPath = "events/${event.id}"
        return try {
            val uid = requireUserId()
            if (uid in event.attendeeUids) return Result.success(Unit)
            db.collection("events").document(event.id).update(
                mapOf(
                    "attendeeUids" to (event.attendeeUids + uid).distinct().take(200),
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
