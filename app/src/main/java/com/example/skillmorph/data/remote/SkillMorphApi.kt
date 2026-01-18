package com.example.skillmorph.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

// 1. The Request Body (Matches Python 'ChatRequest')
data class ChatRequest(
    val message: String,
    val is_voice_mode: Boolean = false,
    val user_id: String = "test_user_123"
)

// 2. The Response Body (Matches Python output)
data class ChatResponse(
    val response: String, // The text to show
    val mode: String      // "text" or "voice"
)

// 3. The Interface
interface SkillMorphApi {
    @POST("/agent/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}