package com.example.skillmorph

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch


@Composable
fun Agent() {
    var isVoiceMode by remember { mutableStateOf(true) }

    // Use a Box to layer everything
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Or your main app background gradient
    ) {
        // --- LAYER 1: CONTENT ---
        // Crossfade makes the switch smooth instead of instant
        androidx.compose.animation.Crossfade(targetState = isVoiceMode, label = "mode") { voice ->
            if (voice) {
                // Your existing Particle Voice Screen
                AgentRing()
            } else {
                // The new Chat Screen
                AgentChat()
            }
        }

        // --- LAYER 2: TOGGLE CHIP ---
        // This floats on top of everything
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd) // Placing it at bottom-right
                .padding(end = 24.dp) // Adjust padding to sit above Send button/Mic
        ) {
            // Assuming you have the GlassInputModeChip from previous steps
            // If using the snippet you pasted, ensure InputModeChip is defined
            GlassInputModeChip(
                isVoiceMode = isVoiceMode,
                onToggle = { isVoiceMode = !isVoiceMode }
            )
        }
    }
}

@Composable
fun GlassInputModeChip(
    isVoiceMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- GLASSY COLORS ---
    val neonCyan = Color(0xFF00E5FF)

    // 1. The Glass Gradient (Top is slightly lighter to simulate reflection)
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f), // Top: lighter
            Color.White.copy(alpha = 0.05f)  // Bottom: darker/transparent
        )
    )

    // 2. The Border Gradient (Shiny top rim, fading bottom)
    val borderGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.3f),
            Color.Transparent
        )
    )

    // Dimensions
    val chipHeight = 54.dp
    val indicatorWidth = 100.dp
    val totalWidth = 200.dp

    // Animation State
    val indicatorOffset by animateDpAsState(
        targetValue = if (isVoiceMode) 0.dp else indicatorWidth,
        animationSpec = tween(300),
        label = "offset"
    )

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(chipHeight)
            .clip(CircleShape)
            // Apply the Glass Gradient Background
            .background(glassGradient)
            // Add the Shiny Border
            .border(1.dp, borderGradient, CircleShape)
            .clickable { onToggle() }
    ) {
        // --- ACTIVE INDICATOR (Cyan Pill) ---
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(CircleShape)
                .background(neonCyan.copy(alpha = 0.8f)) // Slightly see-through cyan
            // Add a subtle glow/blur to the indicator if desired
        )

        // --- CONTENT LAYER ---
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipOption(
                text = "Voice",
                icon = Icons.Rounded.Mic,
                isSelected = isVoiceMode,
                modifier = Modifier.width(indicatorWidth)
            )

            ChipOption(
                text = "Type",
                icon = Icons.Rounded.Keyboard,
                isSelected = !isVoiceMode,
                modifier = Modifier.width(indicatorWidth)
            )
        }
    }
}

// PASTE THIS AT THE BOTTOM OF YOUR InputModeChip.kt FILE, OUTSIDE ANY CLASS
// REMOVE 'private' if you want to use it in other files,
// but usually it's best to keep it in the same file as GlassInputModeChip.

@Composable
fun ChipOption(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    // Text Color Animation: Black on Cyan, White on Glass
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "color"
    )

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
// Data class stays the same
data class RingParticle(
    var angle: Float,
    var baseDistance: Float,
    var size: Float,
    var speed: Float
)

@Composable
fun AgentRing() {
    val context = LocalContext.current

    // --- STATES ---
    var isAgentSpeaking by remember { mutableStateOf(false) }
    var isUserSpeaking by remember { mutableStateOf(false) } // Are we recording?
    var visualAmplitude by remember { mutableStateOf(0f) }

    val particles = remember { mutableStateListOf<RingParticle>() }

    // --- 1. TTS SETUP ---
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) Log.e("TTS", "Init failed")
        }.apply {
            language = Locale.US
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { isAgentSpeaking = true }
                override fun onDone(id: String?) {
                    isAgentSpeaking = false
                    visualAmplitude = 0f
                }
                override fun onError(id: String?) { isAgentSpeaking = false }
            })
        }
    }

    // --- 2. SPEECH RECOGNIZER SETUP ---
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    // Function to handle the result
    val onSpeechResult = { text: String ->
        isUserSpeaking = false // Stop listening visual

        // HARDCODED LOGIC: Check for "Hello"
        if (text.contains("hello", ignoreCase = true)) {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "introID")
            tts.speak(
                "Hello there! I am Skill Morph, your personal AI assistant. How can I help you level up today?",
                TextToSpeech.QUEUE_FLUSH,
                params,
                "introID"
            )
        } else {
            Toast.makeText(context, "You said: $text (Try saying 'Hello')", Toast.LENGTH_SHORT).show()
        }
    }

    // Set up the listener callbacks
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                // Optional: You could make particles react to YOUR voice here using 'rmsdB'
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isUserSpeaking = false }
            override fun onError(error: Int) {
                isUserSpeaking = false
                Log.e("Speech", "Error: $error")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onSpeechResult(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
            tts.shutdown()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isUserSpeaking = true
            speechRecognizer.startListening(speechIntent)
        } else {
            Toast.makeText(context, "Microphone permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    // --- INITIALIZATION: FORM THE RING (Same as before) ---
    LaunchedEffect(Unit) {
        if (particles.isEmpty()) {
            repeat(300) {
                particles.add(
                    RingParticle(
                        angle = Random.nextFloat() * 360,
                        baseDistance = 300f + Random.nextFloat() * 70f,
                        size = Random.nextFloat() * 6 + 3,
                        speed = Random.nextFloat() * 0.3f + 0.2f
                    )
                )
            }
        }
    }

    // --- ANIMATION LOOP ---
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                // AMPLITUDE LOGIC
                if (isAgentSpeaking) {
                    // Agent Speaking: Big Waves
                    val wave = sin(time / 90000000.0).toFloat()
                    visualAmplitude = (Math.abs(wave) * 0.6f) + 0.1f
                } else if (isUserSpeaking) {
                    // User Speaking (Listening Mode): Fast nervous vibration
                    visualAmplitude = 0.3f + Random.nextFloat() * 0.1f
                } else {
                    // Idle: Slow breathing
                    val breath = sin(time / 500000000.0).toFloat()
                    visualAmplitude = (breath * 0.1f)
                }

                // Update Particles
                particles.forEach { p -> p.angle += p.speed }
            }
        }
    }

    // --- UI LAYOUT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {


        // 1. The Particle Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            particles.forEach { p ->
                val expansionFactor = if (isAgentSpeaking) (1f + visualAmplitude * 0.5f) else 1f
                val currentDistance = p.baseDistance * expansionFactor

                // Jitter adds the "electric" look
                val jitter = if (isAgentSpeaking || isUserSpeaking) Random.nextFloat() * 10f else 0f

                val rad = Math.toRadians(p.angle.toDouble())
                val x = center.x + (cos(rad) * currentDistance).toFloat() + jitter
                val y = center.y + (sin(rad) * currentDistance).toFloat() + jitter

                // COLOR LOGIC
                val particleColor = when {
                    isAgentSpeaking -> Color(0xFF00FFFF).copy(alpha = 0.8f) // Cyan (Agent Talking)
                    isUserSpeaking -> Color(0xFF00FF00).copy(alpha = 0.8f)  // Green (Listening to You)
                    else -> Color(0xFF0088AA).copy(alpha = 0.4f)            // Dim Blue (Idle)
                }

                drawCircle(
                    color = particleColor,
                    radius = p.size,
                    center = Offset(x, y)
                )
            }
        }

        // 2. The Mic Button (Bottom Center)
        FloatingActionButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    isUserSpeaking = true
                    speechRecognizer.startListening(speechIntent)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            containerColor = if (isUserSpeaking) Color.Green else Color(0xFF00E5FF),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .size(70.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isUserSpeaking) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = "Mic",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}


data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun AgentChat() {
    // 1. Dummy Chat History
    val messages = remember { mutableStateListOf(
        ChatMessage("Hello! I am Skill Morph.", isUser = false),
        ChatMessage("How can I help you today?", isUser = false)
    )}

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Transparent so app background shows
            .padding(top = 80.dp), // Leave space for TopBar if you have one
        verticalArrangement = Arrangement.Bottom
    ) {
        // --- CHAT LIST ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- INPUT AREA ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Extra padding for Bottom Nav/Chip
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        messages.add(ChatMessage(inputText, isUser = true))
                        inputText = ""
                        // Scroll to bottom
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF00E5FF), CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.5f)
    val align = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isUser) RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(align)
                .clip(shape)
                .background(bubbleColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = message.text, color = Color.White, fontSize = 16.sp)
        }
    }
}
