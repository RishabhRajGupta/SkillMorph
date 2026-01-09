package com.example.skillmorph.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.domain.repository.ChatRepository
import com.example.skillmorph.utils.FileProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.skillmorph.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.skillmorph.data.local.entities.GoalEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    // 1. The Real-Time Chat List
    // We observe the database. Whenever a message is saved, this list updates automatically.
    // We default to an empty list until data loads.
    private val _messages = MutableStateFlow<List<ChatEntity>>(emptyList())
    val messages = _messages.asStateFlow()

    // 2. NEW: The Metro Line State (Today's Tasks)
    private val _metroStations = MutableStateFlow<List<com.example.skillmorph.data.local.entities.TaskEntity>>(emptyList())
    val metroStations = _metroStations.asStateFlow()

    // 2. NEW: The Goal State (For Streak and Dashboard)
    private val _goals = MutableStateFlow<List<com.example.skillmorph.data.local.entities.GoalEntity>>(emptyList())
    val goals = _goals.asStateFlow()

    init {
        // Start listening to the database immediately
        loadMessages()
        loadMetroLine() // Start listening to the Metro Track immediately
        loadGoals()
        ensureDefaultGoalExists() // NEW: Ensure the Metro Track has a home
    }

    private fun loadGoals() {
        viewModelScope.launch {
            repository.getAllGoals().collect { goalList ->
                _goals.value = goalList
            }
        }
    }

    private fun ensureDefaultGoalExists() {
        viewModelScope.launch {
            // Check if there are any goals
            val existingGoals = repository.getAllGoals().firstOrNull()
            if (existingGoals.isNullOrEmpty()) {
                val defaultGoal = com.example.skillmorph.data.local.entities.GoalEntity(
                    id = 1L, // Our Plan: command uses goalId 1L
                    title = "My Learning Journey",
                    description = "The main track for all my study stations.",
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
                    totalTasks = 0,
                    completedTasks = 0,
                    isImportant = true,
                    progressPercentage = 0
                )
                repository.saveGoal(defaultGoal)
            }
        }
    }

    private fun loadMetroLine() {
        viewModelScope.launch {
            repository.getAllTasks().collect { taskList ->
                _metroStations.value = taskList
            }
        }
    }

    // 3. NEW: The "Task Toggle" Action
    fun toggleStation(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTask(taskId, isCompleted)
            // The UI will update automatically because of the Flow in loadMetroLine()
            if (isCompleted) {
                updateStreakLogic()
            }
        }
    }
    // 3. Fixed updateStreakLogic (Using .first() to get Flow data)
    private suspend fun updateStreakLogic() {
        val goalsList = repository.getAllGoals().firstOrNull()
        val targetGoal = goalsList?.find { it.id == 1L }

        targetGoal?.let {
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            val isNextDay = (now - it.lastCompletionTimestamp) >= oneDay

            val updatedGoal = it.copy(
                lastCompletionTimestamp = now,
                currentStreak = if (isNextDay || it.currentStreak == 0) it.currentStreak + 1 else it.currentStreak
            )
            repository.saveGoal(updatedGoal)
        }
    }
    private fun loadMessages() {
        viewModelScope.launch {
            repository.getAllMessages().collect { chatList ->
                _messages.value = chatList
            }
        }
    }

    // 4. The Input Action (Voice or Text)
    // --- UPDATED MESSAGE ROUTER ---
    fun sendMessage(text: String, isUser: Boolean) {
        viewModelScope.launch {
            // 1. Save message to Local Chat History
            val newMessage = ChatEntity(
                message = text,
                isUser = isUser,
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(newMessage)

            if (isUser) {
                val trimmedText = text.trim()

                when {
                    // Path A: Manual Force Search
                    trimmedText.startsWith("Search:", ignoreCase = true) -> {
                        val query = trimmedText.removePrefix("Search:").trim()
                        if (query.isNotEmpty()) {
                            performKnowledgeSearch(query, isManual = true)
                        }
                    }

                    // Path B: Automatic Intelligence (Silent)
                    // We only search if the query is meaningful (> 10 characters)
                    trimmedText.length > 10 -> {
                        performKnowledgeSearch(trimmedText, isManual = false)
                    }

                    // PATH D: Manual Planning Command
                    trimmedText.startsWith("Plan:", ignoreCase = true) -> {
                        val taskName = trimmedText.removePrefix("Plan:").trim()
                        if (taskName.isNotEmpty()) {
                            createNewMetroStation(taskName)
                        }
                    }

                    // Path C: General Chat Fallback
                    else -> {
                        // triggerAiResponse(trimmedText) // Future: API Guy connects Gemini here
                    }

                }
            }
        }

    }

    private fun createNewMetroStation(name: String) {
        viewModelScope.launch {
            try {
                // ARCHITECT FIX: Use the full path for TaskEntity to avoid ambiguity
                // and provide 'goalId = 1L' as a placeholder to satisfy the ForeignKey.
                val newTask = com.example.skillmorph.data.local.entities.TaskEntity(
                    goalId = 1L,
                    title = name,
                    scheduledDate = System.currentTimeMillis(),
                    isCompleted = false,
                    isBufferTask = false,
                    priority = 1
                )

                repository.saveTask(newTask)

                val systemMsg = ChatEntity(
                    message = "System: Added station '$name' to your Metro Line.",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveMessage(systemMsg)
            } catch (e: Exception) {
                sendMessage("System Error: Unable to create station. ${e.localizedMessage}", isUser = false)
            }
        }
    }
    // --- KNOWLEDGE INGESTION ---

    fun processFile(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            // Initialize our new helper
            val fileProcessor = FileProcessor(context, repository)

            // Start processing in the background
            val result = fileProcessor.processFile(uri)

            // Handle the result
            result.onSuccess { chunksCreated ->
                val fileName = uri.lastPathSegment ?: "File"
                sendMessage("System: Processed '$fileName' into $chunksCreated knowledge segments.", isUser = false)
            }.onFailure { error ->
                sendMessage("System Error: ${error.localizedMessage}", isUser = false)
            }
        }
    }

    // --- UPDATED SILENT SEARCH ---
    private fun performKnowledgeSearch(query: String, isManual: Boolean) {
        viewModelScope.launch {
            try {
                // ARCHITECT UPGRADE: Fetching up to 3 results to ensure no data is missed.
                // We use 'firstOrNull' but the Repository will provide the list.
                val results = repository.searchKnowledge(query).firstOrNull()

                if (!results.isNullOrEmpty()) {
                    // 1. Take top 3 relevant chunks
                    val topMatches = results.take(3)

                    // 2. Format the bundle
                    val bundledResponse = StringBuilder().apply {
                        append("--- KNOWLEDGE RETRIEVAL START ---\n")
                        topMatches.forEachIndexed { index, match ->
                            append("[Match ${index + 1} | Source: ${match.source}]\n")
                            append("${match.content}\n\n")
                        }
                        append("--- KNOWLEDGE RETRIEVAL END ---")
                    }.toString()

                    /* TODO: API DEVELOPER HANDOVER
                       Currently, we save this bundledResponse to the chat so the Architect can see it.
                       In the final version, DO NOT save this to chat.
                       Instead, send 'bundledResponse' as a 'Context' or 'System Prompt'
                       to Gemini/GPT so it can summarize the answer for the user.
                    */

                    val systemMessage = ChatEntity(
                        message = bundledResponse,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.saveMessage(systemMessage)
                } else if (isManual) {
                    val systemMessage = ChatEntity(
                        message = "System: No matches found in your library for '$query'.",
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.saveMessage(systemMessage)
                }
            } catch (e: Exception) {
                if (isManual) {
                    sendMessage("Search Error: ${e.localizedMessage}", isUser = false)
                }
            }
        }
    }
    private fun triggerAiResponse(userQuery: String) {
        /* TODO: API DEVELOPER - IMPLEMENT HERE
           1. Take 'userQuery'.
           2. Send to Gemini/OpenAI API.
           3. Get response text.
           4. Save response to DB:
              sendMessage(responseText, isUser = false)
        */
    }
}