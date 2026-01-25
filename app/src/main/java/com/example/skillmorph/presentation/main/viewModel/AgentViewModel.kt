package com.example.skillmorph.presentation.main.viewModel;

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.ChatMessage
import com.example.skillmorph.data.remote.ChatRequest
import com.example.skillmorph.data.remote.SessionResponse
import com.example.skillmorph.data.remote.SkillMorphApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.toMutableList

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val api: SkillMorphApi,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    // --- STATE ---

    // 1. Current Session ID (The "Room" for today)
    private val _currentSessionId = MutableStateFlow<String?>(null)
    private val _pastSessions = MutableStateFlow<List<SessionResponse>>(emptyList())
    val pastSessions = _pastSessions.asStateFlow()


    // 2. Chat Messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isAgentThinking = MutableStateFlow(false)
    val isAgentThinking = _isAgentThinking.asStateFlow()

    private val _ttsText = MutableStateFlow<String?>(null)
    val ttsText = _ttsText.asStateFlow()

    // --- INITIALIZATION ---
    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            try {
                // 1. Calculate Virtual Date
                val now = LocalDateTime.now()
                val cutoff = now.withHour(3).withMinute(30)
                val virtualDate = if (now.isBefore(cutoff)) {
                    now.minusDays(1).toLocalDate().toString()
                } else {
                    now.toLocalDate().toString()
                }

                Log.d("AgentVM", "Initializing for: $virtualDate")

                // 2. Try to Get Session from Backend
                try {
                    val sessionData = api.getOrCreateDailySession()
                    _currentSessionId.value = sessionData.sessionId

                    // Load History only if we succeeded
                    val history = api.getSessionHistory(sessionData.sessionId)
                    if (history.isNotEmpty()) {
                        _messages.value = history.map { ChatMessage(it.text, it.isUser) }
                    } else {
                        triggerDailyBriefing(virtualDate)
                    }

                    // Load Sidebar
                    _pastSessions.value = api.getChatSessions()

                } catch (e: Exception) {
                    Log.e("AgentVM", "Backend Handshake Failed: ${e.message}")
                    throw e // Re-throw to trigger the fallback in outer catch
                }

            } catch (e: Exception) {
                // 🔴 THE FIX: FALLBACK MODE
                // If backend fails, generate a LOCAL ID so the user isn't blocked.
                Log.e("AgentVM", "Entering Offline Fallback Mode")

                if (_currentSessionId.value == null) {
                    _currentSessionId.value = UUID.randomUUID().toString()
                }

                _messages.value = listOf(
                    ChatMessage("⚠️ Offline Mode: I couldn't sync history, but you can still try to chat.", isUser = false)
                )
            }
        }
    }


    // --- CHAT LOGIC ---

    fun sendMessage(text: String, isVoice: Boolean) {
        // 1. Get the Active Session ID (Don't generate a random UUID here!)
        // It should have been set by initializeSession()
        val sessionId = _currentSessionId.value ?: return

        // Optimistic Update (UI)
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text, isUser = true))
        _messages.value = currentList

        _isAgentThinking.value = true

        viewModelScope.launch {
            try {
                // 2. Network Call
                val apiResult = api.chat(
                    ChatRequest(
                        message = text,
                        isVoiceMode = isVoice,
                        sessionId = sessionId // Sending the ID
                    )
                )

                var displayMessage = apiResult.response
                var spokenMessage = apiResult.response

                try{
                    val cleanJson = apiResult.response
                        .replace("```json","")
                        .replace("```","")
                        .trim()

                    if(cleanJson.startsWith("{")){
                        val jsonObject = JSONObject(cleanJson)
                        if(jsonObject.has("spoken_text")){
                            spokenMessage = jsonObject.getString("spoken_text")
                        }
                        if(jsonObject.has("display_text")){
                            displayMessage = jsonObject.getString("display_text")
                        }
                    }
                } catch (e: Exception){
                    // If not Json then stick to raw text
                    Log.d("AgentVM", "Response was not JSON, using raw text.")
                }
                // 3. Handle Success
                _isAgentThinking.value = false
                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage(displayMessage, isUser = false))
                _messages.value = updatedList

                if (isVoice || apiResult.mode == "voice") {
                    _ttsText.value = spokenMessage
                }

            } catch (e: Exception) {
                // 4. Handle Failure (Keep user message, show error)
                _isAgentThinking.value = false
                Log.e("AgentVM", "Send Failed: ${e.message}")

                val errorList = _messages.value.toMutableList()
                errorList.add(ChatMessage("❌ Failed to send. Check Server.", isUser = false))
                _messages.value = errorList
            }
        }
    }

    // --- BRIEFING LOGIC (Integrated) ---

    private fun triggerDailyBriefing(todayDate: String) {
        viewModelScope.launch {
            // 1. Check Cache
            val cachedDate = sharedPrefs.getString("daily_briefing_date", "")
            val cachedText = sharedPrefs.getString("daily_briefing_text", "")

            if (cachedDate == todayDate && !cachedText.isNullOrEmpty()) {
                val currentList = _messages.value.toMutableList()
                currentList.add(ChatMessage(cachedText, isUser = false))
                _messages.value = currentList
                _ttsText.value = cachedText
                return@launch
            }

            // 2. Fetch Live
            try {
                val tasks = api.getTasks(todayDate)
                if (tasks.isEmpty()) {
                    val msg = "Good morning! No tasks for today."
                    _messages.value += ChatMessage(msg, false)
                    _ttsText.value = msg
                    return@launch
                }

                val taskListString = tasks.joinToString(", ") { "${it.title} (${it.goalTitle})" }
                val prompt = "SYSTEM: User tasks: $taskListString. Greet and summarize shortly."

                // Use the session ID we just initialized!
                val sessionId = _currentSessionId.value ?: UUID.randomUUID().toString()

                val response = api.chat(ChatRequest(prompt, false, sessionId))

                _messages.value += ChatMessage(response.response, false)
                _ttsText.value = response.response

            } catch (e: Exception) {
                Log.e("AgentVM", "Briefing failed: ${e.message}")
            }
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            // 1. Set the active session ID
            _currentSessionId.value = sessionId

            // 2. Clear current messages (Instant UI feedback)
            // This prevents seeing "Today's" messages while "Yesterday" loads
            _messages.value = emptyList()

            try {
                Log.d("AgentVM", "Loading history for session: $sessionId")

                // 3. Fetch History from Backend
                val history = api.getSessionHistory(sessionId)

                // 4. Map to UI Model and Update
                if (history.isNotEmpty()) {
                    _messages.value = history.map {
                        ChatMessage(text = it.text, isUser = it.isUser)
                    }
                } else {
                    _messages.value = listOf(ChatMessage("This conversation is empty.", isUser = false))
                }

            } catch (e: Exception) {
                Log.e("AgentVM", "Failed to load session: ${e.message}")
                _messages.value = listOf(ChatMessage("⚠️ Failed to load history. Check connection.", isUser = false))
            }
        }
    }

    fun onTtsFinished() {
        _ttsText.value = null
    }
}