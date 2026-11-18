package com.example.data.repository

import android.util.Log
import com.example.data.model.AiRiskAnalysis
import com.example.data.model.CompatibilityResult
import com.example.data.model.PublicProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "NearMeAiRepo"

class AiRepository {

    private val bioModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "bios" to Schema.array(
                            items = Schema.string(description = "A concise, warm social profile bio (under 240 chars)"),
                            description = "Three distinct bio options"
                        )
                    )
                )
            }
        )
    }

    private val compatibilityModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "interestScore" to Schema.integer(description = "0-100 interest match"),
                        "lifestyleScore" to Schema.integer(description = "0-100 lifestyle & intentions match"),
                        "educationScore" to Schema.integer(description = "0-100 education & career complementarity"),
                        "overallScore" to Schema.integer(description = "0-100 overall compatibility"),
                        "summary" to Schema.string(description = "2-sentence explanation based only on shared interests, profession, and intentions"),
                        "icebreaker" to Schema.string(description = "1 friendly conversation starter referencing a shared interest")
                    )
                )
            }
        )
    }

    private val safetyAssistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "riskLevel" to Schema.enumeration(
                            values = listOf("LOW", "MEDIUM", "HIGH"),
                            description = "Advisory safety or spam risk signal"
                        ),
                        "summary" to Schema.string(description = "Explainable human-readable moderation assessment"),
                        "recommendedAction" to Schema.string(description = "Advisory suggestion for human moderator review")
                    )
                )
            }
        )
    }

    private val conversationHelperModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateProfileBios(
        fullName: String,
        profession: String,
        education: String,
        interests: List<String>,
        intentions: List<String>,
        city: String
    ): Result<List<String>> {
        return try {
            val prompt = buildString {
                appendLine("Write 3 distinct, friendly, authentic social bio options for a NearMe user profile.")
                appendLine("Each bio must be under 220 characters, positive, and natural.")
                appendLine("Name: $fullName")
                if (profession.isNotBlank()) appendLine("Profession: $profession")
                if (education.isNotBlank()) appendLine("Education: $education")
                if (city.isNotBlank()) appendLine("City: $city")
                if (interests.isNotEmpty()) appendLine("Interests: ${interests.joinToString(", ")}")
                if (intentions.isNotEmpty()) appendLine("Looking for: ${intentions.joinToString(", ")}")
            }
            val response = withTimeout(60_000L) {
                bioModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(jsonText)
            val arr = json.getJSONArray("bios")
            val bios = (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
            if (bios.isEmpty()) throw IllegalStateException("No bio suggestions returned.")
            Result.success(bios)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Bio generation timed out", e)
            Result.failure(IllegalStateException("AI Bio generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Bio generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun computeCompatibility(
        me: PublicProfile,
        other: PublicProfile
    ): Result<CompatibilityResult> {
        return try {
            val prompt = buildString {
                appendLine("Evaluate non-discriminatory social compatibility between two consenting adults using ONLY their permitted public interests, hobbies, profession, education, and relationship intentions.")
                appendLine("Do NOT use gender, ethnicity, or protected attributes.")
                appendLine("User A (${me.fullName}): interests=${me.interests}, hobbies=${me.hobbies}, education=${me.education}, profession=${me.profession}, intentions=${me.intentions}")
                appendLine("User B (${other.fullName}): interests=${other.interests}, hobbies=${other.hobbies}, education=${other.education}, profession=${other.profession}, intentions=${other.intentions}")
            }
            val response = withTimeout(60_000L) {
                compatibilityModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty AI compatibility response.")
            val json = JSONObject(jsonText)
            Result.success(
                CompatibilityResult(
                    targetUserId = other.userId,
                    interestScore = json.optInt("interestScore", 75).coerceIn(0, 100),
                    lifestyleScore = json.optInt("lifestyleScore", 75).coerceIn(0, 100),
                    educationScore = json.optInt("educationScore", 75).coerceIn(0, 100),
                    overallScore = json.optInt("overallScore", 75).coerceIn(0, 100),
                    summary = json.optString("summary", "Shared social interests and compatible intentions."),
                    icebreaker = json.optString("icebreaker", "Hey ${other.fullName.substringBefore(" ")}, what's been your favorite spot nearby lately?")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI compatibility timed out", e)
            Result.failure(IllegalStateException("Compatibility analysis timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI compatibility failed", e)
            Result.failure(e)
        }
    }

    suspend fun suggestConversationStarter(
        myName: String,
        targetName: String,
        draftMessage: String
    ): Result<String> {
        return try {
            val prompt = if (draftMessage.isBlank()) {
                "Give 1 warm, respectful, natural first message (1-2 sentences) from $myName to $targetName on NearMe."
            } else {
                "Review this draft message for tone, warmth, and safety, and provide 1 polished respectful version (1-2 sentences): \"$draftMessage\""
            }
            val response = withTimeout(45_000L) {
                conversationHelperModel.generateContent(prompt)
            }
            val text = response.text?.trim()
                ?: throw IllegalStateException("Empty AI safety assistant response.")
            Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI conversation helper timed out", e)
            Result.failure(IllegalStateException("AI assistant timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI conversation helper failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeReportRisk(
        targetType: String,
        reason: String,
        contextSnippet: String
    ): Result<AiRiskAnalysis> {
        return try {
            val prompt = buildString {
                appendLine("You are an explainable moderation risk assistant for NearMe. Provide an advisory risk assessment for human moderator review (never auto-ban).")
                appendLine("Target Type: $targetType")
                appendLine("Report Reason: $reason")
                if (contextSnippet.isNotBlank()) appendLine("Context: $contextSnippet")
            }
            val response = withTimeout(45_000L) {
                safetyAssistantModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty moderation AI response.")
            val json = JSONObject(jsonText)
            Result.success(
                AiRiskAnalysis(
                    riskLevel = json.optString("riskLevel", "MEDIUM"),
                    summary = json.optString("summary", "Requires human moderator verification."),
                    recommendedAction = json.optString("recommendedAction", "Review target history before taking action.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI moderation analysis timed out", e)
            Result.failure(IllegalStateException("AI moderation analysis timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI moderation analysis failed", e)
            Result.failure(e)
        }
    }
}
