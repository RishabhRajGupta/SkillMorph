package com.example.skillmorph.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.domain.repository.ChatRepository
import com.example.skillmorph.utils.FileProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    init {
        // Start listening to the database immediately
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            repository.getAllMessages().collect { chatList ->
                _messages.value = chatList
            }
        }
    }

    // 2. The Input Action (Voice or Text)
    fun sendMessage(text: String, isUser: Boolean) {
        viewModelScope.launch {
            // 1. Always save the user's message to the Chat table
            val newMessage = ChatEntity(
                message = text,
                isUser = isUser,
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(newMessage)

            // 2. ROUTING logic
            if (isUser && text.startsWith("Search:", ignoreCase = true)) {
                // If message starts with "Search:", trigger the Knowledge Shelf
                val query = text.removePrefix("Search:").trim()
                if (query.isNotEmpty()) {
                    performKnowledgeSearch(query)
                }
            } else if (isUser) {
                // Normal chat logic (where the API guy will plug in Gemini/OpenAI)
                triggerAiResponse(text)
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

    // --- THE FINDER logic ---
    private fun performKnowledgeSearch(query: String) {
        viewModelScope.launch {
            // We call the DAO query we wrote in Phase 1
            repository.searchKnowledge(query).collect { results ->
                if (results.isNotEmpty()) {
                    // Pull the best match (first result)
                    val bestMatch = results.first()

                    val response = "Found in ${bestMatch.source}:\n\n\"${bestMatch.content}\""

                    // Save the "System" response to the chat history
                    sendMessage(response, isUser = false)
                } else {
                    sendMessage("No match found in the Knowledge Shelf for '$query'.", isUser = false)
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