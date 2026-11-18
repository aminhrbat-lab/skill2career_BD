package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRoleRecord
import com.example.data.model.ModerationReport
import com.example.data.model.PlatformSettings
import com.example.data.model.PublicProfile
import com.example.data.model.SocialEvent
import com.example.data.model.VerificationRequest
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class AdminRepository(
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
            ?: throw IllegalStateException("Admin must be signed in.")
    }

    fun observePlatformSettings(): Flow<PlatformSettings> =
        db.collection("platform_settings").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(
                        PlatformSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: PlatformSettings()
                } else PlatformSettings()
            }

    fun observeMyAdminRole(userId: String): Flow<AdminRoleRecord?> =
        db.collection("admins").document(userId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AdminRoleRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observeAllUsersForAdmin(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .snapshots()
            .map { snap ->
                snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeAllReports(): Flow<List<ModerationReport>> =
        db.collection("reports")
            .snapshots()
            .map { snap ->
                snap.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeAllVerifications(): Flow<List<VerificationRequest>> =
        db.collection("verifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(VerificationRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeAllEventsForAdmin(): Flow<List<SocialEvent>> =
        db.collection("events")
            .snapshots()
            .map { snap ->
                snap.toObjects(SocialEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeAuditLogs(): Flow<List<AdminAuditLog>> =
        db.collection("admin_audit_logs")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun savePlatformSettings(settings: PlatformSettings, reason: String): Result<Unit> {
        val docPath = "platform_settings/global"
        return try {
            requireUserId()
            db.collection("platform_settings").document("global").set(
                mapOf(
                    "registrationEnabled" to settings.registrationEnabled,
                    "gpsDiscoveryEnabled" to settings.gpsDiscoveryEnabled,
                    "chatroomsEnabled" to settings.chatroomsEnabled,
                    "photoUploadLimit" to settings.photoUploadLimit.coerceIn(1, 20),
                    "dailyConnectionLimit" to settings.dailyConnectionLimit.coerceIn(1, 500),
                    "premiumEnabled" to false, // Disabled in initial release per spec
                    "adsEnabled" to settings.adsEnabled,
                    "minRegistrationAge" to settings.minRegistrationAge.coerceIn(18, 30),
                    "announcement" to settings.announcement.take(300),
                    "maintenanceMode" to settings.maintenanceMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = "UPDATE_PLATFORM_SETTINGS",
                targetId = "global",
                previousState = "previous",
                newState = "maintenance=${settings.maintenanceMode}, reg=${settings.registrationEnabled}, limit=${settings.dailyConnectionLimit}",
                reason = reason.ifBlank { "Admin configuration update" }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserAccountStatus(
        targetUser: PublicProfile,
        newStatus: String,
        reason: String
    ): Result<Unit> {
        val docPath = "public_profiles/${targetUser.userId}"
        return try {
            requireUserId()
            require(newStatus in listOf("active", "suspended", "banned")) { "Invalid admin status" }
            db.collection("public_profiles").document(targetUser.userId).update(
                mapOf(
                    "accountStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = "USER_STATUS_$newStatus",
                targetId = targetUser.userId,
                previousState = targetUser.accountStatus,
                newState = newStatus,
                reason = reason.ifBlank { "Admin review" }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun awardUserBadge(targetUser: PublicProfile, badge: String): Result<Unit> {
        val docPath = "public_profiles/${targetUser.userId}"
        return try {
            requireUserId()
            val updated = (targetUser.badges + badge).distinct().take(10)
            db.collection("public_profiles").document(targetUser.userId).update(
                mapOf(
                    "badges" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = "AWARD_BADGE",
                targetId = targetUser.userId,
                previousState = targetUser.badges.joinToString(","),
                newState = updated.joinToString(","),
                reason = "Awarded $badge badge"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reviewVerification(
        request: VerificationRequest,
        approved: Boolean,
        reviewNote: String
    ): Result<Unit> {
        val docPath = "verifications/${request.id}"
        return try {
            val uid = requireUserId()
            val newStatus = if (approved) "verified" else "rejected"
            db.collection("verifications").document(request.id).update(
                mapOf(
                    "status" to newStatus,
                    "reviewerId" to uid,
                    "reviewNote" to reviewNote.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("public_profiles").document(request.userId).update(
                mapOf(
                    "verificationStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = "VERIFICATION_${newStatus.uppercase()}",
                targetId = request.userId,
                previousState = request.status,
                newState = newStatus,
                reason = reviewNote.ifBlank { "Officer verification decision" }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun resolveReport(
        report: ModerationReport,
        newStatus: String,
        aiSummary: String,
        reason: String
    ): Result<Unit> {
        val docPath = "reports/${report.id}"
        return try {
            val uid = requireUserId()
            db.collection("reports").document(report.id).update(
                mapOf(
                    "status" to newStatus,
                    "aiRiskSummary" to aiSummary.take(600),
                    "reviewedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = "MODERATION_$newStatus",
                targetId = report.targetId,
                previousState = report.status,
                newState = newStatus,
                reason = reason.ifBlank { "Human moderator decision" }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyEvent(event: SocialEvent, verified: Boolean): Result<Unit> {
        val docPath = "events/${event.id}"
        return try {
            requireUserId()
            db.collection("events").document(event.id).update(
                mapOf(
                    "verified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = if (verified) "VERIFY_EVENT" else "UNVERIFY_EVENT",
                targetId = event.id,
                previousState = event.verified.toString(),
                newState = verified.toString(),
                reason = "Event map visibility review"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun writeAuditLog(
        action: String,
        targetId: String,
        previousState: String,
        newState: String,
        reason: String
    ) {
        val uid = requireUserId()
        db.collection("admin_audit_logs").document().set(
            mapOf(
                "adminId" to uid,
                "action" to action.take(120),
                "targetId" to targetId.take(128),
                "previousState" to previousState.take(200),
                "newState" to newState.take(200),
                "reason" to reason.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun generateBackupExportJson(
        users: List<PublicProfile>,
        reports: List<ModerationReport>,
        logs: List<AdminAuditLog>,
        settings: PlatformSettings
    ): String {
        val root = JSONObject()
        root.put("backupTimestamp", Timestamp.now().toDate().toString())
        root.put("note", "Manual NearMe database export (distinct from automated Cloud Firestore scheduled backups)")
        root.put("totalProfiles", users.size)
        root.put("totalReports", reports.size)
        root.put("settings", JSONObject().apply {
            put("registrationEnabled", settings.registrationEnabled)
            put("gpsDiscoveryEnabled", settings.gpsDiscoveryEnabled)
            put("chatroomsEnabled", settings.chatroomsEnabled)
            put("dailyConnectionLimit", settings.dailyConnectionLimit)
            put("minRegistrationAge", settings.minRegistrationAge)
            put("maintenanceMode", settings.maintenanceMode)
            put("premiumEnabled", settings.premiumEnabled)
        })
        val usersJson = JSONArray()
        users.forEach { u ->
            usersJson.put(JSONObject().apply {
                put("userId", u.userId)
                put("username", u.username)
                put("gender", u.gender)
                put("verificationStatus", u.verificationStatus)
                put("accountStatus", u.accountStatus)
                put("cityLabel", u.cityLabel)
            })
        }
        root.put("profilesSummary", usersJson)
        val logsJson = JSONArray()
        logs.take(50).forEach { l ->
            logsJson.put(JSONObject().apply {
                put("action", l.action)
                put("targetId", l.targetId)
                put("reason", l.reason)
            })
        }
        root.put("auditLogs", logsJson)
        return root.toString(2)
    }
}
