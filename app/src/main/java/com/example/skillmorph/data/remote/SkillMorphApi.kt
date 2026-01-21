package com.example.skillmorph.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// 1. The Request Body (Matches Python 'ChatRequest')
data class ChatRequest(
    val message: String,
    val is_voice_mode: Boolean = false,
    val user_id: String = "test_user_123",
    val session_id: String
)

// 2. The Response Body (Matches Python output)
data class ChatResponse(
    val response: String, // The text to show
    val mode: String      // "text" or "voice"
)

data class GoalDto(
    val id: String,
    val title: String,
    val category: String,
    @SerializedName("progress") val progress: Int, // Maps to g.progress_percentage
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("projected_end_date") val endDate: String?
)

// The Root Response
data class MetroMapDto(
    val title: String,
    val category: String,
    val days: List<DayNodeDto>
)

// The Day Item
data class DayNodeDto(
    @SerializedName("day_number") val dayNumber: Int,
    val topic: String,
    @SerializedName("is_locked") val isLocked: Boolean,
    @SerializedName("is_completed") val isCompleted: Boolean
)

data class ProgressResponse(
    val status: String,
    val new_progress: Int
)

data class TaskDto(
    // This is now the UUID (e.g., "550e8400-e29b-41d4-a716-446655440000")
    val id: String? = "",

    // 🟢 NEW: The specific day index (e.g., 5) needed for the API call
    @SerializedName("day_number")
    val dayNumber: Int? = null,

    val title: String? = "Untitled",
    val type: String? = "SIDE_QUEST",

    @SerializedName("goal_title") val goalTitle: String? = null,
    @SerializedName("goal_id") val goalId: String? = null,
    @SerializedName("is_completed") val isCompleted: Boolean? = false
)

// 3. The Interface
interface SkillMorphApi {
    @POST("/agent/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("/goals")
    suspend fun getGoals(@Query("user_id") userId: String = "test_user_123"): List<GoalDto>

    // We will use this in the next step for the Metro Map
    @GET("/goals/{goal_id}/roadmap")
    suspend fun getRoadmap(@Path("goal_id") goalId: String): MetroMapDto

    @POST("/goals/{goal_id}/days/{day_number}/complete")
    suspend fun completeGoalTask(
        @Path("goal_id") goalId: String,
        @Path("day_number") dayNumber: Int
    ): ProgressResponse

    @GET("/tasks/today")
    suspend fun getTasks(@Query("date") date: String): List<TaskDto>

    @POST("/tasks")
    suspend fun createSideQuest(@Body task: Map<String, String>): Any

    @POST("/tasks/{id}/complete")
    suspend fun completeSideQuest(@Path("id") taskId: String): Any
}