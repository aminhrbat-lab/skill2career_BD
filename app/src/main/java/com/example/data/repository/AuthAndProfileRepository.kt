package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ActivityLog
import com.example.data.model.PrivateUserInfo
import com.example.data.model.PublicProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class AuthAndProfileRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing NearMe.")
    }

    fun observeMyProfile(userId: String): Flow<PublicProfile?> =
        db.collection("public_profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PublicProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }

    fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .whereEqualTo("onboardingCompleted", true)
            .whereEqualTo("accountStatus", "active")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observePrivateInfo(userId: String): Flow<PrivateUserInfo?> =
        db.collection("users").document(userId)
            .collection("private").document("info")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PrivateUserInfo::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }

    fun observeActivityLogs(userId: String): Flow<List<ActivityLog>> =
        db.collection("users").document(userId)
            .collection("activity_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ActivityLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun saveInitialOrFullProfile(
        profile: PublicProfile,
        privateInfo: PrivateUserInfo?,
        isNewProfile: Boolean
    ): Result<Unit> {
        var docPath = "public_profiles/${auth.currentUser?.uid ?: "unknown"}"
        return try {
            val uid = requireUserId()
            require(profile.age in 18..120) { "Minimum age is 18 years." }
            require(profile.fullName.isNotBlank()) { "Full name is required." }
            require(profile.username.length >= 2) { "Username must be at least 2 characters." }

            val publicRef = db.collection("public_profiles").document(uid)
            docPath = publicRef.path

            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "fullName" to profile.fullName.trim().take(100),
                "username" to profile.username.trim().lowercase().replace(" ", "_").take(40),
                "age" to profile.age,
                "gender" to profile.gender.ifBlank { "Prefer not to say" }.take(40),
                "bio" to profile.bio.take(500),
                "aboutMe" to profile.aboutMe.take(500),
                "education" to profile.education.take(120),
                "institution" to profile.institution.take(120),
                "profession" to profile.profession.take(120),
                "company" to profile.company.take(120),
                "cityLabel" to profile.cityLabel.take(100),
                "approxLat" to profile.approxLat,
                "approxLng" to profile.approxLng,
                "locationSharingEnabled" to profile.locationSharingEnabled,
                "invisibleMode" to profile.invisibleMode,
                "discoveryRadiusKm" to profile.discoveryRadiusKm.coerceIn(0.1, 200.0),
                "languages" to profile.languages.take(10),
                "hobbies" to profile.hobbies.take(15),
                "interests" to profile.interests.take(15),
                "relationshipStatus" to profile.relationshipStatus.take(40),
                "intentions" to profile.intentions.take(5),
                "profileTheme" to profile.profileTheme.take(40),
                "avatarColorHex" to profile.avatarColorHex.take(16),
                "photoLabels" to profile.photoLabels.take(20),
                "featuredLabels" to profile.featuredLabels.take(3),
                "onlineStatus" to profile.onlineStatus,
                "hideOnlineStatus" to profile.hideOnlineStatus,
                "hideLastSeen" to profile.hideLastSeen,
                "readReceiptsEnabled" to profile.readReceiptsEnabled,
                "typingIndicatorEnabled" to profile.typingIndicatorEnabled,
                "showAge" to profile.showAge,
                "showEducation" to profile.showEducation,
                "showProfession" to profile.showProfession,
                "showDistance" to profile.showDistance,
                "onboardingCompleted" to profile.onboardingCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isNewProfile) {
                payload["verificationStatus"] = "unverified"
                payload["accountStatus"] = "active"
                payload["badges"] = emptyList<String>()
                payload["createdAt"] = FieldValue.serverTimestamp()
                publicRef.set(payload.filterValues { it != null }).await()
            } else {
                publicRef.update(payload.filterValues { it != null }.filterKeys { it != "userId" && it != "createdAt" }).await()
            }

            if (privateInfo != null) {
                val privRef = db.collection("users").document(uid).collection("private").document("info")
                val privMap = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "email" to privateInfo.email.take(200),
                    "mobileNumber" to privateInfo.mobileNumber.take(40),
                    "dateOfBirth" to privateInfo.dateOfBirth.take(30),
                    "permanentAddress" to privateInfo.permanentAddress.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (isNewProfile || privateInfo.createdAt == null) {
                    privMap["createdAt"] = FieldValue.serverTimestamp()
                    privRef.set(privMap.filterValues { it != null }, SetOptions.merge()).await()
                } else {
                    privRef.update(privMap.filterValues { it != null }.filterKeys { it != "userId" && it != "createdAt" }).await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLocationPrivacy(
        locationSharingEnabled: Boolean,
        invisibleMode: Boolean,
        discoveryRadiusKm: Double,
        approxLat: Double?,
        approxLng: Double?,
        cityLabel: String?
    ): Result<Unit> {
        var docPath = "public_profiles/${auth.currentUser?.uid ?: "unknown"}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("public_profiles").document(uid)
            docPath = ref.path
            val effectiveSharing = if (invisibleMode) false else locationSharingEnabled
            val updates = mutableMapOf<String, Any?>(
                "locationSharingEnabled" to effectiveSharing,
                "invisibleMode" to invisibleMode,
                "discoveryRadiusKm" to discoveryRadiusKm.coerceIn(0.1, 200.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (approxLat != null && approxLng != null && effectiveSharing) {
                updates["approxLat"] = approxLat
                updates["approxLng"] = approxLng
            }
            if (!cityLabel.isNullOrBlank()) {
                updates["cityLabel"] = cityLabel.take(100)
            }
            ref.update(updates.filterValues { it != null }).await()
            logActivity("location_privacy", "Location sharing=$effectiveSharing, invisible=$invisibleMode, radius=${discoveryRadiusKm}km")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateChatPrivacySettings(
        hideOnlineStatus: Boolean,
        hideLastSeen: Boolean,
        readReceiptsEnabled: Boolean,
        typingIndicatorEnabled: Boolean
    ): Result<Unit> {
        var docPath = "public_profiles/${auth.currentUser?.uid ?: "unknown"}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("public_profiles").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "hideOnlineStatus" to hideOnlineStatus,
                    "hideLastSeen" to hideLastSeen,
                    "readReceiptsEnabled" to readReceiptsEnabled,
                    "typingIndicatorEnabled" to typingIndicatorEnabled,
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

    suspend fun updateAccountStatusSelf(newStatus: String): Result<Unit> {
        var docPath = "public_profiles/${auth.currentUser?.uid ?: "unknown"}"
        return try {
            val uid = requireUserId()
            require(newStatus in listOf("active", "deactivated")) { "Invalid account status" }
            val ref = db.collection("public_profiles").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "accountStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity("account_status", "User updated account status to $newStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logActivity(eventType: String, description: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unknown"}/activity_logs"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("activity_logs").document()
            docPath = ref.path
            val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
            ref.set(
                mapOf(
                    "userId" to uid,
                    "eventType" to eventType.take(60),
                    "description" to description.take(300),
                    "deviceInfo" to device.take(120),
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

    fun buildPrivacyExportJson(
        profile: PublicProfile?,
        privateInfo: PrivateUserInfo?,
        logs: List<ActivityLog>
    ): String {
        val root = JSONObject()
        root.put("exportedAt", Timestamp.now().toDate().toString())
        root.put("platform", "NearMe")
        if (profile != null) {
            root.put("publicProfile", JSONObject().apply {
                put("userId", profile.userId)
                put("fullName", profile.fullName)
                put("username", profile.username)
                put("age", profile.age)
                put("gender", profile.gender)
                put("cityLabel", profile.cityLabel)
                put("locationSharingEnabled", profile.locationSharingEnabled)
                put("invisibleMode", profile.invisibleMode)
                put("discoveryRadiusKm", profile.discoveryRadiusKm)
                put("interests", JSONArray(profile.interests))
                put("intentions", JSONArray(profile.intentions))
                put("verificationStatus", profile.verificationStatus)
            })
        }
        if (privateInfo != null) {
            root.put("privateInfo", JSONObject().apply {
                put("email", privateInfo.email)
                put("mobileNumber", privateInfo.mobileNumber)
                put("dateOfBirth", privateInfo.dateOfBirth)
                put("permanentAddress", privateInfo.permanentAddress)
            })
        }
        val logsArray = JSONArray()
        logs.take(50).forEach { log ->
            logsArray.put(JSONObject().apply {
                put("eventType", log.eventType)
                put("description", log.description)
                put("deviceInfo", log.deviceInfo)
                put("timestamp", (log.createdAt ?: Timestamp.now()).toDate().toString())
            })
        }
        root.put("activityLogs", logsArray)
        return root.toString(2)
    }
}
