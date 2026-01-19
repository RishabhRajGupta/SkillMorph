package com.example.skillmorph.presentation.main.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.remote.ChatRequest
import com.example.skillmorph.data.remote.SkillMorphApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import org.json.JSONObject
import java.util.UUID

// Data class for chat messages
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isThinking: Boolean = false // New flag for the wavy animation
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val api: SkillMorphApi
) : ViewModel() {

    // Chat History
    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Hello! I am Skill Morph. Ready to level up?", isUser = false)
    ))
    val messages = _messages.asStateFlow()

    // Status Flags
    private val _isAgentThinking = MutableStateFlow(false)
    val isAgentThinking = _isAgentThinking.asStateFlow()

    // Trigger for TTS (The UI will observe this)
    private val _ttsText = MutableStateFlow<String?>(null)
    val ttsText = _ttsText.asStateFlow()

    fun sendMessage(text: String, isVoice: Boolean) {
        // 1. Add User Message
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text, isUser = true))
        _messages.value = currentList

        // Creating session of one chat
        val sessionId = UUID.randomUUID().toString()
        // 2. Set "Thinking"
        _isAgentThinking.value = true

        viewModelScope.launch {
            try {
                // 3. Call API
                val apiResult = api.chat(
                    ChatRequest(
                        message = text,
                        is_voice_mode = isVoice,
                        session_id = sessionId
                    )
                )

                // 4. PARSE THE RESPONSE (The Fix)
                var displayMessage = apiResult.response
                var spokenMessage = apiResult.response

                // We try to parse it as JSON. If it fails, we assume it's just normal text.
                try {
                    // Clean up any potential markdown code blocks like ```json ... ```
                    val cleanJson = apiResult.response
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    if (cleanJson.startsWith("{")) {
                        val jsonObject = JSONObject(cleanJson)
                        if (jsonObject.has("spoken_text")) {
                            spokenMessage = jsonObject.getString("spoken_text")
                        }
                        if (jsonObject.has("display_text")) {
                            displayMessage = jsonObject.getString("display_text")
                        }
                    }
                } catch (e: Exception) {
                    // It wasn't JSON, or parsing failed.
                    // We just stick with the raw text for both.
                    Log.d("AgentVM", "Response was not JSON, using raw text.")
                }

                // 5. Update UI (Stop thinking, show text)
                _isAgentThinking.value = false

                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage(displayMessage, isUser = false))
                _messages.value = updatedList

                // 6. Trigger TTS (Only speak the "spoken" part)
                if (isVoice || apiResult.mode == "voice") {
                    _ttsText.value = spokenMessage
                }

            } catch (e: Exception) {
                _isAgentThinking.value = false
                Log.e("AgentVM", "Error: ${e.message}")
                val errorList = _messages.value.toMutableList()
                errorList.add(ChatMessage("⚠️ Connection Error: ${e.message}", isUser = false))
                _messages.value = errorList
            }
        }
    }

    // Call this after speaking to reset the trigger
    fun onTtsFinished() {
        _ttsText.value = null
    }
}